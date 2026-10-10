package com.igrejalive.app.stream

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.igrejalive.app.ui.LiveStreamActivity
import com.pedro.common.ConnectChecker

/**
 * Serviço em primeiro plano que MANTÉM A LIVE NO AR mesmo que o app saia da tela
 * (Home, outro app, WhatsApp, tela bloqueada, toque acidental).
 *
 * - Dono do StreamManager (câmera + microfone + RTMP).
 * - A tela (LiveStreamActivity) apenas "mostra" o preview: anexa e desanexa a OpenGlView.
 * - Reconecta sozinho em quedas de rede (RootEncoder reTry) e ajusta o bitrate (ABR).
 * - Mantém CPU e Wi-Fi acordados durante a transmissão.
 */
class StreamService : Service(), ConnectChecker {

    companion object {
        private const val CHANNEL_ID = "liveinfe_live_channel"
        private const val NOTIFICATION_ID = 4107
        private const val RETRY_DELAY_MS = 3000L

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, StreamService::class.java)
            )
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): StreamService = this@StreamService
    }

    private val binder = LocalBinder()

    lateinit var manager: StreamManager
        private set

    /** Tela atual (se estiver aberta) que recebe os eventos de conexão. */
    @Volatile
    var listener: ConnectChecker? = null

    // Estado da sessão, para a tela se ressincronizar ao voltar
    @Volatile var sessionActive: Boolean = false
        private set
    @Volatile var connected: Boolean = false
        private set
    @Volatile var lastBitrateKbps: Long = 0
        private set
    @Volatile var startedAtElapsed: Long = 0
        private set
    @Volatile private var stopRequested: Boolean = false

    private var rtmpUrl: String = ""
    private var liveTitle: String = "Culto ao Vivo"

    // Dados da sessão guardados aqui para a tela conseguir se recuperar se for recriada
    // (ex.: o operador volta pela notificação depois de a tela ter sido fechada)
    var broadcastId: String? = null
        private set
    var accessToken: String? = null
        private set
    var watchUrl: String? = null
        private set
    var is1080p: Boolean = true
        private set

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var foregroundStarted = false

    override fun onCreate() {
        super.onCreate()
        manager = StreamManager(applicationContext, this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground("Preparando transmissão…")
        // Se o sistema recriar o serviço sem o app, não há o que retomar
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    // ---------------------------------------------------------------------------------------
    // API usada pela tela
    // ---------------------------------------------------------------------------------------

    /** Inicia o envio RTMP. A câmera/áudio já devem ter sido preparados (prepareStream). */
    fun beginStream(
        url: String,
        title: String,
        broadcastId: String?,
        accessToken: String?,
        watchUrl: String?,
        is1080p: Boolean
    ) {
        rtmpUrl = url
        liveTitle = title
        this.broadcastId = broadcastId
        this.accessToken = accessToken
        this.watchUrl = watchUrl
        this.is1080p = is1080p
        stopRequested = false
        sessionActive = true
        startedAtElapsed = SystemClock.elapsedRealtime()
        acquireLocks()
        updateNotification("Conectando ao YouTube…")
        if (url.isNotEmpty()) manager.startStream(url)
    }

    /** Reconexão manual (toque no indicador de rede). */
    fun reconnectNow() {
        if (stopRequested || rtmpUrl.isEmpty()) return
        updateNotification("Reconectando…")
        if (!manager.reTry(300, "manual")) {
            // Caso a conexão tenha sido encerrada por completo, recomeça do zero
            manager.stopStream()
            manager.startStream(rtmpUrl)
        }
    }

    /** Encerra tudo (somente quando o operador confirmar). */
    fun endSession() {
        stopRequested = true
        sessionActive = false
        connected = false
        manager.stopStream()
        releaseLocks()
        stopForeground(STOP_FOREGROUND_REMOVE)
        foregroundStarted = false
        stopSelf()
    }

    // ---------------------------------------------------------------------------------------
    // ConnectChecker (RootEncoder). Os callbacks chegam na thread principal.
    // ---------------------------------------------------------------------------------------

    override fun onConnectionStarted(url: String) {
        listener?.onConnectionStarted(url)
    }

    override fun onConnectionSuccess() {
        connected = true
        updateNotification("🔴 AO VIVO")
        listener?.onConnectionSuccess()
    }

    override fun onConnectionFailed(reason: String) {
        connected = false
        if (stopRequested) return
        // Tenta de novo sem derrubar câmera/encoders. Só falha de verdade se o endereço for inválido.
        val willRetry = manager.reTry(RETRY_DELAY_MS, reason)
        updateNotification(if (willRetry) "⚠️ Sem rede — reconectando…" else "❌ Falha: $reason")
        listener?.onConnectionFailed(if (willRetry) "RETRY" else reason)
    }

    override fun onNewBitrate(bitrate: Long) {
        lastBitrateKbps = bitrate / 1000
        manager.adaptBitrate(bitrate)
        listener?.onNewBitrate(bitrate)
    }

    override fun onDisconnect() {
        connected = false
        listener?.onDisconnect()
    }

    override fun onAuthError() {
        listener?.onAuthError()
    }

    override fun onAuthSuccess() {
        listener?.onAuthSuccess()
    }

    // ---------------------------------------------------------------------------------------
    // Notificação, travas e ciclo de vida
    // ---------------------------------------------------------------------------------------

    private fun startAsForeground(text: String) {
        if (foregroundStarted) return
        val notification = buildNotification(text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        foregroundStarted = true
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Transmissão ao vivo",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mostra que a live está no ar, mesmo com o app fora da tela"
                setShowBadge(false)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = Intent(this, LiveStreamActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentTitle("LiveinFé • $liveTitle")
            .setContentText("$text  (toque para voltar à transmissão)")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        try {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(NOTIFICATION_ID, buildNotification(text))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun acquireLocks() {
        try {
            if (wakeLock?.isHeld != true) {
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "liveinfe:stream").apply {
                    setReferenceCounted(false)
                    acquire()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            if (wifiLock?.isHeld != true) {
                val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                } else {
                    @Suppress("DEPRECATION")
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF
                }
                wifiLock = wm.createWifiLock(mode, "liveinfe:wifi").apply {
                    setReferenceCounted(false)
                    acquire()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseLocks() {
        try { if (wakeLock?.isHeld == true) wakeLock?.release() } catch (_: Exception) {}
        try { if (wifiLock?.isHeld == true) wifiLock?.release() } catch (_: Exception) {}
        wakeLock = null
        wifiLock = null
    }

    override fun onDestroy() {
        stopRequested = true
        sessionActive = false
        releaseLocks()
        try { manager.release() } catch (e: Exception) { e.printStackTrace() }
        super.onDestroy()
    }
}

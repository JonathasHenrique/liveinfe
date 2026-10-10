package com.igrejalive.app.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.SurfaceHolder
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.pedro.common.ConnectChecker
import com.igrejalive.app.R
import com.igrejalive.app.data.api.YouTubeApiService
import com.igrejalive.app.databinding.ActivityLiveStreamBinding
import com.igrejalive.app.stream.StreamManager
import com.igrejalive.app.stream.StreamService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Tela da transmissão. Ela NÃO é dona da câmera: quem mantém câmera, microfone e RTMP no ar é o
 * StreamService (serviço em primeiro plano). Aqui apenas mostramos o preview e os controles.
 * Por isso a live continua mesmo se o app sair da tela (Home, WhatsApp, tela bloqueada, etc.).
 */
class LiveStreamActivity : AppCompatActivity(), ConnectChecker {

    companion object {
        const val EXTRA_RTMP_URL = "extra_rtmp_url"
        const val EXTRA_BROADCAST_ID = "extra_broadcast_id"
        const val EXTRA_WATCH_URL = "extra_watch_url"
        const val EXTRA_ACCESS_TOKEN = "extra_access_token"
        const val EXTRA_IS_1080P = "extra_is_1080p"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_AUDIO_CHURCH = "extra_audio_church"

        /** Preferência com o link permanente do canal (ex.: https://www.youtube.com/@canal/live) */
        const val PREF_CHANNEL_LIVE_URL = "channel_live_url"

        private const val UNLOCK_HOLD_MS = 2000L
    }

    private lateinit var binding: ActivityLiveStreamBinding
    private val youtubeApi = YouTubeApiService()
    private val prefs by lazy { getSharedPreferences("liveinfe_prefs", Context.MODE_PRIVATE) }

    // Serviço dono da câmera/transmissão
    private var service: StreamService? = null
    private var serviceBound = false
    private val streamManager: StreamManager? get() = service?.manager

    private var rtmpUrl: String = ""
    private var broadcastId: String? = null
    private var watchUrl: String? = null
    private var accessToken: String? = null
    private var is1080p: Boolean = true
    private var isChurchAudio: Boolean = true
    private var liveTitle: String = "Culto ao Vivo"

    private var isLiveStarted: Boolean = false
    private var viewDetached: Boolean = false     // preview solto (app em segundo plano)
    private var isActivityResumed: Boolean = false
    private var userStopped: Boolean = false
    private var hadConnectionProblem: Boolean = false
    private var pinRequested: Boolean = false

    // Travamento de toques
    private var touchLocked: Boolean = false
    private val unlockHandler = Handler(Looper.getMainLooper())
    private val unlockRunnable = Runnable {
        setTouchLock(false)
        vibrateShort()
        showStatusBanner("🔓 Toques liberados", isAlert = false)
    }

    // Zoom
    private var currentZoom: Float = 1.0f
    private lateinit var scaleDetector: ScaleGestureDetector

    // Handlers para feedback animado
    private val bannerHandler = Handler(Looper.getMainLooper())
    private val zoomHudHandler = Handler(Looper.getMainLooper())

    private val hideBannerRunnable = Runnable {
        binding.tvStatusBanner.animate()
            .alpha(0f)
            .translationY(-25f)
            .setDuration(250)
            .withEndAction { binding.tvStatusBanner.visibility = View.GONE }
            .start()
    }

    private val hideZoomHudRunnable = Runnable {
        binding.tvZoomHud.animate()
            .alpha(0f)
            .setDuration(250)
            .withEndAction { binding.tvZoomHud.visibility = View.GONE }
            .start()
    }

    // Timer do culto (calculado a partir do início da sessão no serviço: não "acelera" ao reconectar)
    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            val base = service?.startedAtElapsed ?: 0L
            val total = if (base > 0) ((SystemClock.elapsedRealtime() - base) / 1000).toInt() else 0
            val hours = total / 3600
            val minutes = (total % 3600) / 60
            val seconds = total % 60
            binding.tvLiveTimer.text = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)

            // Simulação de pulso do VU Meter (áudio dinâmico) enquanto transmite
            val simulatedLevel = 0.45f + (Math.random().toFloat() * 0.40f)
            binding.vuMeterView.setAudioLevel(simulatedLevel)

            timerHandler.postDelayed(this, 1000)
        }
    }

    private fun startTimer() {
        timerHandler.removeCallbacks(timerRunnable)
        timerHandler.post(timerRunnable)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val svc = (binder as? StreamService.LocalBinder)?.getService() ?: return
            service = svc
            svc.listener = this@LiveStreamActivity
            onServiceReady(svc)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = ActivityLiveStreamBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enterImmersiveMode()

        // Recupera parâmetros da Intent
        rtmpUrl = intent.getStringExtra(EXTRA_RTMP_URL) ?: ""
        broadcastId = intent.getStringExtra(EXTRA_BROADCAST_ID)
        watchUrl = intent.getStringExtra(EXTRA_WATCH_URL)
        accessToken = intent.getStringExtra(EXTRA_ACCESS_TOKEN)
        is1080p = intent.getBooleanExtra(EXTRA_IS_1080P, true)
        isChurchAudio = intent.getBooleanExtra(EXTRA_AUDIO_CHURCH, true)
        liveTitle = intent.getStringExtra(EXTRA_TITLE) ?: "Culto ao Vivo"

        updateInfoLabels()

        setupScaleDetector()
        setupControls()

        // Bloqueio contra toque acidental no gesto/botão 'Voltar' do Android
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                confirmStopLive()
            }
        })

        if (!hasStreamPermissions()) {
            Toast.makeText(this, "Permita câmera e microfone para transmitir", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Liga o serviço em primeiro plano (precisa ser iniciado com o app visível) e conecta nele
        StreamService.start(this)
        serviceBound = bindService(
            Intent(this, StreamService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    private fun hasStreamPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun updateInfoLabels() {
        binding.tvStreamResolutionInfo.text =
            if (is1080p) "1080p Full HD • 16:9 Nativo" else "720p HD • 16:9 Nativo"
        binding.tvAudioSourceBadge.text = if (isChurchAudio) "IGREJA" else "MÍDIA"
    }

    // ------------------------------------------------------------------------------------------
    // Ligação com o serviço
    // ------------------------------------------------------------------------------------------

    private fun onServiceReady(svc: StreamService) {
        if (svc.sessionActive) {
            // A live já está no ar (voltei pela notificação ou a tela foi recriada): só reanexa o preview
            broadcastId = broadcastId ?: svc.broadcastId
            accessToken = accessToken ?: svc.accessToken
            watchUrl = watchUrl ?: svc.watchUrl
            is1080p = svc.is1080p
            updateInfoLabels()
            isLiveStarted = true
            viewDetached = true // a câmera está em segundo plano: precisa anexar a tela
            refreshUltraWideButton()
            if (svc.manager.usingUltraWide) {
                currentZoom = 0.5f
                updateZoomButtonsState(currentZoom)
            }
            syncUiFromService(svc)
            startTimer()
            if (isActivityResumed) whenSurfaceReady { reattachPreview() }
            return
        }

        if (rtmpUrl.isEmpty()) {
            Toast.makeText(this, "Nenhuma transmissão em andamento", Toast.LENGTH_SHORT).show()
            svc.endSession()
            finish()
            return
        }

        whenSurfaceReady { startFreshSession(svc) }
    }

    private fun startFreshSession(svc: StreamService) {
        if (isLiveStarted) return
        val mgr = svc.manager
        mgr.attachView(binding.openGlView)
        viewDetached = false

        val prepared = mgr.prepareStream(
            is1080p = is1080p,
            bitrateKbps = if (is1080p) 4000 else 2500,
            isChurchAudio = isChurchAudio
        )
        if (!prepared) {
            Toast.makeText(this, "Falha ao preparar câmera ou áudio", Toast.LENGTH_LONG).show()
            svc.endSession()
            finish()
            return
        }

        isLiveStarted = true
        mgr.startPreview()
        refreshUltraWideButton()
        svc.beginStream(rtmpUrl, liveTitle, broadcastId, accessToken, watchUrl, is1080p)
        startTimer()
        requestScreenPinning()
    }

    /** Ao voltar para o app (ou reabrir pela notificação), religa o preview na tela. */
    private fun reattachPreview() {
        val mgr = streamManager ?: return
        if (!viewDetached || !isActivityResumed) return
        mgr.attachView(binding.openGlView)
        viewDetached = false
        // A câmera é reaberta ao trocar de tela: reaplica foco/estabilização e o zoom escolhido
        binding.root.postDelayed({
            streamManager?.reapplyCameraSettings()
            val mgr = streamManager
            if (mgr != null) mgr.setZoom(if (mgr.usingUltraWide) 1.0f else currentZoom)
        }, 800)
    }

    /**
     * A OpenGlView só pode ser anexada quando a Surface existe (senão o OpenGL falha).
     * Executa já se estiver pronta; senão espera o surfaceCreated.
     */
    private fun whenSurfaceReady(block: () -> Unit) {
        val holder = binding.openGlView.holder
        if (holder.surface != null && holder.surface.isValid) {
            block()
            return
        }
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(h: SurfaceHolder) {
                holder.removeCallback(this)
                block()
            }

            override fun surfaceChanged(h: SurfaceHolder, format: Int, width: Int, height: Int) {}
            override fun surfaceDestroyed(h: SurfaceHolder) {}
        })
    }

    private fun syncUiFromService(svc: StreamService) {
        if (svc.connected) {
            binding.viewNetworkStatus.setBackgroundResource(R.drawable.circle_green)
            binding.tvBitrate.text = "${svc.lastBitrateKbps} kbps"
        } else {
            binding.viewNetworkStatus.setBackgroundResource(R.drawable.circle_red)
            binding.tvBitrate.text = "Reconectando..."
        }
    }

    // ------------------------------------------------------------------------------------------
    // Proteção contra toques acidentais
    // ------------------------------------------------------------------------------------------

    /** Esconde as barras do sistema; elas só aparecem com um deslize deliberado da borda. */
    private fun enterImmersiveMode() {
        val controller = WindowInsetsControllerCompat(window, binding.root)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersiveMode()
    }

    /**
     * Fixa o app na tela (Home/Recentes bloqueados). Só funciona se "Fixar app/janelas" estiver ligado
     * nas configurações do Android; caso contrário a chamada é ignorada sem causar erro.
     */
    private fun requestScreenPinning() {
        if (pinRequested) return
        pinRequested = true
        try {
            startLockTask()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseScreenPinning() {
        try {
            stopLockTask()
        } catch (e: Exception) {
            // não estava fixado
        }
    }

    private fun setTouchLock(locked: Boolean) {
        touchLocked = locked
        binding.touchShield.visibility = if (locked) View.VISIBLE else View.GONE
        binding.btnUnlockTouch.visibility = if (locked) View.VISIBLE else View.GONE
        binding.btnUnlockTouch.alpha = 1f
        unlockHandler.removeCallbacks(unlockRunnable)
        if (locked) {
            vibrateShort()
            showStatusBanner("🔒 Toques travados", isAlert = false)
        }
    }

    private fun vibrateShort() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            vibrator?.vibrate(android.os.VibrationEffect.createOneShot(40, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (ignored: Exception) {}
    }

    // ------------------------------------------------------------------------------------------
    // Controles
    // ------------------------------------------------------------------------------------------

    private fun setupScaleDetector() {
        scaleDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val mgr = streamManager ?: return true
                val factor = detector.scaleFactor

                // Na lente ultra-angular (câmera separada): abrir a pinça volta para a câmera principal
                if (mgr.usingUltraWide) {
                    if (factor > 1.03f) {
                        mgr.useMainCamera()
                        currentZoom = 1.0f
                        mgr.setZoom(currentZoom)
                        showZoomHud(currentZoom)
                        updateZoomButtonsState(currentZoom)
                    }
                    return true
                }

                currentZoom *= factor
                val range = mgr.getZoomRange()
                val min = range?.lower ?: 1.0f
                val max = range?.upper ?: 5.0f
                currentZoom = currentZoom.coerceIn(min, max)
                mgr.setZoom(currentZoom)
                showZoomHud(currentZoom)
                updateZoomButtonsState(currentZoom)
                return true
            }
        })
    }

    /** Mostra o botão .5x somente se o aparelho oferecer a câmera ultra-angular. */
    private fun refreshUltraWideButton() {
        val available = streamManager?.hasUltraWide() == true
        binding.btnZoom05x.visibility = if (available) View.VISIBLE else View.GONE
    }

    /** Vai para 0.5x: como zoom da câmera principal (se suportado) ou trocando para a lente ultra-angular. */
    private fun goUltraWide() {
        val mgr = streamManager ?: return
        when {
            mgr.supportsLogicalUltraWide() -> {
                val lower = mgr.getZoomRange()?.lower ?: 0.5f
                currentZoom = maxOf(0.5f, lower)
                mgr.setZoom(currentZoom)
            }
            mgr.useUltraWide() -> {
                currentZoom = 0.5f
            }
            else -> {
                Toast.makeText(this, "Câmera 0.5x indisponível neste aparelho", Toast.LENGTH_SHORT).show()
                return
            }
        }
        showZoomHud(currentZoom)
        updateZoomButtonsState(currentZoom)
    }

    /** Define 1x ou 2x garantindo que estamos na câmera principal. */
    private fun goMainZoom(level: Float) {
        val mgr = streamManager ?: return
        val wasUltraWide = mgr.usingUltraWide
        if (wasUltraWide) mgr.useMainCamera()
        currentZoom = level
        if (wasUltraWide && level != 1.0f) {
            // a câmera principal leva um instante para reabrir; aplica o zoom logo depois
            binding.root.postDelayed({ streamManager?.setZoom(level) }, 700)
        } else {
            mgr.setZoom(currentZoom)
        }
        showZoomHud(currentZoom)
        updateZoomButtonsState(currentZoom)
    }

    /** Link a compartilhar: o da live criada (modo Google) ou o link permanente do canal. */
    private fun liveLink(): String {
        val channelLink = prefs.getString(PREF_CHANNEL_LIVE_URL, null)?.takeIf { it.isNotBlank() }
        return watchUrl ?: channelLink ?: "https://www.youtube.com/live"
    }

    private fun setupControls() {
        // Toque na tela: Gesto de Pinça (Zoom) ou Toque Simples (Foco no Pastor/Altar)
        binding.openGlView.setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)
            if (!scaleDetector.isInProgress && event.action == MotionEvent.ACTION_UP) {
                val focused = streamManager?.tapToFocus(event) ?: false
                if (focused) {
                    showStatusBanner("🎯 Foco Ajustado no Púlpito", isAlert = false)
                }
            }
            true
        }

        // Botões Rápidos de Zoom (0.5x, 1x e 2x)
        binding.btnZoom05x.setOnClickListener {
            animateZoomButtonClick(binding.btnZoom05x)
            goUltraWide()
        }

        binding.btnZoom1x.setOnClickListener {
            animateZoomButtonClick(binding.btnZoom1x)
            goMainZoom(1.0f)
        }

        binding.btnZoom2x.setOnClickListener {
            animateZoomButtonClick(binding.btnZoom2x)
            goMainZoom(2.0f)
        }

        // Trocar Câmera com micro-animação de rotação 180°
        binding.fabSwitchCamera.setOnClickListener {
            animateSwitchCamera()
            streamManager?.switchCamera()
            currentZoom = 1.0f
            updateZoomButtonsState(currentZoom)
            val cameraName = if (streamManager?.isFrontCamera() == true) "Câmera Frontal" else "Câmera Traseira Principal"
            showStatusBanner("🔄 $cameraName", isAlert = false)
        }

        // Mudo / Microfone com micro-animação de pulso e transição de cor
        binding.fabMuteAudio.setOnClickListener {
            val isMuted = streamManager?.toggleMute() ?: return@setOnClickListener
            animateMuteAudio(isMuted)
        }

        // Lanterna / Flash com micro-animação
        binding.fabFlashLight.setOnClickListener {
            val isOn = streamManager?.toggleFlash() ?: return@setOnClickListener
            animateFlashLight(isOn)
        }

        // Toque simples: copia o link (100% seguro, não sai da tela)
        binding.btnShareLive.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Link da Transmissão LiveinFé", liveLink()))
            vibrateShort()
            showStatusBanner("📋 LINK COPIADO COM SUCESSO!", isAlert = false)
            Toast.makeText(this, "Link copiado! Segure o botão para compartilhar direto.", Toast.LENGTH_SHORT).show()
        }

        // Toque longo: abre o compartilhamento (a live continua no ar pelo serviço)
        binding.btnShareLive.setOnLongClickListener {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "A paz do Senhor! Assista ao culto ao vivo pelo YouTube: ${liveLink()}")
                type = "text/plain"
            }
            startActivity(Intent.createChooser(sendIntent, "Compartilhar Link do Culto"))
            true
        }

        // Travar toques
        binding.btnLockTouch.setOnClickListener { setTouchLock(true) }

        // Destravar: segurar 2 segundos (ignora toques rápidos e acidentais)
        binding.btnUnlockTouch.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.alpha = 0.6f
                    unlockHandler.postDelayed(unlockRunnable, UNLOCK_HOLD_MS)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.alpha = 1f
                    unlockHandler.removeCallbacks(unlockRunnable)
                }
            }
            true
        }

        // Encerrar: exige SEGURAR o botão (e ainda confirma no diálogo)
        binding.btnStopLive.setOnClickListener {
            Toast.makeText(this, "Segure o botão para encerrar a transmissão", Toast.LENGTH_SHORT).show()
        }
        binding.btnStopLive.setOnLongClickListener {
            confirmStopLive()
            true
        }

        // Toque no badge de status de rede para reconectar manualmente a qualquer momento
        binding.tvBitrate.setOnClickListener {
            val svc = service ?: return@setOnClickListener
            if (!svc.connected) {
                showStatusBanner("🔄 Reconectando ao YouTube...", isAlert = false)
                svc.reconnectNow()
            }
        }
    }

    private fun animateSwitchCamera() {
        binding.fabSwitchCamera.animate()
            .rotationBy(180f)
            .setDuration(350)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun animateMuteAudio(isMuted: Boolean) {
        binding.fabMuteAudio.animate()
            .scaleX(1.3f)
            .scaleY(1.3f)
            .setDuration(120)
            .withEndAction {
                binding.fabMuteAudio.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(120)
                    .start()
            }
            .start()

        if (isMuted) {
            binding.fabMuteAudio.setImageResource(android.R.drawable.ic_lock_silent_mode)
            binding.fabMuteAudio.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EF4444")) // Vermelho
            binding.vuMeterView.setAudioLevel(0.0f)
            showStatusBanner("🔇 MICROFONE MUTADO", isAlert = true)
        } else {
            binding.fabMuteAudio.setImageResource(android.R.drawable.ic_lock_silent_mode_off)
            binding.fabMuteAudio.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#33FFFFFF"))
            showStatusBanner("🎙️ MICROFONE ABERTO", isAlert = false)
        }
    }

    private fun animateFlashLight(isOn: Boolean) {
        binding.fabFlashLight.animate()
            .scaleX(1.25f)
            .scaleY(1.25f)
            .setDuration(100)
            .withEndAction {
                binding.fabFlashLight.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }
            .start()

        if (isOn) {
            binding.fabFlashLight.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F59E0B")) // Ouro
            showStatusBanner("💡 Iluminação Ligada", isAlert = false)
        } else {
            binding.fabFlashLight.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#33FFFFFF"))
            showStatusBanner("💡 Iluminação Desligada", isAlert = false)
        }
    }

    private fun animateZoomButtonClick(view: View) {
        view.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(100)
            .withEndAction {
                view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }
            .start()
    }

    private fun updateZoomButtonsState(zoom: Float) {
        val green = ContextCompat.getColor(this, R.color.tuned_green)
        val white = Color.WHITE

        binding.btnZoom05x.setTextColor(if (zoom < 0.8f) green else white)
        binding.btnZoom1x.setTextColor(if (zoom >= 0.8f && zoom < 1.5f) green else white)
        binding.btnZoom2x.setTextColor(if (zoom >= 1.5f) green else white)
    }

    private fun showStatusBanner(text: String, isAlert: Boolean) {
        bannerHandler.removeCallbacks(hideBannerRunnable)
        binding.tvStatusBanner.text = text
        binding.tvStatusBanner.setBackgroundResource(if (isAlert) R.drawable.badge_live_bg else R.drawable.badge_dark_bg)
        binding.tvStatusBanner.visibility = View.VISIBLE
        binding.tvStatusBanner.alpha = 0f
        binding.tvStatusBanner.translationY = -25f
        binding.tvStatusBanner.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(180)
            .start()
        bannerHandler.postDelayed(hideBannerRunnable, 1800)
    }

    private fun showZoomHud(zoom: Float) {
        zoomHudHandler.removeCallbacks(hideZoomHudRunnable)
        binding.tvZoomHud.text = String.format(Locale.US, "🔍 %.1fx", zoom)
        binding.tvZoomHud.visibility = View.VISIBLE
        binding.tvZoomHud.alpha = 1f
        zoomHudHandler.postDelayed(hideZoomHudRunnable, 1400)
    }

    private fun confirmStopLive() {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_stop_title)
            .setMessage(R.string.confirm_stop_msg)
            .setPositiveButton(R.string.yes) { _, _ ->
                finishStreaming()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun finishStreaming() {
        userStopped = true
        timerHandler.removeCallbacks(timerRunnable)
        unlockHandler.removeCallbacks(unlockRunnable)
        releaseScreenPinning()

        val bId = broadcastId
        val token = accessToken

        // Para câmera, microfone e envio, e remove a notificação
        service?.endSession()

        // Encerra formalmente a live no YouTube. Fora do lifecycleScope de propósito:
        // a tela fecha logo em seguida e isso cancelaria a chamada de rede no meio.
        if (bId != null && token != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    youtubeApi.endLiveBroadcast(token, bId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        Toast.makeText(this, "Transmissão encerrada com sucesso!", Toast.LENGTH_SHORT).show()
        finish()
    }

    // ------------------------------------------------------------------------------------------
    // Eventos de conexão (repassados pelo StreamService; a reconexão é feita lá)
    // ------------------------------------------------------------------------------------------

    override fun onConnectionStarted(url: String) {
        runOnUiThread {
            binding.tvBitrate.text = "Conectando..."
        }
    }

    override fun onConnectionSuccess() {
        runOnUiThread {
            binding.tvBitrate.text = "Conectado"
            binding.viewNetworkStatus.setBackgroundResource(R.drawable.circle_green)
            showStatusBanner(
                if (hadConnectionProblem) "🟢 Conexão restabelecida" else "🟢 AO VIVO NO YOUTUBE",
                isAlert = false
            )
            hadConnectionProblem = false
        }
    }

    override fun onConnectionFailed(reason: String) {
        runOnUiThread {
            hadConnectionProblem = true
            binding.viewNetworkStatus.setBackgroundResource(R.drawable.circle_red)
            if (reason == "RETRY") {
                // O serviço já está tentando reconectar sozinho a cada poucos segundos
                binding.tvBitrate.text = "Reconectando..."
                showStatusBanner("⚠️ Oscilação de rede - Reconectando...", isAlert = true)
            } else {
                binding.tvBitrate.text = "Falha"
                showStatusBanner("❌ Falha na conexão: confira a chave de transmissão", isAlert = true)
            }
        }
    }

    override fun onNewBitrate(bitrate: Long) {
        runOnUiThread {
            val kbps = bitrate / 1000
            binding.tvBitrate.text = "$kbps kbps"
        }
    }

    override fun onDisconnect() {
        runOnUiThread {
            if (userStopped) return@runOnUiThread
            binding.viewNetworkStatus.setBackgroundResource(R.drawable.circle_red)
            binding.tvBitrate.text = "Desconectado"
            showStatusBanner("⚠️ Transmissão Desconectada", isAlert = true)
        }
    }

    override fun onAuthError() {
        runOnUiThread {
            Toast.makeText(this, "Erro de autenticação RTMP", Toast.LENGTH_LONG).show()
        }
    }

    override fun onAuthSuccess() {
        // Autenticado
    }

    // ------------------------------------------------------------------------------------------
    // Ciclo de vida: a tela entra e sai, a transmissão continua no serviço
    // ------------------------------------------------------------------------------------------

    override fun onResume() {
        super.onResume()
        isActivityResumed = true
        enterImmersiveMode()
        val svc = service
        if (svc != null && isLiveStarted && viewDetached && svc.sessionActive) {
            whenSurfaceReady { reattachPreview() }
        }
    }

    override fun onPause() {
        isActivityResumed = false
        // Solta a tela ANTES de o Android destruir a Surface: sem isso o preview fica preto
        // e o vídeo enviado ao YouTube congela. Câmera, microfone e envio seguem no serviço.
        val mgr = streamManager
        val svc = service
        if (mgr != null && (mgr.isStreaming() || svc?.sessionActive == true) && !viewDetached) {
            mgr.detachView()
            viewDetached = true
        }
        super.onPause()
    }

    override fun onDestroy() {
        timerHandler.removeCallbacks(timerRunnable)
        unlockHandler.removeCallbacks(unlockRunnable)
        bannerHandler.removeCallbacks(hideBannerRunnable)
        zoomHudHandler.removeCallbacks(hideZoomHudRunnable)
        if (service?.listener === this) service?.listener = null
        if (serviceBound) {
            try { unbindService(serviceConnection) } catch (ignored: Exception) {}
            serviceBound = false
        }
        // IMPORTANTE: não para a transmissão aqui. Quem encerra é o operador (finishStreaming).
        super.onDestroy()
    }
}

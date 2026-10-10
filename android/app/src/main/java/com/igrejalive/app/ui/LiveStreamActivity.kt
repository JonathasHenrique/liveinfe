package com.igrejalive.app.ui

import android.app.PictureInPictureParams
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pedro.common.ConnectChecker
import com.igrejalive.app.R
import com.igrejalive.app.data.api.YouTubeApiService
import com.igrejalive.app.databinding.ActivityLiveStreamBinding
import com.igrejalive.app.stream.StreamManager
import kotlinx.coroutines.launch
import java.util.Locale

class LiveStreamActivity : AppCompatActivity(), ConnectChecker {

    companion object {
        const val EXTRA_RTMP_URL = "extra_rtmp_url"
        const val EXTRA_BROADCAST_ID = "extra_broadcast_id"
        const val EXTRA_WATCH_URL = "extra_watch_url"
        const val EXTRA_ACCESS_TOKEN = "extra_access_token"
        const val EXTRA_IS_1080P = "extra_is_1080p"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_AUDIO_CHURCH = "extra_audio_church"
    }

    private lateinit var binding: ActivityLiveStreamBinding
    private lateinit var streamManager: StreamManager
    private val youtubeApi = YouTubeApiService()

    private var rtmpUrl: String = ""
    private var broadcastId: String? = null
    private var watchUrl: String? = null
    private var accessToken: String? = null
    private var is1080p: Boolean = true
    private var isChurchAudio: Boolean = true
    private var isLiveStarted: Boolean = false

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

    // Timer do Culto
    private var streamSeconds = 0
    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            streamSeconds++
            val hours = streamSeconds / 3600
            val minutes = (streamSeconds % 3600) / 60
            val seconds = streamSeconds % 60
            binding.tvLiveTimer.text = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
            
            // Simulação de pulso do VU Meter (áudio dinâmico) enquanto transmite
            val simulatedLevel = 0.45f + (Math.random().toFloat() * 0.40f)
            binding.vuMeterView.setAudioLevel(simulatedLevel)
            
            timerHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = ActivityLiveStreamBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Recupera parâmetros da Intent
        rtmpUrl = intent.getStringExtra(EXTRA_RTMP_URL) ?: ""
        broadcastId = intent.getStringExtra(EXTRA_BROADCAST_ID)
        watchUrl = intent.getStringExtra(EXTRA_WATCH_URL)
        accessToken = intent.getStringExtra(EXTRA_ACCESS_TOKEN)
        is1080p = intent.getBooleanExtra(EXTRA_IS_1080P, true)
        isChurchAudio = intent.getBooleanExtra(EXTRA_AUDIO_CHURCH, true)

        binding.tvStreamResolutionInfo.text = if (is1080p) "1080p Full HD • 16:9 Nativo" else "720p HD • 16:9 Nativo"
        binding.tvAudioSourceBadge.text = if (isChurchAudio) "IGREJA" else "MÍDIA"

        setupScaleDetector()
        setupStreamManager()
        setupControls()

        // Bloqueio contra toque acidental no botão ou gesto 'Voltar' do Android
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                confirmStopLive()
            }
        })

        // Configura Picture-in-Picture automático para Android 12+ (Samsung S21/S22/S23/S24/S25)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val pipParams = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .setAutoEnterEnabled(true)
                    .build()
                setPictureInPictureParams(pipParams)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupScaleDetector() {
        scaleDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val factor = detector.scaleFactor
                currentZoom *= factor
                val range = streamManager.getZoomRange()
                val min = range?.lower ?: 1.0f
                val max = range?.upper ?: 5.0f
                currentZoom = currentZoom.coerceIn(min, max)
                streamManager.setZoom(currentZoom)
                showZoomHud(currentZoom)
                updateZoomButtonsState(currentZoom)
                return true
            }
        })
    }

    private fun setupStreamManager() {
        streamManager = StreamManager(this, binding.openGlView, this)

        binding.openGlView.post {
            val prepared = streamManager.prepareStream(
                is1080p = is1080p,
                bitrateKbps = if (is1080p) 4000 else 2500,
                isChurchAudio = isChurchAudio
            )
            if (prepared) {
                isLiveStarted = true
                streamManager.startPreview()
                if (rtmpUrl.isNotEmpty()) {
                    streamManager.startStream(rtmpUrl)
                }
            } else {
                Toast.makeText(this, "Falha ao preparar câmera ou áudio", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupControls() {
        // Toque na tela: Gesto de Pinça (Zoom) ou Toque Simples (Foco no Pastor/Altar)
        binding.openGlView.setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)
            if (!scaleDetector.isInProgress && event.action == MotionEvent.ACTION_UP) {
                val focused = streamManager.tapToFocus(event)
                if (focused) {
                    showStatusBanner("🎯 Foco Ajustado no Púlpito", isAlert = false)
                }
            }
            true
        }

        // Botões Rápidos de Zoom (1x e 2x)
        binding.btnZoom1x.setOnClickListener {
            animateZoomButtonClick(binding.btnZoom1x)
            currentZoom = 1.0f
            streamManager.setZoom(currentZoom)
            showZoomHud(currentZoom)
            updateZoomButtonsState(currentZoom)
        }

        binding.btnZoom2x.setOnClickListener {
            animateZoomButtonClick(binding.btnZoom2x)
            currentZoom = 2.0f
            streamManager.setZoom(currentZoom)
            showZoomHud(currentZoom)
            updateZoomButtonsState(currentZoom)
        }

        // Trocar Câmera com micro-animação de rotação 180°
        binding.fabSwitchCamera.setOnClickListener {
            animateSwitchCamera()
            streamManager.switchCamera()
            val cameraName = if (streamManager.isFrontCamera()) "Câmera Frontal" else "Câmera Traseira Principal"
            showStatusBanner("🔄 $cameraName", isAlert = false)
        }

        // Mudo / Microfone com micro-animação de pulso e transição de cor
        binding.fabMuteAudio.setOnClickListener {
            val isMuted = streamManager.toggleMute()
            animateMuteAudio(isMuted)
        }

        // Lanterna / Flash com micro-animação
        binding.fabFlashLight.setOnClickListener {
            val isOn = streamManager.toggleFlash()
            animateFlashLight(isOn)
        }

        // Botão Copiar/Compartilhar Link da Live (com suporte a Picture-in-Picture)
        binding.btnShareLive.setOnClickListener {
            val url = watchUrl ?: "https://youtube.com/live"
            
            // 1. Copia imediatamente para o Clipboard
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Link da Transmissão LiveinFé", url)
            clipboard.setPrimaryClip(clip)

            // 2. Feedback tátil suave
            try {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                vibrator?.vibrate(android.os.VibrationEffect.createOneShot(40, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } catch (ignored: Exception) {}

            // 3. Banner animado e orientação sobre PiP
            showStatusBanner("📋 LINK COPIADO COM SUCESSO!", isAlert = false)
            Toast.makeText(this, "Link copiado! Se abrir o WhatsApp, a live continua em janela flutuante (PiP).", Toast.LENGTH_LONG).show()
        }

        // Toque longo no botão: Compartilha direto no WhatsApp abrindo em PiP
        binding.btnShareLive.setOnLongClickListener {
            val url = watchUrl ?: "https://youtube.com/live"
            if (isLiveStarted && streamManager.isStreaming() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val pipParams = PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(16, 9))
                        .build()
                    enterPictureInPictureMode(pipParams)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "A paz do Senhor! Assista ao culto ao vivo pelo YouTube: $url")
                type = "text/plain"
            }
            startActivity(Intent.createChooser(sendIntent, "Compartilhar Link do Culto"))
            true
        }

        // Botão Encerrar Transmissão
        binding.btnStopLive.setOnClickListener {
            confirmStopLive()
        }

        // Toque no badge de status de rede para reconectar manualmente a qualquer momento
        binding.tvBitrate.setOnClickListener {
            if (!streamManager.isStreaming() && rtmpUrl.isNotEmpty()) {
                showStatusBanner("🔄 Reconectando ao YouTube...", isAlert = false)
                streamManager.startStream(rtmpUrl)
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

        if (zoom < 1.5f) {
            binding.btnZoom1x.setTextColor(green)
            binding.btnZoom2x.setTextColor(white)
        } else {
            binding.btnZoom1x.setTextColor(white)
            binding.btnZoom2x.setTextColor(green)
        }
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
        timerHandler.removeCallbacks(timerRunnable)
        streamManager.stopStream()

        // Se tínhamos id de broadcast do YouTube, encerra formalmente a live
        if (broadcastId != null && accessToken != null) {
            lifecycleScope.launch {
                youtubeApi.endLiveBroadcast(accessToken!!, broadcastId!!)
            }
        }

        Toast.makeText(this, "Transmissão encerrada com sucesso!", Toast.LENGTH_SHORT).show()
        finish()
    }

    // Callbacks do RootEncoder ConnectChecker
    override fun onConnectionStarted(url: String) {
        runOnUiThread {
            binding.tvBitrate.text = "Conectando..."
        }
    }

    override fun onConnectionSuccess() {
        runOnUiThread {
            binding.tvBitrate.text = if (is1080p) "4000 kbps" else "2500 kbps"
            binding.viewNetworkStatus.setBackgroundResource(R.drawable.circle_green)
            showStatusBanner("🟢 AO VIVO NO YOUTUBE", isAlert = false)
            timerHandler.post(timerRunnable)
        }
    }

    override fun onConnectionFailed(reason: String) {
        runOnUiThread {
            binding.viewNetworkStatus.setBackgroundResource(R.drawable.circle_red)
            binding.tvBitrate.text = "Reconectando..."
            showStatusBanner("⚠️ Oscilação de rede - Reconectando...", isAlert = true)
            // Loop de auto-reconexão para quedas de link na igreja
            if (!isFinishing && rtmpUrl.isNotEmpty()) {
                Handler(Looper.getMainLooper()).postDelayed({
                    if (!isFinishing && !streamManager.isStreaming()) {
                        streamManager.startStream(rtmpUrl)
                    }
                }, 3000)
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
            binding.viewNetworkStatus.setBackgroundResource(R.drawable.circle_red)
            binding.tvBitrate.text = "Desconectado"
            showStatusBanner("⚠️ Transmissão Desconectada", isAlert = true)
            // Reconexão suave automática se o app ainda estiver aberto
            if (!isFinishing && rtmpUrl.isNotEmpty()) {
                Handler(Looper.getMainLooper()).postDelayed({
                    if (!isFinishing && !streamManager.isStreaming()) {
                        showStatusBanner("🔄 Reconectando ao YouTube...", isAlert = false)
                        streamManager.startStream(rtmpUrl)
                    }
                }, 2500)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Só tenta restaurar se a transmissão já foi inicializada previamente com sucesso
        if (isLiveStarted && streamManager.isPrepared()) {
            try {
                if (!streamManager.isOnPreview()) {
                    streamManager.startPreview()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Se o RTMP desconectou enquanto o usuário estava em outro app:
            if (!streamManager.isStreaming() && rtmpUrl.isNotEmpty()) {
                showStatusBanner("🔄 Restaurando transmissão...", isAlert = false)
                streamManager.startStream(rtmpUrl)
            }
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

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isLiveStarted && streamManager.isStreaming() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val pipParams = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(pipParams)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            // Em modo PiP (janela flutuante no WhatsApp), esconde controles para exibir imagem limpa 16:9
            binding.layoutTopBar.visibility = View.GONE
            binding.layoutBottomControls.visibility = View.GONE
            binding.layoutCameraControls.visibility = View.GONE
            binding.layoutAudioMeter.visibility = View.GONE
            binding.tvStatusBanner.visibility = View.GONE
            binding.tvZoomHud.visibility = View.GONE
        } else {
            // Ao retornar para tela cheia, restaura todos os controles da transmissão
            binding.layoutTopBar.visibility = View.VISIBLE
            binding.layoutBottomControls.visibility = View.VISIBLE
            binding.layoutCameraControls.visibility = View.VISIBLE
            binding.layoutAudioMeter.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacks(timerRunnable)
        streamManager.release()
    }
}

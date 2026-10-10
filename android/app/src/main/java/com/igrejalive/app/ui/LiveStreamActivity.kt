package com.igrejalive.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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
    }

    private lateinit var binding: ActivityLiveStreamBinding
    private lateinit var streamManager: StreamManager
    private val youtubeApi = YouTubeApiService()

    private var rtmpUrl: String = ""
    private var broadcastId: String? = null
    private var watchUrl: String? = null
    private var accessToken: String? = null
    private var is1080p: Boolean = true

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

        binding.tvStreamResolutionInfo.text = if (is1080p) "1080p Full HD • 16:9 Nativo" else "720p HD • 16:9 Nativo"

        setupStreamManager()
        setupControls()
    }

    private fun setupStreamManager() {
        streamManager = StreamManager(this, binding.openGlView, this)

        binding.openGlView.post {
            val prepared = streamManager.prepareStream(is1080p)
            if (prepared) {
                streamManager.startPreview()
                // Inicia a transmissão automaticamente para o YouTube
                if (rtmpUrl.isNotEmpty()) {
                    streamManager.startStream(rtmpUrl)
                }
            } else {
                Toast.makeText(this, "Falha ao preparar câmera ou microfone", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupControls() {
        // Toque na tela para focar e expor (Tap-to-Focus no Altar/Pregador)
        binding.openGlView.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val focused = streamManager.tapToFocus(event)
                if (focused) {
                    Toast.makeText(this, "Foco ajustado", Toast.LENGTH_SHORT).show()
                }
            }
            true
        }

        // Trocar Câmera com 1 toque
        binding.fabSwitchCamera.setOnClickListener {
            streamManager.switchCamera()
            val cameraName = if (streamManager.isFrontCamera()) "Câmera Frontal" else "Câmera Traseira Principal"
            Toast.makeText(this, cameraName, Toast.LENGTH_SHORT).show()
        }

        // Mudo / Microfone
        binding.fabMuteAudio.setOnClickListener {
            val isMuted = streamManager.toggleMute()
            if (isMuted) {
                binding.fabMuteAudio.setImageResource(android.R.drawable.ic_lock_silent_mode)
                Toast.makeText(this, "Microfone Mutado", Toast.LENGTH_SHORT).show()
                binding.vuMeterView.setAudioLevel(0.0f)
            } else {
                binding.fabMuteAudio.setImageResource(android.R.drawable.ic_lock_silent_mode_off)
                Toast.makeText(this, "Microfone Aberto", Toast.LENGTH_SHORT).show()
            }
        }

        // Lanterna / Flash
        binding.fabFlashLight.setOnClickListener {
            val isOn = streamManager.toggleFlash()
            Toast.makeText(this, if (isOn) "Luz Ligada" else "Luz Desligada", Toast.LENGTH_SHORT).show()
        }

        // Compartilhar Link do YouTube
        binding.btnShareLive.setOnClickListener {
            val url = watchUrl ?: "Live no YouTube"
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "Assista ao nosso culto ao vivo: $url")
                type = "text/plain"
            }
            startActivity(Intent.createChooser(sendIntent, "Compartilhar Link da Live"))
        }

        // Botão Encerrar Transmissão
        binding.btnStopLive.setOnClickListener {
            confirmStopLive()
        }
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
            Toast.makeText(this, "Ao vivo no YouTube!", Toast.LENGTH_SHORT).show()
            timerHandler.post(timerRunnable)
        }
    }

    override fun onConnectionFailed(reason: String) {
        runOnUiThread {
            binding.viewNetworkStatus.setBackgroundResource(R.drawable.circle_red)
            binding.tvBitrate.text = "Falha de rede"
            Toast.makeText(this, "Falha na conexão: $reason", Toast.LENGTH_LONG).show()
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

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacks(timerRunnable)
        streamManager.release()
    }
}

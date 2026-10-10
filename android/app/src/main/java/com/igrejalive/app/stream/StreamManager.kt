package com.igrejalive.app.stream

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaRecorder
import android.view.MotionEvent
import com.pedro.common.ConnectChecker
import com.pedro.library.rtmp.RtmpCamera2
import com.pedro.library.util.BitrateAdapter
import com.pedro.library.view.OpenGlView
import kotlin.math.atan

/**
 * Gerenciador de Transmissão de Vídeo e Áudio de Alta Performance
 * Calibrado especificamente para hardware Samsung (Galaxy S / A series).
 *
 * Reproduz e aprimora exatamente os parâmetros do Larix Broadcaster / IRL Pro:
 * - Resolução: 1920x1080 (16:9 Nativo sem esticar)
 * - Codec de Vídeo: H.264 / AVC por Hardware (MediaCodec Exynos/Snapdragon)
 * - Taxa de Quadros: 30 FPS cravados (sem quedas)
 * - Bitrate de Vídeo: 4000 kbps (CBR estável)
 * - Keyframe / GOP: 2 segundos (exigido pelo YouTube Ingest)
 * - Áudio: AAC Estéreo @ 48000 Hz, 128/192 kbps
 * - Fonte de Áudio: CAMCORDER (desativa cancelador de ruído do Samsung que abafa o louvor)
 * - Estabilização: OIS (Óptica) + EIS ativadas no sensor Samsung
 */
class StreamManager(
    private val context: Context,
    connectChecker: ConnectChecker
) {

    companion object {
        /** Piso do bitrate de vídeo no modo adaptativo (1 Mbps) */
        private const val MIN_VIDEO_BITRATE = 1_000_000
    }

    private var rtmpCamera: RtmpCamera2? = null
    private var isAudioMuted: Boolean = false
    private var isFlashOn: Boolean = false
    private var isAeLocked: Boolean = false

    // Parâmetros idênticos ao profile do Larix:
    var videoWidth: Int = 1920
    var videoHeight: Int = 1080
    var currentFps: Int = 30
    var currentBitrate: Int = 4000 * 1000 // 4000 kbps (4 Mbps)
    val keyframeInterval: Int = 2 // 2 segundos (GOP 60 frames a 30fps)
    val audioSampleRate: Int = 48000 // 48kHz (Padrão de mesa de som e YouTube)
    val audioBitrate: Int = 128 * 1024 // 128 kbps AAC Estéreo

    private var isPrepared: Boolean = false

    /** Bitrate adaptativo: reduz quando a fila de envio enche e sobe aos poucos quando a rede melhora. */
    var adaptiveBitrateEnabled: Boolean = true
    private val bitrateAdapter = BitrateAdapter(BitrateAdapter.Listener { adaptedTotal ->
        applyAdaptedBitrate(adaptedTotal)
    })

    init {
        // Construtor sem tela: a câmera funciona em segundo plano (Service).
        // A tela (OpenGlView) é anexada/desanexada com attachView()/detachView().
        rtmpCamera = RtmpCamera2(context.applicationContext, true, connectChecker).also { cam ->
            val client = cam.getStreamClient()
            client.setReTries(Int.MAX_VALUE)   // reconecta enquanto o operador não encerrar
            client.setCheckServerAlive(true)   // detecta conexão "morta" (Wi-Fi caiu sem avisar)
        }
    }

    // ------------------------------------------------------------------------------------
    // Câmera ultra-angular (0.5x)
    // ------------------------------------------------------------------------------------

    private class BackCam(val id: String, val fovDeg: Double)

    private var mainBackCameraId: String? = null
    private var ultraWideCameraId: String? = null

    /** True enquanto a câmera ultra-angular (lente separada) estiver em uso. */
    var usingUltraWide: Boolean = false
        private set

    init {
        detectBackCameras()
    }

    /**
     * Procura, entre as câmeras traseiras que o Android entrega ao app, uma lente com campo de visão
     * bem maior que a principal (a ultra-angular). Em alguns aparelhos a 0.5x vem como zoom da própria
     * câmera principal (zoomRange.lower <= 0.5); em outros é uma câmera separada. Tratamos os dois casos.
     */
    private fun detectBackCameras() {
        try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val backs = mutableListOf<BackCam>()
            for (id in cm.cameraIdList) {
                try {
                    val ch = cm.getCameraCharacteristics(id)
                    if (ch.get(CameraCharacteristics.LENS_FACING) != CameraCharacteristics.LENS_FACING_BACK) continue
                    val caps = ch.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf()
                    if (!caps.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE)) continue
                    val focal = ch.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull() ?: continue
                    val sensor = ch.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) ?: continue
                    val fov = Math.toDegrees(2.0 * atan(sensor.width / (2.0 * focal)))
                    backs.add(BackCam(id, fov))
                } catch (e: Exception) {
                    // câmera oculta/sem permissão: ignora
                }
            }
            if (backs.isEmpty()) return
            val main = backs.firstOrNull { it.id == "0" } ?: backs.first()
            mainBackCameraId = main.id
            val widest = backs.maxByOrNull { it.fovDeg }
            if (widest != null && widest.id != main.id && widest.fovDeg >= main.fovDeg * 1.25) {
                ultraWideCameraId = widest.id
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** 0.5x disponível como zoom da câmera principal (Android 11+, câmera lógica). */
    fun supportsLogicalUltraWide(): Boolean {
        val lower = try { rtmpCamera?.zoomRange?.lower } catch (e: Exception) { null }
        return lower != null && lower <= 0.65f
    }

    /** Há alguma forma de usar a ultra-angular neste aparelho? */
    fun hasUltraWide(): Boolean = supportsLogicalUltraWide() || ultraWideCameraId != null

    /** Troca para a lente ultra-angular sem derrubar a live. */
    fun useUltraWide(): Boolean {
        val id = ultraWideCameraId ?: return false
        return try {
            rtmpCamera?.switchCamera(id)
            usingUltraWide = true
            applySamsungOptimizations()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Volta para a câmera traseira principal. */
    fun useMainCamera(): Boolean {
        val id = mainBackCameraId ?: return false
        return try {
            rtmpCamera?.switchCamera(id)
            usingUltraWide = false
            applySamsungOptimizations()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Mostra o preview na tela. Chamar somente com a Surface da OpenGlView já válida. */
    fun attachView(view: OpenGlView) {
        try {
            rtmpCamera?.replaceView(view)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Solta a tela e mantém câmera, microfone e envio rodando em segundo plano. */
    fun detachView() {
        try {
            rtmpCamera?.replaceView(context.applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Pede reconexão RTMP sem derrubar os encoders. Retorna false se não for possível. */
    fun reTry(delayMs: Long, reason: String): Boolean {
        return try {
            rtmpCamera?.getStreamClient()?.reTry(delayMs, reason) ?: false
        } catch (e: Exception) {
            false
        }
    }

    /** Chamar a cada onNewBitrate (1x por segundo). */
    fun adaptBitrate(actualBps: Long) {
        if (!adaptiveBitrateEnabled || !isStreaming()) return
        try {
            val congestion = rtmpCamera?.getStreamClient()?.hasCongestion() ?: false
            bitrateAdapter.adaptBitrate(actualBps, congestion)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun applyAdaptedBitrate(adaptedTotal: Int) {
        val video = (adaptedTotal - audioBitrate).coerceIn(MIN_VIDEO_BITRATE, currentBitrate)
        try {
            rtmpCamera?.setVideoBitrateOnFly(video)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Prepara a câmera Samsung e o canal de áudio com máxima fidelidade.
     * @param is1080p Resolução 1080p Full HD vs 720p HD
     * @param bitrateKbps Bitrate em kbps (ex: 4000)
     * @param isChurchAudio True = Som da Igreja sem cancelador de ruído; False = Modo Mídia/Voz com filtro anti-eco
     */
    fun prepareStream(
        is1080p: Boolean = true,
        bitrateKbps: Int = 4000,
        isChurchAudio: Boolean = true
    ): Boolean {
        if (is1080p) {
            videoWidth = 1920
            videoHeight = 1080
            currentBitrate = bitrateKbps * 1000
        } else {
            videoWidth = 1280
            videoHeight = 720
            currentBitrate = 2500 * 1000
        }

        // 1. Configurar Codificador de Vídeo H.264 por Hardware MediaCodec
        val videoReady = rtmpCamera?.prepareVideo(
            videoWidth,
            videoHeight,
            currentFps,
            currentBitrate,
            keyframeInterval
        ) ?: false

        // 2. Configurar Áudio (Som da Igreja vs Mídia)
        // Som da Igreja: echoCanceler = false, noiseSuppressor = false (som natural sem cortes no louvor)
        // Som de Mídia / Voz: echoCanceler = true, noiseSuppressor = true (reduz ruídos e eco de playback)
        val echoCanceler = !isChurchAudio
        val noiseSuppressor = !isChurchAudio

        val audioReady = rtmpCamera?.prepareAudio(
            audioBitrate,
            audioSampleRate,
            true, // isStereo: TRUE (Preserva estéreo da mesa de som)
            echoCanceler,
            noiseSuppressor
        ) ?: false

        isPrepared = videoReady && audioReady
        if (isPrepared) {
            bitrateAdapter.setMaxBitrate(currentBitrate + audioBitrate)
        }
        return isPrepared
    }

    /**
     * Inicia a pré-visualização e ativa estabilização de imagem Samsung (OIS / EIS)
     */
    fun startPreview() {
        if (!isPrepared) return
        if (rtmpCamera?.isOnPreview == false) {
            try {
                // Inicia usando a câmera traseira principal (Camera 0 do Larix)
                rtmpCamera?.startPreview()
                applySamsungOptimizations()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Aplica calibrações exclusivas para sensores de celulares Samsung.
     */
    private fun applySamsungOptimizations() {
        try {
            // Ativa Estabilização Óptica (OIS) do hardware Samsung
            rtmpCamera?.enableOpticalVideoStabilization()
            // Ativa Estabilização Eletrônica adicional (EIS)
            rtmpCamera?.enableVideoStabilization()
            // Ativa Foco Automático Contínuo para Vídeo (não fica "pulsando" o foco no altar)
            rtmpCamera?.enableAutoFocus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Reaplica estabilização/foco depois que a câmera é reaberta (ao trocar de tela/segundo plano). */
    fun reapplyCameraSettings() {
        applySamsungOptimizations()
    }

    /**
     * Foco por Toque na Tela: Permite ao operador tocar no pastor/pregador
     * para focar e expor perfeitamente mesmo com luzes de palco fortes.
     */
    fun tapToFocus(event: MotionEvent): Boolean {
        return try {
            rtmpCamera?.tapToFocus(event)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Inicia a transmissão RTMP para o ponto de ingestão do YouTube.
     */
    fun startStream(rtmpEndpointWithKey: String) {
        if (!isPrepared) return
        if (rtmpCamera?.isStreaming == false) {
            try {
                rtmpCamera?.startStream(rtmpEndpointWithKey)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Encerra a transmissão RTMP.
     */
    fun stopStream() {
        if (rtmpCamera?.isStreaming == true) {
            rtmpCamera?.stopStream()
        }
    }

    /**
     * Alterna suavemente entre Câmera Traseira (0) e Frontal (1) sem derrubar o RTMP.
     */
    fun switchCamera() {
        try {
            usingUltraWide = false
            rtmpCamera?.switchCamera()
            applySamsungOptimizations()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Liga/desliga a lanterna/flash da câmera traseira.
     */
    fun toggleFlash(): Boolean {
        isFlashOn = !isFlashOn
        try {
            if (isFlashOn) {
                rtmpCamera?.enableLantern()
            } else {
                rtmpCamera?.disableLantern()
            }
        } catch (e: Exception) {
            isFlashOn = false
        }
        return isFlashOn
    }

    /**
     * Muta/Desmuta o áudio da live.
     */
    fun toggleMute(): Boolean {
        isAudioMuted = !isAudioMuted
        if (isAudioMuted) {
            rtmpCamera?.disableAudio()
        } else {
            rtmpCamera?.enableAudio()
        }
        return isAudioMuted
    }

    /**
     * Retorna o nível de zoom atual da câmera.
     */
    fun getZoom(): Float {
        return try {
            rtmpCamera?.zoom ?: 1.0f
        } catch (e: Exception) {
            1.0f
        }
    }

    /**
     * Retorna os limites suportados de zoom pelo sensor (ex: 1.0f a 8.0f).
     */
    fun getZoomRange(): android.util.Range<Float>? {
        return try {
            rtmpCamera?.zoomRange
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Ajuste de Zoom suave respeitando os limites da câmera Samsung.
     */
    fun setZoom(level: Float) {
        try {
            val range = rtmpCamera?.zoomRange
            val min = range?.lower ?: 1.0f
            val max = range?.upper ?: 5.0f
            val clamped = level.coerceIn(min, max)
            rtmpCamera?.setZoom(clamped)
        } catch (e: Exception) {
            try {
                rtmpCamera?.setZoom(level)
            } catch (ignored: Exception) {}
        }
    }

    fun isStreaming(): Boolean = rtmpCamera?.isStreaming == true
    fun isOnPreview(): Boolean = rtmpCamera?.isOnPreview == true
    fun isPrepared(): Boolean = isPrepared

    fun isFrontCamera(): Boolean = rtmpCamera?.isFrontCamera == true

    fun stopPreview() {
        if (rtmpCamera?.isOnPreview == true) {
            rtmpCamera?.stopPreview()
        }
    }

    fun release() {
        try { stopStream() } catch (e: Exception) { e.printStackTrace() }
        try { rtmpCamera?.stopPreview() } catch (e: Exception) { e.printStackTrace() }
        try { rtmpCamera?.stopCamera() } catch (e: Exception) { e.printStackTrace() }
        rtmpCamera = null
        isPrepared = false
    }
}

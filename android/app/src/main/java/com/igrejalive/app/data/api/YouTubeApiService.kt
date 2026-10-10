package com.igrejalive.app.data.api

import com.google.gson.Gson
import com.igrejalive.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Serviço que automatiza 100% o fluxo da YouTube Live Streaming API.
 * Elimina a necessidade de copiar tokens, links ou chaves de stream na mão.
 */
class YouTubeApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build(),
    private val gson: Gson = Gson()
) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val baseUrl = "https://www.googleapis.com/youtube/v3"

    /**
     * Cria e prepara uma live completa no YouTube com 1 única chamada:
     * 1. Cria o Broadcast (o evento de live)
     * 2. Cria o Stream RTMP (as configurações de vídeo 1080p ou 720p)
     * 3. Faz o bind (conecta o broadcast ao stream)
     * Retorna a URL RTMP completa para transmitir imediatamente.
     */
    suspend fun createAndBindLive(
        accessToken: String,
        title: String,
        description: String = "",
        privacy: String = "public", // "public", "unlisted", "private"
        resolution: String = "1080p", // "1080p", "720p"
        fps: String = "30fps"
    ): Result<YouTubeLiveResult> = withContext(Dispatchers.IO) {
        try {
            // Data atual em formato ISO 8601
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val scheduledTime = isoFormat.format(Date())

            // 1. Criar Broadcast
            val broadcastRequest = LiveBroadcastRequest(
                snippet = BroadcastSnippet(
                    title = title,
                    description = description,
                    scheduledStartTime = scheduledTime
                ),
                status = BroadcastStatus(privacyStatus = privacy),
                contentDetails = BroadcastContentDetails(
                    enableAutoStart = true,
                    enableAutoStop = false, // Impede que o YouTube mate a transmissão em quedas de sinal
                    recordFromStart = true
                )
            )

            val broadcastJson = gson.toJson(broadcastRequest)
            val createBroadcastReq = Request.Builder()
                .url("$baseUrl/liveBroadcasts?part=snippet,status,contentDetails")
                .addHeader("Authorization", "Bearer $accessToken")
                .post(broadcastJson.toRequestBody(jsonMediaType))
                .build()

            val broadcastResponse = client.newCall(createBroadcastReq).execute().use { resp ->
                val body = resp.body?.string() ?: throw IOException("Resposta vazia ao criar Broadcast")
                if (!resp.isSuccessful) {
                    throw IOException("Erro ao criar live: HTTP ${resp.code} - $body")
                }
                gson.fromJson(body, LiveBroadcastResponse::class.java)
            }

            val broadcastId = broadcastResponse.id

            // 2. Criar Stream RTMP (com resolução travada 1080p ou 720p)
            val streamRequest = LiveStreamRequest(
                snippet = StreamSnippet(title = "Stream Culto - $title"),
                cdn = StreamCdn(
                    frameRate = fps,
                    ingestionType = "rtmp",
                    resolution = resolution
                )
            )

            val streamJson = gson.toJson(streamRequest)
            val createStreamReq = Request.Builder()
                .url("$baseUrl/liveStreams?part=snippet,cdn")
                .addHeader("Authorization", "Bearer $accessToken")
                .post(streamJson.toRequestBody(jsonMediaType))
                .build()

            val streamResponse = client.newCall(createStreamReq).execute().use { resp ->
                val body = resp.body?.string() ?: throw IOException("Resposta vazia ao criar Stream")
                if (!resp.isSuccessful) {
                    throw IOException("Erro ao configurar stream: HTTP ${resp.code} - $body")
                }
                gson.fromJson(body, LiveStreamResponse::class.java)
            }

            val streamId = streamResponse.id
            val ingestionInfo = streamResponse.cdn?.ingestionInfo
                ?: throw IOException("Servidor do YouTube não retornou informações de ingestão RTMP")

            // 3. Vincular (Bind) o Broadcast ao Stream
            val bindReq = Request.Builder()
                .url("$baseUrl/liveBroadcasts/bind?id=$broadcastId&part=id,contentDetails&streamId=$streamId")
                .addHeader("Authorization", "Bearer $accessToken")
                .post("{}".toRequestBody(jsonMediaType))
                .build()

            client.newCall(bindReq).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val body = resp.body?.string()
                    throw IOException("Erro ao vincular transmissão ao stream: HTTP ${resp.code} - $body")
                }
            }

            val rtmpBase = ingestionInfo.ingestionAddress
            val streamKey = ingestionInfo.streamName
            val fullRtmp = if (rtmpBase.endsWith("/")) "$rtmpBase$streamKey" else "$rtmpBase/$streamKey"

            val result = YouTubeLiveResult(
                broadcastId = broadcastId,
                streamId = streamId,
                rtmpUrl = rtmpBase,
                streamKey = streamKey,
                fullRtmpTarget = fullRtmp,
                watchUrl = "https://www.youtube.com/watch?v=$broadcastId"
            )

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Encerra a live no YouTube manualmente caso o autoStop não tenha disparado.
     */
    suspend fun endLiveBroadcast(
        accessToken: String,
        broadcastId: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$baseUrl/liveBroadcasts/transition?broadcastStatus=complete&id=$broadcastId&part=id,status")
                .addHeader("Authorization", "Bearer $accessToken")
                .post("{}".toRequestBody(jsonMediaType))
                .build()

            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(IOException("Erro ao finalizar live: HTTP ${resp.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

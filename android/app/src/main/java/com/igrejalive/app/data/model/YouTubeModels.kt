package com.igrejalive.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Modelos de dados para comunicação com a YouTube Data API v3 (Live Streaming).
 */

// 1. Criação de Broadcast (Transmissão)
data class LiveBroadcastRequest(
    val snippet: BroadcastSnippet,
    val status: BroadcastStatus,
    val contentDetails: BroadcastContentDetails
)

data class BroadcastSnippet(
    val title: String,
    val description: String = "",
    val scheduledStartTime: String
)

data class BroadcastStatus(
    val privacyStatus: String // "public", "unlisted", "private"
)

data class BroadcastContentDetails(
    val enableAutoStart: Boolean = true,
    val enableAutoStop: Boolean = true,
    val recordFromStart: Boolean = true
)

data class LiveBroadcastResponse(
    val id: String,
    val snippet: BroadcastSnippetResponse?,
    val status: BroadcastStatusResponse?
)

data class BroadcastSnippetResponse(
    val title: String
)

data class BroadcastStatusResponse(
    val lifeCycleStatus: String
)

// 2. Criação de Stream (Ponto de Ingestão RTMP)
data class LiveStreamRequest(
    val snippet: StreamSnippet,
    val cdn: StreamCdn
)

data class StreamSnippet(
    val title: String
)

data class StreamCdn(
    val frameRate: String = "30fps",
    val ingestionType: String = "rtmp",
    val resolution: String = "1080p" // "1080p" ou "720p"
)

data class LiveStreamResponse(
    val id: String,
    val cdn: StreamCdnResponse?
)

data class StreamCdnResponse(
    val ingestionInfo: IngestionInfo?
)

data class IngestionInfo(
    val ingestionAddress: String, // ex: rtmp://a.rtmp.youtube.com/live2
    val streamName: String        // chave da stream (chave secreta)
)

// Resultado final pronto para transmissão
data class YouTubeLiveResult(
    val broadcastId: String,
    val streamId: String,
    val rtmpUrl: String,
    val streamKey: String,
    val fullRtmpTarget: String, // rtmpUrl + "/" + streamKey
    val watchUrl: String        // https://www.youtube.com/watch?v={broadcastId}
)

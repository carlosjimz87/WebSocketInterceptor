package com.carlosjimz87.websocketinterceptor5
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import kotlin.time.Duration

class WebSocketManager(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(Duration.parse("200s"))
        .build(),
    private val url: String = "ws://10.0.2.2:8000/ws"
) {

    val events =
        MutableSharedFlow<String>(replay = 20, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    @Volatile private var socket: WebSocket? = null

    fun connect(token: String) {
        disconnect()

        val request = Request.Builder()
            .url(url)
            // —— The important bit: header on the HTTP Upgrade handshake
            .addHeader("Authorization", "Bearer $token")
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                socket = webSocket
                events.tryEmit("✅ OPEN  (code=${response.code})")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                events.tryEmit("📥 $text")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                events.tryEmit("↘️  CLOSING  ($code/$reason)")
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                events.tryEmit("🔒 CLOSED  ($code/$reason)")
                socket = null
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                events.tryEmit("❌ FAILURE: ${t.message}  resp=${response?.code}")
                socket = null
            }
        }

        client.newWebSocket(request, listener)
    }

    fun send(text: String) {
        val ok = socket?.send(text) ?: false
        if (!ok) events.tryEmit("⚠️ SEND failed (not connected)")
        else events.tryEmit("📤 $text")
    }

    fun disconnect() {
        socket?.close(1000, "bye")
        socket = null
    }
}
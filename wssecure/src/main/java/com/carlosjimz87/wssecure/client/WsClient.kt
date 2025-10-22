package com.carlosjimz87.wssecure.client

import com.carlosjimz87.wssecure.data.envelope.WsEnvelopeSigner
import com.carlosjimz87.wssecure.data.model.WsConfig
import com.carlosjimz87.wssecure.data.plugin.WsPlugin
import com.carlosjimz87.wssecure.presentation.events.WsEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class WsClient(
    private val config: WsConfig,
    private val client: OkHttpClient,
    private val plugins: List<WsPlugin>,
    private val signer: WsEnvelopeSigner? = null
) {
    val events = MutableSharedFlow<WsEvent>(replay = 0)
    @Volatile private var socket: WebSocket? = null

    fun connect() {
        disconnect()
        // 1) URL transforms
        val finalUrl = plugins.filter { it.isEnabled() }
            .fold(config.url) { acc, p -> p.willOpen(acc) }
        // 2) Request + intercept
        var req = Request.Builder().url(finalUrl).build()
        req = plugins.fold(req) { r, p -> p.intercept(r) }

        val listener = object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, resp: Response) {
                socket = ws
                events.tryEmit(WsEvent.Open(resp.code))
                // fire-and-forget plugin hook
                GlobalScope.launch(Dispatchers.IO) { plugins.forEach { it.didOpen(resp) } }
            }
            override fun onMessage(ws: WebSocket, text: String) {
                events.tryEmit(WsEvent.Message(signer?.unwrapIncoming(text) ?: text))
            }
            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                events.tryEmit(WsEvent.Closing(code, reason)); ws.close(code, reason)
            }
            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                events.tryEmit(WsEvent.Closed(code, reason)); socket = null
            }
            override fun onFailure(ws: WebSocket, t: Throwable, resp: Response?) {
                events.tryEmit(WsEvent.Failure(t.message, resp?.code)); socket = null
            }
        }
        client.newWebSocket(req, listener)
    }

    fun sendJson(json: String): Boolean {
        val payload = signer?.wrapOutgoing(json) ?: json
        return (socket?.send(payload) ?: false).also { ok ->
            if (!ok) events.tryEmit(WsEvent.Failure("Not connected", null))
        }
    }

    fun disconnect() { socket?.close(1000, "bye"); socket = null }
}


fun buildWsOkHttp(config: WsConfig): OkHttpClient =
    OkHttpClient.Builder()
        .connectTimeout(config.connectTimeout, TimeUnit.MILLISECONDS)
        .pingInterval(config.pingInterval, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(config.retryOnFailure)
        .build()
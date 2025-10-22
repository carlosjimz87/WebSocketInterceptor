package com.carlosjimz87.wssecure.client

import android.util.Log
import com.carlosjimz87.wssecure.data.envelope.WsEnvelopeSigner
import com.carlosjimz87.wssecure.data.model.WsConfig
import com.carlosjimz87.wssecure.data.plugin.WsPlugin
import com.carlosjimz87.wssecure.presentation.events.WsEvent
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BufferOverflow
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
    private val TAG = "WsClient"

    val events = MutableSharedFlow<WsEvent>(
        replay = 1,                 // keep the latest event for late collectors
        extraBufferCapacity = 16,   // absorb bursts
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    @Volatile private var socket: WebSocket? = null

    @OptIn(DelicateCoroutinesApi::class)
    fun connect() {
        Log.d(TAG, "connect() called — disconnecting any existing socket first")
        disconnect()

        // ---- URL construction with plugins
        Log.d(TAG, "Base URL from config: ${config.wsUrl}")
        val finalUrl: String = plugins
            .filter { it.isEnabled() }
            .fold(config.wsUrl) { acc, p ->
                val newUrl = p.willOpen(acc)
                Log.d(TAG, "Plugin[${p.id}] transformed URL: $acc → $newUrl")
                newUrl
            }

        // ---- Request building with plugins
        var req = Request.Builder().url(finalUrl).build()
        Log.d(TAG, "Initial request built for $finalUrl")
        req = plugins.fold(req) { r, p ->
            val newReq = p.intercept(r)
            Log.d(TAG, "Plugin[${p.id}] intercepted request (headers now: ${newReq.headers})")
            newReq
        }

        Log.i(TAG, "Final WebSocket request URL: ${req.url}")
        Log.i(TAG, "Final headers: ${req.headers}")

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "onOpen() — code=${response.code}, message=${response.message}")
                socket = webSocket
                events.tryEmit(WsEvent.Open(response.code))
                GlobalScope.launch(Dispatchers.IO) {
                    plugins.forEach {
                        try {
                            it.didOpen(response)
                            Log.d(TAG, "Plugin[${it.id}] didOpen() executed OK")
                        } catch (e: Exception) {
                            Log.e(TAG, "Plugin[${it.id}] didOpen() failed", e)
                        }
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val msg = signer?.unwrapIncoming(text) ?: text
                Log.d(TAG, "onMessage() — raw=${text.take(80)} parsed=${msg.take(80)}")
                events.tryEmit(WsEvent.Message(msg))
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "onClosing() — code=$code reason=$reason")
                events.tryEmit(WsEvent.Closing(code, reason))
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "onClosed() — code=$code reason=$reason")
                events.tryEmit(WsEvent.Closed(code, reason))
                socket = null
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "onFailure() — ${t.message} resp=${response?.code}", t)
                events.tryEmit(WsEvent.Failure(t.message, response?.code))
                socket = null
            }
        }

        Log.d(TAG, "Creating new WebSocket connection now…")
        client.newWebSocket(req, listener)
    }

    fun sendJson(json: String): Boolean {
        val payload = signer?.wrapOutgoing(json) ?: json
        Log.d(TAG, "sendJson() — sending ${payload.take(100)}")
        return (socket?.send(payload) ?: false).also { ok ->
            if (!ok) {
                Log.w(TAG, "sendJson() failed — socket not connected")
                events.tryEmit(WsEvent.Failure("Not connected", null))
            }
        }
    }

    fun disconnect() {
        if (socket != null) {
            Log.d(TAG, "disconnect() — closing active socket")
            socket?.close(1000, "bye")
            socket = null
        } else {
            Log.d(TAG, "disconnect() — no active socket")
        }
    }
}

fun buildWsOkHttp(config: WsConfig): OkHttpClient {
    val TAG = "WsOkHttp"
    Log.d(TAG, "Building OkHttpClient with: timeout=${config.connectTimeout}, ping=${config.pingInterval}, retry=${config.retryOnFailure}")
    return OkHttpClient.Builder()
        .connectTimeout(config.connectTimeout, TimeUnit.MILLISECONDS)
        .pingInterval(config.pingInterval, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(config.retryOnFailure)
        .build()
}
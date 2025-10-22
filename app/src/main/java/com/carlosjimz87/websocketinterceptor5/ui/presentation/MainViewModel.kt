package com.carlosjimz87.websocketinterceptor5.ui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carlosjimz87.wssecure.client.WsClient
import com.carlosjimz87.wssecure.presentation.events.WsEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainViewModel(
    private val ws: WsClient
) : ViewModel() {

    enum class ConnState { Disconnected, Connecting, Open, Closing, Closed, Failure }

    private val _state = MutableStateFlow(ConnState.Disconnected)
    val state: StateFlow<ConnState> = _state

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log

    init {
        viewModelScope.launch {
            ws.events.collect { ev ->
                val line = when (ev) {
                    is WsEvent.Open    -> "✅ OPEN (${ev.code})".also { _state.value = ConnState.Open }
                    is WsEvent.Message -> "📥 ${ev.text}"
                    is WsEvent.Closing -> "↘️ CLOSING ${ev.code}/${ev.reason}".also { _state.value = ConnState.Closing }
                    is WsEvent.Closed  -> "🔒 CLOSED ${ev.code}/${ev.reason}".also { _state.value = ConnState.Closed }
                    is WsEvent.Failure -> "❌ FAILURE ${ev.message ?: "unknown"} resp=${ev.code}".also { _state.value = ConnState.Failure }
                }
                _log.update { it + line }
            }
        }
    }

    fun connect() {
        _state.value = ConnState.Connecting   // we set this proactively
        ws.connect()
        _log.update { it + "⏳ connecting…" }
    }

    fun send(msg: String) {
        val ok = ws.sendJson("""{"text":${JSONObject.quote(msg)}}""")
        if (ok) _log.update { it + "📤 $msg" }
    }

    fun disconnect() {
        ws.disconnect()
        _state.value = ConnState.Disconnected
        _log.update { it + "👋 disconnect requested" }
    }
}
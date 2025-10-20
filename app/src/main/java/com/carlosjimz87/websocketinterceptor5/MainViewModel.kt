package com.carlosjimz87.websocketinterceptor5

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val ws = WebSocketManager()

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log.asStateFlow()

    init {
        viewModelScope.launch {
            ws.events.collect { line ->
                _log.update { it + line }
            }
        }
    }

    fun connect(token: String) = ws.connect(token)
    fun send(msg: String) = ws.send(msg)
    fun disconnect() = ws.disconnect()
}
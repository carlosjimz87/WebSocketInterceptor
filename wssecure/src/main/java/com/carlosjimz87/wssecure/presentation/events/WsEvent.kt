package com.carlosjimz87.wssecure.presentation.events

sealed interface WsEvent {
    data class Open(val code: Int) : WsEvent
    data class Message(val text: String) : WsEvent
    data class Closing(val code: Int, val reason: String) : WsEvent
    data class Closed(val code: Int, val reason: String) : WsEvent
    data class Failure(val message: String?, val code: Int?) : WsEvent
}
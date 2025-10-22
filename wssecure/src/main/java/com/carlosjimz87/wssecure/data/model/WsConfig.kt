package com.carlosjimz87.wssecure.data.model

data class WsConfig(
    val wsUrl: String,
    val connectTimeout: Long = 10000L,
    val pingInterval: Long = 20000L,
    val retryOnFailure: Boolean = true
)
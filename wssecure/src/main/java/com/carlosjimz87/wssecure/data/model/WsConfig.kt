package com.carlosjimz87.wssecure.data.model

import okhttp3.HttpUrl

data class WsConfig(
    val url: HttpUrl,
    val connectTimeout: Long = 10000L,
    val pingInterval: Long = 20000L,
    val retryOnFailure: Boolean = true
)
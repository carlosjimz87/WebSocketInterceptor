package com.carlosjimz87.wssecure.data.plugin

import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.Response

interface WsPlugin {
    val id: String
    fun isEnabled(): Boolean = true
    fun willOpen(url: HttpUrl): HttpUrl = url
    fun intercept(request: Request): Request = request
    suspend fun didOpen(response: Response) {}
}
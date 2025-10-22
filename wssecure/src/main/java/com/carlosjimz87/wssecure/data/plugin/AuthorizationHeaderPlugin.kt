package com.carlosjimz87.wssecure.data.plugin

import com.carlosjimz87.wssecure.data.provider.TokenProvider
import okhttp3.Request

class AuthorizationHeaderPlugin(
    private val tokenProvider: TokenProvider,
    override val id: String = "auth-header"
) : WsPlugin {
    override fun intercept(request: Request): Request {
        val token = tokenProvider.token() ?: return request
        return request.newBuilder().header("Authorization", "Bearer $token").build()
    }
}
package com.carlosjimz87.wssecure.data.plugin

import com.carlosjimz87.wssecure.data.provider.TokenProvider
import okhttp3.HttpUrl

class QueryTokenPlugin(
    private val tokenProvider: TokenProvider,
    override val id: String = "query-token"
) : WsPlugin {
    override fun willOpen(url: HttpUrl): HttpUrl {
        val t = tokenProvider.token() ?: return url
        return url.newBuilder().addQueryParameter("token", t).build()
    }
}
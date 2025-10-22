package com.carlosjimz87.wssecure.data.plugin

import androidx.core.net.toUri
import com.carlosjimz87.wssecure.data.provider.TokenProvider

class QueryTokenPlugin(
    private val tokenProvider: TokenProvider,
    override val id: String = "query-token"
) : WsPlugin {
    override fun willOpen(url: String): String {
        val t = tokenProvider.token() ?: return url
        val newUri = url.toUri().buildUpon()
            .appendQueryParameter("token", t)
            .build()
        return newUri.toString()
    }
}
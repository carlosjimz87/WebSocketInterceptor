package com.carlosjimz87.wssecure.di

import com.carlosjimz87.wssecure.client.WsClient
import com.carlosjimz87.wssecure.client.buildWsOkHttp
import com.carlosjimz87.wssecure.data.envelope.HmacEnvelopeSigner
import com.carlosjimz87.wssecure.data.envelope.WsEnvelopeSigner
import com.carlosjimz87.wssecure.data.plugin.AuthorizationHeaderPlugin
import com.carlosjimz87.wssecure.data.plugin.QueryTokenPlugin
import com.carlosjimz87.wssecure.data.plugin.WsPlugin
import com.carlosjimz87.wssecure.data.provider.TokenProvider
import org.koin.core.qualifier.named
import org.koin.dsl.module

val WsSecureModule = module {

    // App must provide these two in its own module:
    // single<TokenProvider> { ... }
    // single<WsConfig> { ... }

    // Default plugins
    factory<WsPlugin>(named("auth-header")) { AuthorizationHeaderPlugin(get()) }
    factory<WsPlugin>(named("query-token")) { QueryTokenPlugin(get()) }

    // Compose plugin list; apps can override by redefining this binding
    factory<List<WsPlugin>> {
        listOf(get<WsPlugin>(named("auth-header")))
    }

    // Optional message signer (demo: uses token as secret)
    single<WsEnvelopeSigner> { HmacEnvelopeSigner { get<TokenProvider>().token() } }

    // Dedicated OkHttp
    single { buildWsOkHttp(get()) }

    // The client
    single { WsClient(config = get(), client = get(), plugins = get(), signer = get()) }
}
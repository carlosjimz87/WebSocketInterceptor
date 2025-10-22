package com.carlosjimz87.wssecure.di

import com.carlosjimz87.wssecure.Constants
import com.carlosjimz87.wssecure.client.WsClient
import com.carlosjimz87.wssecure.client.buildWsOkHttp
import com.carlosjimz87.wssecure.data.envelope.HmacEnvelopeSigner
import com.carlosjimz87.wssecure.data.envelope.WsEnvelopeSigner
import com.carlosjimz87.wssecure.data.model.WsConfig
import com.carlosjimz87.wssecure.data.plugin.AuthorizationHeaderPlugin
import com.carlosjimz87.wssecure.data.plugin.QueryTokenPlugin
import com.carlosjimz87.wssecure.data.plugin.WsPlugin
import com.carlosjimz87.wssecure.data.provider.TokenProvider
import org.koin.core.qualifier.named
import org.koin.dsl.module

val WsSecureModule = module {
    // Configuration
    single {
        WsConfig(wsUrl = Constants.BASE_URL)
    }
    // Default plugins
    factory<WsPlugin>(named("auth-header")) { AuthorizationHeaderPlugin(get()) }
    factory<WsPlugin>(named("query-token")) { QueryTokenPlugin(get()) }

    // Compose plugin list; apps can override by redefining this binding
    factory<List<WsPlugin>> {
        listOf(get<WsPlugin>(named("auth-header")))
    }

    // Optional message signer (demo: uses token as secret)
    // Note: The app module will need to provide a TokenProvider implementation
    single<WsEnvelopeSigner> { HmacEnvelopeSigner { get<TokenProvider>().token() } }

    // The client
    single {
        WsClient(
            config = get(),
            client = buildWsOkHttp(get()),
            plugins = get(),
            signer = getOrNull() // Use getOrNull in case the app doesn't define a signer
        )
    }
}
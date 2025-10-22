package com.carlosjimz87.websocketinterceptor5.di

import com.carlosjimz87.websocketinterceptor5.ui.presentation.MainViewModel
import com.carlosjimz87.websocketinterceptor5.providers.AuthStore
import com.carlosjimz87.wssecure.data.provider.TokenProvider
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val AppModule = module {
    single<TokenProvider> {
        object : TokenProvider { override fun token() = AuthStore.currentAccessToken }
    }
    viewModel { MainViewModel(get()) }
}
package com.carlosjimz87.websocketinterceptor5

import android.app.Application
import com.carlosjimz87.websocketinterceptor5.di.AppModule
import com.carlosjimz87.wssecure.di.WsSecureModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(
                AppModule,
                WsSecureModule
            )
        }
    }
}
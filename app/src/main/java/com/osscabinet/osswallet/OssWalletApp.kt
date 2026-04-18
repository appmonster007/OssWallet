package com.osscabinet.ossWallet

import android.app.Application
import com.osscabinet.ossWallet.di.AppContainer
import com.osscabinet.ossWallet.ui.widget.WidgetUpdateWorker

class OssWalletApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        WidgetUpdateWorker.enqueuePeriodicWork(this)
    }
}

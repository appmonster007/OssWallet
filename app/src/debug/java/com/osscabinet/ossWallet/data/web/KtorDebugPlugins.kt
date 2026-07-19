package com.osscabinet.ossWallet.data.web

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging

fun Application.installDebugPlugins() {
    install(CallLogging)
}

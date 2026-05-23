package com.osscabinet.ossWallet.domain.model.web

sealed class WebServerState {
    object Stopped : WebServerState()
    object Starting : WebServerState()
    data class Running(
        val ip: String,
        val port: Int,
        val accessCode: String,
        val accessUrl: String = "http://$ip:$port"
    ) : WebServerState()
    data class Error(val message: String) : WebServerState()
}

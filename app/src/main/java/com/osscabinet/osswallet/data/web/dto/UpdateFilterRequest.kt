package com.osscabinet.ossWallet.data.web.dto

data class UpdateFilterRequest(
    val name: String? = null,
    val filterData: CreateFilterRequest? = null
)

package com.osscabinet.gleemanager.domain.model

data class SharedWallet(
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val memberIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

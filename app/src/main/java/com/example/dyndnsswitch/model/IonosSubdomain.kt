package com.example.dyndnsswitch.model

import kotlinx.serialization.Serializable

data class IonosSubdomain(
    override val name: String,
    override val ipv4: String,
    override val ipv6: String,
) : Subdomain

// Helper classes for decoding JSON responses
@Serializable
data class IonosResponse(
    val name: String,
    val type: String,
    val id: String,
    val records: List<IonosEntry>
)

@Serializable
data class IonosEntry(
    val name: String,
    val rootName: String,
    val type: String,
    val content: String,
    val changeDate: String,
    val ttl: Int,
    val disabled: Boolean,
    val id: String
)
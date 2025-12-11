package com.example.dyndnsswitch.data

import com.example.dyndnsswitch.model.Subdomain

interface Provider {
    val zoneID: String
    val apiKey: String
    val domain: List<String>
    val apiURL: String

    suspend fun getSubdomains(): List<Subdomain>
    suspend fun setSubdomain(subdomain: Subdomain, ipv4: String, ipv6: String)
}
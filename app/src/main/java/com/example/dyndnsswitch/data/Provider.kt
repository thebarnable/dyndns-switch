package com.example.dyndnsswitch.data

import com.example.dyndnsswitch.model.Subdomain

interface Provider {
    val zoneID: String
    val apiKey: String
    val domain: String
    val apiURL: String

    suspend fun getSubdomains(): List<Subdomain>
    suspend fun setSubdomain(subdomain: Subdomain)
}
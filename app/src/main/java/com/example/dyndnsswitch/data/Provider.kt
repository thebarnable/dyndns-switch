package com.example.dyndnsswitch.data

import com.example.dyndnsswitch.model.Subdomain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface Provider {
    val apiKey: String
    val apiURL: String

    //private val _subdomains = MutableStateFlow<Map<Provider, List<Subdomain>>>(emptyMap()) // Mutable inside repo
    val subdomains: StateFlow<List<Subdomain>>

    // First initialization, should fetch things like domains and zone IDs
    suspend fun initProvider()
    // Updates 'subdomains' list by sending GET request to provider
    suspend fun updateSubdomains()
    // Return 'subdomains' list TODO: necessary?
    //suspend fun getSubdomains(): List<Subdomain>
    // Override DNS entries for given subdomains
    suspend fun setSubdomainsFromNames(subdomainList: List<String>, ipv4: String, ipv6: String)
    suspend fun setSubdomains(subdomainList: List<Subdomain>, ipv4: String, ipv6: String)
}
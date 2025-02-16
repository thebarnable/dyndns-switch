package com.example.dyndnsswitch.data

import android.util.Log
import com.example.dyndnsswitch.model.Subdomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest

class ProviderRepository(
    private val providers: List<Provider>
) {
    //private var subdomains = mutableMapOf<Provider, List<Subdomain>>()
    private val _subdomains = MutableStateFlow<Map<Provider, List<Subdomain>>>(emptyMap()) // Mutable inside repo
    val subdomains: StateFlow<Map<Provider, List<Subdomain>>> = _subdomains.asStateFlow() // Read-only for consumers

    suspend fun fetchSubdomains() {
        /*val subdomains = mutableMapOf<Provider, List<Subdomain>>()
        providers.forEach() { provider ->
            subdomains[provider] = provider.getSubdomains()
        }
        return subdomains*/
        _subdomains.value = providers.associateWith { it.getSubdomains() }.toMutableMap()
    }

    fun resolve(domain: String, ipv6: Boolean = false): String {
        var ip = ""
        // Search for 'domain name'->'ip' mapping in all subdomain lists in all providers
        providers.forEach() { provider ->
            _subdomains.value[provider]?.forEach() { subdomain ->
                if (subdomain.name == domain) {
                    if (ipv6)
                        ip = subdomain.ipv6
                    else
                        ip = subdomain.ipv4
                }
            }
        }
        if (ip == "")
            throw Exception("Failed to resolve ${domain} (ipv6 = ${ipv6}): not in list of existing subdomains: ${subdomains.value}")

        Log.d("DNS", "Resolved $domain to $ip")
        return ip
    }

    fun getSubdomainsOfServer(serverIP: String): MutableList<Subdomain> {
        Log.d("MAIN", "Requesting subdomains for server $serverIP")
        var serverSubdomains = mutableListOf<Subdomain>()
        providers.forEach() { provider ->
            _subdomains.value[provider]?.forEach() { subdomain ->
                if (subdomain.ipv4 == serverIP || subdomain.ipv6 == serverIP) {
                    serverSubdomains += subdomain
                    Log.d("DNS", "Found ${subdomain.name}")
                }
            }
        }
        if (serverSubdomains.size == 0)
            Log.w("MAIN", "Not subdomains found on server $serverIP")
        return serverSubdomains
    }
}
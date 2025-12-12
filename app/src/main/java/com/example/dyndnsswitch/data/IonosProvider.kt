package com.example.dyndnsswitch.data

import android.util.Log
import com.example.dyndnsswitch.model.IonosDYNDNSResponse
import com.example.dyndnsswitch.model.IonosEntry
import com.example.dyndnsswitch.model.IonosResponse
import com.example.dyndnsswitch.model.IonosSubdomain
import com.example.dyndnsswitch.model.IonosZones
import com.example.dyndnsswitch.model.Subdomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class IonosProvider(
    override val apiKey: String,
    override val apiURL: String = "https://api.hosting.ionos.com"
) : Provider {
    var domains: List<String> = emptyList()
    var zones: List<String> = emptyList()

    private val _subdomains = MutableStateFlow<List<Subdomain>>(emptyList())
    override val subdomains: StateFlow<List<Subdomain>> = _subdomains.asStateFlow()

    private val getDynDnsURL = "$apiURL/dns/v1/dyndns"

    override suspend fun initProvider() {
        withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            Log.d("Init", "Building GET request to retrieve domains and zone IDs")
            val request = Request.Builder()
                .url("https://api.hosting.ionos.com/dns/v1/zones")
                .get()
                .addHeader("accept", "application/json")
                .addHeader("X-API-Key", apiKey)
                .build()
            Log.d("Init", "Executing GET request to retrieve domains and zone IDs")
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    try {
                        val responseStr = response.body?.string() ?: ""
                        Log.d("DNS", "Got response: ${responseStr}")
                        if (responseStr == "") {
                            throw Exception("Response was successful, but is empty")
                        }
                        val responseDecoded: List<IonosZones> = Json.decodeFromString(responseStr)
                        domains = responseDecoded.map { it.name }
                        zones = responseDecoded.map { it.id }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e("DNS", "JSON decoding error: ${e.localizedMessage}")
                        throw Exception("JSON decoding error: ${e.localizedMessage}")
                    }
                } else {
                    throw Exception("HTTP error: ${response.code}")
                }
            }
        }
    }

    override suspend fun setSubdomainsFromNames(subdomainList: List<String>, ipv4: String, ipv6: String) {
        if(subdomainList.isEmpty()) {
            Log.w("DNS", "setSubdomainsFromNames: subdomainList is empty!")
            return
        }
        if(subdomains.value.isEmpty()) {
            Log.w("DNS", "setSubdomainsFromNames: subdomains is empty!")
            return
        }

        val subdomainsConverted = mutableListOf<Subdomain>()
        subdomainList.forEach() { subdomainName ->
            subdomains.value.forEach() { subdomain ->
                if(subdomainName == subdomain.name) {
                    subdomainsConverted += subdomain
                }
            }
        }
        setSubdomains(subdomainsConverted, ipv4, ipv6)
    }

    override suspend fun setSubdomains(subdomainList: List<Subdomain>, ipv4: String, ipv6: String) {
        if(subdomainList.isEmpty()) {
            Log.w("DNS", "setSubdomains: subdomainList is empty!")
            return
        }

        // get DynDNS Update URLs
        var updateURL: String = ""
        val client = OkHttpClient()
        val subdomainNames = subdomainList.joinToString(separator="\",\n\"") {it.name}
        Log.d("DNS", "Building POST request with $getDynDnsURL for $subdomainNames")
        val content: String = "{\n" +
                "  \"domains\": [\n\"" +
                subdomainNames +
                "\"\n" +
                "  ],\n" +
                "  \"description\": \"My DynamicDns\"\n" +
                "}"
        Log.d("DNS", "Content: $content")
        /*val request = Request.Builder()
            .url(getDynDnsURL)
            .post(content.toRequestBody())
            .addHeader("accept", "application/json")
            .addHeader("X-API-Key", apiKey)
            .addHeader("Content-Type", "application/json")
            .build()
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                try {
                    val responseStr = response.body?.string() ?: ""
                    Log.d("DNS", "Got response: ${responseStr}")
                    if (responseStr == "") {
                        throw Exception("Response was successful, but is empty")
                    }
                    val responseDecoded = Json.decodeFromString<IonosDYNDNSResponse>(responseStr)
                    updateURL = responseDecoded.updateUrl
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("DNS", "JSON decoding error: ${e.localizedMessage}")
                    throw Exception("JSON decoding error: ${e.localizedMessage}")
                }
            } else {
                throw Exception("HTTP error: ${response.code}")
            }
        }

        Log.d("DNS", "Building GET request to update DNS entry")
        val url = updateURL +
                if(ipv4 != "") "&ipv4=" + ipv4 else "" +
                if(ipv6 != "") "&ipv6=" + ipv6 else ""
        val updateRequest = Request.Builder()
            .url(url)
            .get()
            .build()
        Log.d("DNS", "Executing GET request")*/
        /*client.newCall(updateRequest).execute().use { response ->
            if (response.isSuccessful) {
                Log.d("DNS", "Successfully updated DNS entry for ${subdomain.name}")
            } else {
                throw Exception("HTTP error: ${response.code}")
            }
        }*/
    }

    override suspend fun updateSubdomains() {
        withContext(Dispatchers.IO) {
            _subdomains.value = emptyList<Subdomain>()
            zones.forEach() { zone ->
                val getDomainsURL = "$apiURL/dns/v1/zones/$zone?recordType=A%2CAAAA"
                val client = OkHttpClient()
                Log.d("DNS", "Building GET request for $getDomainsURL")
                val request = Request.Builder()
                    .url(getDomainsURL)
                    .get()
                    .addHeader("accept", "application/json")
                    .addHeader("X-API-Key", apiKey)
                    .build()
                Log.d("DNS", "Executing GET request")
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        try {
                            val responseStr = response.body?.string() ?: ""
                            Log.d("DNS", "Got response: ${responseStr}")
                            if (responseStr == "") {
                                throw Exception("Response was successful, but is empty")
                            }
                            val responseDecoded = Json.decodeFromString<IonosResponse>(responseStr)
                            _subdomains.value += ionosEntryToSubdomain(responseDecoded.records)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.e("DNS", "JSON decoding error: ${e.localizedMessage}")
                            throw Exception("JSON decoding error: ${e.localizedMessage}")
                        }
                    } else {
                        throw Exception("HTTP error: ${response.code}")
                    }
                }
            }
        }
    }

    private fun ionosEntryToSubdomain(entries: List<IonosEntry>): List<IonosSubdomain> {
        Log.d("DNS", "Mapping entries to subdomain list. Entries:")
        val subdomains = mutableListOf<IonosSubdomain>()
        for (entry in entries) {
            Log.d("DNS", "Entry: ${entry}")
            val existingSubdomain = subdomains.find { subdomain -> subdomain.name == entry.name }

            // most domains have two ionos entries, one for ipv4 and one for ipv6
            // -> if current entry is already in subdomain list, we have added it for the respective other ip version before
            // -> add other ip address now
            if (existingSubdomain != null) { // entry exists
                if (existingSubdomain.ipv4 == "") {
                    val updatedSubdomain = existingSubdomain.copy(ipv4 = entry.content)
                    subdomains[subdomains.indexOf(existingSubdomain)] = updatedSubdomain
                } else if (existingSubdomain.ipv6 == "") {
                    val updatedSubdomain = existingSubdomain.copy(ipv6 = entry.content)
                    subdomains[subdomains.indexOf(existingSubdomain)] = updatedSubdomain
                } else {
                    Log.w("DNS", "Found duplicate domain entry? This should never happen")
                }
            } else { // entry new
                // append new entry to list
                if (entry.type == "A") {
                    subdomains.add(
                        IonosSubdomain(
                            name = entry.name,
                            ipv4 = entry.content,
                            ipv6 = ""
                        )
                    )
                } else {
                    subdomains.add(
                        IonosSubdomain(
                            name = entry.name,
                            ipv4 = "",
                            ipv6 = entry.content
                        )
                    )
                }
            }
        }
        return subdomains
    }
}
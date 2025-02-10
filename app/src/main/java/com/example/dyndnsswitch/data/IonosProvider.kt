package com.example.dyndnsswitch.data

import android.util.Log
import com.example.dyndnsswitch.model.IonosEntry
import com.example.dyndnsswitch.model.IonosResponse
import com.example.dyndnsswitch.model.IonosSubdomain
import com.example.dyndnsswitch.model.Subdomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

class IonosProvider(
    override val zoneID: String,
    override val apiKey: String,
    override val domain: String,
    override val apiURL: String = "https://api.hosting.ionos.com"
) : Provider {
    private val getDomainsURL = "$apiURL/dns/v1/zones/$zoneID?recordType=A%2CAAAA"

    // TODO: implement
    override suspend fun setSubdomain(subdomain: Subdomain) {
        Log.d("DNS", "Setting subdomain not implemented")
    }

    override suspend fun getSubdomains(): List<Subdomain> {
        return withContext(Dispatchers.IO) {
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
                        ionosEntryToSubdomain(responseDecoded.records)
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
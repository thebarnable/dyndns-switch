package com.example.dyndnsswitch.data

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

// Server wraps functionality for checking the availability of a server & saving its subdomain list
// - domain: domain name that can be used to figure out server's IP address (usually: vpn*.thebarnable.de)
class Server (var ipv4: String,
              var ipv6: String,
              val name: String,
              private val processBuilder: ProcessBuilder = ProcessBuilder()) {
    var isConnected by mutableStateOf(false) // Backed by Compose state

    suspend fun ping(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val process = processBuilder
                    .command("ping", "-c", "4", ipv4)
                    .redirectErrorStream(true)
                    .start()

                val output = process.inputStream.bufferedReader().use { it.readText() }

                process.waitFor()

                val packetLoss = Pattern.compile("(\\d+(\\.\\d+)?)% packet loss").matcher(output)
                if (packetLoss.find()) {
                    isConnected = packetLoss.group(1)?.toInt() != 100
                } else {
                    isConnected = false
                }
            } catch (e: Exception) {
                Log.e("PING", "${e.message}")
                isConnected = false
            }
            isConnected
        }
    }
}
package com.example.dyndnsswitch.data

import android.util.Log
import com.example.dyndnsswitch.util.Location
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ServerRepository(
    private val coroutineScope: CoroutineScope,
    private val exceptionHandler: CoroutineExceptionHandler,
    private val providerRepository: ProviderRepository,
    private val pingInterval: Int = 60
) {
    // expose server list (TODO: done like that because recommended MVVM pattern, not 100% understood)
    private val _servers = MutableStateFlow<List<Server>>(emptyList()) // Mutable inside repo
    val servers: StateFlow<List<Server>> = _servers.asStateFlow() // Read-only for consumers

    // expose server state (aka pingable or not)
    private val _serverStatus = MutableStateFlow<Map<Location, Boolean>>(emptyMap())
    val serverStatus: StateFlow<Map<Location, Boolean>> = _serverStatus.asStateFlow()

    // business logic
    init {
        _servers.value = listOf(Server(ipv4=providerRepository.resolve("vpn.thebarnable.de"),
                                        ipv6=providerRepository.resolve("vpn.thebarnable.de", true),
                                        name=Location.HOME),
                                Server(ipv4=providerRepository.resolve("vpn-kamen.thebarnable.de"),
                                    ipv6=providerRepository.resolve("vpn-kamen.thebarnable.de", true),
                                    name=Location.KAMEN))
    }

    fun ping() {
        coroutineScope.launch {
            _servers.collectLatest { serverList ->
                while (true) {
                    serverList.forEach { server ->
                        Log.d("PING", "Pinging ${server.name}")
                        launch(Dispatchers.IO + exceptionHandler) {
                            val reachable = server.ping()
                            _serverStatus.update { currentStatus ->
                                currentStatus.toMutableMap().apply {
                                    this[server.name] = reachable
                                }
                            }
                            Log.d("PING", "result: ${reachable.toString()}")
                        }
                    }
                    delay(pingInterval * 1000L)
                }
            }
        }
    }
}
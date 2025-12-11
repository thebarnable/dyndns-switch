package com.example.dyndnsswitch.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dyndnsswitch.data.IonosProvider
import com.example.dyndnsswitch.data.Provider
import com.example.dyndnsswitch.data.ProviderRepository
import com.example.dyndnsswitch.data.Server
import com.example.dyndnsswitch.data.ServerRepository
import com.example.dyndnsswitch.model.Subdomain
import com.example.dyndnsswitch.util.Location
import com.example.dyndnsswitch.util.readStrFromAsset
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.launch

class ServerViewModel(context: Context) : ViewModel() {
    private lateinit var serverRepository: ServerRepository
    private lateinit var providerRepository: ProviderRepository
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("CoroutineException", "Error: ${throwable.localizedMessage}")
    }

    // Expose flows directly for UI
    val servers: StateFlow<List<Server>> = MutableStateFlow(emptyList())
    val serverStatus: StateFlow<Map<Location, Boolean>> = MutableStateFlow(emptyMap())

    init {
        viewModelScope.launch {
            Log.d("ViewModel", "Initializing provider repo")
            providerRepository = ProviderRepository(listOf(IonosProvider(
                zoneID = readStrFromAsset(context, "zoneid.key"),
                apiKey = readStrFromAsset(context, "api.key"),
                domain = listOf("thebarnable.de", "timstadtmann.de")
            )))
            Log.d("ViewModel", "Fetching subdomains")
            providerRepository.fetchSubdomains()
            Log.d("ViewModel", "Subdomains fetched. Initializing server repo.")
            serverRepository = ServerRepository(viewModelScope, exceptionHandler, providerRepository)
            Log.d("ViewModel", "Server repo initialized with ${serverRepository.servers.value.size} servers")

            (servers as MutableStateFlow).value = serverRepository.servers.value
            (serverStatus as MutableStateFlow).value = serverRepository.serverStatus.value

        }
    }

    private fun fetch() {
        viewModelScope.launch {
            providerRepository.fetchSubdomains()
        }
    }
    private fun ping() {
        viewModelScope.launch {
            serverRepository.ping()
        }
    }

    fun getSubdomainsOfServer(serverIP: String): MutableList<Subdomain> {
        return providerRepository.getSubdomainsOfServer(serverIP)
    }

    // TODO: all hard-coded right now for websites
    // move subdomains from one server (e.g. Aachen) to the other (e.g. Kamen)
    fun toggleSubdomains(currentServer: Location) {
        viewModelScope.launch {
            // Get target server IP from name
            val serverList: List<Server> = servers.value
            var targetServerIPv4: String = ""
            var targetServerIPv6: String = ""
            serverList.forEach() { server ->
                if ((currentServer == Location.HOME && server.name == Location.KAMEN) ||
                    (currentServer == Location.KAMEN && server.name == Location.HOME)) {
                    targetServerIPv4 = server.ipv4
                    targetServerIPv6 = server.ipv6
                }
            }

            // Move hard-coded list of subdomains (websites)
            providerRepository.setSubdomainsOfServer(
                listOf("timstadtmann.de", "www.timstadtmann.de", "neuroaix.de", "www.neuroaix.de"),
                targetServerIPv4,
                targetServerIPv6
            )
        }
    }

//    fun setSubdomainsOfServer(sourceServerIP: String, targetServerName: Location) {
//        viewModelScope.launch {
//            // Get target server IP from name
//            val serverList: List<Server> = servers.value
//            var targetServerIPv4: String = ""
//            var targetServerIPv6: String = ""
//            serverList.forEach() { server ->
//                if(server.name == targetServerName) {
//                    targetServerIPv4 = server.ipv4
//                    targetServerIPv6 = server.ipv6
//                }
//            }
//            // TODO: error checks on ips
//            providerRepository.setSubdomainsOfServer(sourceServerIP, targetServerIPv4, targetServerIPv6)
//        }
//    }
}
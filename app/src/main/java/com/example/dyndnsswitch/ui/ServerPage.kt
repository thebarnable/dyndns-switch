package com.example.dyndnsswitch.ui

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dyndnsswitch.data.Server
import com.example.dyndnsswitch.model.IonosSubdomain
import com.example.dyndnsswitch.model.Subdomain
import com.example.dyndnsswitch.util.Location

// TODO: modifier instance vs Modifier

@Composable
fun ServerPage(server: Server, subdomains: MutableList<Subdomain>, toggleDomains: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Row(modifier = Modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = server.name.toString(),
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )
            if(server.isConnected) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = "Server connected icon",
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = "Server not connected icon",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn {
            items(subdomains.size) {
                Text(
                    text = subdomains[it].name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.height(40.dp)
                )
            }
        }

        Button(onClick = toggleDomains) {
            Text("Toggle Domains")
        }
    }
}

@Preview
@Composable
fun ServerPagePreview() {
    fun dummyClick() {
        Log.d("DEBUG", "Clicked button")
    }
    val server = Server("1.1.1.1", "::1", Location.HOME)
    val subdomains = mutableListOf<Subdomain>(
        IonosSubdomain("nextcloud.thebarnable.de", "1.1.1.2", "::2"),
        IonosSubdomain("bitwarden.thebarnable.de", "1.1.1.3", "::3"),
        IonosSubdomain("bitwarden2.thebarnable.de", "1.1.1.3", "::3"),
    )
    ServerPage(server, subdomains, ::dummyClick)
}
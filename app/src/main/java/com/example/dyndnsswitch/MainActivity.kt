package com.example.dyndnsswitch

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import com.example.dyndnsswitch.data.Server
import com.example.dyndnsswitch.model.IonosSubdomain
import com.example.dyndnsswitch.model.Subdomain
import com.example.dyndnsswitch.ui.ServerPage
import com.example.dyndnsswitch.ui.ServerViewModel

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent{
            Log.d("MAIN", "Init view model")
            val serverViewModel = ServerViewModel(this)
            val servers by serverViewModel.servers.collectAsState(initial = emptyList())

            val pagerState = rememberPagerState(
                pageCount = { servers.size } // Recompute dynamically
            )
            Log.d("MAIN", "Init pager")
            HorizontalPager(state = pagerState) { page ->
                val server = servers.getOrNull(page)
                if (server != null) {
                    ServerPage(
                        server = servers[page],
                        subdomains = serverViewModel.getSubdomainsOfServer(servers[page].ipv4)
                    )
                }
            }
        }
    }
}

package com.example.dyndnsswitch

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dyndnsswitch.ui.theme.DynDNSSwitchTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.intellij.lang.annotations.JdkConstants.HorizontalAlignment
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern
import javax.net.ssl.HttpsURLConnection

class Domain (var name: String, var domain: String, var domainAlt: String) {
    init {
        Log.d("DEBUG", "Domain $name created for: $domain and $domainAlt")
        // TODO
    }
}

class Server (var ip: String, var name: String) {
    var domains = mutableStateListOf<Domain>()
    var isConnected by mutableStateOf(false) // Backed by Compose state

    suspend fun ping(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val process = ProcessBuilder()
                    .command("ping", "-c", "4", ip)
                    .redirectErrorStream(true)
                    .start()

                val output = StringBuilder()
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String? = reader.readLine()

                while (line != null) {
                    output.appendLine(line)
                    line = reader.readLine()
                }

                process.waitFor()
                output.toString()
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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DynDNSSwitchTheme {
                val servers = remember { mutableStateListOf<Server>() }
                servers.add(Server("84.118.234.215", stringResource(R.string.aachen)))
                servers.add(Server("217.249.129.139", stringResource(R.string.kamen)))

                CheckServers(servers)
                MainScreen(servers)
            }
        }
    }

    @Composable
    fun CheckServers(servers: MutableList<Server>) {
        val interval = 60 // seconds
        val coroutineScope = rememberCoroutineScope()
        LaunchedEffect(servers) {
            while(true) {
                servers.forEach {server ->
                    Log.d("DEBUG", "Pinging ${server.name}")
                    coroutineScope.launch(Dispatchers.IO) {
                        val result = server.ping()
                        Log.d("DEBUG", "result: ${result.toString()}")
                    }
                }
                delay(interval * 1000L)
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun MainScreen(servers: MutableList<Server>) {
        val state = rememberPagerState(pageCount = {2})
        Column() {
            // List of domains & string on top
            Column(modifier = Modifier
                .fillMaxHeight(0.95f)
                .fillMaxWidth()
            ) {
                HorizontalPager(
                    modifier = Modifier,
                    state = state
                ) { page ->
                    ServerScreen(servers[page], modifier = Modifier)
                }
            }
            // Dots on the bottom, notifying which page we are on
            Row(
                modifier = Modifier
                    .height(15.dp)
                    .fillMaxWidth()
                ,
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(state.pageCount) { it ->
                    val color = if (state.currentPage == it) Color.DarkGray else Color.LightGray
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(15.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun ServerScreen(server: Server, modifier: Modifier = Modifier) {
        var showDialog by remember { mutableStateOf(false) }
        var domainName by remember { mutableStateOf("")}
        var domainAddress by remember { mutableStateOf("")}

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier,
                verticalAlignment =Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier
                        .weight(1f),
                    text = server.name,
                    fontSize = 40.sp,
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
            if(showDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showDialog = false
                    },
                    title = {
                        Text(text = "Add a new domain")
                    },
                    text = {
                        Column {
                            TextField(
                                value = domainName,
                                onValueChange = { domainName = it },
                                placeholder = { Text("Enter domain ID here") }
                            )
                            TextField(
                                value = domainAddress,
                                onValueChange = { domainAddress = it },
                                placeholder = { Text("Enter domain address here") }
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                server.domains.add(Domain(domainName, domainAddress, domainAddress))
                                showDialog = false
                                domainName = ""
                                domainAddress = ""
                            }
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = {
                                showDialog = false
                                domainName = ""
                                domainAddress = ""
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
            Column(
                modifier = Modifier
                    .height(600.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                server.domains.forEachIndexed {
                    idx, it -> DomainButton(buttonID = idx, buttonString = it.name)
                }
                /*var result by remember { mutableStateOf("Fetching...") }
                val scope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    scope.launch {
                        result = httpGet("https://www.topfschlagwm.thebarnable.de")
                    }
                }
                Greeting(result)*/
            }
            Button(
                modifier= Modifier
                    .weight(1f)
                    .fillMaxWidth(0.9f),
                onClick = {
                    showDialog = true
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Preview
    @Composable
    fun PreviewPageIndicator() {
        val state = rememberPagerState(pageCount = {2})
        Column() {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.5f)
                    .fillMaxWidth()
            ) {
                Text("test")
            }
            Row(
                modifier = Modifier
                    .height(20.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(2) { it ->
                    val color = if (1 == it) Color.DarkGray else Color.LightGray
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(20.dp)
                    )
                }
            }
        }
    }
    @Composable
    fun DomainButton(buttonID : Int, buttonString : String) {
        Button(
            onClick = {Log.d("DEBUG", "Pressed domain button")},
            modifier = Modifier.height(80.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Text(
                        text = buttonString,
                        fontSize = 30.sp
                    )
                }
                Spacer(modifier = Modifier.width(60.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Button(
                        onClick = { Log.d("DEBUG", "Pressed delete button on $buttonID") },
                        colors = ButtonDefaults.buttonColors(
                            contentColor = Color.White,
                            containerColor = Color.Red
                        ),
                        modifier = Modifier
                            .size(width = 80.dp, height = 80.dp)
                            .padding(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Delete",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    //@Preview(widthDp = 350)
    @Composable
    fun DomainButtonPreview() {
        DomainButton(0, "Preview")
    }
}


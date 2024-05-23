package com.example.dyndnsswitch

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.dyndnsswitch.ui.theme.DynDNSSwitchTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DynDNSSwitchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var result by remember { mutableStateOf("Fetching...") }
                    val scope = rememberCoroutineScope()

                    LaunchedEffect(Unit) {
                        scope.launch {
                            result = httpGet("https://www.topfschlagwm.thebarnable.de")
                        }
                    }
                    Greeting(result)
                }
            }
        }
    }

    private suspend fun httpGet(url: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val urlObj = URL(url)
                val urlConnection = urlObj.openConnection() as HttpsURLConnection
                urlConnection.requestMethod = "GET"
                urlConnection.connectTimeout = 5000
                urlConnection.readTimeout = 5000
                urlConnection.doInput = true

                try {
                    val responseCode = urlConnection.responseCode
                    Log.d("HTTP_GET", "Response Code: $responseCode")
                    if (responseCode == HttpsURLConnection.HTTP_OK) {
                        val inputStream = urlConnection.inputStream
                        val reader = BufferedReader(InputStreamReader(inputStream))
                        val result = StringBuilder()
                        reader.forEachLine { result.append(it) }
                        reader.close()
                        result.toString()
                    } else {
                        "Error: HTTP $responseCode"
                    }
                } finally {
                    urlConnection.disconnect()
                }
            } catch (e: Exception) {
                Log.e("HTTP_GET", "Exception: ${e.message}", e)
                "Error: ${e.message}"
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = name,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DynDNSSwitchTheme {
        Greeting("Preview")
    }
}

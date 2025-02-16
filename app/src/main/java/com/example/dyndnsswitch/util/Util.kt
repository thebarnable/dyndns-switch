package com.example.dyndnsswitch.util

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

fun readStrFromAsset(context: Context, fileName: String): String {
    val inputStream = context.assets.open(fileName)
    val reader = BufferedReader(InputStreamReader(inputStream))
    return reader.use { it.readText().trim() }
}
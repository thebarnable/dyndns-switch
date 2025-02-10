package com.example.dyndnsswitch.model

interface Subdomain {
    val name: String
    val ipv4: String
    val ipv6: String
}

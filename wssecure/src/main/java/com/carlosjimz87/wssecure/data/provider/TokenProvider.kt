package com.carlosjimz87.wssecure.data.provider

fun interface TokenProvider {
    fun token(): String?
}
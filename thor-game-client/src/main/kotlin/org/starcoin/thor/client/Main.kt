package org.starcoin.thor.server.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.ws
import io.ktor.http.HttpMethod
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.filterNotNull
import kotlinx.coroutines.channels.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    val client = HttpClient(CIO).config { install(WebSockets) }
    GlobalScope.launch {
        client.ws(method = HttpMethod.Get, host = "127.0.0.1", port = 8082, path = "/ws") {
            send(Frame.Text("hello world"))
            for (message in incoming.map { it as? Frame.Text }.filterNotNull()) {
                val msg = message.readText()
                println("$msg")
            }
        }
    }

    runBlocking {
        delay(100000)
    }
}
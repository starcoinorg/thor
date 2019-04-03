package org.starcoin.thor.server.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.ws
import io.ktor.http.HttpMethod
import io.ktor.http.cio.websocket.Frame
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.starcoin.thor.core.ConnData
import org.starcoin.thor.core.MsgType
import org.starcoin.thor.core.WsMsg

fun main(args: Array<String>) {
    val om = ObjectMapper().registerModule(KotlinModule())
    val client = HttpClient(CIO).config {
        install(WebSockets)
    }
    GlobalScope.launch {
        client.ws(method = HttpMethod.Get, host = "127.0.0.1", port = 8082, path = "/ws") {
            val data = om.writeValueAsString(ConnData())
            val conn = om.writeValueAsString(WsMsg("111", MsgType.CONN, data))
            send(Frame.Text(conn))
        }
    }

    runBlocking {
        delay(100000)
    }
}
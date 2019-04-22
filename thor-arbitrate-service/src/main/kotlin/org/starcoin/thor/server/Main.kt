package org.starcoin.thor.server

import org.starcoin.thor.core.GameInfo
import org.starcoin.thor.manager.GameManager
import org.starcoin.thor.manager.RoomManager
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.toList


fun loadGames(): List<GameInfo> {
    val uri = GameService::class.java.getResource("/games").toURI()
    val path: Path
    if (uri.getScheme().equals("jar")) {
        val fileSystem = FileSystems.newFileSystem(uri, mutableMapOf<String, Any>())
        path = fileSystem.getPath("/")
    } else {
        path = Paths.get(uri)
    }
    return Files.list(path).map { p ->
        println("scan $p")
        val gameName = p.fileName.toFile().name
        var engine: ByteArray? = null
        var gui: ByteArray? = null
        Files.list(p).forEach {
            println(it)
            if (it.fileName.endsWith("engine.wasm")) {
                engine = it.toFile().readBytes()
            } else if (it.fileName.endsWith("gui.wasm")) {
                gui = it.toFile().readBytes()
            }
        }
        check(engine != null && gui != null) { "can not find engine and gui file at path $p" }
        GameInfo(gameName, engine!!, gui!!)
    }.toList()
}

fun main(args: Array<String>) {
    val gameManager = GameManager()
    val roomManager = RoomManager()
    loadGames().forEach { game ->
        println("create preconfig game: $game")
        gameManager.createGame(game)
    }
    val websocketServer = WebsocketServer(gameManager, roomManager)
    websocketServer.start()
}
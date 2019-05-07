package org.starcoin.thor.server

import org.starcoin.sirius.util.WithLogging
import org.starcoin.sirius.util.logger
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
    path = if (uri.scheme == "jar") {
        val fileSystem = FileSystems.newFileSystem(uri, mutableMapOf<String, Any>())
        fileSystem.getPath("/games")
    } else {
        Paths.get(uri)
    }
    WithLogging.logger().info("path:${path}")
    return Files.list(path).map { p ->
        WithLogging.logger().info("scan $p")
        val gameName = p.fileName.toString().let { if (it.endsWith("/")) it.substring(0, it.length - 1) else it }
        var engine: ByteArray? = null
        var gui: ByteArray? = null
        Files.list(p).forEach {
            WithLogging.logger().info(it.toString())
            if (it.fileName.endsWith("engine.wasm")) {
                engine = Files.newInputStream(it).use { it.readBytes() }
            } else if (it.fileName.endsWith("gui.wasm")) {
                gui = Files.newInputStream(it).use { it.readBytes() }
            }
        }
        check(engine != null && gui != null) { "can not find engine and gui file at path $p" }
        GameInfo(gameName, engine!!, gui!!)
    }.toList()
}

fun main(args: Array<String>) {
    WithLogging.enableAllLog()
    val gameManager = GameManager()
    val roomManager = RoomManager()
    loadGames().forEach { game ->
        WithLogging.logger().info(("create pre config game: ${game.base}"))
        gameManager.createGame(game)
    }
    val websocketServer = WebsocketServer(gameManager, roomManager)
    websocketServer.start()
}
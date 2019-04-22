package org.starcoin.thor.server

import org.starcoin.thor.core.GameInfo
import org.starcoin.thor.manager.GameManager
import org.starcoin.thor.manager.RoomManager
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


fun loadGames(): List<GameInfo> {
    val uri = GameService::class.java.getResource("/games").toURI()
    val path: Path
    if (uri.getScheme().equals("jar")) {
        val fileSystem = FileSystems.newFileSystem(uri, mutableMapOf<String, Any>())
        path = fileSystem.getPath("/")
    } else {
        path = Paths.get(uri)
    }
    Files.walk(path, 1).map { p -> }
//    val it = walk.iterator()
//    val list = mutableListOf<GameInfo>()
//    while (it.hasNext()) {
//        val p = it.next()
//        val gameName = p.fileName
//        Files.walk(p, 1).
//    }
    return emptyList()
}

fun main(args: Array<String>) {
    val gameManager = GameManager()
    val roomManager = RoomManager()
    loadGames().forEach { game ->
        gameManager.createGame(game)
    }
    val websocketServer = WebsocketServer(gameManager, roomManager)
    websocketServer.start()
}
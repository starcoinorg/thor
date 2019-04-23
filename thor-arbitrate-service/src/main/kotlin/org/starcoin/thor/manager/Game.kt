package org.starcoin.thor.manager

import org.starcoin.thor.core.GameBaseInfo
import org.starcoin.thor.core.GameInfo

class GameManager {
    private val games = mutableMapOf<String, GameInfo>()
    private val gameHashSet = mutableSetOf<String>()
    private var count: Int = 0
    private val lock = java.lang.Object()

    fun createGame(game: GameInfo): Boolean {
        return synchronized(lock) {
            when (!gameHashSet.contains(game.base.hash)) {
                true -> {
                    gameHashSet.add(game.base.hash)
                    games[game.base.hash] = game
                    count = count.inc()
                    true
                }
                else -> false
            }
        }
    }

    fun count(): Int {
        return this.count
    }

    fun list(begin: Int, end: Int): List<GameBaseInfo> {
        val keys = gameHashSet.toList().subList(begin, end).toSet()
        return games.filterKeys { keys.contains(it) }.values.map { it.base }
    }

    fun queryGameBaseInfoByHash(hash: String): GameBaseInfo? {
        return games[hash]?.let { games[hash]!!.base }
    }

    fun queryGameInfoByHash(hash: String): GameInfo {
        return games[hash]!!
    }
}
package org.starcoin.thor.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.sirius.serialization.ByteArrayWrapper
import sun.misc.BASE64Encoder
import java.security.MessageDigest
import java.util.*

@Serializable
data class GameBaseInfo(@SerialId(1) val hash: String, @SerialId(2) val gameName: String, @SerialId(3) val count: Int) : Data() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameBaseInfo) return false

        if (hash != other.hash) return false
        if (gameName != other.gameName) return false
        if (count != other.count) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hash.hashCode()
        result = 31 * result + gameName.hashCode()
        result = 31 * result + count.hashCode()
        return result
    }

    override fun toString(): String {
        return "{ gameName:$gameName ; hash:$hash ; count: $count}"
    }
}

@Serializable
data class GameInfo(@SerialId(1) val base: GameBaseInfo, @SerialId(2) val bytes: ByteArrayWrapper) : Data() {
    constructor(gameName: String, bytes: ByteArrayWrapper, count: Int) : this(GameInfo.gameHash(gameName, bytes, count), bytes)

    companion object {
        fun gameHash(gameName: String, bytes: ByteArrayWrapper, count: Int): GameBaseInfo {
            val md = MessageDigest.getInstance("MD5")
            md.update(bytes.bytes)
            return GameBaseInfo(BASE64Encoder().encode(md.digest()), gameName, count)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameInfo) return false

        if (base != other.base) return false
        if (!Arrays.equals(bytes.bytes, other.bytes.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = base.hashCode()
        result = 31 * result + bytes.hashCode()
        return result
    }

    override fun toString(): String {
        return "{ game:$base }"
    }
}
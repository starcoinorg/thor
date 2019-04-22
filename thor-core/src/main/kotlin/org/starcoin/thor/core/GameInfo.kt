package org.starcoin.thor.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.sirius.serialization.ByteArrayWrapper
import java.security.MessageDigest
import java.util.*

@Serializable
data class GameBaseInfo(@SerialId(1) val hash: String, @SerialId(2) val gameName: String) : Data()

@Serializable
data class GameInfo(@SerialId(1) val base: GameBaseInfo, @SerialId(2) val engineBytes: ByteArrayWrapper, val guiBytes: ByteArrayWrapper) : Data() {
    constructor(gameName: String, engineBytes: ByteArray, guiBytes: ByteArray) : this(gameName, ByteArrayWrapper(engineBytes), ByteArrayWrapper(guiBytes))
    constructor(gameName: String, engineBytes: ByteArrayWrapper, guiBytes: ByteArrayWrapper) : this(GameBaseInfo(gameHash(engineBytes.bytes), gameName), engineBytes, guiBytes)

    companion object {
        fun gameHash(bytes: ByteArray): String {
            val md = MessageDigest.getInstance("MD5")
            md.update(bytes)
            return Base64.getEncoder().encodeToString(md.digest())
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameInfo) return false

        if (base != other.base) return false
        if (engineBytes != other.engineBytes) return false
        if (guiBytes != other.guiBytes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = base.hashCode()
        result = 31 * result + engineBytes.hashCode()
        result = 31 * result + guiBytes.hashCode()
        return result
    }
}
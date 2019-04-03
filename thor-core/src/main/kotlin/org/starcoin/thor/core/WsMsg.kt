package org.starcoin.thor.core

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator

enum class MsgType(private val type:Int) {
    CONN(1),JOIN_GAME(2), UNKNOWN(100);

    companion object {
        @JvmStatic @JsonCreator
        fun fromInt(t: Int): MsgType =
            values().firstOrNull { it.type == t } ?: MsgType.UNKNOWN
    }

    @JsonValue
    fun toInt(): Int {
        return type
    }

}

abstract class Data

class ConnData: Data()

class MsgData: Data()

data class WsMsg(val addr:String, val type:MsgType, val data:String)
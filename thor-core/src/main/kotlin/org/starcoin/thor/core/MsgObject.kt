package org.starcoin.thor.core

import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

@UseExperimental(ImplicitReflectionSerializer::class)
abstract class MsgObject {
    companion object {
        fun <T : MsgObject> fromJson(json: String, cla: KClass<T>): T {
            return Json.parse(cla.serializer(), json)
        }
    }

    fun toJson(): String {
        return Json.stringify(this.javaClass.kotlin.serializer(), this)
    }
}

@UseExperimental(ImplicitReflectionSerializer::class)
abstract class Data : MsgObject()
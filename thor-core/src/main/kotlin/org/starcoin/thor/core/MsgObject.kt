package org.starcoin.thor.core

import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.context.getOrDefault
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
        return Json.stringify(Json.plain.context.getOrDefault(this::class) as KSerializer<MsgObject>, this)
    }
}

@UseExperimental(ImplicitReflectionSerializer::class)
abstract class Data : MsgObject()
package org.starcoin.thor.server

import io.ktor.application.*
import io.ktor.content.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.io.*
import kotlinx.coroutines.io.jvm.javaio.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import io.ktor.util.pipeline.PipelineContext

@UseExperimental(ImplicitReflectionSerializer::class, KtorExperimentalAPI::class, UnstableDefault::class)
class JsonSerializableConverter(private val json: Json = Json.plain) : ContentConverter {
    override suspend fun convertForReceive(context: PipelineContext<ApplicationReceiveRequest, ApplicationCall>): Any? {
        val request = context.subject
        val type = request.type
        val value = request.value as? ByteReadChannel ?: return null
        val text = value.toInputStream().reader(context.call.request.contentCharset() ?: Charsets.UTF_8).readText()
        println(text)
        return json.parse(type.serializer(), text)
    }

    override suspend fun convertForSend(context: PipelineContext<Any, ApplicationCall>, contentType: ContentType, value: Any): Any? {
        println(value.javaClass.kotlin)
        return TextContent(
                text = json.stringify(value.javaClass.kotlin.serializer(), value),
                contentType = ContentType.Application.Json.withCharset(context.call.suitableCharset())
        )
    }
}
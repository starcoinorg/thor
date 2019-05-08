package org.starcoin.thor.utils

import com.google.common.io.BaseEncoding
import org.starcoin.sirius.serialization.ByteArrayWrapper
import kotlin.experimental.and

val base64 = BaseEncoding.base64()

fun ByteArray.toBase64(): String {
    return base64.encode(this)
}

fun String.decodeBase64(): ByteArray {
    return base64.decode(this)
}

fun ByteArrayWrapper.toHex(): String {
    val result = StringBuilder()
    for (b in bytes) result.append(Integer.toString((b and 0xff.toByte()) + 0x100, 16).substring(1))
    return result.toString()
}
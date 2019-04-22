package org.starcoin.thor.utils

import com.google.common.io.BaseEncoding

val base64 = BaseEncoding.base64()

fun ByteArray.toBase64(): String {
    return base64.encode(this)
}

fun String.decodeBase64(): ByteArray {
    return base64.decode(this)
}
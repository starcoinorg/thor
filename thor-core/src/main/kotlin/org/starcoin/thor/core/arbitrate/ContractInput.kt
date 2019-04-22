package org.starcoin.thor.core.arbitrate

interface ContractInput : Iterator<ByteArray> {
    fun getUser(): Int
}

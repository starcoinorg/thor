package org.starcoin.thor.core.arbitrate

abstract class Proof {
    abstract fun getInput(): ByteArray
}
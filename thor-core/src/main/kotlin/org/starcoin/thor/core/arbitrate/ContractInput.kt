package org.starcoin.thor.core.arbitrate
interface ContractInput:Iterator<ArbitrateData>{
    fun getUser(): String
}

package org.starcoin.thor.core.arbitrate

import com.github.kittinunf.fuel.Fuel

class ContractImpl(val url: String, val id: Int) : Contract() {
    private val initPath = "$url/api/vm"
    private val execPath = "$url/api/execute"

    init {
        Fuel.post(initPath).body("id=$id")
                .body("srcpath=wasm/engine_optimized.wasm")
                .response { result ->
                    println(result)
                }

        Fuel.post(execPath).body("id=$id")
                .body("cmd=0").response { result ->
                    println(result)
                }
    }

    override fun getWinner(): Int? {
        var winner: Int? = null
        Fuel.post(execPath).body("id=$id").body("cmd=2")
                .response { result ->
                    winner = result.toString().toInt()
                }
        return winner
    }

    override fun update(userId: Int, state: ByteArray) {
        Fuel.post(execPath).body("id=$id")
                .body("cmd=1,$userId,$state").response { result ->
                    println(result)
                }
    }
}
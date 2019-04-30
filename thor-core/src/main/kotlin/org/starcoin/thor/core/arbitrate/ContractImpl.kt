package org.starcoin.thor.core.arbitrate

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelManager
import java.lang.RuntimeException


class ContractImpl(url: String, private val id: String) : Contract() {

    private val initPath = "/api/vm"
    private val execPath = "/api/execute"

    init {
        FuelManager.instance.basePath = url
        var resp = Fuel.post(initPath,
                listOf("id" to id, "srcpath" to "wasm/engine_optimized.wasm")).response().second
        if (resp.statusCode != 200) {
            throw RuntimeException("Init contract failed,code:${resp.statusCode}, ${resp.body().asString("text/plain")}")
        }
        resp = Fuel.post(execPath, listOf("id" to id, "cmd" to "0")).response().second
        if (resp.statusCode != 200) {
            throw RuntimeException("Call init of contract failed, code:${resp.statusCode}")
        }
    }


    override fun getWinner(): String {
        var w: Int?
        val resp = Fuel.post(execPath, listOf("id" to id, "cmd" to "2")).response().second
        if (resp.statusCode != 200) {
            throw RuntimeException("Call getWinner of contract failed, code:${resp.statusCode}")
        }
        return resp.body().asString("text/plain")
    }

    override fun update(userId: String, state: ByteArray) {
        val opcode = 1
        val cmd = "$opcode,$userId,$state"
        val resp = Fuel.post(execPath, listOf("id" to id, "cmd" to cmd)).response().second
        if (resp.statusCode != 200) {
            throw RuntimeException("Call update of contract failed, code:${resp.statusCode}")
        }
    }

    override fun getSourceCode(): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

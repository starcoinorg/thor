package org.starcoin.thor.core

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.starcoin.lightning.client.core.AddInvoiceResponse
import org.starcoin.lightning.client.core.Invoice
import kotlin.reflect.KClass

enum class MsgType(private val type: Int) {
    CONN(1),
    //    CONFIRM_REQ(2), CONFIRM_RESP(3),//TODO()
    START_INVITE_REQ(4),
    START_INVITE_RESP(5),
    INVITE_PAYMENT_REQ(9), INVITE_PAYMENT_RESP(10),
    SURRENDER_REQ(9), SURRENDER_RESP(10),
    CHALLENGE_REQ(1),
    UNKNOWN(100);

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromInt(t: Int): MsgType =
                values().firstOrNull { it.type == t } ?: MsgType.UNKNOWN
    }

    @JsonValue
    fun toInt(): Int {
        return type
    }

}

abstract class Data {
    fun data2Str(): String {
        return om.writeValueAsString(this)
    }
}

class ConnData : Data()

data class StartAndInviteReq(val gameHash: String) : Data()

data class InvitedAndPaymentReq(val gameHash: String, val instanceId: String, val rHash: ByteArray) : Data()

data class StartAndInviteResp(val succ: Boolean, val iap: InvitedAndPaymentReq? = null) : Data()

data class InvitedAndPaymentResp(val instanceId: String, val invoice: String) : Data()

data class SurrenderReq(val instanceId: String):Data()

data class SurrenderResp(val r: String):Data()

data class ChallengeReq(val instanceId: String):Data()

data class WsMsg(val from: String, val to: String, val type: MsgType, val data: String) {
    companion object {
        fun str2WsMsg(msg: String): WsMsg {
            return om.readValue(msg, WsMsg::class.java)
        }
    }

    fun <T : Data> str2Data(cls: KClass<T>): T {
        return om.readValue(this.data, cls.java)
    }

    fun msg2Str(): String {
        return om.writeValueAsString(this)
    }
}

private val om = ObjectMapper().registerModule(KotlinModule())


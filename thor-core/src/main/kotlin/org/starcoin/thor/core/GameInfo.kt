package org.starcoin.thor.core

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.sirius.core.SiriusObject
import org.starcoin.sirius.core.SiriusObjectCompanion
import org.starcoin.sirius.serialization.ProtobufSchema
import org.starcoin.sirius.util.MockUtils
import org.starcoin.thor.proto.Thor

@ProtobufSchema(Thor.ProtoGameInfo::class)
@Serializable
class GameInfo(@SerialId(1) val addr: String, @SerialId(2) val name: String, @SerialId(3) val gameHash: String, @SerialId(4) val cost: Long) : SiriusObject() {

    companion object : SiriusObjectCompanion<GameInfo, Thor.ProtoGameInfo>(GameInfo::class) {

        override fun mock(): GameInfo {
            return GameInfo("Game1", "Game1", "Game1", MockUtils.nextLong())
        }

        fun paseFromProto(proto: Thor.ProtoGameInfo): GameInfo {
            return GameInfo(proto.addr, proto.name, proto.gameHash, proto.cost)
        }
    }

    fun toGameProto(): Thor.ProtoGameInfo {
        return Thor.ProtoGameInfo.newBuilder().setName(this.name).setCost(this.cost).setGameHash(this.gameHash).setAddr(this.addr).build()
    }
}
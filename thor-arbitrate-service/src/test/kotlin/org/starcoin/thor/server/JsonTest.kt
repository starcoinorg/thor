package org.starcoin.thor.server

import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.parse
import kotlinx.serialization.stringify
import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.serialization.ByteArrayWrapper
import org.starcoin.thor.core.CreateGameReq
import org.starcoin.thor.core.HttpMsg
import org.starcoin.thor.core.HttpType


enum class TestType {
    ONE, TWO
}

@Serializable
data class TestClass(val type: TestType, val name: String)

class JsonTest {

    @UseExperimental(ImplicitReflectionSerializer::class)
    @Test
    fun testHttpMsgJson() {
        val httpMsg = HttpMsg(HttpType.CREATE_GAME, CreateGameReq("hash", ByteArrayWrapper(ByteArray(10)), 10))
        val str = httpMsg.toJson()
        println(str)
        val httpMsg2 = Json.parse<HttpMsg>(str)
        Assert.assertEquals(httpMsg, httpMsg2)
    }


    @UseExperimental(ImplicitReflectionSerializer::class)
    @Test
    fun enumJsonTest() {
        val testObj = TestClass(TestType.ONE, "test")
        val str = Json.stringify(testObj)
        println(str)
        val testObj2 = Json.parse<TestClass>(str)
        Assert.assertEquals(testObj, testObj2)
    }

}
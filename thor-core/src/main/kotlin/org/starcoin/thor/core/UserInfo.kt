package org.starcoin.thor.core

import org.starcoin.thor.sign.getID
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey

enum class UserStatus {
    NORMAL,ROOM,PLAYING;
}

data class UserSelf(val privateKey: PrivateKey, val userInfo: UserInfo) {

    companion object {
        fun paseFromKeyPair(keyPair: KeyPair): UserSelf {
            return UserSelf(keyPair.private, UserInfo(keyPair.public))
        }
    }
}

data class UserInfo(val publicKey: PublicKey) {
    val id: String
        get() = publicKey.getID()
}
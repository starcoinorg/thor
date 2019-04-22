import {crypto as btccrypto, ECPair, Network, payments} from "bitcoinjs-lib";

namespace crypto {

  export type ECPair = ECPair.ECPairInterface

  export function generateKeyPair(): ECPair {
    return ECPair.makeRandom()
  }

  export function toAddress(keyPair: ECPair): string {
    const {address} = payments.p2pkh({pubkey: keyPair.publicKey})
    return address!
  }

  export function fromWIF(wifString: string, network?: Network | Network[]): ECPair {
    return ECPair.fromWIF(wifString, network)
  }

  export function fromPublicKey(buffer: Buffer): ECPair {
    return ECPair.fromPublicKey(buffer)
  }

  export function hash(buffer: Buffer): string {
    return btccrypto.hash256(buffer).toString('base64')
  }

}

export default crypto;

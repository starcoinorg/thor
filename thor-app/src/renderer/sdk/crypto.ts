import {crypto as btccrypto, ECPair, Network} from "bitcoinjs-lib";

namespace crypto {

  export type ECPair = ECPair.ECPairInterface

  export function generateKeyPair(): ECPair {
    return ECPair.makeRandom()
  }

  export function toAddress(keyPair: ECPair): string {
    return "0x" + btccrypto.hash160(keyPair.publicKey!).toString('hex')
  }

  export function fromWIF(wifString: string, network?: Network | Network[]): ECPair {
    return ECPair.fromWIF(wifString, network)
  }

  export function fromPublicKey(buffer: Buffer): ECPair {
    return ECPair.fromPublicKey(buffer)
  }

  export function hash(buffer: Buffer): Buffer {
    return btccrypto.sha256(buffer)
  }

}

export default crypto;

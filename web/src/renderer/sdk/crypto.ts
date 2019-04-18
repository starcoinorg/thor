import {ECPair, payments} from "bitcoinjs-lib/types/";


export function generateKeyPair(): ECPair.ECPairInterface {
  return ECPair.makeRandom()
}

export function toAddress(keyPair: ECPair.ECPairInterface): string {
  const {address} = payments.p2pkh({pubkey: keyPair.publicKey})
  return address!
}

import crypto from "./crypto";

namespace util {

  export function decodeSignature(signStr: string): Buffer {
    let signature = Buffer.from(signStr, 'base64');
    //server signature length is 65, but bitcoinjs is 64.
    if (signature.length == 65) {
      signature = signature.slice(1);
    }
    return signature;
  }

  export function doVerify(hash: Buffer, signStr: string, pubKey: crypto.ECPair): boolean {
    let signature = decodeSignature(signStr)
    //bitcoinjs bug, result should be boolean.
    let result = pubKey.verify(hash, signature);
    if (!result) {
      console.error("verify signature fail:", result, "hash:", hash.toString('hex'), "signature:", signStr);
    }
    return <any>result;
  }

  export function numberTo8Bytes(number: number): Buffer {
    let buf = new Buffer(8);
    buf.fill(0);
    buf.writeUInt32BE(number >> 8, 0); //write the high order bits (shifted over)
    buf.writeUInt32BE(number & 0x00ff, 4); //write the low order bits
    return buf;
  }

  export function check(condition: boolean, msg?: string): void {
    if (!condition) {
      if (msg) {
        throw msg;
      } else {
        console.error("check error");
        throw "check error.";
      }
    }
  }

}

export default util


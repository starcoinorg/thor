import crypto from "./crypto";
import {Int64BE} from "int64-buffer";

namespace util {

  export function decodeSignature(signStr: string): Buffer {
    let signature = Buffer.from(signStr, 'base64');
    //server signature length is 65, but bitcoinjs is 64.
    if (signature.length == 65) {
      signature = signature.slice(1);
    }
    return signature;
  }

  export function doVerifyByJson(jsonObj: any, signStr: string, pubKey: crypto.ECPair): boolean {
    let json = JSON.stringify(jsonObj);
    let msgData = Buffer.from(json);
    let result = doVerifyByData(msgData, signStr, pubKey);
    if (!result) {
      console.error("verify signature fail:", "originJsonObj:", jsonObj, "json:", json);
    }
    return result;
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

  export function doVerifyByData(data: Buffer, signStr: string, pubKey: crypto.ECPair) {
    return doVerify(crypto.hash(data), signStr, pubKey);
  }

  export function doSign(data: Buffer, key: crypto.ECPair): string {
    console.debug("doSign data:", data.toString('hex'));
    let hash = crypto.hash(data);
    console.debug("doSign hash:", hash.toString('hex'));
    return key.sign(hash).toString('base64');
  }

  export function doSignWithString(data: string, key: crypto.ECPair) {
    console.debug("doSign string:", data);
    return doSign(Buffer.from(data), key);
  }

  export function numberToBuffer(number: number): Buffer {
    let big = new Int64BE(number);
    let buf = big.toBuffer();
    //console.debug("numberToBuffer ", number, buf.toString('hex'));
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

  export function decodeHex(hex: string): Buffer {
    if (hex.startsWith('0x')) {
      hex = hex.slice(2);
    }
    return Buffer.from(hex, 'hex');
  }

  export function unmarshal<T>(obj: T, jsonObj: any): T {
    try {
      // @ts-ignore
      if (typeof obj["initWithJSON"] === "function") {
        // @ts-ignore
        obj["initWithJSON"](jsonObj);
      } else {
        for (var propName in jsonObj) {
          // @ts-ignore
          obj[propName] = jsonObj[propName]
        }
      }
    } catch (e) {
      console.error("unmarshal error.", e);
    }
    return obj;
  }
}

export default util


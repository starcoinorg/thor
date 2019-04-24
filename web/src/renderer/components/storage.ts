import crypto from "../sdk/crypto";
import {WitnessData} from "../sdk/client";


namespace storage {
  const keyPairKey: string = 'key_pair';
  const witnessDataKey: string = 'witnessData';

  export function loadOrGenerateKeyPair(): crypto.ECPair {
    let keyPair: crypto.ECPair;
    let keyPairWIF = localStorage.getItem(keyPairKey);
    if (keyPairWIF != null) {
      keyPair = crypto.fromWIF(keyPairWIF);
    } else {
      keyPair = crypto.generateKeyPair();
      localStorage.setItem(keyPairKey, keyPair.toWIF())
    }
    return keyPair
  }

  export function addWitnessData(roomId: string, witnessData: WitnessData) {
    let witnessDatas = loadWitnessDatas(roomId);
    witnessDatas.witnessData.push(witnessData.toJSONObj())
    saveWitnessDatas(roomId, witnessDatas);
  }

  export function loadWitnessDatas(roomId: string) {
    let witnessDataStr = localStorage.getItem(roomId + "-" + witnessDataKey);
    if (witnessDataStr == null) {
      return {"roomId": roomId, "witnessData": []}
    } else {
      return JSON.parse(witnessDataStr);
    }
  }

  export function saveWitnessDatas(roomId: string, witnessDatas: any) {
    localStorage.setItem(roomId + "-" + witnessDataKey, JSON.stringify(witnessDatas));
  }
}

// @ts-ignore
export default storage

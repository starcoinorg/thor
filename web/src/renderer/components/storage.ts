import crypto from "../sdk/crypto";
import {WitnessData, Witnesses} from "../sdk/client";


namespace storage {
  const keyPairKey: string = 'key_pair';
  const witnessDataKey: string = 'witness_data';
  const configKey: string = 'config';

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
    let witnesses = loadWitnesses(roomId);
    witnesses.witnessList.push(witnessData)
    saveWitnesses(roomId, witnesses);
  }

  export function loadWitnesses(roomId: string): Witnesses {
    let key = roomId + "-" + witnessDataKey;
    let witnessDataStr = localStorage.getItem(key);
    let witnesses = new Witnesses();
    witnesses.roomId = roomId;
    if (witnessDataStr != null) {
      try {
        let jsonObj = JSON.parse(witnessDataStr);
        witnesses.initWithJson(jsonObj);
      } catch (e) {
        console.error(e);
        localStorage.removeItem(key);
      }
    }
    return witnesses;
  }

  export function saveWitnesses(roomId: string, witnesses: Witnesses) {
    localStorage.setItem(roomId + "-" + witnessDataKey, JSON.stringify(witnesses.toJSONObj()));
  }

  export function getLatestWitnessData(roomId: string): WitnessData | null {
    let witnesses = loadWitnesses(roomId);
    if (witnesses.witnessList.length == 0) {
      return null
    }
    return witnesses.witnessList[0];
  }

  export function saveConfig(config: any) {
    localStorage.setItem(configKey, JSON.stringify(config));
  }

  export function loadConfig(): any {
    let configStr = localStorage.getItem(configKey);
    if (configStr == null) {
      return {}
    } else {
      return JSON.parse(configStr);
    }
  }
}

// @ts-ignore
export default storage

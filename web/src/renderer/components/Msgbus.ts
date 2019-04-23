import Vue from "vue";
import * as client from "../sdk/client";
import {WsMsg, WSMsgType} from "../sdk/client";
import StorageKeys from "./StorageKeys";
import crypto from "../sdk/crypto";

let bus = new Vue({
  methods: {
    init() {
      let self = this;
      let keyPair: crypto.ECPair;
      let keyPairWIF = localStorage.getItem(StorageKeys.keyPair);
      if (keyPairWIF != null) {
        keyPair = crypto.fromWIF(keyPairWIF);
      } else {
        keyPair = crypto.generateKeyPair();
        localStorage.setItem(StorageKeys.keyPair, keyPair.toWIF())
      }
      client.init(keyPair);
      client.subscribe(function (msg: WsMsg): void {
        console.log("emit", msg);
        self.$emit(WSMsgType[msg.type], msg.data);
      });
    }
  }
});

export default bus

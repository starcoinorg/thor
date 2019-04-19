import Vue from "vue";
import * as client from "../sdk/client";
import {WSMessage, WSMsgType} from "../sdk/client";
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
      client.subscribe(function (msg: WSMessage): void {
        console.log("emit", msg);
        if (msg.type == WSMsgType.GAME_BEGIN) {
          self.$emit('game-begin', msg.data);
        } else if (msg.type == WSMsgType.ROOM_DATA_MSG) {
          self.$emit("game-state", msg.data);
        }
      });
    }
  }
});

export default bus

import Vue from "vue";
import * as client from "../sdk/client";
import {WsMsg, WSMsgType} from "../sdk/client";
import * as lightning from "../sdk/lightning";
import storage from "./storage";

let bus = new Vue({
  methods: {
    init() {
      let self = this;
      let keyPair = storage.loadOrGenerateKeyPair();
      client.init(keyPair);
      let config = storage.loadConfig();
      lightning.init(config);
      client.subscribe(function (msg: WsMsg): void {
        console.log("emit", msg);
        self.$emit(WSMsgType[msg.type], msg.data);
      });
    }
  }
});

export default bus

export function newSuccessHandler(msg: string) {
  return function () {
    bus.$emit("message", msg)
  }
}

export function newErrorHandler() {
  return function (e: any) {
    bus.$emit("error", e)
  }
}

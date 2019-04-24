import Vue from "vue";
import * as client from "../sdk/client";
import {WsMsg, WSMsgType} from "../sdk/client";
import storage from "./storage";

let bus = new Vue({
  methods: {
    init() {
      let self = this;
      let keyPair = storage.loadOrGenerateKeyPair();
      client.init(keyPair);
      client.subscribe(function (msg: WsMsg): void {
        console.log("emit", msg);
        self.$emit(WSMsgType[msg.type], msg.data);
      });
    }
  }
});

export default bus

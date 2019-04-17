import Vue from "vue";
import * as client from "../sdk/client";
import {WSMessage, WSMsgType} from "../sdk/client";

let bus = new Vue({
  methods: {
    init() {
      let self = this;
      client.init();
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

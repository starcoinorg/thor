import Vue from "vue";
import * as vm from "../sdk/vm"
import * as client from "../sdk/client"
import {WitnessData} from "../sdk/client"
import MsgBus from "./Msgbus"
import crypto from "../sdk/crypto"

export default Vue.extend({
  template: `
        <div>
        <div class="loading" v-if="loading">
            Loading...
        </div>
        <div v-if="error" class="error">
        {{ error }}
        </div>
        <div v-if="room"> roomId:{{room.id}} <template v-for="player in room.players">player:{{player}} </template> </div>
        <canvas id="as2d" width="600" height="600"/>
        </div>
    `,
  props: ["roomId"],
  data() {
    return {
      loading: false,
      error: null,
      room: null,
      game: null,
    }
  },
  created() {
    console.log("room:" + this.roomId);
  },
  watch: {},
  methods: {
    init: function () {
      this.error = null;
      this.loading = true;
      client.getRoom(this.roomId).then(room => {
        this.loading = false;
        this.room = room
        return room
      }).then(room => {
        let role = room.players[0] == client.getMyAddress() ? 1 : 2;
        return vm.init(role, this.stateUpdate);
      }).then(game => {
        // @ts-ignore
        this.game = game;
        let self = this;
        // @ts-ignore
        if (this.room && this.room.players.length == 2) {
          this.startGame()
        } else {
          MsgBus.$on("game-begin", function (event: any) {
            console.log("handle game-begin event", event);
            self.room = event.room;
            self.startGame();
          })
        }
        MsgBus.$on("game-state", function (event: any) {
          console.log("handle game-state event", event);
          //convert to TypedArray
          let state = Int8Array.from(event.state);
          self.rivalStateUpdate(state);
        })

      }).catch(error => {
        this.error = error
      })
    },
    startGame: function () {
      vm.startGame();
    },
    rivalStateUpdate: function (state: Int8Array) {
      console.log("rivalStateUpdate:", state);
      vm.rivalUpdate(state);
    },
    stateUpdate: function (fullState: Int8Array, state: Int8Array) {
      //convert to normal array, for JSON.stringify
      let newState = Array.from(state);
      console.log("stateUpdate:", newState);
      let data = {"fullStateHash": crypto.hash(Buffer.from(fullState)), "state": Buffer.from(state).toString("base64")};
      let witnessData = new WitnessData("preHash", Buffer.from(JSON.stringify(data)));
      client.sendRoomGameData(this.roomId, witnessData);
    }
  }
});

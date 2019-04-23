import Vue from "vue";
import * as vm from "../sdk/vm"
import * as client from "../sdk/client"
import {Room, WitnessData} from "../sdk/client"
import MsgBus from "./Msgbus"
import crypto from "../sdk/crypto"
import {ICanvasSYS} from "as2d/src/util/ICanvasSYS";
import * as loader from "assemblyscript/lib/loader";
import {GameGUI} from "../sdk/GameGUI";


interface ComponentData {
  loading: boolean;
  error: any;
  room: Room | null;
  gameInfo: any;
  game?: ICanvasSYS & loader.ASUtil & GameGUI | null;
}

export default Vue.extend({
  template: `
        <div>
        <div class="loading" v-if="loading">
            Loading...
        </div>
        <div v-if="error" class="error">
        {{ error }}
        </div>
        <div v-if="room"> roomId:{{room.roomId}} <template v-for="player in room.players">player:{{player}} </template> </div>
        <canvas id="as2d" width="600" height="600"/>
        </div>
    `,
  props: ["roomId"],
  data(): ComponentData {
    return {
      loading: false,
      error: null,
      room: null,
      gameInfo: null,
      game: null
    }
  },
  created() {
    console.log("create component:" + this.roomId);
    this.init();
  },
  watch: {},
  methods: {
    init() {
      console.log("init room", this.roomId);
      this.error = null;
      this.loading = true;
      client.getRoom(this.roomId).then(room => {
        console.log("room", room);
        this.room = room;
        return room
      }).then(room => {
        return client.gameInfo(room.gameId)
      }).then(gameInfo => {
        this.gameInfo = gameInfo;
        this.loading = false;
        console.log("gameInfo", gameInfo);
        let role = this.room!.players[0] == client.getMyAddress() ? 1 : 2;
        let engineBuffer = Buffer.from(gameInfo.engineBytes.slice(2), 'hex');
        let guiBuffer = Buffer.from(gameInfo.guiBytes.slice(2), 'hex');
        console.log("engineBuffer length", engineBuffer.length);
        console.log("guiBuffer length", guiBuffer.length);
        vm.init(role, this.stateUpdate, engineBuffer, guiBuffer).then(module => {
          this.game = module;
          let self = this;
          // @ts-ignore
          if (this.room.players.length == 2) {
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
        });

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

import Vue from "vue";
import * as vm from "../sdk/vm"
import * as client from "../sdk/client"
import {Room, WitnessData, WSMsgType} from "../sdk/client"
import MsgBus from "./Msgbus"
//import crypto from "../sdk/crypto"
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
        <div v-if="room"> roomId:{{room.roomId}} payment:{{room.payment}} <template v-for="player in room.players">player:{{player}} </template><br/> 
        <button v-on:click="ready">Ready</button>
        </div>
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

      let self = this;
      MsgBus.$on(WSMsgType[WSMsgType.GAME_BEGIN], function (event: any) {
        console.log("handle game-begin event", event);
        self.room = event.room;
        self.startGame();
      });
      MsgBus.$on(WSMsgType[WSMsgType.ROOM_GAME_DATA_MSG], function (event: any) {
        console.log("handle game-data event", event);
        let witnessData = new WitnessData();
        witnessData.initWithJSON(event.witness);
        //convert to TypedArray
        let state = Int8Array.from(witnessData.data);
        self.rivalStateUpdate(state);
      });

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
        });

      }).catch(error => {
        this.error = error
      })
    },
    ready: function () {
      client.readyForGame(this.roomId);
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
      //let data = {"fullStateHash": crypto.hash(Buffer.from(fullState)), "state": Buffer.from(state).toString("base64")};
      let witnessData = new WitnessData();
      witnessData.data = Buffer.from(state);
      //"preHash", Buffer.from(JSON.stringify(data)));
      client.sendRoomGameData(this.roomId, witnessData);
    }
  }
});

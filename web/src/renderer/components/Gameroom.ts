import Vue from "vue";
import * as vm from "../sdk/vm"
import * as client from "../sdk/client"
import {Room, WitnessData, WSMsgType} from "../sdk/client"
import MsgBus from "./Msgbus"
import crypto from "../sdk/crypto"
import {ICanvasSYS} from "as2d/src/util/ICanvasSYS";
import * as loader from "assemblyscript/lib/loader";
import {GameGUI} from "../sdk/GameGUI";


interface ComponentData {
  message: string;
  error: any;
  prepare: boolean;
  ready: boolean;
  gameBegin: boolean;
  room: Room | null;
  myRole: number;
  gameInfo: any;
  game?: ICanvasSYS & loader.ASUtil & GameGUI | null;
  gameOver: boolean;
  winner: number;
}

export default Vue.extend({
  template: `
        <v-container>
        
        <v-alert
        :value="message"
        type="success"
        transition="scale-transition"
      >
        {{message}}
      </v-alert>
      
      <v-alert
        :value="error"
        type="error"
        transition="scale-transition"
      >
        {{error}}
      </v-alert>
        
        <v-card>
        <v-card-text v-if="room">
        <span >roomId:{{room.roomId}} cost:{{room.cost}} </span><br/>
        <span>players:</span><br/>
        <template v-for="(player,index) in room.players"><span>player-{{index}}:{{player.playerUserId}} ready:{{player.ready}} </span><br/></template>
        <v-container>
        
        <v-dialog v-model="prepare" persistent max-hegith="600" max-width="600">
        <v-container>
          <v-card v-if="!ready">
            <v-card-title>
              Ready to play game?
            </v-card-title>
            <v-card-actions><v-btn v-on:click="doReady">Ready</v-btn></v-card-actions>
          </v-card>
          <v-card v-if="ready && !gameBegin">
            <v-card-title>
              Waiting rival player ..
            </v-card-title>
            <v-card-actions></v-card-actions>
          </v-card>
        </v-container>
      </v-dialog>
      
      <v-dialog v-model="gameOver" persistent max-hegith="600" max-width="600">
        <v-container>
          <v-card>
            <v-card-title>
            <span v-if="myRole == winner">You Win!!!</span>
            <span v-if="myRole != winner">You Lost!!</span>
            </v-card-title>
            <v-card-actions>
              <v-btn v-if="myRole != winner" v-on:click="doSurrender">Surrender</v-btn>
              <v-btn v-if="myRole != winner" v-on:click="doChallenge">Challenge</v-btn>
            </v-card-actions>
          </v-card>
        </v-container>
      </v-dialog>
      
        <canvas id="as2d" width="600" height="600"/>
        </v-container>
        </v-card-text>
        </v-card>
        
        </v-container>
    `,
  props: ["roomId"],
  data(): ComponentData {
    return {
      message: "",
      error: null,
      prepare: true,
      ready: false,
      gameBegin: false,
      room: null,
      myRole: 0,
      gameInfo: null,
      game: null,
      gameOver: false,
      winner: 0,
    }
  },
  created() {
    console.log("create component:" + this.roomId);
    this.init();
  },
  watch: {
    message: function (newMessage, oldMessage) {
      if (!oldMessage) {
        setTimeout(() => {
          this.message = "";
        }, 1000)
      }
    },
    error: function (newError, oldError) {
      if (!oldError) {
        setTimeout(() => {
          this.error = "";
        }, 1000)
      }
    }
  },
  methods: {
    init() {
      console.log("init room", this.roomId);
      this.error = null;
      MsgBus.$emit("loading", true);

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
        MsgBus.$emit("loading", false);
        console.log("gameInfo", gameInfo);
        this.myRole = this.room!.players[0].playerUserId == client.getMyAddress() ? 1 : 2;
        let engineBuffer = Buffer.from(gameInfo.engineBytes.slice(2), 'hex');
        let guiBuffer = Buffer.from(gameInfo.guiBytes.slice(2), 'hex');
        console.log("engineBuffer length", engineBuffer.length);
        console.log("guiBuffer length", guiBuffer.length);
        vm.init(this.myRole, this.stateUpdate, engineBuffer, guiBuffer, function (player: number) {
          self.gameOver = true;
          self.winner = player;
        }, function (error: string) {
          self.error = error
        }).then(module => {
          this.game = module;
          if (this.allReady()) {
            this.startGame();
          }
        });

      }).catch(error => {
        this.error = error
      })
    },
    allReady: function () {
      if (!this.room || this.room.players.length < 2) {
        return false;
      }
      let len = this.room.players.length;
      for (let i = 0; i < len; i++) {
        if (!this.room.players[i].ready) {
          return false;
        }
      }
      return true;
    },
    doReady: function () {
      client.doReady(this.roomId);
      this.ready = true;
    },
    doSurrender: function () {
      client.doSurrender(this.roomId);
    },
    doChallenge: function () {
    },
    startGame: function () {
      this.message = "game begin.";
      vm.startGame();
      this.prepare = false;
      this.ready = true;
      this.gameBegin = true;
    },
    rivalStateUpdate: function (state: Int8Array) {
      console.log("rivalStateUpdate:", state);
      vm.rivalUpdate(state);
    },
    stateUpdate: function (fullState: Int8Array, state: Int8Array) {
      //convert to normal array, for JSON.stringify
      let newState = Array.from(state);
      console.log("stateUpdate:", newState);
      let witnessData = new WitnessData();
      witnessData.stateHash = crypto.hash(Buffer.from(fullState));
      witnessData.data = Buffer.from(state);
      //"preHash", Buffer.from(JSON.stringify(data)));
      client.sendRoomGameData(this.roomId, witnessData);
    }
  }
});

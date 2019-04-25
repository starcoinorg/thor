import Vue from "vue";
import * as vm from "../sdk/vm"
import * as client from "../sdk/client"
import {Room, User, WitnessData, WSMsgType} from "../sdk/client"
import MsgBus from "./Msgbus"
import crypto from "../sdk/crypto"
import {ICanvasSYS} from "as2d/src/util/ICanvasSYS";
import * as loader from "assemblyscript/lib/loader";
import {GameGUI} from "../sdk/GameGUI";
import storage from "./storage";
import util from "../sdk/util";


interface ComponentData {
  me: User;
  message: string;
  error: any;
  prepare: boolean;
  ready: boolean;
  gameBegin: boolean;
  room: Room | null;
  rival: User | null;
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
      me: client.getMe(),
      message: "",
      error: null,
      prepare: true,
      ready: false,
      gameBegin: false,
      room: null,
      rival: null,
      myRole: 0,
      gameInfo: null,
      game: null,
      gameOver: false,
      winner: 0,
    }
  },
  created() {
    console.debug("create component:" + this.roomId);
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
      console.debug("init room", this.roomId);
      this.error = null;
      MsgBus.$emit("loading", true);

      let self = this;
      MsgBus.$on(WSMsgType[WSMsgType.GAME_BEGIN], function (event: any) {
        if (event.room.roomId == self.roomId) {
          console.debug("handle game-begin event", event);
          self.room = event.room;
          self.startGame();
        }
      });

      MsgBus.$on(WSMsgType[WSMsgType.GAME_END], function (event: any) {
        if (event.roomId == self.roomId) {
          console.debug("handle game-end event", event);
          self.message = "Game end";
          self.$router.push({name: 'home'})
        }
      });

      MsgBus.$on(WSMsgType[WSMsgType.ROOM_GAME_DATA_MSG], function (event: any) {
        if (event.to != self.roomId) {
          return
        }
        console.debug("handle game-data event", event);
        let witnessData = new WitnessData();
        witnessData.initWithJSON(event.witness);
        util.check(witnessData.verifyArbiterSign(client.getServerPubKey()), "check arbiter sign error.");
        console.log(witnessData.userId, self.getRival()!.id)
        if (witnessData.userId == self.getRival()!.id) {
          util.check(witnessData.verifySign(self.getRival()!.key));
          let preWitnessData = storage.getLatestWitnessData(self.roomId);
          if (preWitnessData == null) {
            console.debug("Can not find pre witness data, use begin time.");
            util.check(witnessData.verifyPreSignByBeginTime(self.room!.begin, self.getRival()!.key));
          } else {
            util.check(witnessData.verifyPreSign(preWitnessData.userId, preWitnessData.stateHash, preWitnessData.data, self.getRival()!.key));
          }
          //convert to TypedArray
          let state = Int8Array.from(witnessData.data);
          self.rivalStateUpdate(state);
          let pointer = self.game!.getState();
          let fullState = self.game!.getArray(Int8Array, pointer);
          let stateHash = crypto.hash(Buffer.from(fullState));
          console.debug("my stateHash:", stateHash.toString('hex'), "rival stateHash:", witnessData.stateHash.toString('hex'));
          util.check(stateHash.compare(witnessData.stateHash) == 0, "stateHash miss match, rival player may be cheat");
        }
        storage.addWitnessData(self.roomId, witnessData);

      });

      client.getRoom(this.roomId).then(room => {
        console.debug("room", room);
        this.room = room;
        return room
      }).then(room => {
        return client.gameInfo(room.gameId)
      }).then(gameInfo => {
        this.gameInfo = gameInfo;
        MsgBus.$emit("loading", false);
        console.debug("gameInfo", gameInfo);
        this.myRole = this.room!.players[0].playerUserId == client.getMyAddress() ? 1 : 2;
        let engineBuffer = util.decodeHex(gameInfo.engineBytes);
        let guiBuffer = util.decodeHex(gameInfo.guiBytes);
        console.debug("engineBuffer length", engineBuffer.length);
        console.debug("guiBuffer length", guiBuffer.length);
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
    getRival: function () {
      if (this.rival != null) {
        return this.rival;
      }
      if (this.room != null) {
        let rivalInfo = this.room.players.filter((it) => it.playerUserId && it.playerUserId != this.me.id).pop();
        if (rivalInfo != null) {
          this.rival = new User(crypto.fromPublicKey(util.decodeHex(rivalInfo.playerPubKey)));
        }
      }
      return this.rival;
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
      this.message = "Game begin, rival is " + this.getRival()!.id;
      vm.startGame();
      this.prepare = false;
      this.ready = true;
      this.gameBegin = true;
    },
    rivalStateUpdate: function (state: Int8Array) {
      console.debug("rivalStateUpdate:", state);
      this.game!.rivalUpdate(this.game!.newArray(state));
    },
    stateUpdate: function (fullState: Int8Array, state: Int8Array) {
      let preWitnessData = storage.getLatestWitnessData(this.roomId);
      let preSign = "";
      if (preWitnessData == null) {
        preSign = util.doSign(util.numberToBuffer(this.room!.begin), this.me.key);
      } else {
        preSign = util.doSign(preWitnessData.signData(), this.me.key);
      }
      //convert to normal array, for JSON.stringify
      let newState = Array.from(state);
      console.debug("stateUpdate:", newState);
      let witnessData = new WitnessData();
      witnessData.userId = this.me.id;
      witnessData.preSign = preSign;
      witnessData.stateHash = crypto.hash(Buffer.from(fullState));
      witnessData.data = Buffer.from(state);
      client.sendRoomGameData(this.roomId, witnessData);
    }
  }
});

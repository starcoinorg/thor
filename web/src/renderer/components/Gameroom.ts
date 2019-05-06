import Vue from "vue";
import * as client from "../sdk/client";
import {Room, User, WitnessData, WSMsgType} from "../sdk/client";
import * as lightning from "../sdk/lightning";
import Msgbus, {errorHandler, newSuccessHandler} from "./Msgbus";
import crypto from "../sdk/crypto";
import {ICanvasSYS} from "as2d/src/util/ICanvasSYS";
import * as loader from "assemblyscript/lib/loader";
import {GameGUI} from "../sdk/GameGUI";
import storage from "./storage";
import util from "../sdk/util";
import GamebordComponent from "./Gameboard";


interface ComponentData {
  me: User;
  prepare: boolean;
  ready: boolean;
  gameBegin: boolean;
  room: Room | null;
  rival: User | null;
  myRole: number;
  gameInfo: any;
  game?: ICanvasSYS & loader.ASUtil & GameGUI | null;
  gameOver: boolean;
  gameEnd: boolean;
  roomCloseCountDownTime: number;
  winner: number;
  rHash: Buffer;
  myPaymentRequest: string;
  rivalPaymentRequest: string;
  myInvoice: any;
  hasPay: boolean;
  hasChallenge: boolean;
  rivalChallenge: any;
  gameTimeout: boolean;
  arbitrateGameResult: number;
  gameResult: number; // -1 unknown 0 draw 1 win 2 lost
  gameResultNames: string[];
  surrenderCountDownTime: number;
}

export default Vue.extend({
  template: `
        <v-container fluid>
        <v-layout
          justify-center
          align-center
        >
        
        <v-dialog v-model="prepare" v-if="room" persistent max-hegith="600" max-width="600">
          <v-card v-if="!ready && room.isFree()">
            <v-card-title>
              Ready to play game?
            </v-card-title>
            <v-card-actions><v-btn flat v-on:click="doReady">Ready</v-btn><v-btn flat v-on:click="doLeave">Leave</v-btn></v-card-actions>
          </v-card>
          <!-- todo use state machine to manage  -->
          <v-card v-if="!ready && !room.isFree()">
            <v-card-title>
              <span v-if="rHash.length == 0 && rivalPaymentRequest == ''">
              Waiting to do prepare payment ...
              </span>
              <span v-if="rHash.length > 0 && rivalPaymentRequest !='' && !selfHasPay()">
              Please pay rival payment request.
              </span>
              <span v-if="selfHasPay() && !rivalHasPay()">
              Waiting rival to pay ... 
              </span>
            </v-card-title>
            <v-card-text >
              <span>cost:{{room.cost}}</span><br/>
              <span v-if="rHash.length > 0">rHash:{{rHash.toString('hex')}}</span><br/>
              <span v-if="rivalPaymentRequest">paymentRequest:{{rivalPaymentRequest}}</span><br/>
              <span >hasPay:{{hasPay}}</span><br/>
            </v-card-text>
            <v-card-actions><v-btn flat v-if="rivalPaymentRequest && !selfHasPay()" v-on:click="doPay">Pay {{room.cost}} satoshis</v-btn><v-btn flat v-on:click="doLeave">Leave</v-btn></v-card-actions>
          </v-card>
          <v-card v-if="ready && !gameBegin">
            <v-card-title>
              Waiting rival player ..
            </v-card-title>
            <v-card-actions><v-btn flat v-on:click="doLeave">Leave</v-btn></v-card-actions>
          </v-card>
      </v-dialog>
        
      <v-dialog v-model="gameOver" v-if="room" persistent max-hegith="600" max-width="600">
          <v-card>
            <v-card-title>
            <span v-if="gameResult == 1">You Win!!!</span>
            <span v-else-if="gameResult == 2">You Lost!!</span>
            <span v-else>Draw game, No winner.</span>
            </v-card-title>
            <v-card-text>
              <span v-if="gameTimeout">Your time out.</span><br/>
              <span v-if="!gameEnd && gameResult == 2">Please choice Surrender or Challenge in {{surrenderCountDownTime}} second, otherwise game will auto Surrender.</span><br/>
              <span v-if="rivalChallenge != null">Rival has challenge, Do you want challenge?</span><br/>
              <span v-if="gameEnd">Game end，room will close in {{roomCloseCountDownTime}} second.</span><br/>
              <span v-if="arbitrateGameResult>=0">Arbitrate result: {{gameResultNames[arbitrateGameResult]}}</span>
              <br/>
            </v-card-text>
            <v-card-actions v-if="!gameEnd">
              <v-btn flat v-if="gameResult == 2" v-on:click="doSurrender">Surrender</v-btn>
              <v-btn flat v-if="(myRole != winner && !hasChallenge)||(!hasChallenge && rivalChallenge != null)" v-on:click="doChallenge">Challenge</v-btn>
            </v-card-actions>
          </v-card>
      </v-dialog>
        
      <v-card v-model="room" v-if="room">
        <v-card-text >
        <v-responsive>
        <v-card>
        <v-card-title>{{room.name}} <v-spacer></v-spacer> <span v-if="room.cost>0"><v-icon>attach_money</v-icon> {{room.cost}} satoshis</span>
                  <span v-else><v-icon>money_off</v-icon></span>
        </v-card-title>
        <v-card-text>
      
          <v-list tow-line>
            <v-list-tile
              v-for="player in room.players"
              :key="player.playerUserId"
              avatar
              @click="">
                <v-list-tile-avatar>
                  <v-icon>person</v-icon>
                </v-list-tile-avatar>
                
                <v-list-tile-content>
                  <v-list-tile-title v-html="player.playerName"></v-list-tile-title>
                  <v-list-tile-sub-title v-html="player.ready"></v-list-tile-sub-title>
                </v-list-tile-content>
            </v-list-tile>
          </v-list>
        
        </v-card-text>
        </v-card>
        </v-responsive>
        <gameboard ref="gameboard"
             @gameOver="onGameOver"
             @gameStateUpdate="onGameStateUpdate"
             @gameTimeout="onGameTimout"
             @error="onError"
             v-model="gameInfo" v-bind:gameInfo="gameInfo" v-bind:role="myRole" v-bind:timeout="room.timeout"></gameboard>
        
        </v-card-text>
        <v-card-actions>
          <v-btn flat v-if="gameBegin" v-on:click="doSurrender">Surrender</v-btn>
          <v-btn flat v-on:click="doLeave">Leave</v-btn>
        </v-card-actions>
      </v-card>
      
      </v-layout>
      </v-container>
    `,
  props: ["roomId"],
  data(): ComponentData {
    return {
      me: client.getMe(),
      prepare: true,
      ready: false,
      gameBegin: false,
      room: null,
      rival: null,
      myRole: 0,
      gameInfo: null,
      game: null,
      gameOver: false,
      gameEnd: false,
      roomCloseCountDownTime: 10,
      winner: 0,
      rHash: Buffer.alloc(0),
      myPaymentRequest: "",
      rivalPaymentRequest: "",
      hasPay: false,
      myInvoice: null,
      hasChallenge: false,
      rivalChallenge: null,
      gameTimeout: false,
      gameResult: -1,
      arbitrateGameResult: -1,
      surrenderCountDownTime: 10,
      gameResultNames: ["draw", "win", "lost"]
    }
  },
  created() {
    console.debug("create component:" + this.roomId);
    this.init();
  },

  methods: {
    init() {
      console.debug("init room", this.roomId);
      Msgbus.$emit("loading", true);

      let self = this;
      Msgbus.$on(WSMsgType[WSMsgType.GAME_BEGIN], function (event: any) {
        if (event.room.roomId == self.roomId) {
          console.debug("handle game-begin event", event);
          self.room = event.room;
          self.startGame();
        }
      });

      Msgbus.$on(WSMsgType[WSMsgType.GAME_END], function (event: any) {
        if (event.roomId == self.roomId) {
          console.debug("handle game-end event", event);
          if (!event.winner) {
            self.arbitrateGameResult = 0;
          } else if (event.winner == client.getMyAddress()) {
            self.arbitrateGameResult = 1;
          } else {
            self.arbitrateGameResult = 2;
          }
          console.debug("arbitrateGameResult", self.gameResultNames[self.arbitrateGameResult]);
          self.gameEnd = true;
          self.doEnd();
        }
      });

      Msgbus.$on(WSMsgType[WSMsgType.SURRENDER_RESP], function (event: any) {
        if (event.roomId == self.roomId) {
          self.gameOver = true;
          self.gameResult = 1;
          console.debug("handle surrender resp event", event);
          if (event.r) {
            Msgbus.$emit("info", "Rival surrender, settleInvoice.");
            lightning.settleInvoice(util.decodeHex(event.r)).then(newSuccessHandler("settleInvoice success.")).catch(errorHandler);
          } else {
            Msgbus.$emit("info", "Rival surrender, game end.");
          }
        }
      });

      Msgbus.$on(WSMsgType[WSMsgType.HASH_DATA], function (event: any) {
        if (event.roomId == self.roomId) {
          self.rHash = util.decodeHex(event.rHash);
          lightning.addInvoice(self.rHash, event.cost).then(resp => {
            let payment_request = resp.payment_request;
            self.myPaymentRequest = payment_request;
            client.sendInvoiceData(self.roomId, payment_request);
            self.myInvoice = resp;
            setTimeout(self.watchInvoice, 3000);
          }).catch(errorHandler)
        }
      });

      Msgbus.$on(WSMsgType[WSMsgType.INVOICE_DATA], function (event: any) {
        if (event.roomId == self.roomId) {
          self.rivalPaymentRequest = event.paymentRequest;
        }
      });

      Msgbus.$on(WSMsgType[WSMsgType.ROOM_GAME_DATA_MSG], function (event: any) {
        if (event.to != self.roomId) {
          return
        }
        console.debug("handle game-data event", event);
        let witnessData = new WitnessData();
        witnessData.initWithJSON(event.witness);
        util.check(witnessData.verifyArbiterSign(client.getServerPubKey()), "check arbiter sign error.");
        console.log(witnessData.userId, self.getRival()!.id);
        if (witnessData.userId == self.getRival()!.id) {
          util.check(witnessData.verifySign(self.getRival()!.key));
          let preWitnessData = storage.getLatestWitnessData(self.roomId);
          if (preWitnessData == null) {
            console.debug("Can not find pre witness data, use begin time.");
            util.check(witnessData.verifyPreSignByBeginTime(self.room!.begin, self.getRival()!.key), "check preSign by BeginTime:" + self.room!.begin + " fail.");
          } else {
            util.check(witnessData.verifyPreSign(preWitnessData.userId, preWitnessData.stateHash, preWitnessData.data, self.getRival()!.key), "check preSign by Pre WitnessData:" + JSON.stringify(preWitnessData.toJSONObj()) + " fail.");
          }
          //convert to TypedArray
          let state = Int8Array.from(witnessData.data);
          self.rivalStateUpdate(state);
          //@ts-ignore
          let fullState = self.$refs.gameboard.getState();
          let stateHash = crypto.hash(Buffer.from(fullState));
          console.debug("my stateHash:", stateHash.toString('hex'), "rival stateHash:", witnessData.stateHash.toString('hex'));
          util.check(stateHash.compare(witnessData.stateHash) == 0, "stateHash miss match, rival player may be cheat");
        }
        storage.addWitnessData(self.roomId, witnessData);
      });

      Msgbus.$on(WSMsgType[WSMsgType.CHALLENGE_REQ], function (event: any) {
        self.rivalChallenge = event;
      });

      client.getRoom(this.roomId).then(room => {
        console.debug("room", room);
        this.room = room;
        this.getRHash();
        return room
      }).then(room => {
        return client.gameInfo(room.gameId)
      }).then(gameInfo => {
        this.gameInfo = gameInfo;
        Msgbus.$emit("loading", false);
        console.debug("gameInfo", gameInfo);
        this.myRole = this.room!.players[0].playerUserId == client.getMyAddress() ? 1 : 2;
      }).catch(errorHandler)
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
    getRHash: function () {
      if (this.room != null && this.rHash.length == 0) {
        let myInfo = this.room.players.filter((it) => it.playerUserId && it.playerUserId == this.me.id).pop();
        if (myInfo.rHash) {
          this.rHash = util.decodeHex(myInfo.rHash);
        }
      }
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
    doPay: function () {
      if (this.rivalPaymentRequest) {
        //FIXME lighting sendPayment with rHash will block until settle r.
        lightning.sendPayment(this.rivalPaymentRequest).then(json => {
          //this.hasPay = true;
          //this.doReady();
        }).then(newSuccessHandler("pay success.")).catch(errorHandler)
        this.hasPay = true;
        this.doReady();
      }
    },
    canReady: function (): boolean {
      if (!this.room) {
        return false;
      }
      if (this.room.isFree()) {
        return true;
      }
      return this.rivalHasPay() && this.selfHasPay();
    },
    doReady: function () {
      if (this.canReady()) {
        client.doReady(this.roomId);
        this.ready = true;
      }
    },
    countDownSurrender: function () {
      if (this.hasChallenge) {
        return;
      }
      this.surrenderCountDownTime = this.surrenderCountDownTime - 1;
      if (this.surrenderCountDownTime <= 0) {
        this.doSurrender();
      } else {
        setTimeout(this.countDownSurrender, 1000);
      }
    },
    doSurrender: function () {
      client.doSurrender(this.roomId);
    },
    doChallenge: function () {
      client.doChallenge(storage.loadWitnesses(this.roomId));
      this.hasChallenge = true;
    },
    doLeave: function () {
      client.leaveRoom(this.roomId);
    },
    doEnd: function () {
      if (!this.gameBegin) {
        this.goHome();
      } else {
        this.roomCloseCountDownTime = this.roomCloseCountDownTime - 1;
        if (this.roomCloseCountDownTime <= 0) {
          this.goHome();
        } else {
          setTimeout(this.doEnd, 1000);
        }
      }
    },
    goHome: function () {
      this.$router.push({name: 'home'});
    },
    startGame: function () {
      Msgbus.$emit("info", "Game begin, rival is " + this.getRival()!.id);
      // @ts-ignore
      this.$refs.gameboard.startGame();
      this.prepare = false;
      this.ready = true;
      this.gameBegin = true;
    },
    rivalStateUpdate: function (state: Int8Array) {
      console.debug("rivalStateUpdate:", state);
      // @ts-ignore
      this.$refs.gameboard.rivalStateUpdate(state);
    },
    lookupInvoice: function () {
      return lightning.lookupInvoice(this.rHash).catch(errorHandler)
    },
    selfHasPay: function () {
      return this.hasPay;
    },
    rivalHasPay: function () {
      return this.myInvoice && this.myInvoice.state == lightning.InvoiceState[lightning.InvoiceState.ACCEPTED];
    },
    watchInvoice: function () {
      this.lookupInvoice().then(invoice => {
        this.myInvoice = invoice;
        if (invoice.state == lightning.InvoiceState[lightning.InvoiceState.ACCEPTED]) {
          console.info("invoice state is " + invoice.state + " doReady")
          this.doReady()
        } else {
          setTimeout(this.watchInvoice, 2000)
        }
      }).catch(errorHandler);
    },
    onGameOver: function (event: any) {
      this.gameOver = true;
      this.winner = event;
      if (this.winner == 0) {
        this.gameResult = 0;
      } else if (this.winner == this.myRole) {
        this.gameResult = 1;
      } else {
        this.gameResult = 2;
        this.countDownSurrender();
      }

    },
    onGameStateUpdate: function (event: any) {
      let player = event.player;
      let state = event.state;
      let fullState = event.fullState;
      if (player == this.myRole) {
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
    },
    onGameTimout: function () {
      this.gameOver = true;
      this.gameTimeout = true;
      this.gameResult = 2;
      this.countDownSurrender();
    },
    onError: function (error: string) {
      Msgbus.$emit("error", error);
    }
  },
  components: {
    "gameboard": GamebordComponent,
  }
});

import Vue from "vue";
import * as vm from "../sdk/vm";
import * as client from "../sdk/client";
import {Room, User, WitnessData, WSMsgType} from "../sdk/client";
import * as lightning from "../sdk/lightning";
import Msgbus, {newErrorHandler, newSuccessHandler} from "./Msgbus";
import crypto from "../sdk/crypto";
import {ICanvasSYS} from "as2d/src/util/ICanvasSYS";
import * as loader from "assemblyscript/lib/loader";
import {GameGUI} from "../sdk/GameGUI";
import storage from "./storage";
import util from "../sdk/util";


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
  countDownTime: number;
  winner: number;
  rHash: Buffer;
  myPaymentRequest: string;
  rivalPaymentRequest: string;
  myInvoice: any;
  hasPay: boolean;
  hasChallenge: boolean;
  rivalChallenge: any;
}

export default Vue.extend({
  template: `
        <v-container>
        <v-card>
        <v-card-text v-if="room">
        <span >roomId:{{room.roomId}} cost:{{room.cost}} </span><br/>
        <span>players:</span><br/>
        <template v-for="(player,index) in room.players"><span>player-{{index}}:{{player.playerUserId}} ready:{{player.ready}} </span><br/></template>
        <v-container>
        
        <v-dialog v-model="prepare" persistent max-hegith="600" max-width="600">
        <v-container>
          <v-card v-if="!ready && room.isFree()">
            <v-card-title>
              Ready to play game?
            </v-card-title>
            <v-card-actions><v-btn v-on:click="doReady">Ready</v-btn><v-btn v-on:click="doLeave">Leave</v-btn></v-card-actions>
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
            <v-card-actions><v-btn v-if="rivalPaymentRequest && !selfHasPay()" v-on:click="doPay">Pay {{room.cost}} satoshis</v-btn><v-btn v-on:click="doLeave">Leave</v-btn></v-card-actions>
          </v-card>
          <v-card v-if="ready && !gameBegin">
            <v-card-title>
              Waiting rival player ..
            </v-card-title>
            <v-card-actions><v-btn v-on:click="doLeave">Leave</v-btn></v-card-actions>
          </v-card>
        </v-container>
      </v-dialog>
      
      <v-dialog v-model="gameOver" persistent max-hegith="600" max-width="600">
        <v-container>
          <v-card>
            <v-card-title>
            <span v-if="myRole == winner">You Win!!!</span><br/>
            <span v-if="myRole != winner">You Lost!!</span><br/>
            <span v-if="gameEnd">Game end，room will close in {{countDownTime}} second.</span>
            </v-card-title>
            <v-card-actions v-if="!gameEnd">
              <v-btn v-if="myRole != winner" v-on:click="doSurrender">Surrender</v-btn>
              <v-btn v-if="(myRole != winner && !hasChallenge)||(!hasChallenge && rivalChallenge != null)" v-on:click="doChallenge">Challenge</v-btn>
            </v-card-actions>
          </v-card>
        </v-container>
      </v-dialog>
      <v-card>
        <v-responsive>
        <canvas id="as2d" width="600" height="600"/>
        </v-responsive>
        <v-card-actions>
          <v-btn v-if="gameBegin" v-on:click="doSurrender">Surrender</v-btn>
          <v-btn v-on:click="doLeave">Leave</v-btn>
        </v-card-actions>
      </v-card>
        
        </v-container>
        </v-card-text>
        </v-card>
        
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
      countDownTime: 10,
      winner: 0,
      rHash: Buffer.alloc(0),
      myPaymentRequest: "",
      rivalPaymentRequest: "",
      hasPay: false,
      myInvoice: null,
      hasChallenge: false,
      rivalChallenge: null,
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
          self.gameEnd = true;
          self.doEnd();
        }
      });

      Msgbus.$on(WSMsgType[WSMsgType.SURRENDER_RESP], function (event: any) {
        if (event.roomId == self.roomId) {
          console.debug("handle surrender resp event", event);
          Msgbus.$emit("message", "Rival surrender, settleInvoice.");
          if (event.r) {
            lightning.settleInvoice(util.decodeHex(event.r)).then(newSuccessHandler("settleInvoice success.")).catch(newErrorHandler());
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
          }).catch(newErrorHandler())
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
        let engineBuffer = util.decodeHex(gameInfo.engineBytes);
        let guiBuffer = util.decodeHex(gameInfo.guiBytes);
        console.debug("engineBuffer length", engineBuffer.length);
        console.debug("guiBuffer length", guiBuffer.length);
        vm.init(this.myRole, this.stateUpdate, engineBuffer, guiBuffer, function (player: number) {
          self.gameOver = true;
          self.winner = player;
        }, function (error: string) {
          Msgbus.$emit("error", error);
        }).then(module => {
          this.game = module;
          if (this.allReady()) {
            this.startGame();
          }
        });

      }).catch(error => {
        Msgbus.$emit("error", error);
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
        }).then(newSuccessHandler("pay success.")).catch(newErrorHandler())
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
      this.countDownTime = this.countDownTime - 1;
      if (this.countDownTime <= 0) {
        this.$router.push({name: 'home'});
      } else {
        setTimeout(this.doEnd, 1000);
      }
    },
    startGame: function () {
      Msgbus.$emit("message", "Game begin, rival is " + this.getRival()!.id);
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
    },
    lookupInvoice: function () {
      return lightning.lookupInvoice(this.rHash).catch(newErrorHandler())
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
      }).catch(newErrorHandler());
    }
  }
});

import Vue from "vue";
import * as vm from "../sdk/vm";
import {ICanvasSYS} from "as2d/src/util/ICanvasSYS";
import * as loader from "assemblyscript/lib/loader";
import {GameGUI} from "../sdk/GameGUI";
import util from "../sdk/util";

let VueCountdown = require('@chenfengyuan/vue-countdown');

Vue.component(VueCountdown.name, VueCountdown);

interface ComponentData {
  prepare: boolean;
  ready: boolean;
  gameBegin: boolean;
  myRole: number;
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
  countDownStarted: boolean;
}

export default Vue.extend({
  template: `
        <v-container>
          <v-card v-if="gameInfo">
          <v-card-title>
          <span class="headline">{{gameInfo.base.gameName}}</span><span>&nbsp;&nbsp;</span>
          <span>
            <countdown ref="countdown" v-bind:time="timeout*1000" :auto-start="false" @end="onCountDownEnd">
            <template slot-scope="props">Remaining：{{ props.minutes }} minutes, {{ props.seconds }} seconds.</template>
            </countdown>
          </span>
          </v-card-title>
          <v-responsive>
            <canvas id="as2d" width="600" height="600"/>
          </v-responsive>
          </v-card>
        </v-container>
  `,
  props: {
    role: {
      type: Number,
      default: 1
    },
    gameInfo: {
      type: Object
    },
    startTime: {
      type: Number,
      default: Date.now()
    },
    timeout: {
      type: Number,
      default: 60
    },
    playWithAI: {
      type: Boolean,
      default: false
    }
  },
  data(): ComponentData {
    return {
      prepare: true,
      ready: false,
      gameBegin: false,
      myRole: 0,
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
      countDownStarted: false
    }
  },
  created() {

  },
  methods: {
    startGame: function () {
      let engineBuffer = util.decodeHex(this.gameInfo.engineBytes);
      let guiBuffer = util.decodeHex(this.gameInfo.guiBytes);
      let self = this;
      vm.init(this.role, function (player, fullState, state) {
        if (player == self.role) {
          // @ts-ignore
          self.$refs.countdown.pause();
        } else {
          if (self.countDownStarted) {
            // @ts-ignore
            self.$refs.countdown.continue();
          } else {
            self.startCountDown();
          }
        }
        self.$emit("gameStateUpdate", {player: player, fullState: fullState, state: state});
      }, engineBuffer, guiBuffer, function (player: number) {
        self.$emit("gameOver", player);
      }, function (error: string) {
        self.$emit("error", error);
      }, this.playWithAI).then(module => {
        this.game = module;
        this.game.startGame();
        if (this.role == 1) {
          this.startCountDown();
        }
        return module;
      });
    },
    startCountDown: function () {
      // @ts-ignore
      this.$refs.countdown.start();
      this.countDownStarted = true;
    },
    onCountDownEnd: function () {
      this.$emit("gameTimeout");
    }
  },
  computed: {},
  components: {}
});

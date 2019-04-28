import Vue from "vue";
import Msgbus, {newErrorHandler} from "./Msgbus";
import GamebordComponent from "./Gameboard";
import * as client from "../sdk/client";
import util from "../sdk/util";

export default Vue.extend({
  template: `
        <v-container>
          <v-card>
            <gameboard ref="gameboard"
             @gameOver="onGameOver"
             @gameStateUpdate="onGameStateUpdate"
             @gameTimeout="onGameTimout"
             @error="onError"
             v-model="gameInfo" v-bind:gameInfo="gameInfo" v-bind:role="myRole" v-bind:timeout="timeout" v-bind:playWithAI="true"></gameboard>
            <v-card-actions>
              <v-btn @click="restart">Restart Game</v-btn>
            </v-card-actions>
          </v-card>
          
        <v-dialog v-model="prepare" persistent max-hegith="600" max-width="600">
        <v-container>
          <v-card v-if="gameInfo">
            <v-card-title>
            Start {{gameInfo.base.gameName}} game with AI:
            </v-card-title>
            <v-radio-group v-model="myRole" :mandatory="false">
              <v-radio label="White" v-bind:value="1"></v-radio>
              <v-radio label="Blank" v-bind:value="2"></v-radio>
            </v-radio-group>
            <v-text-field label="Timeout:" v-model.number="timeout" type="number" suffix="seconds"></v-text-field>
            <v-card-actions>
              <v-btn @click="startGame">Start</v-btn>
            </v-card-actions>
          </v-card>
        </v-container>
        </v-dialog>
      
      <v-dialog v-model="gameOver" persistent max-hegith="600" max-width="600">
        <v-container>
          <v-card>
            <v-card-title>
            <span v-if="myRole == winner">You Win!!!</span><br/>
            <span v-if="myRole != winner">You Lost!!</span><br/>
            <span v-if="gameTimeout">Your time out.</span><br/>
            </v-card-title>
            <v-card-actions>
              <v-btn v-on:click="restart">Restart Game</v-btn>
            </v-card-actions>
          </v-card>
        </v-container>
      </v-dialog>
      
        </v-container>
    `,
  props: ['gameId'],
  data() {
    return {
      prepare: true,
      timeout: 60,
      gameInfo: null,
      myRole: 1,
      gameOver: false,
      gameTimeout: false,
      winner: 0,
    }
  },
  created() {
    this.init();
  },
  methods: {
    init: function () {
      Msgbus.$emit("loading", true);
      client.gameInfo(this.gameId).then(gameInfo => {
        this.gameInfo = gameInfo;
        Msgbus.$emit("loading", false);
        console.debug("gameInfo", gameInfo);
        let engineBuffer = util.decodeHex(gameInfo.engineBytes);
        let guiBuffer = util.decodeHex(gameInfo.guiBytes);
        console.debug("engineBuffer length", engineBuffer.length);
        console.debug("guiBuffer length", guiBuffer.length);
      }).catch(newErrorHandler())
    },
    startGame: function () {
      // @ts-ignore
      this.$refs.gameboard.startGame();
      this.prepare = false;
    },
    restart: function () {
      this.$router.go(0);
    },
    onGameOver: function (event: any) {
      this.gameOver = true;
      this.winner = event;
    },
    onGameStateUpdate: function (event: any) {
      console.log(event)
    },
    onGameTimout: function () {
      this.gameOver = true;
      this.gameTimeout = true;
    },
    onError: function (error: string) {
      Msgbus.$emit(error, error);
    }
  },
  computed: {},
  components: {
    "gameboard": GamebordComponent,
  }
});

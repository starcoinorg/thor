import Vue from "vue";
import Msgbus, {errorHandler} from "./Msgbus";
import GamebordComponent from "./Gameboard";
import * as client from "../sdk/client";
import util from "../sdk/util";

export default Vue.extend({
  template: `
        <v-container fluid>
        <v-layout
          justify-center
          align-center
        >
          <v-card>
            <gameboard ref="gameboard"
             @gameOver="onGameOver"
             @gameStateUpdate="onGameStateUpdate"
             @gameTimeout="onGameTimout"
             @error="onError"
             v-model="gameInfo" v-bind:gameInfo="gameInfo" v-bind:role="myRole" v-bind:timeout="timeout" v-bind:playWithAI="true"></gameboard>
            <v-card-actions>
              <v-layout justify-center align-center>
              <v-tooltip top>
                <template v-slot:activator="{ on }">
                <v-btn flat icon v-on:click="restart()" v-on="on"><v-icon>replay</v-icon></v-btn>
                </template>
                <span>Restart Game</span>
              </v-tooltip>
              </v-layout>
            </v-card-actions>
          </v-card>
          
        <v-dialog v-model="prepare" persistent max-hegith="600" max-width="600">
          <v-card v-if="gameInfo">
            <v-card-title>
            <span class="headline">Start {{gameInfo.base.gameName}} game with AI:</span>
            </v-card-title>
            <v-card-text>
            <v-container grid-list-md>
            <v-layout column>
            <v-flex>
            <v-radio-group label="Chess:" v-model="myRole" :mandatory="false">
              <v-radio label="White" v-bind:value="1"></v-radio>
              <v-radio label="Blank" v-bind:value="2"></v-radio>
            </v-radio-group>
            </v-flex>
            <v-flex>
            <v-text-field label="Timeout:" v-model.number="timeout" type="number" suffix="seconds"></v-text-field>
            </v-flex>
            </v-layout>
            </v-container>
            </v-card-text>
            <v-card-actions>
              <v-layout justify-center align-center>
              <v-tooltip top>
                <template v-slot:activator="{ on }">
                  <v-btn flat icon @click="startGame" v-on="on"><v-icon>play_arrow</v-icon></v-btn>
                </template>
                <span>Start Game</span>
              </v-tooltip>
              </v-layout>
            </v-card-actions>
          </v-card>
        </v-dialog>
      
      <v-dialog v-model="gameOver" persistent max-hegith="600" max-width="600">
          <v-card>
            <v-card-title>
            <span v-if="myRole == winner">You Win!!!</span><br/>
            <span v-if="myRole != winner">You Lost!!</span><br/>
            <span v-if="gameTimeout">You time out.</span><br/>
            </v-card-title>
            <v-card-actions>
              <v-layout justify-center align-center>
              <v-tooltip top>
                <template v-slot:activator="{ on }">
                <v-btn flat icon v-on:click="restart()" v-on="on"><v-icon>replay</v-icon></v-btn>
                </template>
                <span>Restart Game</span>
              </v-tooltip>
              <v-tooltip top>
                <template v-slot:activator="{ on }">
                  <v-btn flat icon v-on:click="quit()" v-on="on"><v-icon>exit_to_app</v-icon></v-btn>
                </template>
                <span>Quit to home</span>
              </v-tooltip>
              </v-layout>
              </v-card-actions>
            </v-card>
          </v-card>
      </v-dialog>
      </v-layout>
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
      }).catch(errorHandler)
    },
    startGame: function () {
      // @ts-ignore
      this.$refs.gameboard.startGame();
      this.prepare = false;
    },
    restart: function () {
      this.$router.go(0);
    },
    quit: function () {
      this.$router.push({name: "home"});
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
      Msgbus.$emit("error", error);
    }
  },
  computed: {},
  components: {
    "gameboard": GamebordComponent,
  }
});

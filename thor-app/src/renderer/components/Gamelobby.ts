import Vue from "vue";
import * as client from "../sdk/client";
import {Room} from "../sdk/client";
import Msgbus, {errorHandler} from "./Msgbus";
import * as lightning from "../sdk/lightning";
import util from "../sdk/util";

export default Vue.extend({
  template: `
     <v-responsive>
         <v-card>
         <v-card-title>
          <span class="subheading">Game List</span>
         </v-card-title>
         <v-card-text>
           <v-list tow-line>
            <v-list-tile
              v-for="game in gameList"
              :key="game.hash"
              avatar
              @click=""
            >
                <v-list-tile-content>
                  <v-list-tile-title v-html="game.gameName"></v-list-tile-title>
                  <v-list-tile-sub-title>id:{{game.hash}}</v-list-tile-sub-title>
                </v-list-tile-content>
                <v-list-tile-action>
                <v-btn color="primary" flat @click="showCreateRoom(game)"><v-icon>add</v-icon> Room</v-btn>
                </v-list-tile-action>
                <v-list-tile-action>
                <v-btn flat @click="soloPlay(game.hash)"><v-icon>play_arrow</v-icon> with AI</v-btn>
                </v-list-tile-action>
              </v-list-tile>
          </v-list>
          </v-card-text>
          </v-card>
          <v-card>
          <v-card-title>
            <span class="subheading">Room List</span>
          </v-card-title>
          <v-card-text>
            <v-list three-line>
            <v-list-tile
              v-for="room in roomList"
              :key="room.roomId"
              avatar
              @click=""
            >
                <v-list-tile-avatar>
                  <v-icon v-if="room.cost>0">attach_money</v-icon>
                  <v-icon v-else="room.cost>0">money_off</v-icon>
                </v-list-tile-avatar>
                <v-list-tile-content>
                  <v-list-tile-title v-html="room.name"></v-list-tile-title>
                  <v-list-tile-sub-title>game:{{getGame(room.gameId).gameName}}</v-list-tile-sub-title>
                  <v-list-tile-sub-title><template v-for="player in room.players">player:{{player.playerName}} </template></v-list-tile-sub-title>
                </v-list-tile-content>
                <v-list-tile-action>
                <v-tooltip top>
                <template v-slot:activator="{ on }">
                  <v-btn flat icon v-on:click="joinRoom(room.roomId)" v-on="on"><v-icon>person_add</v-icon></v-btn>
                </template>
                <span>Join Room</span>
                </v-tooltip>
                </v-list-tile-action>
            </v-list-tile>
            <v-list-tile v-if="roomList.length == 0">
            <v-list-tile-content>No Room. Please create a new Room.</v-list-tile-content>
            </v-list-tile>
          </v-list>
         </v-card-text>
         </v-card>
          
        <v-dialog v-model="createRoomGame" v-if="createRoomGame" max-width="500px">
          <v-card>
            <v-card-title>
              <span>Create Room for Game {{createRoomGame.gameName}}</span>
            </v-card-title>
            <v-card-text>
              <v-text-field label="Name:" v-model="roomName"></v-text-field>
              <v-text-field label="Cost:" v-model.number="roomCost"></v-text-field>
              <v-text-field label="Timeout:" v-model.number="roomTimeout" suffix="seconds"></v-text-field>
            </v-card-text>
            <v-card-actions>
              <v-btn color="primary" flat @click="createRoom()">Create</v-btn>
              <v-btn flat @click="createRoomGame=null">Close</v-btn>
            </v-card-actions>
          </v-card>
        </v-dialog>
        
     </v-responsive>
    `,
  data() {
    return {
      createRoomGame: null,
      roomCost: 0,
      roomName: "",
      roomTimeout: 60,
      gameList: [],
      roomList: [],
      myAddress: client.getMyAddress()
    }
  },
  created() {
    this.init();
  },
  watch: {},
  methods: {
    init: function () {
      Msgbus.$on("refresh", () => {
        this.refresh();
      });
      this.refresh();
    },
    showCreateRoom: function (game: any) {
      this.createRoomGame = game;
      this.roomName = game.gameName + '-' + Math.random().toString(36).substr(2, 3);
    },
    createRoom: function () {
      // @ts-ignore
      client.createRoom(this.createRoomGame!.hash, this.roomName, this.roomCost, this.roomTimeout);
      this.createRoomGame = null;
      setTimeout(() => this.fetchRoomList(), 500);

    },
    fetchGameList: function () {
      Msgbus.$emit("loading", true);
      return client.gameList().then(resp => {
        this.gameList = resp.data;
        Msgbus.$emit("loading", false);
        return resp;
      }).catch(errorHandler);
    },
    fetchRoomList: function () {
      this.roomList = [];
      Msgbus.$emit("loading", true);
      client.roomList().then(resp => {
        resp.data.forEach((jsonObj: any) => {
          // @ts-ignore
          this.roomList.push(util.unmarshal(new Room(), jsonObj))
        });
        Msgbus.$emit("loading", false);
        return resp;
      }).catch(errorHandler);
    },
    refresh: function () {
      this.fetchGameList().then(() => {
        this.fetchRoomList();
      });
    },
    getGame(gameId: string): any {
      return this.gameList.find((value: any) => value.hash == gameId);
    },
    getRoom(roomId: string): any {
      return this.roomList.find((value: any) => value.roomId == roomId)
    },
    joinRoom: function (roomId: string) {
      let room = this.getRoom(roomId);
      if (room && !room.isFree()) {
        //check lnd config
        if (!lightning.hasInit()) {
          Msgbus.$emit("error", "Please config lightning network first.");
          this.$router.push({name: "config"});
          return
        }
      }
      if (room && room.players.find((value: any) => value.playerUserId == this.myAddress)) {
        this.$router.push({name: 'room', params: {roomId: room.roomId}})
      } else {
        client.joinRoom(roomId);
      }
    },
    soloPlay(gameId: string) {
      this.$router.push({name: 'solo', params: {gameId: gameId}})
    }
  }
});

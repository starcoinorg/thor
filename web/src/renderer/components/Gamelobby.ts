import Vue from "vue";
import * as client from "../sdk/client";
import {Room} from "../sdk/client";
import Msgbus, {newErrorHandler} from "./Msgbus";
import * as lightning from "../sdk/lightning";
import util from "../sdk/util";

export default Vue.extend({
  template: `
     <v-container>
         <v-list tow-line>
            <v-subheader>
            Game List
            </v-subheader>
            <v-list-tile
              v-for="game in gameList"
              :key="game.hash"
              avatar
              @click=""
            >
                <v-list-tile-content>
                  <v-list-tile-title v-html="game.gameName"></v-list-tile-title>
                  <v-list-tile-sub-title v-html="game.hash"></v-list-tile-sub-title>
                </v-list-tile-content>
                <v-list-tile-action>
                <v-btn @click="createRoomGame=game.hash;createRomeDialog=true">Create Room</v-btn>
                </v-list-tile-action>
                <v-list-tile-action>
                <v-btn @click="soloPlay(game.hash)">Player with AI</v-btn>
                </v-list-tile-action>
              </v-list-tile>
        </v-list>
        
        <v-list three-line>
            <v-subheader>
            Room List 
            </v-subheader>
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
                  <v-list-tile-title v-html="room.roomId"></v-list-tile-title>
                  <v-list-tile-sub-title v-html="room.gameId"></v-list-tile-sub-title>
                  <v-list-tile-sub-title><template v-for="player in room.players">player:{{player.playerUserId}} </template></v-list-tile-sub-title>
                </v-list-tile-content>
                <v-list-tile-action>
                <v-btn v-on:click="joinRoom(room.roomId)">Join Room</v-btn>
                </v-list-tile-action>
              </v-list-tile>
        </v-list>
        
        <v-dialog v-model="createRomeDialog" max-width="500px">
          <v-card>
            <v-card-title>
              <span>Create Room</span>
            </v-card-title>
            <v-card-text>
              <span>game:{{createRoomGame}}</span>
              <v-text-field label="Cost:" v-model="cost"></v-text-field>
              <v-text-field label="Timeout:" v-model="timeout" suffix="seconds"></v-text-field>
            </v-card-text>
            <v-card-actions>
              <v-btn color="primary" flat @click="createRoom()">Create</v-btn>
              <v-btn flat @click="createRomeDialog=false">Close</v-btn>
            </v-card-actions>
          </v-card>
        </v-dialog>
        
     </v-container>
    `,
  data() {
    return {
      createRomeDialog: false,
      createRoomGame: "",
      cost: 0,
      timeout: 60,
      gameList: [],
      roomList: [],
      myAddress: client.getMyAddress()
    }
  },
  created() {
    this.init();
  },
  watch: {
  },
  methods: {
    init: function () {
      Msgbus.$on("refresh", () => {
        this.refresh();
      });
      this.refresh();
    },
    createRoom: function () {
      client.createRoom(this.createRoomGame, this.cost, this.timeout).then(resp => {
        this.createRomeDialog = false;
        this.fetchRoomList();
      })
    },
    fetchGameList: function () {
      Msgbus.$emit("loading", true);
      client.gameList().then(resp => {
        this.gameList = resp.data
        Msgbus.$emit("loading", false);
      }).catch(newErrorHandler());
    },
    fetchRoomList: function () {
      this.roomList = [];
      Msgbus.$emit("loading", true);
      client.roomList().then(resp => {
        resp.data.forEach((jsonObj: any) => {
          // @ts-ignore
          this.roomList.push(util.unmarshal(new Room(), jsonObj))
        })
        Msgbus.$emit("loading", false);
      }).catch(newErrorHandler());
    },
    refresh: function () {
      this.fetchGameList();
      this.fetchRoomList();
    },
    getRoom(roomId: string): any {
      return this.roomList.find((value: any) => value.roomId == roomId)
    },
    joinRoom: function (roomId: string) {
      let room = this.getRoom(roomId);
      if (!room.isFree()) {
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

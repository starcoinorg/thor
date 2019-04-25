import Vue from "vue";
import * as client from "../sdk/client";
import MsgBus from "./Msgbus";

export default Vue.extend({
  template: `
        <div>
        <v-container
      id="input-usage"
      grid-list-xl
      fluid>
      <v-layout wrap>
        <v-flex xs12>
        
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
              </v-list-tile>
          </v-list>
        
        <v-list three-line>
            <v-subheader>
            Room List 
            </v-subheader>
            <v-subheader>
            <v-btn icon @click="fetchRoomList()" >Refresh</v-btn>
            </v-subheader>
            <v-list-tile
              v-for="room in roomList"
              :key="room.roomId"
              avatar
              @click=""
            >
                <v-list-tile-avatar>
                  <v-icon v-if="room.cost>0">money</v-icon>
                  <v-icon v-else="room.cost>0">free</v-icon>
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
            </v-card-text>
            <v-card-actions>
              <v-btn color="primary" flat @click="createRoom()">Create</v-btn>
              <v-btn flat @click="createRomeDialog=false">Close</v-btn>
            </v-card-actions>
          </v-card>
        </v-dialog>
        
        </v-flex>
        </v-layout>
        </v-container>
        </div>
    `,
  data() {
    return {
      error: null,
      message: "",
      createRomeDialog: false,
      createRoomGame: "",
      cost: 0,
      gameList: [],
      roomList: [],
      myAddress: client.getMyAddress()
    }
  },
  created() {
    // fetch the data when the view is created and the data is
    // already being observed
    MsgBus.$emit("loading", true);
    this.fetchGameList();
    this.fetchRoomList();
    MsgBus.$emit("loading", false);
  },
  watch: {
    // call again the method if the route changes
    //'$route': 'fetchGameList',
    //'$route': 'fetchGameList'
  },
  methods: {
    createRoom: function () {
      client.createRoom(this.createRoomGame, this.cost).then(resp => {
        this.createRomeDialog = false;
        this.fetchRoomList();
      })
    },
    fetchGameList: function () {
      this.error = null;
      MsgBus.$emit("loading", true);
      return client.gameList().then(resp => {
        this.gameList = resp.data
      }).catch(error => {
        this.error = error
      })
    },
    fetchRoomList: function () {
      this.error = null;
      return client.roomList().then(resp => {
        this.roomList = resp.data
      }).catch(error => {
        this.error = error
      })
    },
    getRoom(roomId: string): any {
      return this.roomList.find((value: any) => value.roomId == roomId)
    },
    joinRoom: function (roomId: string) {
      let room = this.getRoom(roomId);
      if (room && room.players.find((value: any) => value.playerUserId == this.myAddress)) {
        this.$router.push({name: 'room', params: {roomId: room.roomId}})
      } else {
        client.joinRoom(roomId);
      }

      // let self = this
      // setTimeout(function () {
      //     self.fetchRoomList();
      // }, 100);

    }
  }
});

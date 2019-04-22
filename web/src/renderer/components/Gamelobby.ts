import Vue from "vue";
import * as client from "../sdk/client";

export default Vue.extend({
  template: `
        <div>
        <div class="list">
        Game List
        <div class="loading" v-if="loading">
            Loading...
        </div>
        <div v-if="error" class="error">
        {{ error }}
        </div>
        <ul>
            <template v-for="game in gameList">
            <li>
                game: {{game.gameName}} {{ game.hash }}
                <button v-on:click="createRoom(game.hash)">Create Room</button>
            </li>
            </template>
        </ul>
        </div>
        <div class="list">Room List
        <ul>
            <template v-for="room in roomList">
            <li>
                room:{{ room.roomId }} game:{{room.gameId}} <template v-for="player in room.players">player:{{player}} </template>
                <button v-on:click="joinRoom(room.roomId)">Join Room</button>
            </li>
            </template>
        </ul>
        </div>
        </div>
    `,
  data() {
    return {
      loading: false,
      error: null,
      gameList: [],
      roomList: []
    }
  },
  created() {
    // fetch the data when the view is created and the data is
    // already being observed
    this.fetchGameList()
    this.fetchRoomList()
  },
  watch: {
    // call again the method if the route changes
    //'$route': 'fetchGameList',
    //'$route': 'fetchGameList'
  },
  methods: {
    createRoom: function (gameHash: string) {
      client.createRoom(gameHash).then(resp => {
        this.fetchRoomList()
      })
    },
    fetchGameList: function () {
      this.error = null;
      this.loading = true;
      return client.gameList().then(resp => {
        this.gameList = resp.data
        this.loading = false
      }).catch(error => {
        this.error = error
      })
    },
    fetchRoomList: function () {
      this.error = null;
      this.loading = true;
      return client.roomList().then(resp => {
        this.roomList = resp.data
        this.loading = false
      }).catch(error => {
        this.error = error
      })
    },
    joinRoom: function (roomId: string) {
      client.joinRoom(roomId);
      this.$router.push({name: 'room', params: {roomId: roomId}})
      // let self = this
      // setTimeout(function () {
      //     self.fetchRoomList();
      // }, 100);

    }
  }
});

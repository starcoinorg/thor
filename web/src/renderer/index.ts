import Vue from "vue";
import VueRouter from "vue-router";
import Vuetify from 'vuetify'
import 'vuetify/dist/vuetify.min.css';
import HelloComponent from "./components/Hello";
import GamelobbyComponent from "./components/Gamelobby";
import GameroomComponent from "./components/Gameroom";
import LightningComponent from "./components/Lightning";
import MsgBus from "./components/Msgbus";
import * as client from "./sdk/client";
import {WSMsgType} from "./sdk/client";

Vue.use(VueRouter)
Vue.use(Vuetify)

//const Home = Vue.component("home", {template: '<div>home</div>'});

const routes = [
  {name: "home", path: '/', component: GamelobbyComponent},
  {name: "lobby", path: '/lobby', component: GamelobbyComponent},
  {name: "config", path: '/config', component: LightningComponent},
  {name: "room", path: '/room/:roomId', component: GameroomComponent, props: true},
  {name: "hello", path: '/hello/:name/:initialEnthusiasm', component: HelloComponent, props: true}
];

const router = new VueRouter({
  routes
});


const app = new Vue({
  template: `
        <div id="app">
        <v-app id="inspire">
        
        <v-dialog v-model="loading" persistent fullscreen content-class="loading-dialog">
        <v-container fill-height>
          <v-layout row justify-center align-center>
            <v-progress-circular indeterminate :size="70" :width="7" color="purple"></v-progress-circular>
          </v-layout>
        </v-container>
      </v-dialog>
        
        <v-toolbar>
      <v-toolbar-title>Thor App</v-toolbar-title>
      <v-spacer></v-spacer>
      <v-toolbar-items>
        <v-btn flat @click="$router.push('/')">Home</v-btn>
        <v-btn flat @click="$router.push('/config')">Config</v-btn>
      </v-toolbar-items>
    </v-toolbar>
      <v-container>
        <router-view></router-view>
      </v-container>  
        <v-footer>
        <div>my address:{{address}}</div>
        <div>current location:{{location}}</div>
        </v-footer>
        </v-app>
        </div>
    `,
  router,
  data() {
    return {
      loading: false
    }
  },
  created() {
    console.log("app create", this.$refs);
    MsgBus.init();
    this.initGlobalEventsHander();
  },
  methods: {
    initGlobalEventsHander: function () {
      let self = this;
      MsgBus.$on(WSMsgType[WSMsgType.JOIN_ROOM_RESP], function (event: any) {
        console.log("handle JOIN_ROOM_RESP event", event);
        if (event.succ) {
          let room = event.room;
          self.$router.push({name: 'room', params: {roomId: room.roomId}})
        }
      });
      MsgBus.$on("loading", function (loading: any) {
        self.loading = loading;
      })
    }
  },
  computed: {
    location: function () {
      return window.location;
    },
    address: function () {
      return client.getMyAddress();
    }
  },
  components: {}
});
app.$mount('#app');

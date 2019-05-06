import Vue from "vue";
import VueRouter from "vue-router";
import Vuetify from 'vuetify'
import 'vuetify/dist/vuetify.min.css';
import 'material-design-icons/iconfont/material-icons.css'
import 'typeface-roboto/index.css'
import HelloComponent from "./components/Hello";
import GamelobbyComponent from "./components/Gamelobby";
import GameroomComponent from "./components/Gameroom";
import WalletComponent from "./components/Wallet";
import ConfigComponent from "./components/Config";
import SoloComponent from "./components/Solo";
import DebugComponent from "./components/Debug";
import Msgbus from "./components/Msgbus";
import * as client from "./sdk/client";
import {WSMsgType} from "./sdk/client";

import colors from 'vuetify/es5/util/colors'

Vue.use(VueRouter);
Vue.use(Vuetify, {
  theme: {
    primary: colors.indigo.base,
    secondary: colors.lightBlue.base,
    accent: colors.lightGreen.base,
    error: colors.red.base,
    warning: colors.orange.base,
    info: colors.grey.base,
    success: colors.green.base
  }
});

const routes = [
  {name: "home", path: '/', component: GamelobbyComponent},
  {name: "lobby", path: '/lobby', component: GamelobbyComponent},
  {name: "config", path: '/config', component: ConfigComponent},
  {name: "wallet", path: '/wallet', component: WalletComponent},
  {name: "room", path: '/room/:roomId', component: GameroomComponent, props: true},
  {name: "solo", path: '/solo/:gameId', component: SoloComponent, props: true},
  {name: "hello", path: '/hello/:name/:initialEnthusiasm', component: HelloComponent, props: true},
  {name: "debug", path: '/debug', component: DebugComponent, props: true},
];

const router = new VueRouter({
  routes
});


const app = new Vue({
  template: `
    <v-app id="thor">
    <v-content>
    
    
    <v-dialog v-model="loading" persistent content-class="loading-dialog">
        <v-container fill-height>
          <v-layout row justify-center align-center>
            <v-btn icon @click="$router.push({name:'home'})"><v-icon>refresh</v-icon></v-btn>
            <v-progress-circular indeterminate :size="70" :width="7" color="purple"></v-progress-circular>
          </v-layout>
        </v-container>
      </v-dialog>
      
    <v-toolbar color="primary" dark fixed app>
      <v-toolbar-title>Thor App</v-toolbar-title>
      <v-spacer></v-spacer>
      <v-toolbar-items>
        <v-btn flat @click="$router.push('/')">Home</v-btn>
        <v-btn flat @click="$router.push('/wallet')">Wallet</v-btn>
        <v-btn flat @click="$router.push('/config')">Config</v-btn>
        <v-btn v-if="isDevelopment" flat @click="$router.push('/debug')">Debug</v-btn>
        <v-btn icon @click="refresh()">
          <v-icon>refresh</v-icon>
        </v-btn>
      </v-toolbar-items>
    </v-toolbar>
    
    
      <v-snackbar
        v-model="showMessage"
        :color="color"
        :multi-line="false"
        :timeout="2000"
        :vertical="false">
        {{ message }}
        <v-btn
          dark
          flat
          icon
          @click="showMessage = false"
        >
        <v-icon>close</v-icon>
        </v-btn>
      </v-snackbar>
    
      <v-container fluid>
        <router-view></router-view>
      </v-container>
      
      <v-footer fixed dark app>
        <v-card class="flex" flat tile>
        <v-card-text>
        <span>@Thor</span>
        <span v-if="isDevelopment">
        <br/><span>address:{{address}}</span><span>&nbsp;&nbsp;&nbsp;</span><span>location:{{location}}</span>
        </span>
        </v-card-text>
        </v-card>
      </v-footer>
      
    </v-content>
    </v-app>
    `,
  router,
  data() {
    return {
      loading: false,
      message: "",
      color: "",
      showMessage: false,
      isDevelopment: process.env.NODE_ENV !== 'production'
    }
  },
  created() {
    console.log("app create", this.$refs);
    Msgbus.init();
    this.initGlobalEventsHander();
  },
  watch: {},
  methods: {
    initGlobalEventsHander: function () {
      let self = this;
      Msgbus.$on(WSMsgType[WSMsgType.JOIN_ROOM_RESP], function (event: any) {
        console.log("handle JOIN_ROOM_RESP event", event);
        if (event.succ) {
          let room = event.room;
          self.$router.push({name: 'room', params: {roomId: room.roomId}})
        }
      });
      Msgbus.$on(WSMsgType[WSMsgType.CREATE_ROOM_RESP], function (event: any) {
        console.log("handle CREATE_ROOM_RESP event", event);
        let room = event.room;
        self.$router.push({name: 'room', params: {roomId: room.roomId}})
      });
      Msgbus.$on(WSMsgType[WSMsgType.ERR], function (event: any) {
        self.$emit("error", event.err);
        if (event.code == 404) {
          self.$router.push({name: "home"});
        }
      });
      Msgbus.$on("loading", function (loading: any) {
        self.loading = loading;
      });
      Msgbus.$on("error", function (message: any) {
        self.message = message;
        self.color = "error";
        self.showMessage = true;
      });
      Msgbus.$on("info", function (message: any) {
        self.color = "info";
        self.message = message;
        self.showMessage = true;
      });
      Msgbus.$on("success", function (message: any) {
        self.color = "success";
        self.message = message;
        self.showMessage = true;
      })
    },
    refresh: function () {
      Msgbus.$emit("refresh");
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

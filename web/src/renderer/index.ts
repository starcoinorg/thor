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
import Msgbus from "./components/Msgbus";
import * as client from "./sdk/client";
import {WSMsgType} from "./sdk/client";

Vue.use(VueRouter)
Vue.use(Vuetify)

//const Home = Vue.component("home", {template: '<div>home</div>'});

const routes = [
  {name: "home", path: '/', component: GamelobbyComponent},
  {name: "lobby", path: '/lobby', component: GamelobbyComponent},
  {name: "config", path: '/config', component: ConfigComponent},
  {name: "wallet", path: '/wallet', component: WalletComponent},
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
      <v-content>
      <v-container fluid>
    
      <v-dialog v-model="loading" persistent fullscreen content-class="loading-dialog">
        <v-container fill-height>
          <v-layout row justify-center align-center>
            <v-progress-circular indeterminate :size="70" :width="7" color="purple"></v-progress-circular>
          </v-layout>
        </v-container>
      </v-dialog>
        
    <v-toolbar app>
      <v-toolbar-title>Thor App</v-toolbar-title>
      <v-spacer></v-spacer>
      <v-toolbar-items>
        <v-btn flat @click="$router.push('/')">Home</v-btn>
        <v-btn flat @click="$router.push('/wallet')">Wallet</v-btn>
        <v-btn flat @click="$router.push('/config')">Config</v-btn>
        <v-btn icon @click="refresh()">
          <v-icon>refresh</v-icon>
        </v-btn>
      </v-toolbar-items>
    </v-toolbar>
    
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
    
        <router-view></router-view>
        <v-footer>
        <div>my address:{{address}}</div>
        <div>current location:{{location}}</div>
        </v-footer>
        </v-container>
      </v-content>
      </v-app>
      </div>
    `,
  router,
  data() {
    return {
      loading: false,
      message: "",
      error: ""
    }
  },
  created() {
    console.log("app create", this.$refs);
    Msgbus.init();
    this.initGlobalEventsHander();
  },
  watch: {
    message: function (newMessage, oldMessage) {
      if (!oldMessage) {
        setTimeout(() => {
          this.message = "";
        }, 1000)
      }
    },
    error: function (newError, oldError) {
      if (!oldError) {
        setTimeout(() => {
          this.error = "";
        }, 1000)
      }
    }
  },
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
      Msgbus.$on(WSMsgType[WSMsgType.ERR], function (event: any) {
        self.$emit("error", event.err);
        if (event.code == 404) {
          self.$router.push({name: "home"});
        }
      });
      Msgbus.$on("loading", function (loading: any) {
        self.loading = loading;
      });
      Msgbus.$on("error", function (error: any) {
        self.error = error;
      });
      Msgbus.$on("message", function (message: any) {
        self.message = message;
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

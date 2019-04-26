import Vue from "vue";
import * as lightning from "../sdk/lightning";
import storage from "./storage";
import Msgbus from "./Msgbus";

export default Vue.extend({
  template: `
        <v-container>
          <v-card>
            <v-card-title>
              Config
            </v-card-title>
            <v-card-text>
            <v-text-field label="lndUrl:" v-model="lndUrl"></v-text-field>
            <v-text-field label="lndMacaroon:" v-model="lndMacaroon"></v-text-field>
            </v-card-text>
            <v-card-actions><v-btn v-on:click="testConnect">Test</v-btn> <v-btn v-on:click="save">Save</v-btn> </v-card-actions>
          </v-card>
        </v-container>
    `,
  data() {
    return {
      lndUrl: '',
      lndMacaroon: '',
    }
  },
  created() {
    let config = storage.loadConfig();
    this.lndUrl = config.lndUrl;
    this.lndMacaroon = config.lndMacaroon;
  },
  methods: {
    testConnect() {
      if (this.lndUrl == '' || this.lndMacaroon == '') {
        Msgbus.$emit("error", "Please set lndUrl and lndMacaroon");
        return
      }
      lightning.init(this.$data);
      lightning.getinfo().then(json => {
        Msgbus.$emit("message", "connect success:" + JSON.stringify(json))
      }).catch(e => Msgbus.$emit("error", "connect fail:" + e));
    },
    save() {
      if (this.lndUrl == '' || this.lndMacaroon == '') {
        Msgbus.$emit("error", "Please set lndUrl and lndMacaroon");
        return
      }
      storage.saveConfig(this.$data)
      Msgbus.$emit("message", "Save success.");
    }
  },
  computed: {}
});

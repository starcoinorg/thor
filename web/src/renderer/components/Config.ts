import Vue from "vue";
import * as lightning from "../sdk/lightning";
import storage from "./storage";
import Msgbus from "./Msgbus";

export default Vue.extend({
  template: `
          <v-card>
            <v-card-title>
              <span class="headline">Config</span>
            </v-card-title>
            <v-card-text>
            <v-text-field label="lndUrl:" v-model="lndUrl"></v-text-field>
            <v-text-field label="lndMacaroon:" v-model="lndMacaroon"></v-text-field>
            </v-card-text>
            <v-card-actions><v-btn flat v-on:click="testConnect">Test</v-btn> <v-btn flat v-on:click="save">Save</v-btn> </v-card-actions>
          </v-card>
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
        Msgbus.$emit("success", "connect " + this.lndUrl + " success")
      }).catch(e => Msgbus.$emit("error", "connect fail:" + e));
    },
    save() {
      if (this.lndUrl == '' || this.lndMacaroon == '') {
        Msgbus.$emit("error", "Please set lndUrl and lndMacaroon");
        return
      }
      storage.saveConfig(this.$data)
      Msgbus.$emit("success", "Save success.");
    }
  },
  computed: {}
});

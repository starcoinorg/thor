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
      lndUrl: 'https://starcoin-firstbox:9080',
      lndMacaroon: '0201036c6e6402cf01030a1077ee9770560ab03adeef296fd961d7551201301a160a0761646472657373120472656164120577726974651a130a04696e666f120472656164120577726974651a170a08696e766f69636573120472656164120577726974651a160a076d657373616765120472656164120577726974651a170a086f6666636861696e120472656164120577726974651a160a076f6e636861696e120472656164120577726974651a140a057065657273120472656164120577726974651a120a067369676e6572120867656e65726174650000062088232ae979d750e917d4b0131576adbdf139311eb4f27376dd396a3ea628fd29',
    }
  },
  created() {

  },
  methods: {
    testConnect() {
      lightning.init(this.$data);
      lightning.getinfo().then(json => {
        Msgbus.$emit("message", "connect success:" + JSON.stringify(json))
      }).catch(e => Msgbus.$emit("error", "connect fail:" + e));
    },
    save() {
      storage.saveConfig(this.$data)
    }
  },
  computed: {}
});

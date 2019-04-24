import Vue from "vue";
import * as lightning from "../sdk/lightning";

export default Vue.extend({
  template: `
        <div>
            <div>Lightning Config</div>
            <label for="lndUrl">lndUrl</label>:<input v-model="lndUrl" placeholder="" ><br/>
            <label for="lndMacaroon">lndMacaroon</label><input v-model="lndMacaroon" placeholder="" ><br/>
            <button @click="connect">Test Connect to Lnd</button>
        </div>
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
    connect() {
      let config = new lightning.Config(this.lndUrl, this.lndMacaroon);
      lightning.init(config);
      lightning.invoice().then(json => alert("connect success:" + JSON.stringify(json))).catch(e => alert("connect fail:" + e));
    },
  },
  computed: {}
});

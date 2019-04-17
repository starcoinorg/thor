import Vue from "vue";
import * as lightning from "../sdk/lightning";

export default Vue.extend({
  template: `
        <div>
            <div>Lightning Config</div>
            <label>lndUrl</label>:<input v-model="lndUrl" placeholder="starcoin-firstbox:30009"><br/>
            <label>lndPassword</label><input v-model="lndPassword" placeholder="starcoin"><br/>
            <label>lndCert</label><br/>
            <textarea v-model="lndCert"></textarea><br/>
            <button @click="connect">Connect to Lnd</button>
        </div>
    `,
  data() {
    return {
      lndUrl: '',
      lndPassword: '',
      lndCert: ''
    }
  },
  methods: {
    connect() {
      let config = new lightning.Config(this.lndUrl, this.lndPassword, Buffer.from(this.lndCert, 'ascii'));
      lightning.init(config)
    },
  },
  computed: {}
});

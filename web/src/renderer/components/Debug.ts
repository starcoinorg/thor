import Vue from "vue";
import Msgbus from "./Msgbus"

export default Vue.extend({
  template: `
        <v-container>
          <v-card>
            <v-card-title>
              Debug
            </v-card-title>
            <v-card-text>
            </v-card-text>
            <v-card-actions>
              <v-btn flat v-on:click="triggerError()">triggerError</v-btn>
              <v-btn flat v-on:click="triggerInfo()">triggerInfo</v-btn>
              <v-btn flat v-on:click="triggerSuccess()">triggerSuccess</v-btn>
            </v-card-actions>
          </v-card>
        </v-container>
    `,
  data() {
    return {}
  },
  created() {
  },
  methods: {
    triggerError: function () {
      Msgbus.$emit("error", "test error msg.");
    },
    triggerSuccess: function () {
      Msgbus.$emit("success", "test success msg.");
    },
    triggerInfo: function () {
      Msgbus.$emit("info", "test info msg.");
    }
  },
  computed: {}
});

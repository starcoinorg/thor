import Vue from "vue";

export default Vue.extend({
  template: `
              <v-card>
              <v-card-title class="subheading">{{title}}</v-card-title>
              <v-divider></v-divider>
              <v-list dense subheader>
                <v-list-tile v-for="(value,name) in object" :key="name">
                      <v-list-tile-content>{{name}}</v-list-tile-content>
                      <v-list-tile-content class="align-end">{{ value }}</v-list-tile-content>
                </v-list-tile>
               </v-list>
             </v-card>
    `,
  props: {
    object: {
      type: Object,
      // 对象或数组默认值必须从一个工厂函数获取
      default: function () {
        return {}
      }
    },
    title: {
      type: String,
      default: ""
    }
  },
  created() {
  },
  methods: {},
  computed: {}
});

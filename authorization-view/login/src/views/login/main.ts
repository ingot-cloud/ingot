import { createApp } from "vue";
import { ElButton, ElInput } from "element-plus";
import "element-plus/dist/index.css";
import "element-plus/theme-chalk/dark/css-vars.css";
import "@/assets/main.css";
import "./app.css";

import IconClose from "./IconClose.vue";

const app = createApp({
  components: {
    IconClose,
    ElButton,
    ElInput,
  },
  data() {
    return {
      message: "Hello Vue!",
    };
  },
  methods: {
    handleLogin() {},
  },
});

app.mount("#app");

import { createApp } from "vue";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import "element-plus/theme-chalk/dark/css-vars.css";
import "@/assets/main.css";
import "./app.css";

import IconClose from "./IconClose.vue";

const app = createApp({
  components: {
    IconClose,
  },
  data() {
    return {
      message: "Hello Vue!",
    };
  },
});

app.use(ElementPlus);
app.mount("#app");

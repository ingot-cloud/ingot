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
      errorMessage: "Hello Vue!",
      username: "",
      password: "",
    };
  },
  methods: {
    handleLogin() {
      if (!this.username) {
        this.errorMessage = "请输入账号";
        return;
      }
      if (!this.password) {
        this.errorMessage = "请输入密码";
        return;
      }
      this.$refs.loginForm.submit();
    },
    handleCloseErrorHint() {
      this.errorMessage = "";
    },
  },
});

app.mount("#app");

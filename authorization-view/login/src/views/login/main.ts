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
      showError: false,
      errorMessage: "",
      username: "",
      password: "",
    };
  },
  methods: {
    handleLogin() {
      if (!this.username) {
        this.errorMessage = "请输入账号";
        this.showError = true;
        return;
      }
      if (!this.password) {
        this.errorMessage = "请输入密码";
        this.showError = true;
        return;
      }
      this.$refs.loginForm.submit();
    },
    handleCloseErrorHint() {
      this.showError = false;
    },
    ifErrorHint(params: string) {
      if (params && !this.errorMessage) {
        this.errorMessage = params;
        this.showError = true;
      }
      return this.showError;
    },
  },
});

app.mount("#app");

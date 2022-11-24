<script setup lang="ts">
import { ref } from "vue";
import IconClose from "./IconClose.vue";
const loginForm = ref();
const errorMessage = ref("");
const username = ref("");
const password = ref("");

const handleCloseErrorHint = () => {
  errorMessage.value = "";
};

const handleLogin = () => {
  if (!username.value) {
    errorMessage.value = "请输入账号";
    return;
  }
  if (!password.value) {
    errorMessage.value = "请输入密码";
    return;
  }
  loginForm.value.submit();
};
</script>
<template>
  <div class="logo-container">
    <img src="@/assets/logo.png" class="logo" />
  </div>
  <div class="welcome-container">登录到Ingot</div>
  <div shadow="never" v-if="errorMessage" class="error-container">
    <div class="error-text">
      {{ errorMessage }}
    </div>
    <IconClose @click="handleCloseErrorHint" />
  </div>
  <div class="login-container">
    <form action="/oauth2/form" method="post" ref="loginForm">
      <el-input
        v-model="username"
        name="username"
        placeholder="账号"
        clearable
        class="input-item"
      />
      <el-input
        v-model="password"
        name="password"
        type="password"
        show-password
        clearable
        placeholder="密码"
        class="input-item"
      />
      <el-button color="#4e8e2f" class="btn-item" @click="handleLogin">
        登录
      </el-button>
    </form>
  </div>
</template>
<style scoped>
.logo-container {
  margin-top: 50px;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 15px;
  height: 50px;
}
.logo-container .logo {
  height: 100%;
}
.welcome-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: rgb(201, 209, 217);
  line-height: 36px;
  height: 36px;
  font-size: 24px;
  font-weight: 800;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "Noto Sans",
    Helvetica, Arial, sans-serif, "Apple Color Emoji", "Segoe UI Emoji";
  margin-bottom: 15px;
}
.error-container {
  height: 55px;
  margin-bottom: 15px;
  border-radius: 4px;
  border: 1px solid rgba(248, 81, 73, 0.4);
  background-image: linear-gradient(
    rgba(248, 81, 73, 0.15),
    rgba(248, 81, 73, 0.15)
  );
  color: rgb(201, 209, 217);
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
}
.error-container .error-text {
  font-size: 14px;
  height: 14px;
  line-height: 14px;
  word-wrap: break-word;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "Noto Sans",
    Helvetica, Arial, sans-serif, "Apple Color Emoji", "Segoe UI Emoji";
}
.login-container {
  background-color: rgb(22, 27, 34);
  padding: 16px;
  border: 1px solid rgb(33, 38, 45);
  border-radius: 6px;
}
.login-container form {
  width: 274px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.login-container .input-item {
  margin-bottom: 20px;
  height: 35px;
}
.login-container .btn-item {
  height: 35px;
  border-radius: 6px;
}
</style>

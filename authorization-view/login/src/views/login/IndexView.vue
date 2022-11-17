<script setup lang="ts">
import { ref } from "vue";
import IconClose from "./IconClose.vue";

const showError = ref(true);
const username = ref("");
const password = ref("");
const cardBodyStyle = {
  paddingLeft: "20px",
  paddingRight: "20px",
  display: "flex",
  flexDirection: "column",
  justifyContent: "center",
};

const handleCloseErrorHint = () => {
  showError.value = false;
};
</script>
<template>
  <div class="logo-container">
    <img src="@/assets/logo-transparent.png" class="logo" />
  </div>
  <div class="welcome-container">登录到Ingot</div>
  <div shadow="never" v-if="showError" class="error-container">
    <div class="error-text">error message</div>
    <IconClose @click="handleCloseErrorHint" />
  </div>
  <el-card shadow="never" class="login-container" :body-style="cardBodyStyle">
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
    <el-button color="#4e8e2f" class="btn-item">登录</el-button>
  </el-card>
</template>
<style scoped>
.logo-container {
  margin-top: 50px;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 15px;
}
.logo-container .logo {
  width: 64px;
  height: 50px;
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
  padding: 20px;
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
  width: 330px;
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

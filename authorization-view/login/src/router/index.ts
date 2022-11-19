import { createRouter, createWebHistory } from "vue-router";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/",
      redirect: "/oauth2/login",
    },
    {
      path: "/oauth2/login",
      name: "login",
      component: () => import("@/views/login/IndexView.vue"),
    },
  ],
});

export default router;

import { createRouter, createWebHistory } from "vue-router";
import LoginView from "../views/LoginView.vue";
import RegisterView from "../views/RegisterView.vue";
import DashboardView from "../views/DashboardView.vue";
import ATMView from "../views/ATMView.vue";
import AccountsView from "../views/AccountsView.vue";
import DashboardLayout from "../layout/DashboardLayout.vue";
import TransferPageView from "../views/TransferView.vue";
import ApproveAccountsView from "../views/ApproveAccountsView.vue";
import ApproveUsersView from "../views/ApproveUsersView.vue";
import EmployeeUsersView from "../views/EmployeeUsersView.vue";
import EmployeeAccountsView from "../views/EmployeeAccountsView.vue";
import TransactionHistoryView from "../views/TransactionHistoryView.vue"; // ✅ NEW

import { useAuthStore } from "../store/auth";

const routes = [
  { path: "/login", component: LoginView },
  { path: "/register", component: RegisterView },
  {
    path: "/dashboard",
    component: DashboardLayout,
    children: [
      { path: "", component: DashboardView },
      { path: "accounts", component: AccountsView },
      { path: "transfer", component: TransferPageView },
      { path: "atm", component: ATMView, meta: { requiresAuth: true } },
      {
        path: "history",
        component: TransactionHistoryView,
        meta: { requiresAuth: true },
      },
      {
        path: "approve",
        component: ApproveAccountsView,
        meta: { requiresRole: "employee" },
      },
      {
        path: "approve-users",
        component: ApproveUsersView,
        meta: { requiresRole: "employee" },
      },
      {
        path: "users",
        component: EmployeeUsersView,
        meta: { requiresRole: "employee" },
      },
      {
        path: "employee-accounts",
        component: EmployeeAccountsView,
        meta: { requiresRole: "employee" },
      },
    ],
  },
  { path: "/", redirect: "/login" },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore();

  if (to.meta.requiresAuth && !authStore.token) {
    return next("/login");
  }

  if (
    to.meta.requiresRole &&
    authStore.user?.role?.toLowerCase() !== to.meta.requiresRole
  ) {
    return next("/dashboard");
  }

  next();
});

export default router;

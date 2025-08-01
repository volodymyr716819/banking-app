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
import EmployeeTransferView from "../views/EmployeeTransferView.vue";
import TransactionHistoryView from "../views/TransactionHistoryView.vue";
import SearchCustomerView from "../views/SearchCustomerView.vue";
import PinSettingsView from "../views/PinSettingsView.vue";
import AwaitingApproval from "../views/AwaitingApproval.vue";
import CustomersView from "../views/CustomersView.vue";

import { useAuthStore } from "../store/auth";

const routes = [
  { 
    path: "/login", 
    component: LoginView,
    meta: { requiresGuest: true }
  },
  { 
    path: "/register", 
    component: RegisterView,
    meta: { requiresGuest: true }
  },
  {
    path: '/awaiting-approval',
    name: 'AwaitingApproval',
    component: AwaitingApproval,
    meta: { requiresAuth: true, allowPending: true }
  },  
  {
    path: "/dashboard",
    component: DashboardLayout,
    meta: { requiresAuth: true },
    children: [
      { path: "", component: DashboardView },
      { path: "accounts", component: AccountsView },
      { path: "transfer", component: TransferPageView },
      { path: "atm", component: ATMView },
      { path: "pin-settings", component: PinSettingsView },
      { path: "history", component: TransactionHistoryView },
      { path: "search-customer", component: SearchCustomerView },
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
      {
        path: "employee-transfer",
        component: EmployeeTransferView,
        meta: { requiresRole: "employee" },
      },
      {
        path: "customers",
        component: CustomersView,
        meta: { requiresRole: "employee" },
      },
    ],
  },
  { path: "/", redirect: "/login" },
  // Catch-all route
  { path: "/:pathMatch(.*)*", redirect: "/login" }
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
});

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore();
  const isAuthenticated = !!authStore.token;
  
  // Redirect authenticated users away from login/register pages
  if (to.meta.requiresGuest && isAuthenticated) {
    return next("/dashboard");
  }

  // Redirect unauthenticated users to login
  if (to.meta.requiresAuth && !isAuthenticated) {
    return next("/login");
  }
  
  // Check if user is pending approval and route accordingly
  if (
    isAuthenticated && 
    authStore.user?.registrationStatus === 'PENDING' && 
    to.path !== '/awaiting-approval' && 
    !to.meta.allowPending
  ) {
    return next('/awaiting-approval');
  }
  
  // If user is approved but tries to access the waiting page, redirect to dashboard
  if (
    isAuthenticated && 
    authStore.user?.registrationStatus === 'APPROVED' && 
    to.path === '/awaiting-approval'
  ) {
    return next('/dashboard');
  }

  // Check role-based access
  if (
    to.meta.requiresRole &&
    (!authStore.user?.role || authStore.user.role.toLowerCase() !== to.meta.requiresRole)
  ) {
    return next("/dashboard");
  }

  next();
});

export default router;
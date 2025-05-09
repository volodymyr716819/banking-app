import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import DashboardView from '../views/DashboardView.vue'
import ATMView from '../views/ATMView.vue'
import AccountsView from '../views/AccountsView.vue'
import DashboardLayout from '../layout/DashboardLayout.vue'
import { useAuthStore } from '../store/auth'

const routes = [
  { path: '/login', component: LoginView },
  { path: '/register', component: RegisterView },
  { 
    path: '/dashboard', 
    component: DashboardLayout,
    children: [
      { path: '', component: DashboardView },  // << Welcome page after login
      { path: 'accounts', component: AccountsView },
      // { path: 'transfer', component: TransferPageView },
      // { path: 'atm', component: AtmPageView },
      // { path: 'history', component: HistoryPageView }
    ]
  },
  {
    path: '/atm',
    component: ATMView,
    meta: { requiresAuth: true },
  },
  { path: '/', redirect: '/login' }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.token) {
    next('/login')
  } else {
    next()
  }
})

export default router

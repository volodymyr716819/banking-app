import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import { createPinia } from 'pinia';
import { useAuthStore } from './store/auth';

// Import CSS
import './assets/styles/main.css';

// Create application
const app = createApp(App);
const pinia = createPinia();

app.use(pinia);
app.use(router);

// Initialize auth state before mounting the app
const authStore = useAuthStore(pinia);
authStore.initAuth();

// Mount the app
app.mount('#app');
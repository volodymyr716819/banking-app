<template>
  <div class="search-page">
    <div class="card">
      <div class="card-header">
        <h3 class="title"><span class="material-icons">person_search</span> Customer Search</h3>
        <p>Find customer details and account information</p>
      </div>
   
      <form class="form" @submit.prevent="searchCustomers">
        <div class="input-container">
          <div class="input-wrapper">
            <span class="material-icons">search</span>
            <input 
              type="text" 
              v-model="searchTerm" 
              placeholder="Enter customer name" 
              required
            />
            <button 
              v-if="searchTerm" 
              type="button" 
              class="clear-btn" 
              @click="clearSearch"
            >
              <span class="material-icons">close</span>
            </button>
          </div>
          <p class="hint"><span class="material-icons">info</span> Search by customer name</p>
        </div>

        <button 
          type="submit" 
          class="btn primary" 
          :disabled="loading || !searchTerm.trim()"
          :class="{ 'loading': loading }"
        >
          <span v-if="loading" class="spinner"></span>
          <span v-else><span class="material-icons">search</span> Search</span>
        </button>
      </form>
    </div>

    <div v-if="loading" class="status-panel">
      <div class="spinner"></div>
      <span>Searching...</span>
    </div>

    <div v-if="errorMessage" class="alert error">
      <span class="material-icons">error_outline</span>
      <p>{{ errorMessage }}</p>
      <button @click="errorMessage = ''" class="close-btn">
        <span class="material-icons">close</span>
      </button>
    </div>

    <div v-if="searchResults.length > 0" class="results">
      <div class="results-header">
        <h2><span class="material-icons">people</span> Results</h2>
        <span class="badge">{{ searchResults.length }} found</span>
      </div>
      
      <div class="results-grid">
        <div v-for="result in searchResults" :key="result.id" class="customer-card">
          <div class="customer-header">
            <div class="avatar">{{ getInitials(result.name) }}</div>
            <h3>{{ result.name }}</h3>
            <div>ID: {{ result.id }}</div>
          </div>
          
          <div class="accounts">
            <h4>
              <span class="material-icons">account_balance</span>
              Accounts ({{ result.ibans.length }})
            </h4>
            
            <div v-if="result.ibans.length > 0" class="iban-list">
              <div v-for="(iban, index) in result.ibans" :key="index" class="iban-item">
                <div><span class="material-icons">credit_card</span> {{ formatIban(iban) }}</div>
                <button 
                  @click="copyIban(iban)" 
                  :class="{ 'copied': iban === copiedIban }"
                >
                  <span class="material-icons">{{ iban === copiedIban ? 'check' : 'content_copy' }}</span>
                  {{ iban === copiedIban ? 'Copied' : 'Copy' }}
                </button>
              </div>
            </div>
            
            <div v-else class="no-accounts">
              <span class="material-icons">account_balance_wallet</span>
              <p>No accounts available</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="searchPerformed && searchResults.length === 0 && !loading && !errorMessage" class="empty-state">
      <span class="material-icons">search_off</span>
      <h3>No Customers Found</h3>
      <p>Try a different search term or check spelling</p>
      <button @click="clearSearch" class="btn secondary">Clear</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { api } from '../api';
import { useAuthStore } from '../store/auth';

const auth = useAuthStore();
const searchTerm = ref('');
const searchResults = ref([]);
const loading = ref(false);
const errorMessage = ref('');
const searchPerformed = ref(false);
const copiedIban = ref('');

defineExpose({ __pageTitle: 'Find Customer' });

// Search customers by name
const searchCustomers = async () => {
  if (!searchTerm.value.trim()) {
    errorMessage.value = 'Please enter a search term';
    return;
  }

  loading.value = true;
  errorMessage.value = '';
  searchPerformed.value = true;
  
  try {
    const response = await api.get('/users/search', {
      params: { name: searchTerm.value },
      headers: { Authorization: `Bearer ${auth.token}` }
    });
    searchResults.value = response.data;
  } catch (err) {
    if (err.response) {
      errorMessage.value = err.response.data || 'Search failed. Please try again.';
    } else {
      errorMessage.value = 'Connection error. Please check your network.';
    }
    searchResults.value = [];
  } finally {
    loading.value = false;
  }
};

// Format IBAN with spaces
const formatIban = (iban) => iban.replace(/(.{4})/g, '$1 ').trim();

// Copy IBAN to clipboard
const copyIban = (text) => {
  navigator.clipboard.writeText(text)
    .then(() => {
      copiedIban.value = text;
      setTimeout(() => copiedIban.value = '', 2000);
    })
    .catch(() => {
      errorMessage.value = 'Failed to copy to clipboard';
    });
};

// Clear search form
const clearSearch = () => {
  searchTerm.value = '';
  searchResults.value = [];
  searchPerformed.value = false;
  errorMessage.value = '';
};

// Get customer initials for avatar
const getInitials = (name) => {
  if (!name) return 'C';
  const parts = name.split(' ');
  return parts.length === 1 
    ? parts[0].charAt(0).toUpperCase()
    : (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
};
</script>

<style scoped>
.search-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 1rem;
}

/* Card styles */
.card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  margin-bottom: 1.5rem;
  border: 1px solid #eee;
}

.card-header {
  padding: 1rem;
  border-bottom: 1px solid #eee;
  background: var(--primary-50, #f0f7ff);
}

.title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1.25rem;
  margin: 0 0 0.5rem;
}

/* Form styles */
.form {
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.input-container {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-wrapper .material-icons {
  position: absolute;
  left: 0.75rem;
  color: #666;
}

.input-wrapper input {
  padding: 0.75rem 0.75rem 0.75rem 2.5rem;
  border-radius: 4px;
  border: 1px solid #ccc;
  font-size: 1rem;
  width: 100%;
}

.input-wrapper input:focus {
  outline: none;
  border-color: var(--primary-color, #2b6cb0);
  box-shadow: 0 0 0 3px rgba(43, 108, 176, 0.2);
}

.clear-btn {
  position: absolute;
  right: 0.75rem;
  background: none;
  border: none;
  cursor: pointer;
  color: #666;
}

.hint {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #666;
  font-size: 0.875rem;
  margin: 0;
}

/* Button styles */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  border: none;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn.primary {
  background: var(--primary-color, #2b6cb0);
  color: white;
}

.btn.secondary {
  background: #e2e8f0;
  color: #4a5568;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn.loading {
  position: relative;
  color: transparent;
}

.spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Status panels */
.status-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 1rem;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  margin: 1rem 0;
}

.alert {
  display: flex;
  align-items: flex-start;
  padding: 1rem;
  border-radius: 8px;
  margin-bottom: 1rem;
}

.alert.error {
  background: #fff5f5;
  border-left: 4px solid #f56565;
  color: #c53030;
}

.alert p {
  flex-grow: 1;
  margin: 0 0.75rem;
}

.close-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: #666;
}

/* Results section */
.results {
  margin-top: 2rem;
}

.results-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid #eee;
}

.results-header h2 {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin: 0;
  font-size: 1.25rem;
}

.badge {
  background: var(--primary-100, #e6f0fd);
  color: var(--primary-700, #2c5282);
  padding: 0.25rem 0.75rem;
  border-radius: 9999px;
  font-size: 0.875rem;
  font-weight: 500;
}

.results-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1rem;
}

.customer-card {
  background: white;
  border-radius: 8px;
  border: 1px solid #eee;
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
}

.customer-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.customer-header {
  padding: 1rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: var(--primary-50, #f0f7ff);
  border-bottom: 1px solid #eee;
}

.avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: var(--primary-color, #2b6cb0);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
  font-weight: bold;
  margin-bottom: 0.75rem;
}

.customer-header h3 {
  margin: 0 0 0.25rem;
  font-size: 1.125rem;
}

.accounts {
  padding: 1rem;
}

.accounts h4 {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin: 0 0 0.75rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid #eee;
  font-size: 1rem;
}

.iban-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.iban-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem;
  background: #f9fafb;
  border-radius: 4px;
  border: 1px solid #eee;
}

.iban-item div {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-family: monospace;
  font-size: 0.875rem;
}

.iban-item button {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  background: white;
  border: 1px solid #ddd;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.75rem;
}

.iban-item button.copied {
  background: #f0fff4;
  border-color: #48bb78;
  color: #2f855a;
}

.no-accounts {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 1rem;
  background: #f9fafb;
  border-radius: 4px;
  border: 1px dashed #ddd;
  color: #666;
  text-align: center;
}

.empty-state {
  margin: 2rem auto;
  max-width: 400px;
  text-align: center;
  padding: 2rem;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.empty-state .material-icons {
  font-size: 3rem;
  color: #a0aec0;
  margin-bottom: 1rem;
}

.empty-state h3 {
  margin: 0 0 0.75rem;
}

.empty-state p {
  margin: 0 0 1rem;
  color: #718096;
}

@media (max-width: 768px) {
  .results-grid {
    grid-template-columns: 1fr;
  }
  
  .btn {
    width: 100%;
  }
}
</style>
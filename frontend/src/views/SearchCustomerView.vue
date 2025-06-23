<template>
  <div class="search-customer-page">
    <div class="card search-container">
      <div class="card-header">
        <h3 class="card-title">
          <span class="material-icons icon-title">person_search</span>
          Customer Search
        </h3>
        <p class="card-subtitle">Find customer details and account information</p>
      </div>
      
      <form class="search-form" @submit.prevent="searchCustomers">
        <div class="search-input-container">
          <div class="input-icon-wrapper">
            <span class="material-icons input-icon">search</span>
            <input 
              type="text" 
              id="searchTerm" 
              v-model="searchTerm" 
              class="search-input"
              placeholder="Enter customer name" 
              required
            />
            <button 
              v-if="searchTerm" 
              type="button" 
              class="clear-input" 
              @click="clearSearch"
            >
              <span class="material-icons">close</span>
            </button>
          </div>
          <p class="field-info">
            <span class="material-icons info-icon">info</span>
            Search by customer name
          </p>
        </div>

        <button 
          type="submit" 
          class="btn btn-primary search-button" 
          :disabled="loading || !searchTerm.trim()"
          :class="{ 'btn-loading': loading }"
        >
          <span v-if="loading" class="button-spinner"></span>
          <span v-else class="button-text">
            <span class="material-icons btn-icon">search</span>
            Search
          </span>
        </button>
      </form>
    </div>

    <div v-if="loading" class="loading-panel">
      <div class="spinner"></div>
      <span>Searching for customers...</span>
    </div>

    <div v-if="errorMessage" class="alert alert-danger">
      <span class="material-icons alert-icon">error_outline</span>
      <div class="alert-content">
        <h4 class="alert-title">Search Error</h4>
        <p class="alert-message">{{ errorMessage }}</p>
      </div>
      <button @click="errorMessage = ''" class="dismiss-alert">
        <span class="material-icons">close</span>
      </button>
    </div>

    <div v-if="searchResults.length > 0" class="results-container">
      <div class="results-header">
        <h2 class="results-title">
          <span class="material-icons results-icon">people</span>
          Search Results 
        </h2>
        <span class="result-badge">{{ searchResults.length }} customers found</span>
      </div>
      
      <div class="results-list">
        <div v-for="result in searchResults" :key="result.id" class="result-card">
          <div class="customer-info">
            <div class="customer-avatar">{{ getCustomerInitials(result.name) }}</div>
            <h3 class="customer-name">{{ result.name }}</h3>
            <div class="customer-meta">Customer #{{ result.id }}</div>
          </div>
          
          <div class="account-section">
            <h4 class="section-title">
              <span class="material-icons section-icon">account_balance</span>
              Accounts
              <span class="account-count">{{ result.ibans.length }}</span>
            </h4>
            
            <div v-if="result.ibans.length > 0" class="accounts-list">
              <div v-for="(iban, index) in result.ibans" :key="index" class="account-item">
                <div class="account-details">
                  <span class="account-icon material-icons">credit_card</span>
                  <span class="iban-text">{{ formatIban(iban) }}</span>
                </div>
                <button 
                  @click="copyToClipboard(iban)" 
                  class="copy-button" 
                  :class="{ 'copied': iban === copiedIban }"
                  title="Copy to clipboard"
                >
                  <span class="material-icons copy-icon">{{ iban === copiedIban ? 'check' : 'content_copy' }}</span>
                  <span>{{ iban === copiedIban ? 'Copied' : 'Copy' }}</span>
                </button>
              </div>
            </div>
            
            <div v-else class="no-accounts">
              <span class="material-icons empty-icon">account_balance_wallet</span>
              <p>No accounts available for this customer</p>
            </div>
          </div>
          
        </div>
      </div>
    </div>

    <div v-if="searchPerformed && searchResults.length === 0 && !loading && !errorMessage" class="empty-state">
      <div class="empty-icon-container">
        <span class="material-icons empty-state-icon">search_off</span>
      </div>
      <h3 class="empty-state-title">No Customers Found</h3>
      <p class="empty-state-message">We couldn't find any customers matching your search criteria.</p>
      <p class="empty-state-suggestion">Try a different search term or check the spelling.</p>
      <button @click="clearSearch" class="btn btn-secondary">Clear Search</button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';
import { api } from '../api';
import { useAuthStore } from '../store/auth';

const auth = useAuthStore();
const searchTerm = ref('');
const searchResults = ref([]);
const loading = ref(false);
const errorMessage = ref('');
const searchPerformed = ref(false);
const copiedIban = ref('');

// Define page title
const __pageTitle = 'Find Customer';

// Expose the page title to the parent component
defineExpose({ __pageTitle });

// Search for customers
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
      headers: {
        Authorization: `Bearer ${auth.token}`
      }
    });
    
    searchResults.value = response.data;
  } catch (err) {
    if (err.response) {
      // Handle server response errors
      if (err.response.status === 401 || err.response.status === 403) {
        errorMessage.value = 'You are not authorized to perform this search. Please log in again.';
      } else if (err.response.status === 404) {
        errorMessage.value = 'Search service not available. Please try again later.';
      } else {
        errorMessage.value = err.response.data || 'An error occurred while searching. Please try again.';
      }
    } else if (err.request) {
      // Handle network errors
      errorMessage.value = 'Unable to connect to the server. Please check your connection and try again.';
    } else {
      // Handle other errors
      errorMessage.value = 'An unexpected error occurred. Please try again.';
    }
    searchResults.value = [];
  } finally {
    loading.value = false;
  }
};

// Format IBAN with spaces for readability when displaying
const formatIban = (iban) => {
  return iban.replace(/(.{4})/g, '$1 ').trim();
};

// Copy IBAN to clipboard
const copyToClipboard = (text) => {
  navigator.clipboard.writeText(text)
    .then(() => {
      copiedIban.value = text;
      setTimeout(() => {
        copiedIban.value = '';
      }, 2000);
    })
    .catch(err => {
      errorMessage.value = 'Failed to copy to clipboard';
    });
};

// Clear search and reset results
const clearSearch = () => {
  searchTerm.value = '';
  searchResults.value = [];
  searchPerformed.value = false;
  errorMessage.value = '';
};

// Get customer initials for avatar
const getCustomerInitials = (name) => {
  if (!name) return 'C';
  
  const nameParts = name.split(' ');
  if (nameParts.length === 1) {
    return nameParts[0].charAt(0).toUpperCase();
  }
  
  return (
    nameParts[0].charAt(0).toUpperCase() + 
    nameParts[nameParts.length - 1].charAt(0).toUpperCase()
  );
};
</script>

<style scoped>
.search-customer-page {
  padding: var(--spacing-4) 0;
  max-width: 1200px;
  margin: 0 auto;
}

/* Card Styling */
.card {
  background-color: var(--white);
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-md);
  overflow: hidden;
  margin-bottom: var(--spacing-6);
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
  border: 1px solid var(--gray-200);
}

.card-header {
  padding: var(--spacing-5);
  border-bottom: 1px solid var(--gray-200);
  background-color: var(--primary-50);
}

.card-title {
  display: flex;
  align-items: center;
  font-size: var(--font-size-xl);
  color: var(--primary-color);
  margin: 0 0 var(--spacing-2);
  font-weight: var(--font-weight-semibold);
}

.icon-title {
  margin-right: var(--spacing-2);
  color: var(--primary-color);
}

.card-subtitle {
  color: var(--gray-600);
  margin: 0;
  font-size: var(--font-size-sm);
}

/* Search Form */
.search-form {
  padding: var(--spacing-5);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4);
}

.search-input-container {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-2);
}

.input-icon-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: var(--spacing-3);
  color: var(--gray-500);
  font-size: var(--font-size-xl);
}

.search-input {
  padding: var(--spacing-4) var(--spacing-4) var(--spacing-4) var(--spacing-10);
  border-radius: var(--border-radius);
  border: 2px solid var(--gray-300);
  font-size: var(--font-size-md);
  width: 100%;
  transition: all var(--transition-fast);
  color: var(--gray-800);
  background-color: var(--white);
}

.search-input:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(var(--primary-rgb), 0.2);
}

.search-input::placeholder {
  color: var(--gray-400);
}

.clear-input {
  position: absolute;
  right: var(--spacing-3);
  background: none;
  border: none;
  cursor: pointer;
  color: var(--gray-500);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--border-radius-full);
  padding: var(--spacing-1);
  transition: all var(--transition-fast);
}

.clear-input:hover {
  background-color: var(--gray-200);
  color: var(--gray-700);
}

.field-info {
  display: flex;
  align-items: center;
  color: var(--gray-500);
  font-size: var(--font-size-sm);
  margin: 0;
  padding-left: var(--spacing-2);
}

.info-icon {
  font-size: var(--font-size-md);
  margin-right: var(--spacing-2);
  color: var(--gray-400);
}

/* Buttons */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-3) var(--spacing-5);
  border-radius: var(--border-radius);
  font-weight: var(--font-weight-medium);
  cursor: pointer;
  transition: all var(--transition-fast);
  border: none;
  font-size: var(--font-size-md);
  gap: var(--spacing-2);
}

.btn-primary {
  background-color: var(--primary-color);
  color: var(--white);
}

.btn-primary:hover:not(:disabled) {
  background-color: var(--primary-700);
  box-shadow: var(--shadow-md);
}

.btn-primary:active:not(:disabled) {
  background-color: var(--primary-800);
  transform: translateY(1px);
}

.btn-secondary {
  background-color: var(--gray-200);
  color: var(--gray-800);
}

.btn-secondary:hover:not(:disabled) {
  background-color: var(--gray-300);
}

.btn-outline {
  background-color: transparent;
  color: var(--primary-color);
  border: 1px solid var(--primary-color);
}

.btn-outline:hover {
  background-color: var(--primary-50);
}

.btn:disabled {
  background-color: var(--gray-300);
  color: var(--gray-500);
  cursor: not-allowed;
  opacity: 0.7;
}

.search-button {
  align-self: flex-start;
  min-width: 120px;
  height: 48px;
}

.btn-icon {
  font-size: var(--font-size-md);
}

.btn-loading {
  position: relative;
  color: transparent;
}

.button-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: var(--white);
  border-radius: 50%;
  animation: button-spin 1s linear infinite;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

@keyframes button-spin {
  0% { transform: translate(-50%, -50%) rotate(0deg); }
  100% { transform: translate(-50%, -50%) rotate(360deg); }
}

/* Loading Indicator */
.loading-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-3);
  margin: var(--spacing-5) 0;
  padding: var(--spacing-5);
  background-color: var(--white);
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-md);
  border: 1px solid var(--gray-200);
  color: var(--gray-700);
}

.spinner {
  width: 24px;
  height: 24px;
  border: 3px solid var(--primary-100);
  border-top-color: var(--primary-color);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* Alerts */
.alert {
  display: flex;
  align-items: flex-start;
  padding: var(--spacing-4);
  border-radius: var(--border-radius-lg);
  margin-bottom: var(--spacing-5);
  position: relative;
}

.alert-danger {
  background-color: var(--error-50);
  border-left: 4px solid var(--error-color);
}

.alert-icon {
  color: var(--error-color);
  font-size: var(--font-size-xl);
  margin-right: var(--spacing-3);
  flex-shrink: 0;
}

.alert-content {
  flex-grow: 1;
}

.alert-title {
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  margin: 0 0 var(--spacing-1);
  color: var(--error-700);
}

.alert-message {
  margin: 0;
  color: var(--error-600);
}

.dismiss-alert {
  border: none;
  background: none;
  cursor: pointer;
  color: var(--gray-500);
  padding: var(--spacing-1);
  border-radius: var(--border-radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: var(--spacing-2);
  transition: background-color var(--transition-fast);
}

.dismiss-alert:hover {
  background-color: rgba(0, 0, 0, 0.05);
  color: var(--gray-700);
}

/* Results Section */
.results-container {
  margin-top: var(--spacing-6);
}

.results-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-4);
  padding-bottom: var(--spacing-3);
  border-bottom: 1px solid var(--gray-200);
}

.results-title {
  display: flex;
  align-items: center;
  font-size: var(--font-size-xl);
  margin: 0;
  color: var(--gray-900);
  font-weight: var(--font-weight-semibold);
}

.results-icon {
  margin-right: var(--spacing-2);
  color: var(--primary-color);
}

.result-badge {
  background-color: var(--primary-100);
  color: var(--primary-700);
  padding: var(--spacing-1) var(--spacing-3);
  border-radius: var(--border-radius-full);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
}

.results-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: var(--spacing-4);
}

.result-card {
  background-color: var(--white);
  border-radius: var(--border-radius-lg);
  overflow: hidden;
  border: 1px solid var(--gray-200);
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
  display: flex;
  flex-direction: column;
}

.result-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}

.customer-info {
  padding: var(--spacing-4);
  display: flex;
  flex-direction: column;
  align-items: center;
  border-bottom: 1px solid var(--gray-200);
  background-color: var(--primary-50);
}

.customer-avatar {
  width: 60px;
  height: 60px;
  border-radius: var(--border-radius-full);
  background-color: var(--primary-color);
  color: var(--white);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  margin-bottom: var(--spacing-3);
}

.customer-name {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  margin: 0 0 var(--spacing-1);
  color: var(--gray-900);
  text-align: center;
}

.customer-meta {
  font-size: var(--font-size-sm);
  color: var(--gray-600);
}

.account-section {
  padding: var(--spacing-4);
  flex-grow: 1;
}

.section-title {
  display: flex;
  align-items: center;
  font-size: var(--font-size-md);
  color: var(--gray-700);
  margin: 0 0 var(--spacing-3);
  padding-bottom: var(--spacing-2);
  border-bottom: 1px solid var(--gray-200);
}

.section-icon {
  font-size: var(--font-size-md);
  margin-right: var(--spacing-2);
  color: var(--primary-color);
}

.account-count {
  margin-left: auto;
  background-color: var(--gray-200);
  color: var(--gray-700);
  border-radius: var(--border-radius-full);
  font-size: var(--font-size-xs);
  padding: 2px 8px;
  font-weight: var(--font-weight-medium);
}

.accounts-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-2);
}

.account-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-3);
  background-color: var(--gray-50);
  border-radius: var(--border-radius);
  border: 1px solid var(--gray-200);
}

.account-details {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
}

.account-icon {
  color: var(--primary-color);
  font-size: var(--font-size-md);
}

.iban-text {
  font-family: var(--font-mono);
  font-size: var(--font-size-sm);
  color: var(--gray-800);
  letter-spacing: 0.5px;
  font-weight: var(--font-weight-medium);
}

.copy-button {
  display: flex;
  align-items: center;
  gap: var(--spacing-1);
  background-color: var(--white);
  border: 1px solid var(--gray-300);
  padding: var(--spacing-1) var(--spacing-2);
  border-radius: var(--border-radius);
  cursor: pointer;
  font-size: var(--font-size-xs);
  color: var(--gray-700);
  transition: all var(--transition-fast);
}

.copy-button:hover {
  background-color: var(--gray-100);
  border-color: var(--gray-400);
}

.copy-button.copied {
  background-color: var(--success-50);
  border-color: var(--success-500);
  color: var(--success-700);
}

.copy-icon {
  font-size: var(--font-size-sm);
}

.no-accounts {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-4);
  background-color: var(--gray-50);
  border-radius: var(--border-radius);
  color: var(--gray-500);
  border: 1px dashed var(--gray-300);
  text-align: center;
}

.empty-icon {
  font-size: var(--font-size-xl);
  margin-bottom: var(--spacing-2);
  color: var(--gray-400);
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  padding: var(--spacing-3) var(--spacing-4);
  background-color: var(--gray-50);
  border-top: 1px solid var(--gray-200);
}

.action-icon {
  font-size: var(--font-size-md);
}

/* Empty State */
.empty-state {
  margin: var(--spacing-8) auto;
  max-width: 400px;
  text-align: center;
  padding: var(--spacing-6);
  background-color: var(--white);
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-md);
  border: 1px solid var(--gray-200);
}

.empty-icon-container {
  width: 80px;
  height: 80px;
  margin: 0 auto var(--spacing-4);
  background-color: var(--gray-100);
  border-radius: var(--border-radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-state-icon {
  font-size: 40px;
  color: var(--gray-500);
}

.empty-state-title {
  font-size: var(--font-size-xl);
  color: var(--gray-800);
  margin: 0 0 var(--spacing-3);
}

.empty-state-message {
  color: var(--gray-600);
  margin: 0 0 var(--spacing-2);
}

.empty-state-suggestion {
  color: var(--gray-500);
  font-style: italic;
  margin: 0 0 var(--spacing-4);
}

/* Responsive Adjustments */
@media (max-width: 768px) {
  .search-customer-page {
    padding: var(--spacing-3);
  }
  
  .results-list {
    grid-template-columns: 1fr;
  }
  
  .search-form {
    padding: var(--spacing-4);
  }
  
  .search-button {
    width: 100%;
    align-self: stretch;
  }
}
</style>
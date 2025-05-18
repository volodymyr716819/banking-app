<template>
  <div class="transfer-page">
    <h1 class="page-title">Transfer Money</h1>
    
    <div v-if="noApprovedAccounts" class="alert alert-warning">
      <div class="alert-icon">⚠️</div>
      <div>You have accounts, but none are approved yet. Please wait for approval before making transfers.</div>
    </div>

    <div v-if="transferSuccess" class="alert alert-success">
      <div class="alert-icon">✓</div>
      <div>{{ message }}</div>
    </div>

    <div v-if="transferError" class="alert alert-danger">
      <div class="alert-icon">⚠️</div>
      <div>{{ message }}</div>
    </div>

    <div v-if="!noApprovedAccounts" class="transfer-container">
      <form class="transfer-form" @submit.prevent="submitTransfer">
        <div class="form-section">
          <h2 class="section-title">From</h2>
          <div class="form-group">
            <label for="from">Select your account:</label>
            <select 
              id="from"
              v-model="fromAccount" 
              class="form-control"
              required
            >
              <option disabled value="">Select an account</option>
              <option v-for="acc in accounts" :key="acc.id" :value="acc.id">
                {{ acc.type }} - IBAN: {{ acc.iban }} - €{{ acc.balance.toFixed(2) }}
              </option>
            </select>
          </div>
        </div>

        <div class="form-section">
          <h2 class="section-title">To</h2>
          
          <div class="form-group">
            <label for="searchName">Find receiver by name:</label>
            <div class="search-container">
              <input 
                type="text" 
                id="searchName"
                v-model="searchName" 
                class="form-control search-input"
                placeholder="Enter recipient's name" 
                @input="searchUsers"
              />
              <button type="button" class="search-button" @click="searchUsers">
                Search
              </button>
            </div>
            <div v-if="searchError" class="search-error">{{ searchError }}</div>
          </div>
          
          <div v-if="searchPerformed && !accountSelected" class="search-results-container">
            <div v-if="searchResults.length > 0" class="search-results">
              <div v-for="result in searchResults" :key="result.accountId" class="search-result-item" @click="selectAccount(result)">
                <div class="result-name">{{ result.userName }}</div>
                <div class="result-details">{{ result.accountType }} - IBAN: {{ result.iban }}</div>
              </div>
            </div>
            <div v-else class="no-results">
              <div class="no-results-icon">🔍</div>
              <p>No accounts found. The user may not exist or have no approved accounts.</p>
            </div>
          </div>

          <div v-if="selectedReceiver" class="selected-receiver">
            <div class="selected-receiver-label">Selected recipient:</div>
            <div class="selected-receiver-info">
              <span class="recipient-name">Account ID: {{ toAccountId }}</span>
              <button type="button" class="clear-receiver" @click="clearRecipient">Change</button>
            </div>
          </div>
          
          <div v-else class="form-group">
            <label for="to">Or enter account ID directly:</label>
            <input 
              type="number" 
              id="to"
              v-model="toAccountId" 
              class="form-control"
              placeholder="Enter receiver's account ID" 
              required 
            />
          </div>
        </div>

        <div class="form-section">
          <h2 class="section-title">Details</h2>
          
          <div class="form-row">
            <div class="form-group amount-group">
              <label for="amount">Amount:</label>
              <div class="amount-input-wrapper">
                <span class="currency-symbol">€</span>
                <input 
                  type="number" 
                  id="amount"
                  v-model="amount" 
                  class="form-control amount-input"
                  step="0.01" 
                  min="0.01" 
                  placeholder="0.00"
                  required 
                />
              </div>
            </div>
          </div>

          <div class="form-group">
            <label for="description">Description (optional):</label>
            <textarea 
              id="description"
              v-model="description" 
              class="form-control"
              placeholder="What's this transfer for?" 
              rows="2"
            ></textarea>
          </div>
        </div>

        <div class="form-actions">
          <button type="submit" class="submit-button" :disabled="isLoading">
            <span v-if="isLoading" class="spinner"></span>
            <span v-else>Complete Transfer</span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import axios from 'axios';
import { useAuthStore } from '../store/auth';

const auth = useAuthStore();
const accounts = ref([]);
const allAccounts = ref([]);
const fromAccount = ref('');
const toAccountId = ref('');
const amount = ref('');
const description = ref('');
const message = ref('');
const searchName = ref('');
const searchResults = ref([]);
const searchError = ref('');
const searchPerformed = ref(false);
const accountSelected = ref(false);
const noApprovedAccounts = ref(false);
const isLoading = ref(false);
const transferSuccess = ref(false);
const transferError = ref(false);

const selectedReceiver = computed(() => {
  return toAccountId.value && accountSelected.value;
});

const fetchAccounts = async () => {
  try {
    const response = await axios.get(`http://localhost:8080/api/accounts/user/${auth.user.id}`, {
      headers: {
        Authorization: `Bearer ${auth.token}`
      }
    });
    
    allAccounts.value = response.data;
    accounts.value = allAccounts.value.filter(acc => acc.approved === true);
    
    // Check if user has accounts but none are approved
    noApprovedAccounts.value = allAccounts.value.length > 0 && accounts.value.length === 0;
    
  } catch (err) {
    message.value = 'Failed to load accounts. Please log in.';
    transferError.value = true;
  }
};

const searchUsers = async () => {
  searchError.value = '';
  searchPerformed.value = false;
  accountSelected.value = false;
  
  if (!auth.token) {
    searchError.value = 'You must be logged in to search';
    return;
  }
  
  if (searchName.value.length < 2) {
    searchResults.value = [];
    return;
  }
  
  try {
    const response = await axios.get(`http://localhost:8080/api/accounts/search`, {
      params: { name: searchName.value },
      headers: { Authorization: `Bearer ${auth.token}` }
    });
    
    searchResults.value = response.data;
    searchPerformed.value = true;
  } catch (err) {
    searchError.value = 'Error searching for users: ' + (err.response?.data || err.message);
  }
};

const clearRecipient = () => {
  toAccountId.value = '';
  accountSelected.value = false;
};

const selectAccount = (result) => {
  toAccountId.value = result.accountId;
  searchResults.value = [];
  accountSelected.value = true;
  message.value = `Selected ${result.userName}'s account with IBAN: ${result.iban}`;
  transferSuccess.value = true;
  transferError.value = false;
  
  // Clear success message after 3 seconds
  setTimeout(() => {
    if (message.value.includes(result.userName)) {
      message.value = '';
      transferSuccess.value = false;
    }
  }, 3000);
};

const submitTransfer = async () => {
  transferSuccess.value = false;
  transferError.value = false;
  message.value = '';
  isLoading.value = true;
  
  try {
    await axios.post('http://localhost:8080/api/transactions/transfer', {
      senderAccountId: fromAccount.value,
      receiverAccountId: toAccountId.value,
      amount: parseFloat(amount.value),
      description: description.value
    }, {
      headers: {
        Authorization: `Bearer ${auth.token}`
      }
    });
    
    message.value = 'Transfer completed successfully!';
    transferSuccess.value = true;
    isLoading.value = false;
    
    // Reset form
    fromAccount.value = '';
    toAccountId.value = '';
    amount.value = '';
    description.value = '';
    searchName.value = '';
    searchResults.value = [];
    accountSelected.value = false;
    
    // Refresh accounts to show updated balances
    await fetchAccounts();
    
  } catch (err) {
    // Display a clear message when daily limit is exceeded
    if (err.response?.data && err.response.data.includes("daily limit")) {
      message.value = "Transaction cannot be completed because it exceeds your daily transfer limit.";
    } else if (err.response?.data) {
      message.value = err.response.data;
    } else if (err.message) {
      message.value = err.message;
    } else {
      message.value = 'Transfer failed. Please check the details.';
    }
    console.log("Transfer error details:", err.response?.data);
    transferError.value = true;
    isLoading.value = false;
  }
};

onMounted(fetchAccounts);
</script>

<style scoped>
.transfer-page {
  padding: 20px 0;
}

.page-title {
  font-size: 2.2rem;
  margin-bottom: 30px;
  color: var(--primary-dark);
  position: relative;
  display: inline-block;
}

.page-title::after {
  content: '';
  position: absolute;
  bottom: -10px;
  left: 0;
  width: 60px;
  height: 4px;
  background-color: var(--secondary-color);
  border-radius: 2px;
}

.transfer-container {
  margin-top: 30px;
}

.transfer-form {
  display: flex;
  flex-direction: column;
  gap: 30px;
  max-width: 600px;
}

.form-section {
  background-color: white;
  border-radius: var(--border-radius);
  padding: 25px;
  box-shadow: var(--box-shadow);
}

.section-title {
  font-size: 1.3rem;
  margin: 0 0 20px 0;
  color: var(--primary-dark);
  position: relative;
  display: inline-block;
  padding-bottom: 8px;
}

.section-title::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 30px;
  height: 3px;
  background-color: var(--primary-color);
  border-radius: 1.5px;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: var(--text-secondary);
}

.form-row {
  display: flex;
  gap: 15px;
  margin-bottom: 15px;
}

.amount-group {
  width: 100%;
  max-width: 200px;
}

.amount-input-wrapper {
  position: relative;
}

.currency-symbol {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-secondary);
  font-weight: 500;
}

.amount-input {
  padding-left: 25px !important;
}

.form-control {
  padding: 12px 15px;
  border-radius: var(--border-radius);
  border: 1px solid var(--light-gray);
  font-size: 1rem;
  width: 100%;
  transition: border-color 0.3s, box-shadow 0.3s;
}

.form-control:focus {
  border-color: var(--primary-color);
  outline: none;
  box-shadow: 0 0 0 3px rgba(30, 136, 229, 0.2);
}

.search-container {
  display: flex;
  gap: 10px;
}

.search-input {
  flex: 1;
}

.search-button {
  padding: 12px 20px;
  background-color: var(--primary-color);
  color: white;
  border: none;
  border-radius: var(--border-radius);
  cursor: pointer;
  font-weight: 600;
  transition: background-color 0.3s, transform 0.2s;
}

.search-button:hover {
  background-color: var(--primary-dark);
  transform: translateY(-2px);
}

.search-results-container {
  margin: 15px 0 20px;
}

.search-results {
  background-color: white;
  border: 1px solid var(--light-gray);
  border-radius: var(--border-radius);
  max-height: 250px;
  overflow-y: auto;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.search-result-item {
  padding: 15px;
  border-bottom: 1px solid var(--light-gray);
  cursor: pointer;
  transition: background-color 0.2s;
}

.search-result-item:hover {
  background-color: var(--ultra-light-gray);
}

.search-result-item:last-child {
  border-bottom: none;
}

.result-name {
  font-weight: 600;
  margin-bottom: 5px;
  color: var(--text-primary);
}

.result-details {
  font-size: 0.9rem;
  color: var(--text-secondary);
}

.search-error {
  margin-top: 8px;
  color: var(--error-color);
  font-size: 0.9rem;
}

.no-results {
  padding: 25px;
  text-align: center;
  background-color: var(--ultra-light-gray);
  border-radius: var(--border-radius);
  color: var(--text-secondary);
}

.no-results-icon {
  font-size: 2rem;
  margin-bottom: 10px;
  opacity: 0.7;
}

.selected-receiver {
  background-color: var(--ultra-light-gray);
  border-radius: var(--border-radius);
  padding: 15px;
  margin-bottom: 15px;
}

.selected-receiver-label {
  font-size: 0.9rem;
  color: var(--text-secondary);
  margin-bottom: 5px;
}

.selected-receiver-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.recipient-name {
  font-weight: 600;
  color: var(--primary-dark);
}

.clear-receiver {
  background: none;
  border: none;
  color: var(--primary-color);
  cursor: pointer;
  font-size: 0.9rem;
  padding: 0;
  text-decoration: underline;
}

.clear-receiver:hover {
  color: var(--primary-dark);
}

.form-actions {
  margin-top: 10px;
}

.alert {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;
  padding: 15px 20px;
  border-radius: var(--border-radius);
}

.alert-success {
  background-color: #f0fff4;
  color: #2f855a;
  border-left: 4px solid #48bb78;
}

.alert-warning {
  background-color: #fffaf0;
  color: #c05621;
  border-left: 4px solid #ed8936;
}

.alert-danger {
  background-color: #fff5f5;
  color: #c53030;
  border-left: 4px solid #f56565;
}

.alert-icon {
  font-size: 1.2rem;
}

.submit-button {
  padding: 14px 24px;
  background-color: var(--secondary-color);
  color: white;
  border: none;
  border-radius: var(--border-radius);
  cursor: pointer;
  font-size: 1rem;
  font-weight: 600;
  width: 100%;
  transition: background-color 0.3s, transform 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.submit-button:not(:disabled):hover {
  background-color: var(--secondary-dark);
  transform: translateY(-2px);
}

.submit-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.spinner {
  width: 20px;
  height: 20px;
  border: 3px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: white;
  animation: spin 1s ease-in-out infinite;
  display: inline-block;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .form-row {
    flex-direction: column;
    gap: 15px;
  }
  
  .amount-group {
    max-width: none;
  }
}
</style>
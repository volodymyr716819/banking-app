<template>
  <div class="transfer-page">
    <h1 class="page-title">Employee Transfer</h1>

    <form class="transfer-form" @submit.prevent="submitTransfer">
      <!-- Customer selection -->
      <div class="form-group">
        <label for="fromCustomer">From Customer:</label>
        <select v-model="selectedFromCustomer" @change="fetchFromAccounts" required>
          <option disabled value="">Select a customer</option>
          <option v-for="customer in customers" :key="customer.id" :value="customer.id">
            {{ customer.name }}
          </option>
        </select>
      </div>

      <!-- From account selection -->
      <div class="form-group">
        <label for="fromAccount">From Account:</label>
        <select v-model="fromAccount" required>
          <option disabled value="">Select an account</option>
          <option v-for="acc in fromAccounts" :key="acc.id" :value="acc.iban">
            {{ acc.type }} - {{ formatIban(acc.iban) }} - €{{ acc.balance.toFixed(2) }}
          </option>
        </select>
      </div>

      <!-- To customer selection -->
      <div class="form-group">
        <label for="toCustomer">To Customer:</label>
        <select v-model="selectedToCustomer" @change="fetchToAccounts" required>
          <option disabled value="">Select a customer</option>
          <option v-for="customer in customers" :key="customer.id" :value="customer.id">
            {{ customer.name }}
          </option>
        </select>
      </div>

      <!-- To account selection -->
      <div class="form-group">
        <label for="toAccount">To Account:</label>
        <select v-model="toIban" required>
          <option disabled value="">Select an account</option>
          <option v-for="acc in toAccounts" :key="acc.id" :value="acc.iban">
            {{ acc.type }} - {{ formatIban(acc.iban) }} - €{{ acc.balance.toFixed(2) }}
          </option>
        </select>
      </div>

      <div class="form-group">
        <label for="amount">Amount (€):</label>
        <input type="number" v-model="amount" step="0.01" min="0.01" required />
      </div>

      <div class="form-group">
        <label for="description">Description:</label>
        <input type="text" v-model="description" placeholder="Optional description" />
      </div>

      <button type="submit" class="submit-button" :disabled="submitDisabled">Transfer</button>
    </form>

    <div v-if="message" :class="['transfer-message', messageType]">
      <span v-if="messageType === 'error'" class="message-icon">❌</span>
      <span v-else-if="messageType === 'success'" class="message-icon">✓</span>
      {{ message }}
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import api from '../lib/api';
import { useAuthStore } from '../store/auth';

const auth = useAuthStore();
const customers = ref([]);
const fromAccounts = ref([]);
const toAccounts = ref([]);
const selectedFromCustomer = ref('');
const selectedToCustomer = ref('');
const fromAccount = ref('');
const toIban = ref('');
const amount = ref('');
const description = ref('');
const message = ref('');
const messageType = ref(''); // 'success' or 'error'

// Computed property to disable submit button when needed
const submitDisabled = computed(() => {
  return !fromAccount.value || !toIban.value || !amount.value;
});

// Format IBAN for display
const formatIban = (iban) => {
  if (!iban) return '';
  return iban.replace(/(.{4})/g, '$1 ').trim();
};

// Fetch all approved customers
const fetchCustomers = async () => {
  try {
    const response = await api.get('/users/approved', {
      headers: {
        Authorization: `Bearer ${auth.token}`
      }
    });
    customers.value = response.data;
  } catch (err) {
    message.value = 'Failed to load customers.';
    messageType.value = 'error';
  }
};

// Fetch accounts for the selected 'from' customer
const fetchFromAccounts = async () => {
  if (!selectedFromCustomer.value) return;
  
  try {
    const response = await api.get(`/accounts/user/${selectedFromCustomer.value}`, {
      headers: {
        Authorization: `Bearer ${auth.token}`
      }
    });
    // Only include approved accounts
    fromAccounts.value = response.data.filter(acc => acc.approved);
    fromAccount.value = ''; // Reset selection
  } catch (err) {
    message.value = 'Failed to load customer accounts.';
    messageType.value = 'error';
  }
};

// Fetch accounts for the selected 'to' customer
const fetchToAccounts = async () => {
  if (!selectedToCustomer.value) return;
  
  try {
    const response = await api.get(`/accounts/user/${selectedToCustomer.value}`, {
      headers: {
        Authorization: `Bearer ${auth.token}`
      }
    });
    // Only include approved accounts
    toAccounts.value = response.data.filter(acc => acc.approved);
    toIban.value = ''; // Reset selection
  } catch (err) {
    message.value = 'Failed to load customer accounts.';
    messageType.value = 'error';
  }
};

const submitTransfer = async () => {
  // Clear previous messages
  message.value = '';
  
  if (parseFloat(amount.value) <= 0) {
    message.value = 'Transfer amount must be greater than zero.';
    messageType.value = 'error';
    return;
  }
  
  // Check account limits
  const selectedAccount = fromAccounts.value.find(acc => acc.iban === fromAccount.value);
  if (selectedAccount) {
    // Check daily limit
    if (selectedAccount.dailyLimit && selectedAccount.dailyLimit > 0) {
      if (parseFloat(amount.value) > selectedAccount.dailyLimit) {
        message.value = `Transfer amount exceeds the daily limit of €${selectedAccount.dailyLimit} for this account.`;
        messageType.value = 'error';
        return;
      }
    }
    
    // Check absolute limit (overdraft limit)
    const newBalance = selectedAccount.balance - parseFloat(amount.value);
    if (selectedAccount.absoluteLimit && newBalance < selectedAccount.absoluteLimit) {
      message.value = `This transfer would exceed the account's overdraft limit. Maximum available: €${(selectedAccount.balance - selectedAccount.absoluteLimit).toFixed(2)}`;
      messageType.value = 'error';
      return;
    }
  }
  
  try {
    const response = await api.post('/transactions/transfer', {
      senderIban: fromAccount.value,
      receiverIban: toIban.value,
      amount: parseFloat(amount.value),
      description: description.value
    }, {
      headers: {
        Authorization: `Bearer ${auth.token}`
      }
    });
    
    // Success case
    message.value = 'Transfer successful!';
    messageType.value = 'success';
    fromAccount.value = '';
    toIban.value = '';
    amount.value = '';
    description.value = '';
    selectedFromCustomer.value = '';
    selectedToCustomer.value = '';
    fromAccounts.value = [];
    toAccounts.value = [];
  } catch (err) {
    // Handle error
    messageType.value = 'error';
    
    if (err.response) {
      message.value = err.response.data || 'Transfer failed. Please check the details.';
    } else if (err.request) {
      message.value = 'Unable to connect to the server. Please check your connection and try again.';
    } else {
      message.value = 'An error occurred. Please try again.';
    }
  }
};

// Load customers on component mount
onMounted(fetchCustomers);

// Define the page title for the component
const __pageTitle = 'Employee Transfer';

// Expose the page title to the parent component
defineExpose({ __pageTitle });
</script>

<style scoped>
.transfer-page {
  padding: 20px;
  max-width: 600px;
  margin: 0 auto;
}

.page-title {
  color: var(--primary-color);
  margin-bottom: 20px;
}

.transfer-form {
  background-color: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
  color: #333;
}

.form-group select,
.form-group input {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 16px;
}

.account-warning {
  color: #f44336;
  font-size: 14px;
  margin-top: 5px;
}

.submit-button {
  background-color: var(--primary-color);
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  width: 100%;
  margin-top: 10px;
}

.submit-button:hover {
  background-color: var(--primary-dark);
}

.submit-button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.transfer-message {
  margin-top: 20px;
  padding: 10px;
  border-radius: 4px;
  display: flex;
  align-items: center;
}

.transfer-message.success {
  background-color: #e8f5e9;
  color: #2e7d32;
  border: 1px solid #a5d6a7;
}

.transfer-message.error {
  background-color: #ffebee;
  color: #c62828;
  border: 1px solid #ef9a9a;
}

.message-icon {
  margin-right: 10px;
  font-size: 18px;
}

.transfer-disabled-message {
  margin-top: 10px;
  color: #f44336;
  font-size: 14px;
}
</style>
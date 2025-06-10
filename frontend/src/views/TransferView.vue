<template>
  <div class="transfer-page">
    <h1 class="page-title">Transfer Money</h1>

    <form class="transfer-form" @submit.prevent="submitTransfer">
      <div class="form-group">
        <label for="from">From Account:</label>
        <select v-model="fromAccount" required>
          <option disabled value="">Select an account</option>
          <option v-for="acc in accounts" :key="acc.id" :value="acc.id">
            {{ acc.type }} (ID: {{ acc.id }}) - €{{ acc.balance.toFixed(2) }}
            {{ !acc.approved ? '- Not Approved' : '' }}
          </option>
        </select>
        <div v-if="selectedAccountIsNotApproved" class="account-warning">
          This account is not approved for transactions yet.
        </div>
      </div>

      <div class="form-group">
        <label for="to">Receiver IBAN:</label>
        <input type="text" v-model="toAccountIban" placeholder="Enter receiver's IBAN" required />
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
      <div v-if="selectedAccountIsNotApproved" class="transfer-disabled-message">
        Transfers from unapproved accounts are not allowed. Please contact support for account approval.
      </div>
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
const accounts = ref([]);
const fromAccount = ref('');
const toAccountIban = ref('');
const amount = ref('');
const description = ref('');
const message = ref('');
const messageType = ref(''); // 'success' or 'error'

// Computed property to check if selected account is not approved
const selectedAccountIsNotApproved = computed(() => {
  if (!fromAccount.value) return false;
  const selectedAccount = accounts.value.find(acc => acc.id === fromAccount.value);
  return selectedAccount && !selectedAccount.approved;
});

// Computed property to disable submit button when needed
const submitDisabled = computed(() => {
  return selectedAccountIsNotApproved.value || !fromAccount.value || !toAccountId.value || !amount.value;
});

const fetchAccounts = async () => {
  try {
    const response = await api.get(`/accounts/user/${auth.user.id}`, {
      headers: {
        Authorization: `Bearer ${auth.token}`
      }
    });
    // Include all accounts, even unapproved ones, to show them with a warning
    accounts.value = response.data;
  } catch (err) {
    message.value = 'Failed to load accounts. Please log in.';
    messageType.value = 'error';
  }
};

const submitTransfer = async () => {
  // Clear previous messages
  message.value = '';
  
  // Additional frontend validation
  if (selectedAccountIsNotApproved.value) {
    message.value = 'Cannot transfer from unapproved accounts. Please contact support for account approval.';
    messageType.value = 'error';
    return;
  }
  
  if (parseFloat(amount.value) <= 0) {
    message.value = 'Transfer amount must be greater than zero.';
    messageType.value = 'error';
    return;
  }
  
  try {
    const response = await api.post('/transactions/transfer', {
      senderAccountId: fromAccount.value,
      receiverIBAN: toAccountIban.value,
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
    toAccountIban.value = '';
    amount.value = '';
    description.value = '';
    await fetchAccounts(); // Refresh accounts to show updated balances
  } catch (err) {
    // Create a user-friendly error message
    messageType.value = 'error';
    
    if (err.response) {
      // The request was made and the server responded with an error
      message.value = err.response.data || 'Transfer failed. Please check the details.';
    } else if (err.request) {
      // The request was made but no response was received
      message.value = 'Unable to connect to the server. Please check your connection and try again.';
    } else {
      // Something happened in setting up the request that triggered an Error
      message.value = 'An error occurred while processing your transfer. Please try again.';
    }
  }
};

onMounted(fetchAccounts);
</script>

<style scoped>
.transfer-page {
  padding: 40px;
  background-color: #fff;
}

.page-title {
  font-size: 2rem;
  margin-bottom: 20px;
  color: #333;
}

.transfer-form {
  display: flex;
  flex-direction: column;
  gap: 15px;
  max-width: 500px;
  margin-bottom: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

label {
  font-weight: 500;
  color: #4a5568;
}

input,
select {
  padding: 10px;
  border-radius: 6px;
  border: 1px solid #cbd5e0;
  font-size: 1rem;
  transition: all 0.2s;
}

input:focus,
select:focus {
  outline: none;
  border-color: #3182ce;
  box-shadow: 0 0 0 3px rgba(49, 130, 206, 0.1);
}

.submit-button {
  padding: 12px 24px;
  background-color: #3182ce;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 500;
  transition: all 0.2s ease;
  margin-top: 10px;
  align-self: flex-start;
}

.submit-button:hover:not(:disabled) {
  background-color: #2b6cb0;
}

.submit-button:disabled {
  background-color: #a0aec0;
  cursor: not-allowed;
  opacity: 0.7;
}

.transfer-message {
  padding: 16px;
  border-radius: 6px;
  margin-top: 20px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 10px;
}

.transfer-message.success {
  background-color: #c6f6d5;
  color: #276749;
  border-left: 4px solid #48bb78;
}

.transfer-message.error {
  background-color: #fed7d7;
  color: #c53030;
  border-left: 4px solid #f56565;
}

.message-icon {
  font-size: 1.2rem;
}

.account-warning {
  font-size: 0.9rem;
  color: #c53030;
  margin-top: 4px;
}

.transfer-disabled-message {
  font-size: 0.9rem;
  padding: 10px;
  background-color: #fed7d7;
  border-radius: 4px;
  color: #c53030;
  margin-bottom: 10px;
}
</style>
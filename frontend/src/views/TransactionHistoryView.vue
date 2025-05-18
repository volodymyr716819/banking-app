<template>
  <div class="transaction-history-page">
    <h1 class="page-title">Transaction History</h1>
    
    <div class="filter-section">
      <div class="filter-controls">
        <label for="account-type-filter">Filter by Account Type:</label>
        <select 
          id="account-type-filter" 
          v-model="selectedAccountType" 
          @change="fetchTransactions"
          class="form-control"
        >
          <option value="">All Accounts</option>
          <option value="CHECKING">Checking</option>
          <option value="SAVINGS">Savings</option>
        </select>
      </div>
    </div>
    
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <div class="loading-text">Loading transactions...</div>
    </div>
    
    <div v-else-if="error" class="alert alert-danger">
      <div class="alert-icon">⚠️</div>
      <div>{{ error }}</div>
    </div>
    
    <div v-else-if="transactions.length === 0" class="empty-state">
      <div class="empty-icon">📊</div>
      <h3>No transactions found</h3>
      <p>Try a different filter or make some transfers first.</p>
    </div>
    
    <div v-else class="transactions-container">
      <div class="transactions-header">
        <h3>
          {{ transactions.length }} 
          {{ transactions.length === 1 ? 'Transaction' : 'Transactions' }}
        </h3>
      </div>
      
      <table class="transactions-table">
        <thead>
          <tr>
            <th>Date</th>
            <th>Type</th>
            <th>Description</th>
            <th>From</th>
            <th>To</th>
            <th>Amount</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="transaction in transactions" :key="transaction.id" 
              :class="getTransactionClass(transaction)">
            <td class="transaction-date">{{ formatDate(transaction.timestamp) }}</td>
            <td class="transaction-type-cell">
              <span :class="'transaction-type ' + transaction.type.toLowerCase()">{{ transaction.type }}</span>
            </td>
            <td>{{ transaction.description || 'N/A' }}</td>
            <td>{{ getSenderAccountInfo(transaction) }}</td>
            <td>{{ getReceiverAccountInfo(transaction) }}</td>
            <td :class="getAmountClass(transaction)">
              {{ formatAmount(transaction) }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { useAuthStore } from '../store/auth';
import { useRoute } from 'vue-router';

const auth = useAuthStore();
const route = useRoute();
const transactions = ref([]);
const loading = ref(true);
const error = ref('');
const selectedAccountType = ref('');
const selectedAccountId = ref(null);

const fetchTransactions = async () => {
  try {
    loading.value = true;
    error.value = '';
    
    let url;
    let params = {};
    
    // If we have a specific account ID, fetch transactions for that account
    if (selectedAccountId.value) {
      url = `http://localhost:8080/api/transactions/account/${selectedAccountId.value}`;
    } else {
      // Otherwise get all user transactions with optional account type filter
      url = `http://localhost:8080/api/transactions/user/${auth.user.id}`;
      if (selectedAccountType.value) {
        params.accountType = selectedAccountType.value;
      }
    }
    
    const response = await axios.get(url, {
      params,
      headers: {
        Authorization: `Bearer ${auth.token}`
      }
    });
    
    transactions.value = response.data;
    loading.value = false;
  } catch (err) {
    console.error('Failed to fetch transactions:', err);
    error.value = 'Failed to load transactions. Please try again later.';
    loading.value = false;
  }
};

const formatDate = (dateString) => {
  if (!dateString) return 'N/A';
  const date = new Date(dateString);
  return new Intl.DateTimeFormat('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date);
};

const formatAmount = (transaction) => {
  if (!transaction.amount) return '€0.00';
  // Format with 2 decimal places
  return `€${parseFloat(transaction.amount).toFixed(2)}`;
};

const getSenderAccountInfo = (transaction) => {
  if (transaction.type === 'DEPOSIT') {
    return 'Cash Deposit';
  }
  
  if (!transaction.senderAccountId) {
    return 'External Account';
  }
  
  return `${transaction.senderAccountType} (IBAN: ${transaction.senderIban})`;
};

const getReceiverAccountInfo = (transaction) => {
  if (transaction.type === 'WITHDRAW') {
    return 'Cash Withdrawal';
  }
  
  if (!transaction.receiverAccountId) {
    return 'External Account';
  }
  
  return `${transaction.receiverAccountType} (IBAN: ${transaction.receiverIban})`;
};

const getTransactionClass = (transaction) => {
  // For ATM operations
  if (transaction.type === 'DEPOSIT') {
    return { 'transaction-row': true, 'incoming': true };
  }
  
  if (transaction.type === 'WITHDRAW') {
    return { 'transaction-row': true, 'outgoing': true };
  }
  
  // For transfers
  const isUserSender = isCurrentUserSender(transaction);
  return {
    'transaction-row': true,
    'outgoing': isUserSender,
    'incoming': !isUserSender
  };
};

const getAmountClass = (transaction) => {
  // For ATM operations
  if (transaction.type === 'DEPOSIT') {
    return { 'transaction-amount': true, 'positive': true };
  }
  
  if (transaction.type === 'WITHDRAW') {
    return { 'transaction-amount': true, 'negative': true };
  }
  
  // For transfers
  const isUserSender = isCurrentUserSender(transaction);
  return {
    'transaction-amount': true,
    'negative': isUserSender,
    'positive': !isUserSender
  };
};

const isCurrentUserSender = (transaction) => {
  // Only applicable for transfers
  if (transaction.type !== 'TRANSFER') return false;
  
  // Check if the sender account belongs to the current user
  return transaction.senderAccountId !== null && 
         (transaction.accountId === auth.user.id || // Direct account match
          selectedAccountId.value === transaction.senderAccountId); // Selected account match
};

onMounted(() => {
  // Check if we were passed a specific account ID in the route
  if (route.query.accountId) {
    selectedAccountId.value = parseInt(route.query.accountId);
  }
  fetchTransactions();
});
</script>

<style scoped>
.transaction-history-page {
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

.filter-section {
  background-color: white;
  border-radius: var(--border-radius);
  padding: 25px;
  margin-bottom: 30px;
  box-shadow: var(--box-shadow);
}

.filter-controls {
  display: flex;
  align-items: center;
  gap: 15px;
}

.filter-controls label {
  font-weight: 500;
  color: var(--text-secondary);
  min-width: 180px;
}

.filter-controls select {
  padding: 12px 15px;
  border-radius: var(--border-radius);
  border: 1px solid var(--light-gray);
  font-size: 1rem;
  background-color: white;
  transition: border-color 0.3s ease, box-shadow 0.3s ease;
  max-width: 250px;
}

.filter-controls select:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(30, 136, 229, 0.2);
  outline: none;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 15px;
  padding: 60px 0;
  background-color: white;
  border-radius: var(--border-radius);
  box-shadow: var(--box-shadow);
  margin-top: 30px;
}

.loading-spinner {
  border: 3px solid rgba(0, 0, 0, 0.1);
  border-top: 3px solid var(--primary-color);
  border-radius: 50%;
  width: 50px;
  height: 50px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-text {
  font-size: 1.1rem;
  color: var(--text-secondary);
  font-weight: 500;
}

.alert {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-top: 30px;
}

.alert-icon {
  font-size: 1.5rem;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  background-color: white;
  border-radius: var(--border-radius);
  box-shadow: var(--box-shadow);
  margin-top: 30px;
  text-align: center;
}

.empty-icon {
  font-size: 3.5rem;
  margin-bottom: 20px;
  opacity: 0.7;
}

.empty-state h3 {
  font-size: 1.5rem;
  margin-bottom: 10px;
  color: var(--text-primary);
}

.empty-state p {
  color: var(--text-secondary);
  max-width: 400px;
}

.transactions-container {
  background-color: white;
  border-radius: var(--border-radius);
  box-shadow: var(--box-shadow);
  margin-top: 30px;
  overflow: hidden;
}

.transactions-header {
  padding: 20px 25px;
  background-color: var(--ultra-light-gray);
  border-bottom: 1px solid var(--light-gray);
}

.transactions-header h3 {
  margin: 0;
  font-size: 1.25rem;
  color: var(--text-primary);
}

.transactions-table {
  width: 100%;
  border-collapse: collapse;
}

.transactions-table th,
.transactions-table td {
  padding: 15px 20px;
  text-align: left;
  vertical-align: middle;
  border-bottom: 1px solid var(--light-gray);
}

.transactions-table th {
  background-color: var(--ultra-light-gray);
  color: var(--text-secondary);
  font-weight: 600;
  font-size: 0.9rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.transactions-table tr:last-child td {
  border-bottom: none;
}

.transaction-date {
  font-size: 0.9rem;
  color: var(--text-secondary);
  white-space: nowrap;
}

.transaction-row {
  transition: background-color 0.2s ease;
}

.transaction-row:hover {
  background-color: var(--ultra-light-gray);
}

.transaction-row.incoming {
  background-color: rgba(46, 204, 113, 0.05);
}

.transaction-row.incoming:hover {
  background-color: rgba(46, 204, 113, 0.1);
}

.transaction-row.outgoing {
  background-color: rgba(231, 76, 60, 0.05);
}

.transaction-row.outgoing:hover {
  background-color: rgba(231, 76, 60, 0.1);
}

.transaction-amount {
  font-weight: 700;
  text-align: right;
  white-space: nowrap;
}

.transaction-amount.positive {
  color: var(--success-color);
}

.transaction-amount.positive::before {
  content: '+';
}

.transaction-amount.negative {
  color: var(--error-color);
}

.transaction-type {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 50px;
  font-size: 0.8rem;
  font-weight: 600;
  text-transform: capitalize;
  white-space: nowrap;
}

.transaction-type-cell {
  text-align: center;
}

.transaction-type.transfer {
  background-color: var(--info-color);
  color: white;
}

.transaction-type.deposit {
  background-color: var(--success-color);
  color: white;
}

.transaction-type.withdraw {
  background-color: var(--error-color);
  color: white;
}

@media (max-width: 992px) {
  .transactions-table {
    display: block;
    overflow-x: auto;
  }
  
  .filter-controls {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .filter-controls select {
    width: 100%;
    max-width: none;
  }
}
</style>
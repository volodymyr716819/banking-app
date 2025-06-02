<template>
  <div class="transaction-history">
    

    <div class="filters-container">
      <div class="filter-section">
        <label>Transaction Type:</label>
        <select v-model="selectedTransactionType">
          <option value="">All</option>
          <option value="TRANSFER">Transfers</option>
          <option value="DEPOSIT">Deposits</option>
          <option value="WITHDRAW">Withdrawals</option>
        </select>
      </div>

      <div class="filter-section">
        <label>Account:</label>
        <select v-model="filterAccount">
          <option value="">All Accounts</option>
          <option v-for="account in accounts" :key="account.id" :value="account.iban">
            {{ account.type.charAt(0) + account.type.slice(1).toLowerCase() }} - {{ formatIban(account.iban) }}
          </option>
        </select>
      </div>

      <button v-if="selectedTransactionType || filterAccount" 
              @click="clearFilters" 
              class="clear-filters-button">
        Clear Filters
      </button>
    </div>

    <p v-if="message" class="status-message">{{ message }}</p>

    <table v-if="getFilteredTransactions().length" class="transaction-table">
      <thead>
        <tr>
          <th>Type</th>
          <th>From</th>
          <th>To</th>
          <th>Amount</th>
          <th>Date</th>
          <th>Description</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(tx, index) in getFilteredTransactions()" :key="index" 
            :class="getTransactionTypeClass(tx.transactionType)">
          <td>
            <span class="transaction-type-badge" :class="`transaction-type-${tx.transactionType.toLowerCase()}`">
              {{ formatTransactionType(tx.transactionType) }}
            </span>
          </td>
          <td>
            <div v-if="tx.fromAccountIban">
              <div class="account-iban">{{ formatIban(tx.fromAccountIban) }}</div>
              <div class="account-holder">{{ tx.fromAccountHolderName }}</div>
            </div>
            <span v-else>-</span>
          </td>
          <td>
            <div v-if="tx.toAccountIban">
              <div class="account-iban">{{ formatIban(tx.toAccountIban) }}</div>
              <div class="account-holder">{{ tx.toAccountHolderName }}</div>
            </div>
            <span v-else>-</span>
          </td>
          <td class="amount">{{ formatAmount(tx.amount) }}</td>
          <td>{{ formatDate(tx.timestamp) }}</td>
          <td>{{ tx.description || '-' }}</td>
        </tr>
      </tbody>
    </table>

    <p v-else class="no-data">No transactions found.</p>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { useAuthStore } from '../store/auth'

export default {
  setup() {
    const auth = useAuthStore()
    const transactions = ref([])
    const selectedTransactionType = ref('')
    const message = ref('')
    const accounts = ref([])
    const filterAccount = ref('')

    const fetchTransactions = async () => {
      if (!auth.user || !auth.user.id) {
        message.value = 'User information not available. Please log in again.';
        return;
      }
      
      message.value = 'Loading transactions...';
      
      try {
        const response = await axios.get(`http://localhost:8080/api/transactions/user/${auth.user.id}`, {
          headers: {
            Authorization: `Bearer ${auth.token}`
          }
        });
        
        if (response.data && Array.isArray(response.data)) {
          transactions.value = response.data;
          message.value = transactions.value.length > 0 ? '' : 'No transactions found.';
        } else {
          message.value = 'Invalid response format.';
          transactions.value = [];
        }
      } catch (error) {
        console.error('Transaction fetch error:', error);
        
        if (error.response) {
          // Server returned error code
          if (error.response.status === 403) {
            message.value = 'Access denied. You may not have permission to view these transactions.';
          } else {
            message.value = `Error: ${error.response.data || 'Failed to load transactions'}`;
          }
        } else if (error.request) {
          // Request made but no response
          message.value = 'Unable to reach the server. Please check your connection.';
        } else {
          // Request setup error
          message.value = 'Failed to load transactions.';
        }
        
        transactions.value = [];
      }
    }

    const fetchUserAccounts = async () => {
      try {
        const response = await axios.get(
          `http://localhost:8080/api/accounts/user/${auth.user.id}`,
          {
            headers: {
              Authorization: `Bearer ${auth.token}`
            }
          }
        )
        accounts.value = response.data
      } catch (error) {
        console.error('Failed to fetch accounts:', error)
      }
    }

    onMounted(() => {
      fetchTransactions()
      fetchUserAccounts()
    })

    const clearFilters = () => {
      selectedTransactionType.value = ''
      filterAccount.value = ''
    }

    const getFilteredTransactions = () => {
      let result = transactions.value

      // Filter by transaction type if selected
      if (selectedTransactionType.value) {
        result = result.filter(tx => 
          tx.transactionType === selectedTransactionType.value
        )
      }

      // Filter by account if selected
      if (filterAccount.value) {
        result = result.filter(tx => 
          tx.fromAccountIban === filterAccount.value || 
          tx.toAccountIban === filterAccount.value
        )
      }

      return result
    }

    const formatAmount = (amount) => {
      return '€' + Number(amount).toFixed(2)
    }

    const formatDate = (timestamp) => {
      const date = new Date(timestamp)
      // Format: 'Jan 01, 2025 14:30'
      return new Intl.DateTimeFormat('en-GB', {
        year: 'numeric',
        month: 'short',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      }).format(date)
    }
    
    const formatIban = (iban) => {
      // Format IBAN with spaces for readability: NL12 BANK 0123 4567 89
      return iban.replace(/(.{4})/g, '$1 ').trim()
    }
    
    const formatTransactionType = (type) => {
      switch(type) {
        case 'TRANSFER': 
          return 'Transfer'
        case 'DEPOSIT': 
          return 'Deposit'
        case 'WITHDRAW': 
          return 'Withdrawal'
        default: 
          return type
      }
    }
    
    const getTransactionTypeClass = (type) => {
      return `transaction-${type.toLowerCase()}`
    }

    return {
      selectedTransactionType,
      transactions,
      message,
      getFilteredTransactions,
      formatAmount,
      formatDate,
      formatIban,
      formatTransactionType,
      getTransactionTypeClass,
      accounts,
      filterAccount,
      clearFilters
    }
  }
}
</script>

<style scoped>
.transaction-history {
  width: 100%;
}

h1 {
  font-size: var(--font-size-2xl);
  margin-bottom: var(--spacing-4);
  font-weight: var(--font-weight-semibold);
  color: var(--gray-900);
}

.filters-container {
  display: flex;
  gap: var(--spacing-4);
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: var(--spacing-6);
  background-color: var(--gray-50);
  padding: var(--spacing-4);
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-sm);
}

.filter-section {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
}

.filter-section label {
  font-weight: var(--font-weight-medium);
  color: var(--gray-700);
  white-space: nowrap;
}

.filter-section select {
  padding: var(--spacing-2) var(--spacing-3);
  border-radius: var(--border-radius);
  border: 1px solid var(--gray-300);
  min-width: 180px;
  font-size: var(--font-size-sm);
  background-color: var(--white);
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);
}

.filter-section select:focus {
  border-color: var(--primary-light);
  outline: 0;
  box-shadow: 0 0 0 0.2rem rgba(25, 118, 210, 0.25);
}

.clear-filters-button {
  background-color: var(--error-color);
  color: var(--white);
  border: none;
  padding: var(--spacing-2) var(--spacing-3);
  border-radius: var(--border-radius);
  cursor: pointer;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  transition: background-color var(--transition-fast);
}

.clear-filters-button:hover {
  background-color: var(--error-color);
  opacity: 0.9;
}

.status-message {
  color: var(--info-color);
  margin-bottom: var(--spacing-3);
}

.transaction-table {
  width: 100%;
  border-collapse: collapse;
  box-shadow: var(--shadow);
  border-radius: var(--border-radius-lg);
  overflow: hidden;
}

.transaction-table th {
  background-color: var(--gray-50);
  font-weight: var(--font-weight-semibold);
  text-align: left;
  padding: var(--spacing-3);
  border-bottom: 2px solid var(--gray-200);
  color: var(--gray-700);
  font-size: var(--font-size-sm);
}

.transaction-table td {
  padding: var(--spacing-3);
  border-bottom: 1px solid var(--gray-200);
  vertical-align: top;
  font-size: var(--font-size-sm);
}

/* Transaction type styling */
.transaction-transfer {
  background-color: rgba(33, 150, 243, 0.05);
}

.transaction-deposit {
  background-color: rgba(76, 175, 80, 0.05);
}

.transaction-withdraw {
  background-color: rgba(244, 67, 54, 0.05);
}

/* Hover effect */
.transaction-table tr:hover {
  background-color: var(--gray-100);
}

.account-iban {
  font-family: var(--font-family-mono);
  font-size: var(--font-size-xs);
  letter-spacing: 0.05em;
}

.account-holder {
  font-size: var(--font-size-xs);
  color: var(--gray-600);
  margin-top: var(--spacing-1);
}

.amount {
  font-weight: var(--font-weight-semibold);
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.no-data {
  margin-top: var(--spacing-4);
  font-style: italic;
  color: var(--gray-600);
  text-align: center;
  padding: var(--spacing-5);
  background-color: var(--gray-50);
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-sm);
}

.transaction-type-badge {
  display: inline-block;
  padding: var(--spacing-1) var(--spacing-2);
  border-radius: var(--border-radius-full);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  text-align: center;
  white-space: nowrap;
}

.transaction-type-transfer {
  background-color: rgba(33, 150, 243, 0.15);
  color: var(--info-color);
}

.transaction-type-deposit {
  background-color: rgba(76, 175, 80, 0.15);
  color: var(--success-color);
}

.transaction-type-withdraw {
  background-color: rgba(244, 67, 54, 0.15);
  color: var(--error-color);
}

@media (max-width: 768px) {
  .filters-container {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .filter-section {
    width: 100%;
  }
  
  .filter-section select {
    flex-grow: 1;
  }
  
  .clear-filters-button {
    align-self: flex-end;
  }
}
</style>

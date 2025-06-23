<template>
  <div class="transaction-history">
    <div class="filters-container">
      <!-- Employee-only customer selection -->
      <div v-if="auth && auth.isAuthenticated && auth.isEmployee" class="filter-section">
        <label>Customer:</label>
        <select v-model="selectedUserId">
          <option value="">All Customers</option>
          <option v-for="user in customerList" :key="user.id" :value="user.id">
            {{ user.name }} ({{ user.email }})
          </option>
        </select>
      </div>
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
        <label>Date Range:</label>
        <div class="date-range-inputs">
          <input 
            type="date" 
            v-model="startDate" 
            placeholder="Start Date"
            class="date-input"
          />
          <span class="date-separator">to</span>
          <input 
            type="date" 
            v-model="endDate" 
            placeholder="End Date"
            class="date-input"
          />
        </div>
      </div>
      
      <div class="filter-section">
        <label>Amount Range:</label>
        <div class="amount-range-inputs">
          <div class="amount-input-wrapper">
            <span class="currency-symbol">€</span>
            <input 
              type="number" 
              v-model="minAmount" 
              placeholder="Min" 
              min="0"
              step="0.01"
              class="amount-input"
            />
          </div>
          <span class="amount-separator">to</span>
          <div class="amount-input-wrapper">
            <span class="currency-symbol">€</span>
            <input 
              type="number" 
              v-model="maxAmount" 
              placeholder="Max" 
              min="0"
              step="0.01"
              class="amount-input"
            />
          </div>
        </div>
      </div>

      <button
        v-if="isAnyFilterActive"
        @click="clearFilters"
        class="clear-filters-button"
      >
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
        <tr
          v-for="(tx, index) in getFilteredTransactions()"
          :key="index"
          :class="getTransactionTypeClass(tx.transactionType)"
        >
          <td>
            <span
              class="transaction-type-badge"
              :class="`transaction-type-${tx.transactionType.toLowerCase()}`"
            >
              {{ formatTransactionType(tx.transactionType) }}
            </span>
          </td>
          <td>
            <div v-if="tx.fromAccountIban">
              <div class="account-iban">
                {{ formatIban(tx.fromAccountIban) }}
              </div>
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
          <td>{{ tx.description || "-" }}</td>
        </tr>
      </tbody>
    </table>

    <p v-else class="no-data">No transactions found.</p>
  </div>
</template>

<script>
import { ref, onMounted, watch, computed } from "vue";
import api from '../lib/api';
import { useAuthStore } from "../store/auth";

export default {
  setup() {
    const auth = useAuthStore();
    const transactions = ref([]);
    const selectedTransactionType = ref("");
    const message = ref("");
    const startDate = ref("");
    const endDate = ref("");
    const minAmount = ref("");
    const maxAmount = ref("");
    const selectedUserId = ref("");
    const customerList = ref([]);

    // check if any filter is active
    const isAnyFilterActive = computed(() => {
      return selectedTransactionType.value || 
             startDate.value || 
             endDate.value || 
             minAmount.value || 
             maxAmount.value || 
             selectedUserId.value;
    });

    // Fetch customer list for employees only
    const fetchCustomerList = async () => {
      if (auth && auth.isAuthenticated && auth.isEmployee) {
        try {
          const response = await api.get('/users/approved', {
            headers: {
              Authorization: `Bearer ${auth.token}`,
            },
          });
          
          if (response.data && Array.isArray(response.data)) {
            customerList.value = response.data;
          }
        } catch (error) {
        }
      }
    };

    const fetchTransactions = async () => {
      if (!auth.isAuthenticated) {
        message.value = "User information not available. Please log in again.";
        return;
      }

      message.value = "Loading transactions...";

      try {
        // Create filter parameters using the new format
        const params = new URLSearchParams();
        
        // User ID filter (employee only)
        if (selectedUserId.value) {
          params.append('userId', selectedUserId.value);
        }
        
        // Date filters
        if (startDate.value) params.append('startDate', startDate.value);
        if (endDate.value) params.append('endDate', endDate.value);
        
        // Amount filters
        if (minAmount.value && !isNaN(minAmount.value)) params.append('minAmount', minAmount.value);
        if (maxAmount.value && !isNaN(maxAmount.value)) params.append('maxAmount', maxAmount.value);
        
        // Build query string
        const queryString = params.toString() ? `?${params.toString()}` : '';
        
        // Use the new unified endpoint
        const response = await api.get(
          `/transactions/history${queryString}`,
          {
            headers: {
              Authorization: `Bearer ${auth.token}`,
            },
          }
        );

        if (response.data && Array.isArray(response.data)) {
          transactions.value = response.data;
          message.value =
            transactions.value.length > 0 ? "" : "No transactions found.";
        } else {
          message.value = "Invalid response format.";
          transactions.value = [];
        }
      } catch (error) {

        if (error.response) {
          // Server returned error code
          if (error.response.status === 403) {
            message.value =
              "Access denied. You may not have permission to view these transactions.";
          } else if (error.response.status === 401) {
            message.value = "Authentication error. Please try logging in again.";
          } else {
            message.value = "Error loading transactions. Please try again later.";
          }
        } else if (error.request) {
          // Request made but no response
          message.value =
            "Unable to reach the server. Please check your connection.";
        } else {
          // Request setup error
          message.value = "Failed to load transactions.";
        }

        transactions.value = [];
      }
    };

    onMounted(() => {
      fetchCustomerList();
      fetchTransactions();
    });
    
    // Watch for changes in filter parameters and refetch data from server
    watch([startDate, endDate, minAmount, maxAmount, selectedUserId], () => {
      fetchTransactions();
    });

    const clearFilters = () => {
      selectedTransactionType.value = "";
      startDate.value = "";
      endDate.value = "";
      minAmount.value = "";
      maxAmount.value = "";
      selectedUserId.value = "";
    };

    const getFilteredTransactions = () => {
      let result = transactions.value;

      // Only apply client-side transaction type filtering
      if (selectedTransactionType.value) {
        result = result.filter(tx => tx.transactionType === selectedTransactionType.value);
      }
      
      return result;
    };

    const formatAmount = (amount) => {
      return "€" + Number(amount).toFixed(2);
    };

    const formatDate = (timestamp) => {
      if (!timestamp) return 'Unknown';
      const date = new Date(timestamp);
      const day = date.getDate().toString().padStart(2, '0');
      const month = date.toLocaleString('en-US', { month: 'short' });
      const year = date.getFullYear();
      const hours = date.getHours().toString().padStart(2, '0');
      const minutes = date.getMinutes().toString().padStart(2, '0');
      return `${day} ${month} ${year} at ${hours}:${minutes}`;
    };

    const formatIban = (iban) => {
      if (!iban) return ""; // handle null / undefined
      return iban.replace(/(.{4})/g, "$1 ").trim();
    };

    const formatTransactionType = (type) => {
      switch (type) {
        case "TRANSFER":
          return "Transfer";
        case "DEPOSIT":
          return "Deposit";
        case "WITHDRAW":
          return "Withdrawal";
        default:
          return type;
      }
    };

    const getTransactionTypeClass = (type) => {
      return `transaction-${type.toLowerCase()}`;
    };

    return {
      auth,
      selectedTransactionType,
      transactions,
      message,
      getFilteredTransactions,
      formatAmount,
      formatDate,
      formatIban,
      formatTransactionType,
      getTransactionTypeClass,
      startDate,
      endDate,
      minAmount,
      maxAmount,
      clearFilters,
      selectedUserId,
      customerList,
      isAnyFilterActive,
    };
  },
};
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
  transition: border-color var(--transition-fast),
    box-shadow var(--transition-fast);
}

.filter-section select:focus,
.filter-section input:focus {
  border-color: var(--primary-light);
  outline: 0;
  box-shadow: 0 0 0 0.2rem rgba(25, 118, 210, 0.25);
}

.date-range-inputs,
.amount-range-inputs {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
}

.date-separator,
.amount-separator {
  color: var(--gray-600);
  font-size: var(--font-size-sm);
}

.date-input {
  padding: var(--spacing-2) var(--spacing-3);
  border-radius: var(--border-radius);
  border: 1px solid var(--gray-300);
  background-color: var(--white);
  font-size: var(--font-size-sm);
  min-width: 140px;
}

.amount-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.currency-symbol {
  position: absolute;
  left: var(--spacing-2);
  color: var(--gray-600);
  font-size: var(--font-size-sm);
}

.amount-input {
  padding: var(--spacing-2) var(--spacing-3) var(--spacing-2) var(--spacing-5);
  border-radius: var(--border-radius);
  border: 1px solid var(--gray-300);
  background-color: var(--white);
  font-size: var(--font-size-sm);
  width: 100px;
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
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

      <div class="filter-section date-filters">
        <label>Date Range:</label>
        <div class="date-inputs">
          <input 
            type="date" 
            v-model="startDate" 
            placeholder="Start Date"
            class="date-input"
            aria-label="Start date"
          >
          <span class="date-separator">to</span>
          <input 
            type="date" 
            v-model="endDate" 
            placeholder="End Date"
            class="date-input"
            aria-label="End date"
          >
        </div>
      </div>
      
      <div class="filter-section amount-filters">
        <label>Amount Range:</label>
        <div class="amount-inputs">
          <input 
            type="number" 
            v-model="minAmount" 
            placeholder="Min €"
            class="amount-input"
            min="0"
            step="0.01"
            aria-label="Minimum amount"
          >
          <span class="amount-separator">to</span>
          <input 
            type="number" 
            v-model="maxAmount" 
            placeholder="Max €"
            class="amount-input"
            min="0"
            step="0.01"
            aria-label="Maximum amount"
          >
        </div>
      </div>


      <button
        v-if="selectedTransactionType || startDate || endDate || minAmount || maxAmount"
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
              <div class="account-holder">{{ tx.fromAccountHolderName || 'Unknown' }}</div>
            </div>
            <span v-else>-</span>
          </td>
          <td>
            <div v-if="tx.toAccountIban">
              <div class="account-iban">{{ formatIban(tx.toAccountIban) }}</div>
              <div class="account-holder">{{ tx.toAccountHolderName || 'Unknown' }}</div>
            </div>
            <span v-else>-</span>
          </td>
          <td class="amount">{{ formatAmount(tx.amount) }}</td>
          <td class="date" v-html="formatDate(tx.timestamp)"></td>
          <td>{{ tx.description || "-" }}</td>
        </tr>
      </tbody>
    </table>

    <p v-else class="no-data">No transactions found.</p>
  </div>
</template>

<script>
import { ref, onMounted } from "vue";
import axios from "axios";
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

    // Fetch user's transaction history from the server
    const fetchTransactions = async () => {
      // Check if user is logged in
      if (!isUserLoggedIn()) {
        return;
      }

      // Show loading message
      setLoadingMessage();

      try {
        // Get transactions from API
        const response = await fetchUserTransactions();
        // Process the response
        handleTransactionResponse(response);
      } catch (error) {
        // Handle any errors
        handleTransactionError(error);
      }
    };
    
    // Check if user is logged in with valid ID
    const isUserLoggedIn = () => {
      if (!auth.user || !auth.user.id) {
        message.value = "User information not available. Please log in again.";
        return false;
      }
      return true;
    };
    
    // Set loading message
    const setLoadingMessage = () => {
      message.value = "Loading transactions...";
    };
    
    // Fetch transactions from API
    const fetchUserTransactions = async () => {
      return await axios.get(
        `http://localhost:8080/api/transactions/user/${auth.user.id}`,
        {
          headers: {
            Authorization: `Bearer ${auth.token}`,
          },
        }
      );
    };
    
    // Process API response
    const handleTransactionResponse = (response) => {
      if (response.data && Array.isArray(response.data)) {
        transactions.value = response.data;
        message.value = transactions.value.length > 0 ? "" : "No transactions found.";
      } else {
        message.value = "Invalid response format.";
        transactions.value = [];
      }
    };
    
    // Handle API errors
    const handleTransactionError = (error) => {
      transactions.value = [];
      
      if (error.response) {
        // Server returned error code
        if (error.response.status === 403) {
          message.value = "Access denied. You may not have permission to view these transactions.";
        } else {
          message.value = `Error: ${error.response.data || "Failed to load transactions"}`;
        }
      } else if (error.request) {
        // Request made but no response
        message.value = "Unable to reach the server. Please check your connection.";
      } else {
        // Request setup error
        message.value = "Failed to load transactions.";
      }
    };


    onMounted(() => {
      fetchTransactions();
    });

    // Clear all filters and reset to default view
    const clearFilters = () => {
      // Reset filter values
      selectedTransactionType.value = "";
      startDate.value = "";
      endDate.value = "";
      minAmount.value = "";
      maxAmount.value = "";
    };

    // Apply filters to transactions
    const getFilteredTransactions = () => {
      // Start with all transactions
      let result = transactions.value;
      
      // Apply type filter if selected
      result = filterByTransactionType(result);
      
      // Apply date filter if selected
      result = filterByDateRange(result);
      
      // Apply amount filter if selected
      result = filterByAmount(result);
      
      return result;
    };
    
    // Filter transactions by type
    const filterByTransactionType = (txList) => {
      // If no type filter is selected, return all transactions
      if (!selectedTransactionType.value) {
        return txList;
      }
      
      // Return only transactions matching the selected type
      return txList.filter(tx => 
        tx.transactionType === selectedTransactionType.value
      );
    };
    
    // Filter transactions by date range
    const filterByDateRange = (txList) => {
      // If no date filters are set, return all transactions
      if (!startDate.value && !endDate.value) {
        return txList;
      }
      
      return txList.filter(tx => {
        // Convert transaction timestamp to a Date object
        const txDate = new Date(tx.timestamp);
        
        // Check if transaction date is after startDate (if set)
        const afterStartDate = startDate.value ? 
          txDate >= new Date(startDate.value) : true;
        
        // Check if transaction date is before endDate (if set)
        // For end date, we set the time to 23:59:59 to include the whole day
        const beforeEndDate = endDate.value ? 
          txDate <= new Date(endDate.value + 'T23:59:59') : true;
        
        // Return true if both conditions are met
        return afterStartDate && beforeEndDate;
      });
    };
    
    // Filter transactions by amount range
    const filterByAmount = (txList) => {
      // If no amount filters are set, return all transactions
      if (!minAmount.value && !maxAmount.value) {
        return txList;
      }
      
      return txList.filter(tx => {
        // Parse amount as a number
        const amount = Number(tx.amount);
        
        // Check if amount is greater than or equal to minAmount (if set)
        const aboveMinAmount = minAmount.value ? 
          amount >= Number(minAmount.value) : true;
        
        // Check if amount is less than or equal to maxAmount (if set)
        const belowMaxAmount = maxAmount.value ? 
          amount <= Number(maxAmount.value) : true;
        
        // Return true if both conditions are met
        return aboveMinAmount && belowMaxAmount;
      });
    };

    // Format currency amount with Euro symbol
    const formatAmount = (amount) => {
      return "€" + Number(amount).toFixed(2);
    };

    // Format date in user-friendly format
    const formatDate = (timestamp) => {
      if (!timestamp) return "";
      
      const date = new Date(timestamp);
      
      // Format the date part: 'Jan 01, 2025'
      const datePart = new Intl.DateTimeFormat("en-GB", {
        year: "numeric",
        month: "short",
        day: "2-digit",
      }).format(date);
      
      // Format the time part: '14:30'
      const timePart = new Intl.DateTimeFormat("en-GB", {
        hour: "2-digit",
        minute: "2-digit",
      }).format(date);
      
      // Return formatted date with time
      return `<span class="date-value">${datePart}</span><br><span class="time-value">${timePart}</span>`;
    };

    // Format IBAN with spaces for readability
    const formatIban = (iban) => {
      if (!iban) return ""; // handle null / undefined
      return iban.replace(/(.{4})/g, "$1 ").trim();
    };

    // Convert transaction type to user-friendly format
    const formatTransactionType = (type) => {
      const typeMap = {
        "TRANSFER": "Transfer",
        "DEPOSIT": "Deposit",
        "WITHDRAW": "Withdrawal"
      };
      
      return typeMap[type] || type;
    };

    const getTransactionTypeClass = (type) => {
      return `transaction-${type.toLowerCase()}`;
    };


    return {
      // State
      selectedTransactionType,
      transactions,
      message,
      startDate,
      endDate,
      minAmount,
      maxAmount,
      
      // Methods for UI rendering
      getFilteredTransactions,
      formatAmount,
      formatDate,
      formatIban,
      formatTransactionType,
      getTransactionTypeClass,
      clearFilters,
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

.date-inputs,
.amount-inputs {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
}

.date-input,
.amount-input {
  padding: var(--spacing-2) var(--spacing-3);
  border-radius: var(--border-radius);
  border: 1px solid var(--gray-300);
  font-size: var(--font-size-sm);
  background-color: var(--white);
  width: 120px;
  transition: border-color var(--transition-fast),
    box-shadow var(--transition-fast);
}

.date-separator,
.amount-separator {
  color: var(--gray-600);
  font-size: var(--font-size-sm);
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

.date {
  text-align: center;
  min-width: 110px;
}

.date-value {
  font-weight: var(--font-weight-medium);
  color: var(--gray-800);
}

.time-value {
  font-size: var(--font-size-xs);
  color: var(--gray-600);
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

  .filter-section select,
  .date-inputs,
  .amount-inputs {
    flex-grow: 1;
    width: 100%;
  }
  
  .date-input,
  .amount-input {
    flex-grow: 1;
  }

  .clear-filters-button {
    align-self: flex-end;
  }
}
</style>

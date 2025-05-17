<template>
    <div class="transfer-page">
      <h1 class="page-title">Transfer Money</h1>
      
      <div v-if="noApprovedAccounts" class="approval-warning">
        ⚠️ You have accounts, but none are approved yet. Please wait for approval before making transfers.
      </div>
  
      <form v-if="!noApprovedAccounts" class="transfer-form" @submit.prevent="submitTransfer">
        <div class="form-group">
          <label for="from">From Account:</label>
          <select v-model="fromAccount" required>
            <option disabled value="">Select an account</option>
            <option v-for="acc in accounts" :key="acc.id" :value="acc.id">
              {{ acc.type }} - IBAN: {{ acc.iban }} - €{{ acc.balance.toFixed(2) }}
            </option>
          </select>
        </div>

        <div class="form-group">
          <label for="searchName">Search by Name:</label>
          <div class="search-container">
            <input 
              type="text" 
              v-model="searchName" 
              placeholder="Search receiver by name" 
              @input="searchUsers" 
            />
            <button type="button" class="search-button" @click="searchUsers">Search</button>
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
            <p>No accounts found. The user may exist but has no approved accounts.</p>
          </div>
        </div>
  
        <div class="form-group">
          <label for="to">To Account ID:</label>
          <input type="number" v-model="toAccountId" placeholder="Enter receiver's account ID" required />
        </div>
  
        <div class="form-group">
          <label for="amount">Amount (€):</label>
          <input type="number" v-model="amount" step="0.01" min="0.01" required />
        </div>
  
        <div class="form-group">
          <label for="description">Description:</label>
          <input type="text" v-model="description" placeholder="Optional description" />
        </div>
  
        <button type="submit" class="submit-button">Transfer</button>
      </form>
  
      <div v-if="message" class="transfer-message">{{ message }}</div>
    </div>
  </template>
  
  <script setup>
  import { ref, onMounted } from 'vue';
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
    }
  };
  
  const searchUsers = async () => {
    console.log('searchUsers function called with name:', searchName.value);
    searchError.value = '';
    searchPerformed.value = false;
    accountSelected.value = false; // Reset account selection when starting a new search
    
    if (!auth.token) {
      console.error('No authentication token available');
      searchError.value = 'You must be logged in to search';
      return;
    }
    
    if (searchName.value.length < 2) {
      searchResults.value = [];
      return;
    }
    
    try {
      console.log('Searching for users with name:', searchName.value);
      console.log('Using token:', auth.token);
      
      const response = await axios.get(`http://localhost:8080/api/accounts/search`, {
        params: { name: searchName.value },
        headers: { Authorization: `Bearer ${auth.token}` }
      });
      
      console.log('Search results:', response.data);
      searchResults.value = response.data;
      searchPerformed.value = true;
      
      if (response.data.length === 0) {
        console.log('No matching accounts found');
      }
    } catch (err) {
      console.error('Search error:', err);
      searchError.value = 'Error searching for users: ' + (err.response?.data || err.message);
    }
  };
  
  const selectAccount = (result) => {
    console.log('Selected account:', result);
    toAccountId.value = result.accountId;
    searchResults.value = [];
    accountSelected.value = true; // Mark account as selected
    message.value = `Selected ${result.userName}'s account with IBAN: ${result.iban}`;
  };
  
  const submitTransfer = async () => {
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
      message.value = 'Transfer successful!';
      fromAccount.value = '';
      toAccountId.value = '';
      amount.value = '';
      description.value = '';
      searchName.value = '';
      searchResults.value = [];
      accountSelected.value = false; // Reset account selection after transfer
      await fetchAccounts();
    } catch (err) {
      message.value = 'Transfer failed. Please check the details.';
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
    max-width: 400px;
  }
  
  .form-group {
    display: flex;
    flex-direction: column;
  }
  
  input,
  select {
    padding: 8px;
    border-radius: 6px;
    border: 1px solid #ccc;
    font-size: 1rem;
  }
  
  .submit-button {
    padding: 10px;
    background-color: #4CAF50;
    color: white;
    border: none;
    border-radius: 6px;
    cursor: pointer;
    font-size: 1rem;
  }
  
  .submit-button:hover {
    background-color: #45a049;
  }
  
  .transfer-message {
    margin-top: 20px;
    font-weight: bold;
    color: #2b6cb0;
  }
  
  .search-container {
    display: flex;
    gap: 10px;
  }
  
  .search-button {
    padding: 8px 15px;
    background-color: #4CAF50;
    color: white;
    border: none;
    border-radius: 6px;
    cursor: pointer;
  }
  
  .search-results-container {
    margin-bottom: 15px;
  }
  
  .search-results {
    border: 1px solid #ccc;
    border-radius: 6px;
    max-height: 200px;
    overflow-y: auto;
  }
  
  .no-results {
    border: 1px solid #ccc;
    border-radius: 6px;
    padding: 10px;
    color: #666;
    background-color: #f5f5f5;
    font-style: italic;
  }
  
  .search-result-item {
    padding: 10px;
    border-bottom: 1px solid #eee;
    cursor: pointer;
  }
  
  .search-result-item:hover {
    background-color: #f5f5f5;
  }
  
  .search-result-item:last-child {
    border-bottom: none;
  }
  
  .result-name {
    font-weight: bold;
  }
  
  .result-details {
    font-size: 0.9rem;
    color: #555;
  }
  
  .search-error {
    margin-top: 5px;
    color: #e74c3c;
    font-size: 0.9rem;
  }
  
  .approval-warning {
    background-color: #fff3cd;
    color: #856404;
    padding: 15px;
    border-radius: 5px;
    margin-bottom: 20px;
    font-weight: bold;
    border: 1px solid #ffeeba;
  }
  </style>
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
            </option>
          </select>
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
  
  const accounts = ref([]);
  const fromAccount = ref('');
  const toAccountId = ref('');
  const amount = ref('');
  const description = ref('');
  const message = ref('');
  
  const fetchAccounts = async () => {
    try {
      const res = await axios.get('http://localhost:8080/api/accounts/user', {
        withCredentials: true
      });
      accounts.value = res.data.filter(acc => acc.approved === true);
    } catch (err) {
      message.value = 'Failed to load accounts. Please log in.';
    }
  };
  
  const submitTransfer = async () => {
    try {
      await axios.post('http://localhost:8080/api/transactions/transfer', {
        senderAccountId: fromAccount.value,
        receiverAccountId: toAccountId.value,
        amount: parseFloat(amount.value),
        description: description.value
      }, {
        withCredentials: true
      });
      message.value = 'Transfer successful!';
      fromAccount.value = '';
      toAccountId.value = '';
      amount.value = '';
      description.value = '';
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
  </style>  
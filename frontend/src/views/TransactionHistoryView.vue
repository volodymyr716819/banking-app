<template>
    <div class="dashboard">
      <h2>Transaction History</h2>
  
      <div v-if="transactions.length === 0">
        No transactions found.
      </div>
  
      <table v-else>
        <thead>
          <tr>
            <th>ID</th>
            <th>Type</th>
            <th>Amount</th>
            <th>Timestamp</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="tx in transactions" :key="tx.id">
            <td>{{ tx.id }}</td>
            <td>{{ tx.type }}</td>
            <td>{{ tx.amount }}</td>
            <td>{{ new Date(tx.timestamp).toLocaleString() }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </template>
  
  <script>
  export default {
    name: 'TransactionHistoryView',
    data() {
      return {
        transactions: []
      };
    },
    async mounted() {
      // Fetch transaction history for logged-in user
      try {
        const res = await fetch('/api/transactions');
        if (!res.ok) throw new Error('Could not load transactions');
        this.transactions = await res.json();
      } catch (err) {
        console.error(err.message);
      }
    }
  };
  </script>
  
  <style scoped>
  .dashboard {
    max-width: 800px;
    margin: 60px auto;
    text-align: center;
  }
  
  table {
    margin: auto;
    width: 90%;
    border-collapse: collapse;
    margin-top: 2rem;
  }
  
  th, td {
    border: 1px solid #ddd;
    padding: 0.75rem;
  }
  
  th {
    background-color: #e74c3c;
    color: white;
  }
  
  td {
    background-color: #f9f9f9;
  }
  </style>
  
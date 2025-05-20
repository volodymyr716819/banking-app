<template>
  <div class="search-customer-page">
    <h1 class="page-title">Find Customer by Name</h1>

    <div class="search-container">
      <form class="search-form" @submit.prevent="searchCustomers">
        <div class="form-group">
          <label for="name">Customer Name</label>
          <input 
            type="text" 
            id="name" 
            v-model="searchName" 
            placeholder="Enter customer name" 
            required
          />
        </div>
        <button type="submit" class="search-button">Search</button>
      </form>
    </div>

    <div v-if="loading" class="loading-indicator">
      Loading results...
    </div>

    <div v-if="errorMessage" class="error-message">
      {{ errorMessage }}
    </div>

    <div v-if="searchResults.length > 0" class="search-results">
      <h2>Search Results</h2>
      <div class="results-list">
        <div v-for="result in searchResults" :key="result.id" class="result-card">
          <h3>{{ result.name }}</h3>
          <div v-if="result.ibans.length > 0">
            <p>Available IBANs:</p>
            <ul class="ibans-list">
              <li v-for="(iban, index) in result.ibans" :key="index" class="iban-item">
                <span class="iban-text">{{ formatIban(iban) }}</span>
                <button @click="copyToClipboard(iban)" class="copy-button">
                  Copy
                </button>
              </li>
            </ul>
          </div>
          <p v-else class="no-ibans">No accounts available for this customer.</p>
        </div>
      </div>
    </div>

    <div v-if="searchPerformed && searchResults.length === 0 && !loading && !errorMessage" class="no-results">
      No customers found matching your search criteria.
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { useAuthStore } from '../store/auth';

const auth = useAuthStore();
const searchName = ref('');
const searchResults = ref([]);
const loading = ref(false);
const errorMessage = ref('');
const searchPerformed = ref(false);

const searchCustomers = async () => {
  if (searchName.value.trim() === '') {
    return;
  }

  loading.value = true;
  errorMessage.value = '';
  searchPerformed.value = true;
  
  try {
    const response = await axios.get(`http://localhost:8080/api/users/find-by-name`, {
      params: {
        name: searchName.value
      },
      headers: {
        Authorization: `Bearer ${auth.token}`
      }
    });
    
    searchResults.value = response.data;
  } catch (err) {
    errorMessage.value = err.response?.data || 'An error occurred while searching. Please try again.';
    searchResults.value = [];
  } finally {
    loading.value = false;
  }
};

const formatIban = (iban) => {
  // Format IBAN with spaces for readability: NL12 BANK 0123 4567 89
  return iban.replace(/(.{4})/g, '$1 ').trim();
};

const copyToClipboard = (text) => {
  navigator.clipboard.writeText(text)
    .then(() => {
      // You could add a temporary success message here
    })
    .catch(err => {
      console.error('Failed to copy: ', err);
    });
};
</script>

<style scoped>
.search-customer-page {
  padding: 40px;
  background-color: #fff;
}

.page-title {
  font-size: 2rem;
  margin-bottom: 20px;
  color: #333;
}

.search-container {
  margin-bottom: 40px;
}

.search-form {
  display: flex;
  gap: 15px;
  max-width: 600px;
  align-items: flex-end;
}

.form-group {
  display: flex;
  flex-direction: column;
  flex-grow: 1;
}

label {
  margin-bottom: 8px;
  font-weight: 500;
}

input {
  padding: 10px;
  border-radius: 6px;
  border: 1px solid #ccc;
  font-size: 1rem;
}

.search-button {
  padding: 10px 20px;
  background-color: #3182ce;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1rem;
  height: 42px;
}

.search-button:hover {
  background-color: #2c5282;
}

.loading-indicator {
  margin: 20px 0;
  color: #718096;
}

.error-message {
  margin: 20px 0;
  color: #e53e3e;
  padding: 10px;
  background-color: #fff5f5;
  border-radius: 6px;
  border-left: 4px solid #e53e3e;
}

.no-results {
  margin: 20px 0;
  color: #4a5568;
  font-style: italic;
}

.search-results h2 {
  font-size: 1.5rem;
  margin-bottom: 15px;
  color: #333;
}

.results-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.result-card {
  padding: 20px;
  background-color: #f8fafc;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.result-card h3 {
  margin-top: 0;
  color: #2d3748;
  font-size: 1.2rem;
  margin-bottom: 15px;
}

.ibans-list {
  list-style-type: none;
  padding: 0;
  margin: 10px 0 0;
}

.iban-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #edf2f7;
  padding: 8px 12px;
  border-radius: 4px;
  margin-bottom: 8px;
}

.iban-text {
  font-family: monospace;
  font-size: 0.95rem;
  letter-spacing: 0.5px;
}

.copy-button {
  background-color: #e2e8f0;
  border: none;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.8rem;
}

.copy-button:hover {
  background-color: #cbd5e0;
}

.no-ibans {
  color: #718096;
  font-style: italic;
  margin-top: 5px;
}
</style>
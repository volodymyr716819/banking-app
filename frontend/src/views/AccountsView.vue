<template>
  <div class="accounts-page">
    <h1 class="page-title">Your Accounts</h1>

    <div class="create-account-container">
      <button @click="showForm = !showForm" class="create-account-button">
        {{ showForm ? "Cancel" : "+ Create Account" }}
      </button>

      <div v-if="showForm" class="account-form">
        <label for="accountType">Select Account Type:</label>
        <select
          v-model="newAccountType"
          id="accountType"
          class="account-type-select"
        >
          <option value="CHECKING">Checking</option>
          <option value="SAVINGS">Savings</option>
        </select>
        <button @click="createAccount" class="submit-button">Create</button>
      </div>
    </div>

    <div class="accounts-grid">
      <div v-for="account in accounts" :key="account.id" class="account-card">
        <h2>{{ account.type.charAt(0) + account.type.slice(1).toLowerCase() }} Account</h2>
        <p>Balance: €{{ account.balance.toFixed(2) }}</p>
        <p>
          Status:
          <span :class="account.approved ? 'approved-badge' : 'pending-badge'">
            {{ account.approved ? "Approved" : "Pending Approval" }}
          </span>
        </p>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from "vue";
import axios from "axios";

export default {
  setup() {
    const accounts = ref([]);
    const showForm = ref(false);
    const newAccountType = ref("CHECKING");

    const fetchAccounts = async () => {
      try {
        const user = JSON.parse(localStorage.getItem("user"));
        if (!user || !user.id) {
          alert("User not logged in.");
          return;
        }
        const response = await axios.get(
          `http://localhost:8080/api/accounts/user/${user.id}`
        );

        accounts.value = response.data;
      } catch (err) {
        console.error("Failed to fetch accounts:", err);
        alert("Failed to load accounts. Please make sure you are logged in.");
      }
    };

    const createAccount = async () => {
      try {
        const user = JSON.parse(localStorage.getItem("user"));
        if (!user || !user.id) {
          alert("User not logged in");
          return;
        }
        await axios.post(
          `http://localhost:8080/api/accounts/create?userId=${user.id}&type=${newAccountType.value}`
        );

        showForm.value = false;
        newAccountType.value = "CHECKING";
        await fetchAccounts();
      } catch (err) {
        console.error("Failed to create account", err);
      }
    };

    onMounted(() => {
      fetchAccounts();
    });

    return {
      accounts,
      showForm,
      newAccountType,
      createAccount,
    };
  },
};
</script>

<style scoped>
.accounts-page {
  padding: 40px;
  background-color: #fff;
}

.page-title {
  font-size: 2rem;
  margin-bottom: 20px;
  font-weight: bold;
  color: #333;
}

.create-account-container {
  margin-bottom: 30px;
}

.create-account-button {
  padding: 10px 20px;
  background-color: #4caf50;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 1rem;
}

.create-account-button:hover {
  background-color: #45a049;
}

.account-form {
  margin-top: 15px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.account-type-select {
  padding: 8px;
  border-radius: 6px;
  border: 1px solid #ccc;
}

.submit-button {
  padding: 8px 16px;
  background-color: #3182ce;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1rem;
}

.submit-button:hover {
  background-color: #2b6cb0;
}

.accounts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 20px;
}

.account-card {
  background: #f9f9f9;
  padding: 20px;
  border-radius: 10px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}

.approved-badge {
  color: green;
  font-weight: bold;
}

.pending-badge {
  color: orange;
  font-weight: bold;
}
</style>

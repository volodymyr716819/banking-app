<template>
  <div class="accounts-page">
    <h1 class="page-title">Your Accounts</h1>

    <div class="create-account-container">
      <button @click="showForm = !showForm" class="create-account-button">
        <span class="button-icon">{{ showForm ? "✕" : "+" }}</span>
        {{ showForm ? "Cancel" : "Create Account" }}
      </button>

      <div v-if="showForm" class="account-form">
        <label for="accountType">Select Account Type:</label>
        <select
          v-model="newAccountType"
          id="accountType"
          class="account-type-select form-control"
        >
          <option value="CHECKING">Checking</option>
          <option value="SAVINGS">Savings</option>
        </select>
        <button @click="createAccount" class="submit-button btn">Create</button>
      </div>
    </div>

    <div class="accounts-grid">
      <div 
        v-for="account in accounts" 
        :key="account.id" 
        class="account-card"
        @click="viewTransactions(account.id)"
      >
        <h2>
          {{
            account.type.charAt(0) + account.type.slice(1).toLowerCase()
          }}
          Account
        </h2>
        <p class="account-iban">IBAN: {{ account.iban }}</p>
        <p class="account-balance">€{{ account.balance.toFixed(2) }}</p>
        <p class="account-status">
          <span :class="account.approved ? 'approved-badge' : 'pending-badge'">
            {{ account.approved ? "Approved" : "Pending Approval" }}
          </span>
        </p>
        <div class="view-transactions-hint">
          <span class="hint-icon">↗</span> Click to view transactions
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from "vue";
import axios from "axios";
import { useAuthStore } from "../store/auth";
import { useRouter } from "vue-router";

export default {
  setup() {
    const auth = useAuthStore();
    const router = useRouter();
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
          `http://localhost:8080/api/accounts/user/${auth.user.id}`,
          {
            headers: {
              Authorization: `Bearer ${auth.token}`,
            },
          }
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
          `http://localhost:8080/api/accounts/create?userId=${user.id}&type=${newAccountType.value}`,
          {}, // Empty body
          {
            headers: {
              Authorization: `Bearer ${auth.token}`
            }
          }
        );

        showForm.value = false;
        newAccountType.value = "CHECKING";
        await fetchAccounts();
      } catch (err) {
        console.error("Failed to create account", err);
      }
    };

    const viewTransactions = (accountId) => {
      router.push(`/dashboard/transactions?accountId=${accountId}`);
    };
    
    onMounted(() => {
      fetchAccounts();
    });

    return {
      accounts,
      showForm,
      newAccountType,
      createAccount,
      viewTransactions,
    };
  },
};
</script>

<style scoped>
.accounts-page {
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

.create-account-container {
  margin-bottom: 40px;
  background-color: white;
  border-radius: var(--border-radius);
  padding: 25px;
  box-shadow: var(--box-shadow);
}

.create-account-button {
  padding: 12px 24px;
  background-color: var(--secondary-color);
  color: white;
  border: none;
  border-radius: var(--border-radius);
  cursor: pointer;
  font-size: 1rem;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.button-icon {
  font-size: 1.2rem;
  font-weight: bold;
}

.create-account-button:hover {
  background-color: var(--secondary-dark);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.account-form {
  margin-top: 25px;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 15px;
  align-items: end;
  background-color: var(--ultra-light-gray);
  padding: 20px;
  border-radius: var(--border-radius);
}

.account-form label {
  font-weight: 500;
  margin-bottom: 8px;
  color: var(--text-secondary);
  grid-column: 1 / -1;
}

.account-type-select {
  padding: 12px 15px;
  border-radius: var(--border-radius);
  border: 1px solid var(--light-gray);
  font-size: 1rem;
  width: 100%;
  background-color: white;
  transition: border-color 0.3s ease, box-shadow 0.3s ease;
}

.account-type-select:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(30, 136, 229, 0.2);
  outline: none;
}

.submit-button {
  padding: 12px 24px;
  background-color: var(--primary-color);
  color: white;
  border: none;
  border-radius: var(--border-radius);
  cursor: pointer;
  font-size: 1rem;
  font-weight: 600;
  transition: all 0.3s ease;
  height: 100%;
}

.submit-button:hover {
  background-color: var(--primary-dark);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.accounts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 25px;
}

.account-card {
  background: white;
  padding: 25px;
  border-radius: var(--border-radius);
  box-shadow: var(--box-shadow);
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.account-card:hover {
  transform: translateY(-7px);
  box-shadow: var(--box-shadow-hover);
}

.account-card h2 {
  font-size: 1.5rem;
  margin: 0;
  color: var(--primary-dark);
  display: flex;
  align-items: center;
  gap: 10px;
}

.account-card h2::before {
  content: '';
  display: block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: var(--primary-color);
}

.account-balance {
  font-size: 1.8rem;
  font-weight: 700;
  color: var(--secondary-color);
  margin: 8px 0;
}

.account-status {
  margin: 8px 0;
}

.approved-badge {
  color: white;
  font-weight: 600;
  background-color: var(--success-color);
  padding: 6px 12px;
  border-radius: 50px;
  font-size: 0.8rem;
  display: inline-flex;
  align-items: center;
}

.pending-badge {
  color: white;
  font-weight: 600;
  background-color: var(--warning-color);
  padding: 6px 12px;
  border-radius: 50px;
  font-size: 0.8rem;
  display: inline-flex;
  align-items: center;
}

.account-iban {
  font-family: monospace;
  background-color: var(--ultra-light-gray);
  padding: 12px;
  border-radius: var(--border-radius);
  font-size: 1rem;
  border: none;
  margin: 10px 0;
  letter-spacing: 1px;
  position: relative;
  overflow: hidden;
}

.account-iban::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  width: 4px;
  background-color: var(--primary-color);
}

.view-transactions-hint {
  margin-top: 15px;
  padding: 10px;
  font-size: 0.85rem;
  color: var(--primary-color);
  text-align: center;
  background-color: rgba(33, 150, 243, 0.05);
  border-radius: var(--border-radius);
  font-weight: 500;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}

.hint-icon {
  font-size: 1.1rem;
  transition: transform 0.3s ease;
}

.account-card:hover .view-transactions-hint {
  background-color: rgba(33, 150, 243, 0.1);
}

.account-card:hover .hint-icon {
  transform: translateY(-3px) translateX(3px);
}

@media (max-width: 768px) {
  .accounts-grid {
    grid-template-columns: 1fr;
  }
  
  .account-form {
    grid-template-columns: 1fr;
  }
  
  .create-account-container {
    padding: 20px;
  }
}
</style>
<template>
  <div>
    <div class="accounts-page">
      <div class="create-account-container">
        <button @click="showForm = !showForm" class="create-account-button">
          {{ showForm ? "Cancel" : "+ Create Account" }}
        </button>

        <div v-if="showForm" class="account-form">
          <label for="accountType">Select Account Type:</label>
          <select v-model="newAccountType" id="accountType" class="account-type-select">
            <option value="CHECKING">Checking</option>
            <option value="SAVINGS">Savings</option>
          </select>
          <button @click="createAccount" class="submit-button">Create</button>
        </div>
      </div>

      <div class="accounts-grid">
        <div 
          v-for="account in accounts" 
          :key="account.id" 
          class="account-card" 
          :class="{ 'clickable': account.approved }"
          @click="account.approved && viewAccountHistory(account)"
        >
          <div class="account-header">
            <h2>
              {{ account.type.charAt(0) + account.type.slice(1).toLowerCase() }} Account
            </h2>
            <span :class="account.approved ? 'approved-badge' : 'pending-badge'">
              {{ account.approved ? "Approved" : "Pending Approval" }}
            </span>
          </div>
          
          <div class="account-details">
            <div class="iban-container">
              <div class="iban-label">IBAN:</div>
              <div class="iban-value">{{ formatIban(account.iban) }}</div>
              <button 
                v-if="account.iban" 
                @click.stop="copyIban(account.iban)" 
                class="copy-iban-button" 
                title="Copy IBAN"
              >
                {{ copiedIban === account.iban ? '✓' : 'Copy' }}
              </button>
            </div>
            
            <div class="balance-container">
              <div class="balance-label">Balance:</div>
              <div class="balance-value">€{{ account.balance.toFixed(2) }}</div>
            </div>
          </div>
          
          <div v-if="account.approved" class="account-footer">
            <span class="view-history-text">Click to view transaction history</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from "vue";
import api from '../lib/api';
import { useAuthStore } from "../store/auth";
import { useRouter } from 'vue-router';

export default {
  __pageTitle: 'Your Accounts',
  setup() {
    const auth = useAuthStore();
    const router = useRouter();
    const accounts = ref([]);
    const showForm = ref(false);
    const newAccountType = ref("CHECKING");
    const copiedIban = ref("");

    const fetchAccounts = async () => {
      try {
        const response = await api.get(
          `/accounts/user/${auth.user.id}`,
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
        await api.post(
          `/accounts/create?userId=${auth.user.id}&type=${newAccountType.value}`,
          {},
          {
            headers: {
              Authorization: `Bearer ${auth.token}`,
            },
          }
        );
        showForm.value = false;
        newAccountType.value = "CHECKING";
        await fetchAccounts();
      } catch (err) {
        console.error("Failed to create account", err);
        alert("Could not create account. Make sure you're approved and logged in.");
      }
    };
    
    // Format IBAN for display with spaces
    const formatIban = (iban) => {
      if (!iban) return "Not available";
      return iban.replace(/(.{4})/g, '$1 ').trim();
    };
    
    // Copy IBAN to clipboard
    const copyIban = (iban) => {
      navigator.clipboard.writeText(iban)
        .then(() => {
          copiedIban.value = iban;
          setTimeout(() => {
            copiedIban.value = "";
          }, 2000);
        })
        .catch(err => {
          console.error("Failed to copy IBAN", err);
        });
    };
    
    // Navigate to the transaction history view for this account
    const viewAccountHistory = (account) => {
      router.push({
        path: '/dashboard/history',
        query: { 
          iban: account.iban,
          accountName: `${account.type.charAt(0) + account.type.slice(1).toLowerCase()} Account`
        }
      });
    };

    onMounted(() => {
      fetchAccounts();
    });

    return {
      accounts,
      showForm,
      newAccountType,
      createAccount,
      formatIban,
      copyIban,
      copiedIban,
      viewAccountHistory
    };
  },
};
</script>

<style scoped>
.accounts-page {
  width: 100%;
}

.create-account-container {
  margin-bottom: var(--spacing-6);
}

.create-account-button {
  padding: var(--spacing-2) var(--spacing-4);
  background-color: var(--secondary-color);
  color: var(--white);
  border: none;
  border-radius: var(--border-radius);
  cursor: pointer;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  transition: background-color var(--transition-fast);
}

.create-account-button:hover {
  background-color: var(--secondary-dark);
}

.account-form {
  margin-top: var(--spacing-4);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-3);
  background-color: var(--gray-50);
  padding: var(--spacing-4);
  border-radius: var(--border-radius);
  max-width: 400px;
}

.account-form label {
  font-weight: var(--font-weight-medium);
  color: var(--gray-700);
}

.account-type-select {
  padding: var(--spacing-2) var(--spacing-3);
  border-radius: var(--border-radius);
  border: 1px solid var(--gray-300);
  background-color: var(--white);
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);
}

.account-type-select:focus {
  border-color: var(--primary-light);
  outline: 0;
  box-shadow: 0 0 0 0.2rem rgba(25, 118, 210, 0.25);
}

.submit-button {
  padding: var(--spacing-2) var(--spacing-4);
  background-color: var(--primary-color);
  color: var(--white);
  border: none;
  border-radius: var(--border-radius);
  cursor: pointer;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  transition: background-color var(--transition-fast);
  align-self: flex-start;
}

.submit-button:hover {
  background-color: var(--primary-dark);
}

.accounts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--spacing-5);
}

.account-card {
  background: var(--white);
  padding: var(--spacing-5);
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow);
  transition: transform var(--transition-normal), box-shadow var(--transition-normal);
  display: flex;
  flex-direction: column;
}

.account-card.clickable {
  cursor: pointer;
}

.account-card.clickable:hover {
  transform: translateY(-5px);
  box-shadow: var(--shadow-lg);
}

.account-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-4);
  border-bottom: 1px solid var(--gray-200);
  padding-bottom: var(--spacing-3);
}

.account-header h2 {
  margin: 0;
  font-size: var(--font-size-xl);
  color: var(--gray-900);
  font-weight: var(--font-weight-semibold);
}

.account-details {
  flex-grow: 1;
  margin-bottom: var(--spacing-4);
}

.iban-container, .balance-container {
  display: flex;
  align-items: center;
  margin-bottom: var(--spacing-3);
  flex-wrap: wrap;
}

.iban-label, .balance-label {
  font-weight: var(--font-weight-medium);
  color: var(--gray-700);
  width: 80px;
  flex-shrink: 0;
}

.iban-value {
  font-family: var(--font-family-mono);
  font-size: var(--font-size-sm);
  letter-spacing: 0.05em;
  color: var(--gray-900);
  margin-right: var(--spacing-2);
  flex-grow: 1;
}

.balance-value {
  font-weight: var(--font-weight-semibold);
  color: var(--gray-900);
  flex-grow: 1;
  font-variant-numeric: tabular-nums;
  font-size: var(--font-size-lg);
}

.copy-iban-button {
  background-color: var(--gray-100);
  border: 1px solid var(--gray-300);
  padding: var(--spacing-1) var(--spacing-2);
  border-radius: var(--border-radius-sm);
  font-size: var(--font-size-xs);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.copy-iban-button:hover {
  background-color: var(--gray-200);
}

.account-footer {
  font-size: var(--font-size-sm);
  color: var(--gray-600);
  background-color: var(--gray-50);
  padding: var(--spacing-2);
  border-radius: var(--border-radius);
  text-align: center;
  margin-top: auto;
}

.view-history-text {
  color: var(--gray-600);
  font-style: italic;
}

.approved-badge {
  color: var(--success-color);
  font-weight: var(--font-weight-medium);
  font-size: var(--font-size-xs);
  background-color: rgba(76, 175, 80, 0.1);
  padding: var(--spacing-1) var(--spacing-2);
  border-radius: var(--border-radius-full);
  border: 1px solid rgba(76, 175, 80, 0.2);
}

.pending-badge {
  color: var(--warning-color);
  font-weight: var(--font-weight-medium);
  font-size: var(--font-size-xs);
  background-color: rgba(255, 152, 0, 0.1);
  padding: var(--spacing-1) var(--spacing-2);
  border-radius: var(--border-radius-full);
  border: 1px solid rgba(255, 152, 0, 0.2);
}
</style>

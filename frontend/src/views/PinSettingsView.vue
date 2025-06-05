<template>
  <div class="pin-settings-container">
    <div class="pin-settings-card">
      <h2 class="card-title">PIN Management</h2>
      
      <div v-if="loading" class="loading-spinner"></div>
      
      <div v-else-if="error" class="error-message">
        {{ error }}
      </div>
      
      <div v-else class="pin-settings-content">
        <div class="accounts-list" v-if="accounts.length > 0">
          <h3>Select Account to Manage PIN</h3>
          
          <div v-for="account in accounts" :key="account.id" class="account-item">
            <div class="account-info">
              <div class="account-name">{{ account.type }}</div>
              <div class="account-id">ID: {{ account.id }}</div>
            </div>
            
            <div class="pin-status">
              <span :class="{'pin-set': account.pinCreated, 'pin-not-set': !account.pinCreated}">
                {{ account.pinCreated ? 'PIN Set' : 'No PIN' }}
              </span>
            </div>
            
            <button 
              class="manage-pin-btn" 
              @click="selectAccount(account.id)"
              :class="{'create-btn': !account.pinCreated, 'change-btn': account.pinCreated}"
            >
              {{ account.pinCreated ? 'Change PIN' : 'Create PIN' }}
            </button>
          </div>
        </div>
        
        <div v-else class="no-accounts">
          No approved accounts found. Please contact customer service.
        </div>
      </div>
    </div>
    
    <!-- PIN Management Modal -->
    <div v-if="showPinModal" class="pin-modal-overlay" @click="cancelPin">
      <div class="pin-modal" @click.stop>
        <div class="modal-header">
          <h3>{{ selectedAccountHasPin ? 'Change PIN' : 'Create PIN' }}</h3>
          <button class="close-btn" @click="cancelPin">×</button>
        </div>
        
        <div class="modal-body">
          <div v-if="selectedAccountHasPin" class="pin-form">
            <div class="form-group">
              <label>Current PIN</label>
              <div class="pin-input">
                <input 
                  v-for="i in 4" 
                  :key="'current-'+i" 
                  ref="currentPinFields"
                  type="password" 
                  maxlength="1" 
                  v-model="currentPin[i-1]"
                  @input="focusNextInput('current', i-1)"
                  @keydown.delete="handleDelete('current', i-1)"
                />
              </div>
            </div>
            
            <div class="form-group">
              <label>New PIN</label>
              <div class="pin-input">
                <input 
                  v-for="i in 4" 
                  :key="'new-'+i"
                  ref="newPinFields"
                  type="password" 
                  maxlength="1" 
                  v-model="newPin[i-1]"
                  @input="focusNextInput('new', i-1)"
                  @keydown.delete="handleDelete('new', i-1)"
                />
              </div>
            </div>
            
            <div class="form-group">
              <label>Confirm New PIN</label>
              <div class="pin-input">
                <input 
                  v-for="i in 4" 
                  :key="'confirm-'+i"
                  ref="confirmPinFields"
                  type="password" 
                  maxlength="1" 
                  v-model="confirmPin[i-1]"
                  @input="focusNextInput('confirm', i-1)"
                  @keydown.delete="handleDelete('confirm', i-1)"
                />
              </div>
            </div>
          </div>
          
          <div v-else class="pin-form">
            <div class="form-group">
              <label>New PIN</label>
              <div class="pin-input">
                <input 
                  v-for="i in 4" 
                  :key="'new-'+i"
                  ref="newPinFields"
                  type="password" 
                  maxlength="1" 
                  v-model="newPin[i-1]"
                  @input="focusNextInput('new', i-1)"
                  @keydown.delete="handleDelete('new', i-1)"
                />
              </div>
            </div>
            
            <div class="form-group">
              <label>Confirm PIN</label>
              <div class="pin-input">
                <input 
                  v-for="i in 4" 
                  :key="'confirm-'+i"
                  ref="confirmPinFields"
                  type="password" 
                  maxlength="1" 
                  v-model="confirmPin[i-1]"
                  @input="focusNextInput('confirm', i-1)"
                  @keydown.delete="handleDelete('confirm', i-1)"
                />
              </div>
            </div>
          </div>
          
          <div v-if="modalError" class="error-message">
            {{ modalError }}
          </div>
          
          <div v-if="successMessage" class="success-message">
            {{ successMessage }}
          </div>
        </div>
        
        <div class="modal-footer">
          <button class="cancel-btn" @click="cancelPin">Cancel</button>
          <button 
            class="save-btn" 
            @click="savePinChanges"
            :disabled="!isPinFormValid || pinProcessing"
          >
            {{ pinProcessing ? 'Processing...' : 'Save' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useAuthStore } from '../store/auth';
import api from '../lib/api';

// Auth & API
const authStore = useAuthStore();
const user = authStore.user;
const token = authStore.token;

// Data management
const loading = ref(true);
const error = ref('');
const accounts = ref([]);
const modalError = ref('');
const successMessage = ref('');
const pinProcessing = ref(false);

// PIN management
const showPinModal = ref(false);
const selectedAccountId = ref(null);
const selectedAccountHasPin = ref(false);
const currentPin = ref(['', '', '', '']);
const newPin = ref(['', '', '', '']);
const confirmPin = ref(['', '', '', '']);

// References for PIN input fields
const currentPinFields = ref([]);
const newPinFields = ref([]);
const confirmPinFields = ref([]);

// Check if PIN form is valid
const isPinFormValid = computed(() => {
  // For creating new PIN
  if (!selectedAccountHasPin.value) {
    return newPin.value.every(digit => digit.length === 1) && 
           confirmPin.value.every(digit => digit.length === 1) &&
           newPin.value.join('') === confirmPin.value.join('');
  }
  
  // For changing existing PIN
  return currentPin.value.every(digit => digit.length === 1) &&
         newPin.value.every(digit => digit.length === 1) && 
         confirmPin.value.every(digit => digit.length === 1) &&
         newPin.value.join('') !== currentPin.value.join('') &&
         newPin.value.join('') === confirmPin.value.join('');
});

// Load accounts on mount
onMounted(async () => {
  if (!user?.id) {
    error.value = "Not logged in. Please log in to manage PIN settings.";
    loading.value = false;
    return;
  }

  try {
    // Load accounts
    const res = await api.get(`/accounts/user/${user.id}`, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    });
    
    if (!res.ok) {
      throw new Error(`HTTP ${res.status}: ${res.statusText}`);
    }
    
    const accountsData = await res.json();
    const approvedAccounts = accountsData.filter(acc => acc.approved);
    
    if (approvedAccounts.length === 0) {
      error.value = "No approved accounts found. Please contact customer service.";
      loading.value = false;
      return;
    }
    
    // Check PIN status for each account
    const accountsWithPinStatus = await Promise.all(
      approvedAccounts.map(async (account) => {
        try {
          const pinRes = await api.get(`/atm/pinStatus?accountId=${account.id}`, {
            headers: {
              Authorization: `Bearer ${token}`
            }
          });
          
          if (pinRes.ok) {
            const pinStatus = await pinRes.json();
            return {
              ...account,
              pinCreated: pinStatus.pinCreated
            };
          } else {
            return {
              ...account,
              pinCreated: false
            };
          }
        } catch (err) {
          console.error("Error checking PIN status:", err);
          return {
            ...account,
            pinCreated: false
          };
        }
      })
    );
    
    accounts.value = accountsWithPinStatus;
    loading.value = false;
  } catch (err) {
    error.value = "Failed to load accounts: " + err.message;
    loading.value = false;
  }
});

// Select account for PIN management
function selectAccount(accountId) {
  selectedAccountId.value = accountId;
  const account = accounts.value.find(acc => acc.id === accountId);
  selectedAccountHasPin.value = account ? account.pinCreated : false;
  
  // Reset form values
  modalError.value = '';
  successMessage.value = '';
  currentPin.value = ['', '', '', ''];
  newPin.value = ['', '', '', ''];
  confirmPin.value = ['', '', '', ''];
  
  showPinModal.value = true;
  
  // Focus first input on next tick
  setTimeout(() => {
    if (selectedAccountHasPin.value && currentPinFields.value[0]) {
      currentPinFields.value[0].focus();
    } else if (newPinFields.value[0]) {
      newPinFields.value[0].focus();
    }
  }, 50);
}

// Handle PIN input focus management
function focusNextInput(fieldType, index) {
  if (index < 3) {
    const nextField = getFieldRefByType(fieldType)[index + 1];
    if (nextField) nextField.focus();
  } else if (fieldType === 'current' && newPinFields.value[0]) {
    // Move to the new PIN field after completing current PIN
    newPinFields.value[0].focus();
  } else if (fieldType === 'new' && confirmPinFields.value[0]) {
    // Move to the confirm PIN field after completing new PIN
    confirmPinFields.value[0].focus();
  }
}

// Handle backspace in PIN fields
function handleDelete(fieldType, index) {
  const pinArray = getPinArrayByType(fieldType);
  if (index > 0 && pinArray[index] === '') {
    const prevField = getFieldRefByType(fieldType)[index - 1];
    if (prevField) {
      prevField.focus();
    }
  }
}

// Helper to get the correct field reference by type
function getFieldRefByType(fieldType) {
  switch (fieldType) {
    case 'current': return currentPinFields.value;
    case 'new': return newPinFields.value;
    case 'confirm': return confirmPinFields.value;
    default: return [];
  }
}

// Helper to get the correct PIN array by type
function getPinArrayByType(fieldType) {
  switch (fieldType) {
    case 'current': return currentPin.value;
    case 'new': return newPin.value;
    case 'confirm': return confirmPin.value;
    default: return [];
  }
}

// Close modal and reset values
function cancelPin() {
  showPinModal.value = false;
  selectedAccountId.value = null;
  modalError.value = '';
  successMessage.value = '';
  currentPin.value = ['', '', '', ''];
  newPin.value = ['', '', '', ''];
  confirmPin.value = ['', '', '', ''];
}

// Save PIN changes
async function savePinChanges() {
  modalError.value = '';
  successMessage.value = '';
  pinProcessing.value = true;
  
  try {
    const newPinValue = newPin.value.join('');
    
    if (!/^\d{4}$/.test(newPinValue)) {
      modalError.value = "PIN must be 4 digits";
      pinProcessing.value = false;
      return;
    }
    
    if (newPinValue !== confirmPin.value.join('')) {
      modalError.value = "PINs do not match";
      pinProcessing.value = false;
      return;
    }
    
    if (selectedAccountHasPin.value) {
      // Changing existing PIN
      const currentPinValue = currentPin.value.join('');
      
      if (currentPinValue === newPinValue) {
        modalError.value = "New PIN must be different from current PIN";
        pinProcessing.value = false;
        return;
      }
      
      // Verify current PIN first
      const verifyRes = await api.post('/pin/verify', {
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({
          accountId: selectedAccountId.value,
          pin: currentPinValue
        })
      });
      
      if (!verifyRes.ok) {
        modalError.value = "Failed to verify current PIN";
        pinProcessing.value = false;
        return;
      }
      
      const verifyResult = await verifyRes.json();
      if (!verifyResult.valid) {
        modalError.value = "Current PIN is incorrect";
        pinProcessing.value = false;
        return;
      }
      
      // Change PIN
      const changeRes = await api.post('/pin/change', {
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({
          accountId: selectedAccountId.value,
          pin: currentPinValue,
          newPin: newPinValue
        })
      });
      
      if (!changeRes.ok) {
        throw new Error(`HTTP ${changeRes.status}: ${changeRes.statusText}`);
      }
      
      successMessage.value = "PIN changed successfully";
      
      // Update account PIN status
      const updatedAccounts = accounts.value.map(account => {
        if (account.id === selectedAccountId.value) {
          return { ...account, pinCreated: true };
        }
        return account;
      });
      
      accounts.value = updatedAccounts;
      
    } else {
      // Creating new PIN
      const createRes = await api.post('/pin/create', {
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({
          accountId: selectedAccountId.value,
          pin: newPinValue
        })
      });
      
      if (!createRes.ok) {
        throw new Error(`HTTP ${createRes.status}: ${createRes.statusText}`);
      }
      
      successMessage.value = "PIN created successfully";
      
      // Update account PIN status
      const updatedAccounts = accounts.value.map(account => {
        if (account.id === selectedAccountId.value) {
          return { ...account, pinCreated: true };
        }
        return account;
      });
      
      accounts.value = updatedAccounts;
    }
    
    // Reset form after success
    currentPin.value = ['', '', '', ''];
    newPin.value = ['', '', '', ''];
    confirmPin.value = ['', '', '', ''];
    
    // Auto-close modal after a delay
    setTimeout(() => {
      showPinModal.value = false;
      selectedAccountId.value = null;
      successMessage.value = '';
    }, 2000);
    
  } catch (err) {
    modalError.value = "Failed to save PIN: " + err.message;
  } finally {
    pinProcessing.value = false;
  }
}
</script>

<style scoped>
.pin-settings-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 30px 20px;
}

.pin-settings-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  padding: 30px;
}

.card-title {
  margin-top: 0;
  margin-bottom: 25px;
  color: #2c3e50;
  border-bottom: 2px solid #eee;
  padding-bottom: 10px;
}

.loading-spinner {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}

.loading-spinner::after {
  content: "";
  width: 50px;
  height: 50px;
  border: 6px solid #f3f3f3;
  border-top: 6px solid #3498db;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.accounts-list {
  margin-top: 20px;
}

.accounts-list h3 {
  margin-bottom: 20px;
  color: #555;
  font-weight: 600;
}

.account-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  border: 1px solid #eee;
  border-radius: 8px;
  margin-bottom: 15px;
  transition: all 0.2s;
}

.account-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border-color: #ddd;
}

.account-info {
  flex-grow: 1;
}

.account-name {
  font-weight: 600;
  color: #2c3e50;
  font-size: 16px;
}

.account-id {
  color: #7f8c8d;
  font-size: 13px;
  margin-top: 5px;
}

.pin-status {
  margin: 0 20px;
  font-size: 14px;
}

.pin-set {
  color: #27ae60;
  font-weight: 600;
}

.pin-not-set {
  color: #e74c3c;
  font-weight: 600;
}

.manage-pin-btn {
  padding: 8px 16px;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
  color: white;
}

.create-btn {
  background-color: #3498db;
}

.create-btn:hover {
  background-color: #2980b9;
}

.change-btn {
  background-color: #f39c12;
}

.change-btn:hover {
  background-color: #e67e22;
}

/* PIN Modal Styles */
.pin-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 100;
}

.pin-modal {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 500px;
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.2);
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background-color: #f8f9fa;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
  color: #2c3e50;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #95a5a6;
}

.close-btn:hover {
  color: #7f8c8d;
}

.modal-body {
  padding: 20px;
}

.pin-form {
  margin-bottom: 20px;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #555;
}

.pin-input {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 5px;
}

.pin-input input {
  width: 50px;
  height: 60px;
  text-align: center;
  font-size: 24px;
  border: 2px solid #ddd;
  border-radius: 8px;
  background-color: #f9f9f9;
}

.pin-input input:focus {
  border-color: #3498db;
  outline: none;
  box-shadow: 0 0 5px rgba(52, 152, 219, 0.3);
}

.error-message {
  color: #e74c3c;
  padding: 10px;
  background-color: #fde8e7;
  border-radius: 6px;
  margin: 15px 0;
  font-weight: 600;
}

.success-message {
  color: #27ae60;
  padding: 10px;
  background-color: #e6f9ee;
  border-radius: 6px;
  margin: 15px 0;
  font-weight: 600;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  padding: 15px 20px;
  border-top: 1px solid #eee;
  background-color: #f8f9fa;
}

.modal-footer button {
  padding: 10px 20px;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}

.cancel-btn {
  background-color: #ecf0f1;
  color: #7f8c8d;
  margin-right: 10px;
}

.cancel-btn:hover {
  background-color: #dfe4e6;
}

.save-btn {
  background-color: #2ecc71;
  color: white;
}

.save-btn:hover:not(:disabled) {
  background-color: #27ae60;
}

.save-btn:disabled {
  background-color: #95a5a6;
  cursor: not-allowed;
}

.no-accounts {
  text-align: center;
  padding: 40px 0;
  color: #7f8c8d;
  font-size: 16px;
}
</style>
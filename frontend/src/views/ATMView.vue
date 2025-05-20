<template>
  <div class="atm-container">
    <div class="atm-machine">
      <!-- ATM Top Section - Bank Name and Card Slot -->
      <div class="atm-header">
        <div class="bank-name">BANK APP ATM</div>
        <div class="card-reader">
          <div class="card-slot">
            <div class="card-slot-light" :class="{ active: atmState !== 'idle' }"></div>
          </div>
          <div class="card-text">CARD / ACCOUNT</div>
        </div>
      </div>

      <!-- ATM Screen -->
      <div class="atm-screen">
        <div class="screen-content" :class="atmState">
          <!-- Welcome / Idle State -->
          <div v-if="atmState === 'idle'" class="screen-idle">
            <div class="welcome-message">
              <h2>Welcome to BankApp ATM</h2>
              <p>Please select an account to begin</p>
            </div>
            <div v-if="accounts.length" class="account-selection">
              <select id="account-select" v-model.number="selectedAccountId" @change="selectAccount">
                <option disabled :value="null">Select your account</option>
                <option v-for="account in accounts" :key="account.id" :value="account.id">
                  {{ account.type }} (ID: {{ account.id }})
                </option>
              </select>
              <button class="green-btn" @click="selectAccount" :disabled="!selectedAccountId">
                Proceed →
              </button>
            </div>
            <div v-else class="loading-accounts">
              <div v-if="error" class="error-message">{{ error }}</div>
              <div v-else class="loading-spinner"></div>
            </div>
          </div>

          <!-- Main Menu State -->
          <div v-if="atmState === 'menu'" class="screen-menu">
            <div class="account-info">
              <h3>Account: {{ getCurrentAccountLabel() }}</h3>
              <div class="balance-display">
                <span class="balance-label">Current Balance:</span>
                <span class="balance-amount">€{{ typeof balance === 'number' ? balance.toFixed(2) : '---' }}</span>
              </div>
            </div>
            <div class="menu-options">
              <h4>Please select an operation:</h4>
              <div class="menu-buttons">
                <button class="menu-btn deposit-btn" @click="startDeposit">
                  <span class="icon">↓</span> Deposit
                </button>
                <button class="menu-btn withdraw-btn" @click="startWithdraw">
                  <span class="icon">↑</span> Withdraw
                </button>
                <button class="menu-btn cancel-btn" @click="cancelOperation">
                  <span class="icon">✕</span> Exit
                </button>
              </div>
            </div>
          </div>

          <!-- Amount Entry State -->
          <div v-if="atmState === 'amount'" class="screen-amount">
            <div class="operation-type">
              <h3>{{ operationType === 'deposit' ? 'DEPOSIT' : 'WITHDRAWAL' }}</h3>
              <button class="back-btn" @click="atmState = 'menu'">
                ← Back to Menu
              </button>
            </div>
            <div class="amount-entry">
              <div class="amount-display">
                € <span>{{ displayAmount }}</span>
              </div>
              <div class="entry-instructions">
                Enter amount and press confirm
              </div>
              <div class="error-message" v-if="error">
                {{ error }}
              </div>
            </div>
            <div class="confirm-buttons">
              <button class="cancel-btn" @click="clearAmount">Clear</button>
              <button class="green-btn" :disabled="enteredAmount <= 0" @click="confirmAmount">
                Confirm →
              </button>
            </div>
          </div>

          <!-- Confirmation State -->
          <div v-if="atmState === 'confirm'" class="screen-confirm">
            <h3>Confirm {{ operationType === 'deposit' ? 'Deposit' : 'Withdrawal' }}</h3>
            <div class="confirmation-details">
              <div class="confirm-row">
                <div class="confirm-label">Account:</div>
                <div class="confirm-value">{{ getCurrentAccountLabel() }}</div>
              </div>
              <div class="confirm-row">
                <div class="confirm-label">Amount:</div>
                <div class="confirm-value">€{{ enteredAmount.toFixed(2) }}</div>
              </div>
              <div class="confirm-row">
                <div class="confirm-label">Operation:</div>
                <div class="confirm-value">{{ operationType === 'deposit' ? 'Deposit' : 'Withdrawal' }}</div>
              </div>
            </div>
            <div class="confirm-buttons">
              <button class="cancel-btn" @click="atmState = 'amount'">Cancel</button>
              <button class="green-btn" @click="processTransaction">Confirm</button>
            </div>
          </div>

          <!-- Processing State -->
          <div v-if="atmState === 'processing'" class="screen-processing">
            <div class="processing-animation">
              <div class="spinner"></div>
              <p>Processing your transaction...</p>
              <p class="small">Please wait</p>
            </div>
          </div>

          <!-- Receipt State -->
          <div v-if="atmState === 'receipt'" class="screen-receipt">
            <div class="receipt-header">
              <h3>Transaction Successful</h3>
              <div class="transaction-icon" :class="operationType">
                <span v-if="operationType === 'deposit'">↓</span>
                <span v-else>↑</span>
              </div>
            </div>
            <div class="receipt-details">
              <div class="receipt-row">
                <div class="receipt-label">Account:</div>
                <div class="receipt-value">{{ getCurrentAccountLabel() }}</div>
              </div>
              <div class="receipt-row">
                <div class="receipt-label">Amount:</div>
                <div class="receipt-value">€{{ enteredAmount.toFixed(2) }}</div>
              </div>
              <div class="receipt-row">
                <div class="receipt-label">New Balance:</div>
                <div class="receipt-value">€{{ typeof balance === 'number' ? balance.toFixed(2) : '---' }}</div>
              </div>
              <div class="receipt-row">
                <div class="receipt-label">Date:</div>
                <div class="receipt-value">{{ new Date().toLocaleString() }}</div>
              </div>
              <div class="receipt-row">
                <div class="receipt-label">Status:</div>
                <div class="receipt-value success">COMPLETED</div>
              </div>
            </div>
            <div class="receipt-actions">
              <button class="green-btn" @click="atmState = 'menu'">
                Return to Menu
              </button>
              <button class="blue-btn" @click="downloadReceipt">
                Print Receipt
              </button>
            </div>
          </div>

          <!-- Error State -->
          <div v-if="atmState === 'error'" class="screen-error">
            <div class="error-icon">❌</div>
            <h3>Error</h3>
            <p class="error-message">{{ error }}</p>
            <button class="blue-btn" @click="atmState = 'menu'">
              Return to Menu
            </button>
          </div>
        </div>
      </div>

      <!-- ATM Keypad & Cash Slots -->
      <div class="atm-controls">
        <div class="keypad">
          <div class="keypad-section">
            <button 
              v-for="num in [1, 2, 3, 4, 5, 6, 7, 8, 9, 0]" 
              :key="num" 
              @click="enterDigit(num)"
              :class="{'keypad-btn': true, 'disabled': atmState !== 'amount'}"
            >
              {{ num }}
            </button>
            <button 
              class="keypad-btn keypad-decimal" 
              @click="enterDecimal()"
              :class="{'disabled': atmState !== 'amount' || displayAmount.includes('.')}"
            >
              .
            </button>
            <button 
              class="keypad-btn keypad-delete" 
              @click="deleteDigit()"
              :class="{'disabled': atmState !== 'amount'}"
            >
              ⌫
            </button>
          </div>
        </div>

        <div class="cash-slots">
          <div class="cash-slot deposit-slot" :class="{ active: animateDeposit }">
            <div class="slot-label">INSERT CASH</div>
            <div class="slot-animation">
              <div class="cash-bills" v-if="animateDeposit"></div>
            </div>
          </div>
          
          <div class="cash-slot withdraw-slot" :class="{ active: animateWithdraw }">
            <div class="slot-label">TAKE CASH</div>
            <div class="slot-animation">
              <div class="cash-bills" v-if="animateWithdraw"></div>
            </div>
          </div>
          
          <div class="receipt-slot" :class="{ active: animateReceipt }">
            <div class="slot-label">RECEIPT</div>
            <div class="slot-animation">
              <div class="receipt-paper" v-if="animateReceipt"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '../store/auth'

// Auth & API
const authStore = useAuthStore()
const user = authStore.user
const token = authStore.token

// Account Data
const accounts = ref([])
const selectedAccountId = ref(null)
const balance = ref(null)

// ATM State Management
const atmState = ref('idle') // idle, menu, amount, confirm, processing, receipt, error
const operationType = ref('') // deposit, withdraw
const displayAmount = ref('')
const error = ref('')
const message = ref('')

// Animation Flags
const animateDeposit = ref(false)
const animateWithdraw = ref(false)
const animateReceipt = ref(false)

// Computed Properties
const enteredAmount = computed(() => {
  const amount = parseFloat(displayAmount.value || 0)
  return isNaN(amount) ? 0 : amount
})

// Lifecycle Hooks
onMounted(async () => {
  if (!user?.id) {
    error.value = "Not logged in. Please log in to use the ATM."
    return
  }

  try {
    const res = await fetch(`http://localhost:8080/api/accounts/user/${user.id}`, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })
    
    if (!res.ok) {
      throw new Error(await res.text())
    }
    
    accounts.value = await res.json()
    accounts.value = accounts.value.filter(acc => acc.approved)
    
    if (accounts.value.length === 0) {
      error.value = "No approved accounts found. Please contact customer service."
    }
  } catch (err) {
    error.value = "Failed to load accounts: " + err.message
  }
})

// Helper Functions
function getCurrentAccountLabel() {
  const account = accounts.value.find(acc => acc.id === selectedAccountId.value)
  return account ? `${account.type} (ID: ${account.id})` : 'Unknown Account'
}

// ATM Flow Functions
async function selectAccount() {
  if (!selectedAccountId.value) {
    error.value = "Please select an account"
    return
  }
  
  error.value = ''
  atmState.value = 'processing'
  
  try {
    const res = await fetch(`http://localhost:8080/api/atm/balance?accountId=${selectedAccountId.value}`, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })
    
    if (!res.ok) {
      throw new Error(await res.text())
    }
    
    balance.value = await res.json()
    atmState.value = 'menu'
  } catch (err) {
    error.value = "Failed to load balance: " + err.message
    atmState.value = 'error'
  }
}

function startDeposit() {
  operationType.value = 'deposit'
  displayAmount.value = ''
  error.value = ''
  atmState.value = 'amount'
}

function startWithdraw() {
  operationType.value = 'withdraw'
  displayAmount.value = ''
  error.value = ''
  atmState.value = 'amount'
}

function cancelOperation() {
  atmState.value = 'idle'
  operationType.value = ''
  displayAmount.value = ''
  error.value = ''
}

// Amount Input Functions
function enterDigit(digit) {
  if (atmState.value !== 'amount') return
  
  // Limit to reasonable amount length
  if (displayAmount.value.replace('.', '').length >= 8) return
  
  if (displayAmount.value === '0') {
    displayAmount.value = digit.toString()
  } else {
    displayAmount.value += digit.toString()
  }
}

function enterDecimal() {
  if (atmState.value !== 'amount') return
  if (displayAmount.value.includes('.')) return
  
  if (displayAmount.value === '') {
    displayAmount.value = '0.'
  } else {
    displayAmount.value += '.'
  }
}

function deleteDigit() {
  if (atmState.value !== 'amount') return
  if (displayAmount.value.length > 0) {
    displayAmount.value = displayAmount.value.slice(0, -1)
  }
}

function clearAmount() {
  displayAmount.value = ''
}

function confirmAmount() {
  error.value = ''
  
  if (enteredAmount.value <= 0) {
    error.value = "Amount must be greater than zero"
    return
  }
  
  if (operationType.value === 'withdraw' && enteredAmount.value > balance.value) {
    error.value = "Insufficient funds"
    return
  }
  
  atmState.value = 'confirm'
}

// Transaction Processing
async function processTransaction() {
  error.value = ''
  atmState.value = 'processing'
  
  try {
    const res = await fetch(`http://localhost:8080/api/atm/${operationType.value}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      },
      body: JSON.stringify({
        accountId: selectedAccountId.value,
        amount: enteredAmount.value
      })
    })
    
    const text = await res.text()
    if (!res.ok) throw new Error(text)
    
    // Update balance
    await updateBalance()
    
    // Animate cash movement
    if (operationType.value === 'deposit') {
      animateDeposit.value = true
    } else {
      animateWithdraw.value = true
    }
    
    // Show completion after short delay
    setTimeout(() => {
      animateDeposit.value = false
      animateWithdraw.value = false
      atmState.value = 'receipt'
    }, 2000)
    
  } catch (err) {
    error.value = err.message
    atmState.value = 'error'
  }
}

async function updateBalance() {
  try {
    const res = await fetch(`http://localhost:8080/api/atm/balance?accountId=${selectedAccountId.value}`, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })
    
    if (!res.ok) throw new Error(await res.text())
    balance.value = await res.json()
  } catch (err) {
    console.error("Failed to update balance:", err)
  }
}

function downloadReceipt() {
  animateReceipt.value = true
  
  setTimeout(() => {
    animateReceipt.value = false
    
    // Generate receipt content
    const receiptContent = `
      BANK APP ATM RECEIPT
      -------------------
      Date: ${new Date().toLocaleString()}
      
      Account: ${getCurrentAccountLabel()}
      Transaction: ${operationType.value.toUpperCase()}
      Amount: €${enteredAmount.value.toFixed(2)}
      Current Balance: €${balance.value.toFixed(2)}
      
      Thank you for using BankApp ATM
    `.replace(/\n\s+/g, '\n').trim()
    
    // Create temporary download link
    const element = document.createElement('a')
    element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(receiptContent))
    element.setAttribute('download', `atm-receipt-${Date.now()}.txt`)
    element.style.display = 'none'
    
    document.body.appendChild(element)
    element.click()
    document.body.removeChild(element)
  }, 2000)
}
</script>

<style scoped>
.atm-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f5f7fa;
  padding: 20px;
}

.atm-machine {
  width: 100%;
  max-width: 800px;
  background: linear-gradient(145deg, #2c3e50, #34495e);
  border-radius: 20px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  padding-bottom: 20px;
}

/* ATM Header Section */
.atm-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #1a2530;
  padding: 15px 20px;
}

.bank-name {
  font-size: 24px;
  font-weight: 700;
  color: #ecf0f1;
  letter-spacing: 2px;
}

.card-reader {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.card-slot {
  width: 60px;
  height: 10px;
  background-color: #222;
  border: 1px solid #555;
  border-radius: 3px;
  margin-bottom: 5px;
  position: relative;
}

.card-slot-light {
  position: absolute;
  right: 5px;
  top: 50%;
  transform: translateY(-50%);
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #444;
  transition: all 0.3s;
}

.card-slot-light.active {
  background-color: #2ecc71;
  box-shadow: 0 0 5px #2ecc71;
}

.card-text {
  font-size: 10px;
  color: #bbb;
}

/* ATM Screen */
.atm-screen {
  background-color: #ecf0f1;
  margin: 20px;
  border-radius: 10px;
  padding: 3px;
  border: 1px solid #95a5a6;
  box-shadow: inset 0 0 10px rgba(0, 0, 0, 0.1);
  height: 350px;
}

.screen-content {
  background-color: #fff;
  height: 100%;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  padding: 20px;
  position: relative;
}

/* ATM Controls (Keypad & Cash Slots) */
.atm-controls {
  display: flex;
  justify-content: space-between;
  padding: 0 20px;
}

.keypad {
  display: flex;
  flex-direction: column;
  width: 250px;
}

.keypad-section {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.keypad-btn {
  background-color: #34495e;
  color: white;
  border: none;
  border-radius: 5px;
  height: 50px;
  font-size: 20px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 3px 0 #2c3e50;
}

.keypad-btn:hover:not(.disabled) {
  background-color: #2c3e50;
}

.keypad-btn:active:not(.disabled) {
  transform: translateY(3px);
  box-shadow: 0 0 0 #2c3e50;
}

.keypad-btn.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.keypad-decimal, .keypad-delete {
  background-color: #3498db;
  box-shadow: 0 3px 0 #2980b9;
}

.keypad-decimal:hover:not(.disabled), .keypad-delete:hover:not(.disabled) {
  background-color: #2980b9;
}

.cash-slots {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 250px;
}

.cash-slot, .receipt-slot {
  background-color: #222;
  padding: 5px;
  border-radius: 5px;
  text-align: center;
  height: 40px;
  position: relative;
  transition: all 0.3s;
}

.cash-slot.active, .receipt-slot.active {
  box-shadow: 0 0 10px rgba(46, 204, 113, 0.8);
}

.slot-label {
  color: #aaa;
  font-size: 10px;
  position: absolute;
  top: 5px;
  left: 0;
  right: 0;
}

.slot-animation {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 20px;
}

.cash-bills {
  position: absolute;
  height: 4px;
  background-color: #2ecc71;
  bottom: 3px;
  left: 10%;
  right: 10%;
  animation: cashAnimation 2s ease;
}

.receipt-paper {
  position: absolute;
  height: 20px;
  width: 60px;
  background-color: white;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  animation: receiptAnimation 2s ease;
}

@keyframes cashAnimation {
  0% { 
    transform: translateY(-20px);
    opacity: 0;
  }
  50% {
    transform: translateY(0);
    opacity: 1;
  }
  100% {
    transform: translateY(0);
    opacity: 1;
  }
}

@keyframes receiptAnimation {
  0% { 
    height: 0;
  }
  70% {
    height: 20px;
  }
  100% {
    height: 20px;
  }
}

/* Button Styles */
.green-btn, .blue-btn, .cancel-btn, .menu-btn {
  padding: 12px 20px;
  border: none;
  border-radius: 5px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.2s;
  margin: 5px;
}

.green-btn {
  background-color: #2ecc71;
  color: white;
}

.green-btn:hover:not(:disabled) {
  background-color: #27ae60;
}

.blue-btn {
  background-color: #3498db;
  color: white;
}

.blue-btn:hover {
  background-color: #2980b9;
}

.cancel-btn {
  background-color: #e74c3c;
  color: white;
}

.cancel-btn:hover {
  background-color: #c0392b;
}

.menu-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 110px;
  height: 80px;
  text-align: center;
  background-color: #3498db;
  color: white;
}

.deposit-btn {
  background-color: #2ecc71;
}

.deposit-btn:hover {
  background-color: #27ae60;
}

.withdraw-btn {
  background-color: #e67e22;
}

.withdraw-btn:hover {
  background-color: #d35400;
}

.menu-btn .icon {
  font-size: 24px;
  margin-bottom: 5px;
}

.back-btn {
  background: none;
  border: none;
  color: #3498db;
  cursor: pointer;
  padding: 5px;
  font-weight: bold;
}

/* Screen State Styles */
.screen-idle, .screen-menu, .screen-amount, .screen-confirm, 
.screen-processing, .screen-receipt, .screen-error {
  display: flex;
  flex-direction: column;
  height: 100%;
  justify-content: space-between;
}

.welcome-message {
  text-align: center;
  margin-top: 30px;
}

.welcome-message h2 {
  color: #2c3e50;
  margin-bottom: 10px;
}

.account-selection {
  margin: 20px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
}

.account-selection select {
  width: 100%;
  max-width: 300px;
  padding: 12px;
  border-radius: 5px;
  border: 1px solid #bdc3c7;
  background-color: white;
  font-size: 16px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  margin: 20px auto;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #3498db;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-message {
  color: #e74c3c;
  text-align: center;
  margin: 20px 0;
  font-weight: bold;
}

/* Menu Screen */
.account-info {
  text-align: center;
  padding: 15px;
  border-bottom: 1px solid #eee;
}

.balance-display {
  margin: 15px 0;
  font-size: 18px;
}

.balance-label {
  color: #7f8c8d;
  margin-right: 10px;
}

.balance-amount {
  font-weight: bold;
  color: #2c3e50;
  font-size: 22px;
}

.menu-options {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 20px 0;
}

.menu-options h4 {
  margin-bottom: 20px;
  color: #7f8c8d;
}

.menu-buttons {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 20px;
}

/* Amount Entry Screen */
.operation-type {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  border-bottom: 1px solid #eee;
}

.amount-entry {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-grow: 1;
}

.amount-display {
  font-size: 42px;
  font-weight: bold;
  color: #2c3e50;
  margin-bottom: 20px;
  min-height: 50px;
}

.entry-instructions {
  color: #7f8c8d;
  margin-bottom: 15px;
}

.confirm-buttons {
  display: flex;
  justify-content: space-between;
  padding: 0 20px 20px;
}

/* Confirm Screen */
.confirmation-details, .receipt-details {
  margin: 20px 0;
  background-color: #f8f9fa;
  border-radius: 5px;
  padding: 15px;
}

.confirm-row, .receipt-row {
  display: flex;
  margin-bottom: 10px;
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
}

.confirm-label, .receipt-label {
  width: 40%;
  color: #7f8c8d;
  font-weight: bold;
}

.confirm-value, .receipt-value {
  width: 60%;
  color: #2c3e50;
}

/* Processing Screen */
.processing-animation {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.processing-animation .spinner {
  width: 50px;
  height: 50px;
  border: 5px solid #f3f3f3;
  border-top: 5px solid #3498db;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 20px;
}

.processing-animation p {
  color: #2c3e50;
  margin: 5px 0;
}

.processing-animation .small {
  font-size: 14px;
  color: #95a5a6;
}

/* Receipt Screen */
.receipt-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.transaction-icon {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 24px;
  color: white;
}

.transaction-icon.deposit {
  background-color: #2ecc71;
}

.transaction-icon.withdraw {
  background-color: #e67e22;
}

.receipt-value.success {
  color: #2ecc71;
  font-weight: bold;
}

.receipt-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 20px;
}

/* Error Screen */
.screen-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.error-icon {
  font-size: 50px;
  color: #e74c3c;
  margin-bottom: 20px;
}

.screen-error h3 {
  color: #e74c3c;
  margin-bottom: 15px;
}

.screen-error .error-message {
  text-align: center;
  margin-bottom: 30px;
  max-width: 80%;
}

/* Responsive Adjustments */
@media (max-width: 768px) {
  .atm-machine {
    border-radius: 0;
  }
  
  .atm-controls {
    flex-direction: column;
    align-items: center;
  }
  
  .keypad, .cash-slots {
    width: 100%;
    max-width: 350px;
    margin-bottom: 20px;
  }
  
  .menu-buttons {
    flex-direction: column;
  }
}
</style>
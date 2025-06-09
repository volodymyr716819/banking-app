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
      <div class="screen-container">
        <div class="atm-screen">
          <div class="screen-content" :class="atmState">
          <!-- Welcome / Idle State -->
          <div v-if="atmState === 'idle'" class="screen-idle">
            <div class="welcome-message">
              <h2>Welcome to BankApp ATM</h2>
              <p>Please select an account to begin</p>
              <div class="atm-instructions">
                <p class="instruction-title">ATM Instructions:</p>
                <ul class="instruction-list">
                  <li>Use the numeric keypad to enter amounts and PIN</li>
                  <li>Press the buttons on screen to select options</li>
                  <li>Follow on-screen instructions for each operation</li>
                  <li>Use function keys for quick navigation</li>
                </ul>
              </div>
            </div>
            <div v-if="accounts.length" class="account-selection">
              <select id="account-select" v-model.number="selectedAccountId" @change="selectAccount">
                <option disabled :value="null">Select your account</option>
                <option v-for="account in accounts" :key="account.id" :value="account.id">
                  {{ account.type }} (ID: {{ account.id }})
                </option>
              </select>
              <button class="green-btn" @click="selectAccount" :disabled="!selectedAccountId">
                Proceed
              </button>
            </div>
            <div v-else class="loading-accounts">
              <div v-if="error" class="error-message">{{ error }}</div>
              <div v-else class="loading-spinner"></div>
            </div>
          </div>
          
          <!-- PIN Creation State -->
          <div v-if="atmState === 'pin-create'" class="screen-pin">
            <div class="pin-header">
              <h3>Create Your PIN</h3>
              <p>Please create a 4-digit PIN for your account</p>
            </div>
            <div class="pin-entry">
              <div class="pin-display">
                <div v-for="(digit, index) in 4" :key="index" class="pin-digit">
                  {{ (pinValue.length > index) ? '•' : '' }}
                </div>
              </div>
              <div class="pin-instructions">
                Enter a 4-digit PIN and press confirm
              </div>
              <div v-if="error" class="error-message">
                {{ error }}
              </div>
            </div>
            <div class="confirm-row">
              <button class="cancel-btn" @click="cancelOperation">Cancel</button>
              <button class="green-btn" :disabled="pinValue.length !== 4" @click="createPin">
                Create PIN
              </button>
            </div>
          </div>
          
          <!-- PIN Verification State -->
          <div v-if="atmState === 'pin-verify'" class="screen-pin">
            <div class="pin-header">
              <h3>Enter Your PIN</h3>
              <p>Please enter your 4-digit PIN</p>
            </div>
            <div class="pin-entry">
              <div class="pin-display">
                <div v-for="(digit, index) in 4" :key="index" class="pin-digit">
                  {{ (pinValue.length > index) ? '•' : '' }}
                </div>
              </div>
              <div class="pin-instructions">
                Enter your PIN and press confirm
              </div>
              <div v-if="error" class="error-message">
                {{ error }}
              </div>
            </div>
            <div class="confirm-row">
              <button class="cancel-btn" @click="cancelOperation">Cancel</button>
              <button class="green-btn" :disabled="pinValue.length !== 4" @click="verifyPin">
                Confirm
              </button>
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
            <!-- Spacer to push content up -->
            <div class="menu-spacer"></div>
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
            <div class="amount-presets">
              <div class="preset-row">
                <div class="preset-label">Quick Amounts:</div>
                <div class="preset-amounts">
                  <button class="preset" @click="displayAmount = '10'">€10</button>
                  <button class="preset" @click="displayAmount = '20'">€20</button>
                  <button class="preset" @click="displayAmount = '50'">€50</button>
                  <button class="preset" @click="displayAmount = '100'">€100</button>
                </div>
              </div>
            </div>
            <div class="confirm-buttons">
              <div class="confirm-row">
                <button class="cancel-btn" @click="clearAmount">Clear</button>
                <button class="green-btn" :disabled="enteredAmount <= 0" @click="confirmAmount">
                  Confirm
                </button>
              </div>
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
              <div class="confirm-row">
                <button class="cancel-btn" @click="atmState = 'amount'">Cancel</button>
                <button class="green-btn" @click="processTransaction">Confirm</button>
              </div>
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

          <!-- Transaction success message will be shown in place of receipt prompt -->
          <div v-if="atmState === 'success'" class="screen-success">
            <div class="success-icon">✓</div>
            <h3>Transaction Successful</h3>
            <div class="transaction-details">
              <p>Your {{ operationType === 'deposit' ? 'deposit' : 'withdrawal' }} of 
                <span class="amount">€{{ enteredAmount.toFixed(2) }}</span> has been completed.</p>
              <p>New Balance: <span class="balance">€{{ typeof balance === 'number' ? balance.toFixed(2) : '---' }}</span></p>
            </div>
            <button class="green-btn" @click="returnToMenu">
              Return to Menu
            </button>
          </div>

          <!-- Error State -->
          <div v-if="atmState === 'error'" class="screen-error">
            <div class="error-icon">❌</div>
            <h3>Error</h3>
            <p class="error-message">{{ error }}</p>
            <div class="action-row">
              <button class="blue-btn" @click="returnToMenu">
                Return to Menu
              </button>
            </div>
          </div>
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
              :class="{'keypad-btn': true, 'disabled': !['amount', 'pin-create', 'pin-verify'].includes(atmState)}"
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
              :class="{'disabled': !['amount', 'pin-create', 'pin-verify'].includes(atmState)}"
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
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * ATM Component
 * 
 * A full-featured ATM interface that allows users to:
 * - Select accounts
 * - Create/verify PINs
 * - Deposit and withdraw funds
 * - See success/error messages
 * 
 * The component implements a state machine pattern for managing the ATM flow
 * between different screens and operations.
 */
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '../store/auth'

// Auth & API integration
const authStore = useAuthStore()
const user = authStore.user
const token = authStore.token

// Account Data - Managed reactively
const accounts = ref([])
const selectedAccountId = ref(null)
const balance = ref(null)

// ATM State Management - Implements state machine pattern
const atmState = ref('idle') // States: idle, pin-create, pin-verify, menu, amount, confirm, processing, success, error
/**
 * Operation state
 * @type {import('vue').Ref<string>} - 'deposit' or 'withdraw'
 */
const operationType = ref('') 

/**
 * Amount input display value
 * @type {import('vue').Ref<string>}
 */
const displayAmount = ref('')

/**
 * Error message for user feedback
 * @type {import('vue').Ref<string>}
 */
const error = ref('')

/**
 * Success/info message for user feedback
 * @type {import('vue').Ref<string>}
 */
const message = ref('')

/**
 * PIN input value during creation/verification
 * @type {import('vue').Ref<string>}
 */
const pinValue = ref('')

/**
 * Stores verified PIN for transaction operations
 * @type {import('vue').Ref<string>}
 */
const verifiedPin = ref('')

/**
 * Flag indicating if PIN has been created for the account
 * @type {import('vue').Ref<boolean>}
 */
const pinCreated = ref(false)

/**
 * Stores account balance before transaction for comparison
 * @type {import('vue').Ref<number|null>}
 */
const previousBalance = ref(null)

/**
 * Animation state flags for cash movements
 * @type {import('vue').Ref<boolean>}
 */
const animateDeposit = ref(false)
const animateWithdraw = ref(false)

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
    
    // Get all accounts including unapproved ones
    const allAccounts = await res.json()
    
    // Check if the user has any accounts at all
    if (allAccounts.length === 0) {
      error.value = "No accounts found. Please open an account first."
      return
    }
    
    // Check if the user has any approved accounts
    accounts.value = allAccounts.filter(acc => acc.approved)
    
    if (accounts.value.length === 0) {
      const pendingAccounts = allAccounts.filter(acc => !acc.approved)
      if (pendingAccounts.length > 0) {
        error.value = "Your accounts are pending approval. ATM operations are only available for approved accounts. Please contact customer service for assistance."
      } else {
        error.value = "No approved accounts found. Please contact customer service."
      }
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


/**
 * ATM Flow Functions
 * These functions control the state transitions and main functionality of the ATM
 */

/**
 * Initiates account selection process
 * Verifies PIN status and retrieves current balance
 * Transitions to appropriate PIN state based on account status
 * @returns {Promise<void>}
 */
async function selectAccount() {
  if (!selectedAccountId.value) {
    error.value = "Please select an account"
    return
  }
  
  error.value = ''
  atmState.value = 'processing'
  
  try {
    // First check PIN status
    const pinStatusRes = await fetch(`http://localhost:8080/api/atm/pinStatus?accountId=${selectedAccountId.value}`, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })
    
    if (!pinStatusRes.ok) {
      throw new Error(await pinStatusRes.text())
    }
    
    const pinStatus = await pinStatusRes.json()
    pinCreated.value = pinStatus.pinCreated
    
    // Get balance
    const balanceRes = await fetch(`http://localhost:8080/api/atm/balance?accountId=${selectedAccountId.value}`, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })
    
    if (!balanceRes.ok) {
      throw new Error(await balanceRes.text())
    }
    
    balance.value = await balanceRes.json()
    
    // Route to appropriate state based on PIN status
    if (!pinCreated.value) {
      // PIN needs to be created first
      pinValue.value = ''
      atmState.value = 'pin-create'
    } else {
      // PIN verification needed
      pinValue.value = ''
      atmState.value = 'pin-verify'
    }
  } catch (err) {
    error.value = "Failed to load account: " + err.message
    atmState.value = 'error'
  }
}

/**
 * PIN Management Functions
 * Handle PIN creation and verification for account security
 */

/**
 * Creates a new PIN for the selected account
 * Validates PIN format and sends to backend for storage
 * @returns {Promise<void>}
 */
async function createPin() {
  if (pinValue.value.length !== 4) {
    error.value = "PIN must be 4 digits"
    return
  }

  error.value = ''
  atmState.value = 'processing'
  
  try {
    const res = await fetch('http://localhost:8080/api/pin/create', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      },
      body: JSON.stringify({
        accountId: selectedAccountId.value,
        pin: pinValue.value
      })
    })
    
    if (!res.ok) {
      throw new Error(await res.text())
    }
    
    // Save the PIN value to use for transactions
    verifiedPin.value = pinValue.value
    pinCreated.value = true
    
    // Skip PIN verification since we just created it
    message.value = "PIN created successfully"
    atmState.value = 'menu'
  } catch (err) {
    error.value = "Failed to create PIN: " + err.message
    atmState.value = 'error'
  }
}

async function verifyPin() {
  // Make sure PIN is 4 digits
  if (pinValue.value.length !== 4) {
    error.value = "PIN must be 4 digits"
    return
  }

  error.value = ''
  atmState.value = 'processing'
  
  try {
    // Attempt to verify PIN with server
    const res = await fetch('http://localhost:8080/api/pin/verify', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      },
      body: JSON.stringify({
        accountId: selectedAccountId.value,
        pin: pinValue.value
      })
    })
    
    if (!res.ok) {
      throw new Error(await res.text())
    }
    
    const result = await res.json()
    
    if (result.valid) {
      // PIN is correct - proceed to menu
      verifiedPin.value = pinValue.value
      atmState.value = 'menu'
    } else {
      // PIN is incorrect - show error message
      error.value = "Invalid PIN. Please try again."
      pinValue.value = '' // Clear PIN
      atmState.value = 'pin-verify'
      
      // Return to welcome screen after 3 seconds
      setTimeout(() => {
        if (atmState.value === 'pin-verify') {
          cancelOperation() // This resets to idle state
        }
      }, 3000)
    }
  } catch (err) {
    // Handle errors from server
    error.value = "PIN verification failed: " + err.message
    atmState.value = 'pin-verify'
    
    // Return to welcome screen after 3 seconds
    setTimeout(() => {
      if (atmState.value === 'pin-verify') {
        cancelOperation() // This resets to idle state
      }
    }, 3000)
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
  selectedAccountId.value = null // Reset the account selection
  operationType.value = ''
  displayAmount.value = ''
  pinValue.value = ''
  verifiedPin.value = '' // Clear the verified PIN
  error.value = ''
}


// Amount Input Functions
function enterDigit(digit) {
  // Handle PIN entry
  if (atmState.value === 'pin-create' || atmState.value === 'pin-verify') {
    // Limit PIN to 4 digits
    if (pinValue.value.length >= 4) return
    
    pinValue.value += digit.toString()
    return
  }
  
  // Handle amount entry
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
  // Only relevant for amount entry
  if (atmState.value !== 'amount') return
  if (displayAmount.value.includes('.')) return
  
  if (displayAmount.value === '') {
    displayAmount.value = '0.'
  } else {
    displayAmount.value += '.'
  }
}

function deleteDigit() {
  // Handle PIN entry
  if (atmState.value === 'pin-create' || atmState.value === 'pin-verify') {
    if (pinValue.value.length > 0) {
      pinValue.value = pinValue.value.slice(0, -1)
    }
    return
  }
  
  // Handle amount entry
  if (atmState.value !== 'amount') return
  if (displayAmount.value.length > 0) {
    displayAmount.value = displayAmount.value.slice(0, -1)
  }
}

function clearAmount() {
  if (atmState.value === 'pin-create' || atmState.value === 'pin-verify') {
    pinValue.value = ''
  } else {
    displayAmount.value = ''
  }
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

/**
 * Transaction Processing
 * Core function that handles deposit/withdrawal transaction execution
 * Manages the transaction lifecycle: validation, processing, animation, and feedback
 * @returns {Promise<void>}
 */
async function processTransaction() {
  error.value = ''
  atmState.value = 'processing'
  let transactionSuccessful = false
  
  try {
    // Check if the verified PIN is available
    if (!verifiedPin.value) {
      throw new Error("PIN verification required. Please restart the transaction.");
    }
    
    // Store current balance as previous balance before transaction
    previousBalance.value = balance.value;
    
    const res = await fetch(`http://localhost:8080/api/atm/${operationType.value}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      },
      body: JSON.stringify({
        accountId: selectedAccountId.value,
        amount: enteredAmount.value,
        pin: verifiedPin.value
      })
    })
    
    const text = await res.text()
    if (!res.ok) {
      // Create more specific error messages based on the response
      if (text.includes("not approved")) {
        throw new Error("Your account is not approved for ATM operations. Please contact customer service.");
      } else if (text.includes("closed")) {
        throw new Error("This account is closed and cannot perform ATM operations.");
      } else if (text.includes("Insufficient balance")) {
        throw new Error("Insufficient balance for this withdrawal. Please enter a smaller amount.");
      } else {
        throw new Error(text);
      }
    }
    
    // Transaction was successful
    transactionSuccessful = true
    
    // Try to update balance - but don't block the transaction flow if this fails
    try {
      await updateBalance()
    } catch (balanceErr) {
      // Continue with transaction even if balance update fails
    }
    
    // Animate cash movement
    if (operationType.value === 'deposit') {
      animateDeposit.value = true
    } else {
      animateWithdraw.value = true
    }
    
  } catch (err) {
    error.value = err.message
    atmState.value = 'error'
  }
  
  // Show success message after successful transaction
  if (transactionSuccessful) {
    // Show cash animation first
    if (operationType.value === 'deposit') {
      animateDeposit.value = true
    } else {
      animateWithdraw.value = true
    }
    
    // Transition to success state after animation
    setTimeout(() => {
      // Stop cash animations
      animateDeposit.value = false
      animateWithdraw.value = false
      
      // Show success message
      atmState.value = 'success'
      // Reset operation type immediately to ensure menu buttons are visible
      operationType.value = ''
      
      // Automatically return to menu after 5 seconds
      setTimeout(() => {
        // If user is still on the success screen, reset to idle state
        if (atmState.value === 'success') {
          atmState.value = 'idle'
          selectedAccountId.value = null  // Reset the account selection
          operationType.value = ''
          displayAmount.value = ''
          pinValue.value = ''
          verifiedPin.value = ''
          error.value = ''
        }
      }, 5000)
    }, 2000)
  }
}

/**
 * Updates account balance from backend
 * Retrieves the current balance for the selected account
 * @returns {Promise<void>}
 * @throws {Error} If balance update fails
 */
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
    // Error handled by caller
    throw err;
  }
}

/**
 * Returns to the main menu and resets operation state
 * Called when user manually clicks the "Return to Menu" button
 */
function returnToMenu() {
  // Reset all operation-related states
  operationType.value = ''
  displayAmount.value = ''
  error.value = ''
  
  // After transaction success, return to idle state and reset account selection
  if (atmState.value === 'success') {
    atmState.value = 'idle'
    selectedAccountId.value = null  // Reset the account selection
    pinValue.value = ''
    verifiedPin.value = ''
  } else {
    // For other scenarios, go back to menu
    atmState.value = 'menu'
  }
}
</script>

<style scoped>
/* PIN Screen Styles */
.pin-header {
  text-align: center;
  margin-bottom: 30px;
}

.pin-header h3 {
  margin-bottom: 10px;
  color: white;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.pin-entry {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 20px 0;
}

.pin-display {
  display: flex;
  justify-content: center;
  gap: 15px;
  margin-bottom: 20px;
}

.pin-digit {
  width: 30px;
  height: 40px;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 4px;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 24px;
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.pin-instructions {
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  margin-bottom: 15px;
}

.atm-container {
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f0f0f0;
  padding: 20px;
  box-sizing: border-box;
  min-height: 85vh; /* Adjusted height to ensure visibility within dashboard */
  width: 100%;
}

.atm-machine {
  width: 100%;
  max-width: 650px;
  background-color: #e0e0e0;
  border-radius: 16px;
  box-shadow: 
    0 10px 25px rgba(0, 0, 0, 0.2),
    0 0 0 10px #d0d0d0;
  overflow: visible; /* Changed from hidden to ensure buttons are fully visible */
  display: flex;
  flex-direction: column;
  position: relative;
  border: 1px solid #c0c0c0;
  margin: 0 auto 20px; /* Added bottom margin for spacing */
}

/* ATM Header Section */
.atm-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #2c3e50;
  padding: 15px 20px;
  border-bottom: 1px solid #34495e;
  color: white;
}

.bank-name {
  font-size: 24px;
  font-weight: 700;
  color: white;
  letter-spacing: 2px;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.3);
}

.card-reader {
  display: flex;
  flex-direction: column;
  align-items: center;
  background-color: #34495e;
  padding: 8px 12px;
  border-radius: 6px;
  border: 1px solid #243342;
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.3);
}

.card-slot {
  width: 70px;
  height: 5px;
  background-color: #111;
  border: 1px solid #000;
  border-radius: 2px;
  margin-bottom: 5px;
  position: relative;
  box-shadow: inset 0 1px 4px rgba(0, 0, 0, 0.5);
}

.card-slot-light {
  position: absolute;
  right: 5px;
  top: 50%;
  transform: translateY(-50%);
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #ccc;
  transition: all 0.3s;
}

.card-slot-light.active {
  background-color: #4caf50;
}

.card-text {
  font-size: 10px;
  color: #eee;
  margin-top: 4px;
  font-weight: 500;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

/* ATM Screen */
.screen-container {
  margin: 20px 20px 0;
  position: relative;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
  border-radius: 12px;
  overflow: hidden;
}

/* ATM Screen */
.atm-screen {
  background-color: #222;
  flex-grow: 1;
  padding: 12px;
  height: 380px; /* Reduced height to prevent overflow */
  position: relative;
  overflow: hidden;
  box-shadow: inset 0 2px 10px rgba(0, 0, 0, 0.5);
  border: 8px solid #333;
}

.screen-content {
  background-color: #004080;
  height: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  padding: 20px;
  position: relative;
  color: white;
  box-shadow: inset 0 0 20px rgba(0, 0, 0, 0.3);
}

/* ATM Controls (Keypad & Cash Slots) */
.atm-controls {
  display: flex;
  justify-content: space-between;
  padding: 20px;
  background-color: #555;
  margin: 10px 20px 20px;
  border-radius: 8px;
  border: 1px solid #444;
  box-shadow: inset 0 1px 5px rgba(0, 0, 0, 0.3);
}

/* Keypad Styling */
.keypad {
  display: flex;
  flex-direction: column;
  width: 250px;
  padding: 10px;
  background-color: #444;
  border-radius: 8px;
  border: 1px solid #333;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.2);
}

.keypad-section {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  padding: 10px;
}

.keypad-btn {
  background: linear-gradient(to bottom, #555, #444);
  color: white;
  border: 1px solid #333;
  border-radius: 6px;
  height: 45px;
  font-size: 18px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  text-shadow: 0 1px 1px rgba(0, 0, 0, 0.5);
}

.keypad-btn:hover:not(.disabled) {
  background: linear-gradient(to bottom, #666, #555);
}

.keypad-btn:active:not(.disabled) {
  background: linear-gradient(to bottom, #444, #555);
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.3);
}

.keypad-btn.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.keypad-decimal, .keypad-delete {
  background: linear-gradient(to bottom, #205080, #104070);
  border-color: #003366;
  color: white;
}

.keypad-decimal:hover:not(.disabled), .keypad-delete:hover:not(.disabled) {
  background: linear-gradient(to bottom, #306090, #205080);
}

/* Cash Slots Styling */
.cash-slots {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 250px;
  padding: 10px;
  background-color: #444;
  border-radius: 8px;
  border: 1px solid #333;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.2);
}

.cash-slot, .receipt-slot {
  background-color: #222;
  padding: 8px;
  border-radius: 4px;
  text-align: center;
  height: 40px;
  position: relative;
  transition: all 0.3s;
  border: 1px solid #111;
  overflow: hidden;
  box-shadow: inset 0 2px 6px rgba(0, 0, 0, 0.5);
}

.cash-slot.active, .receipt-slot.active {
  border-color: #4caf50;
  box-shadow: 0 0 5px rgba(76, 175, 80, 0.3);
}


.slot-label {
  color: #aaa;
  font-size: 10px;
  position: absolute;
  top: 5px;
  left: 0;
  right: 0;
  text-transform: uppercase;
  letter-spacing: 1px;
  text-shadow: 0 1px 1px rgba(0, 0, 0, 0.7);
}

.slot-animation {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 25px;
}

.cash-bills {
  position: absolute;
  height: 6px;
  background-color: #4caf50;
  bottom: 3px;
  left: 10%;
  right: 10%;
  animation: cashAnimation 1.5s ease;
  border-radius: 1px;
}

.receipt-paper {
  position: absolute;
  height: 35px;
  width: 70px;
  background-color: white;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  animation: receiptAnimation 2.5s ease;
  border: 1px solid #eee;
  border-bottom: none;
  box-shadow: 0 -2px 4px rgba(0, 0, 0, 0.1);
  background-image: 
    linear-gradient(90deg, transparent 0%, transparent 90%, rgba(200, 200, 200, 0.2) 91%, transparent 92%),
    linear-gradient(rgba(0, 0, 0, 0.05) 1px, transparent 1px);
  background-size: 100% 100%, 100% 5px;
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
    opacity: 0.8;
  }
  15% {
    height: 10px;
    opacity: 1;
  }
  30% {
    height: 15px;
  }
  45% {
    height: 20px;
  }
  60% {
    height: 25px;
  }
  75% {
    height: 30px;
  }
  90% {
    height: 35px;
  }
  95% {
    transform: translateX(-50%) translateY(0) rotate(0);
  }
  100% {
    height: 35px;
    transform: translateX(-50%) translateY(-2px) rotate(1deg);
  }
}

/* Button Styles */
.green-btn, .blue-btn, .cancel-btn, .menu-btn {
  padding: 10px 16px;
  border-radius: 4px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  margin: 5px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
  text-shadow: 0 1px 1px rgba(0, 0, 0, 0.3);
  display: inline-block; /* Ensure buttons are properly sized */
}

.green-btn {
  background: linear-gradient(to bottom, #4caf50, #388e3c);
  color: white;
  border: 1px solid #2e7d32;
}

.green-btn:hover:not(:disabled) {
  background: linear-gradient(to bottom, #5cb860, #4caf50);
}

.green-btn:active:not(:disabled) {
  background: linear-gradient(to bottom, #388e3c, #4caf50);
  box-shadow: inset 0 1px 5px rgba(0, 0, 0, 0.2);
}

.blue-btn {
  background: linear-gradient(to bottom, #2196f3, #1976d2);
  color: white;
  border: 1px solid #0d47a1;
}

.blue-btn:hover {
  background: linear-gradient(to bottom, #42a5f5, #2196f3);
}

.blue-btn:active {
  background: linear-gradient(to bottom, #1976d2, #2196f3);
  box-shadow: inset 0 1px 5px rgba(0, 0, 0, 0.2);
}

.orange-btn {
  background: linear-gradient(to bottom, #ff9800, #f57c00);
  color: white;
  border: 1px solid #e65100;
}

.orange-btn:hover {
  background: linear-gradient(to bottom, #ffa726, #ff9800);
}

.orange-btn:active {
  background: linear-gradient(to bottom, #f57c00, #ff9800);
  box-shadow: inset 0 1px 5px rgba(0, 0, 0, 0.2);
}

.purple-btn {
  background: linear-gradient(to bottom, #9c27b0, #7b1fa2);
  color: white;
  border: 1px solid #6a1b9a;
}

.purple-btn:hover {
  background: linear-gradient(to bottom, #ab47bc, #9c27b0);
}

.purple-btn:active {
  background: linear-gradient(to bottom, #7b1fa2, #9c27b0);
  box-shadow: inset 0 1px 5px rgba(0, 0, 0, 0.2);
}

.cancel-btn {
  background: linear-gradient(to bottom, #f44336, #d32f2f);
  color: white;
  border: 1px solid #b71c1c;
}

.cancel-btn:hover {
  background: linear-gradient(to bottom, #ef5350, #f44336);
}

.cancel-btn:active {
  background: linear-gradient(to bottom, #d32f2f, #f44336);
  box-shadow: inset 0 1px 5px rgba(0, 0, 0, 0.2);
}

.menu-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 80px;
  height: 70px;
  text-align: center;
  background: linear-gradient(to bottom, #2196f3, #1976d2);
  color: white;
  border: 1px solid #0d47a1;
  border-radius: 8px;
  padding: 8px;
}

.menu-btn:hover {
  background: linear-gradient(to bottom, #42a5f5, #2196f3);
}

.menu-btn:active {
  background: linear-gradient(to bottom, #1976d2, #2196f3);
  box-shadow: inset 0 1px 5px rgba(0, 0, 0, 0.2);
}

.deposit-btn {
  background: linear-gradient(to bottom, #4caf50, #388e3c);
  border: 1px solid #2e7d32;
}

.deposit-btn:hover {
  background: linear-gradient(to bottom, #5cb860, #4caf50);
}

.deposit-btn:active {
  background: linear-gradient(to bottom, #388e3c, #4caf50);
}

.withdraw-btn {
  background: linear-gradient(to bottom, #ff9800, #f57c00);
  border: 1px solid #ef6c00;
}

.withdraw-btn:hover {
  background: linear-gradient(to bottom, #ffa726, #ff9800);
}

.withdraw-btn:active {
  background: linear-gradient(to bottom, #f57c00, #ff9800);
}

.menu-btn .icon {
  font-size: 20px;
  margin-bottom: 5px;
}

.back-btn {
  background: none;
  border: none;
  color: #2196f3;
  cursor: pointer;
  padding: 5px;
  font-weight: 600;
}

/* Screen State Styles */
.screen-idle, .screen-amount, .screen-confirm, 
.screen-processing, .screen-receipt, .screen-error, .screen-pin {
  display: flex;
  flex-direction: column;
  height: 100%;
  justify-content: space-between;
}

.screen-menu {
  display: flex;
  flex-direction: column;
  height: 100%;
  justify-content: flex-start;
}

.welcome-message {
  text-align: center;
  margin-top: 20px;
}

.welcome-message h2 {
  color: white;
  margin-bottom: 10px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

.atm-instructions {
  margin-top: 15px;
  padding: 10px;
  background-color: rgba(33, 150, 243, 0.2);
  border-radius: 4px;
  border-left: 3px solid #2196f3;
}

.instruction-title {
  font-weight: 600;
  color: #2196f3;
  margin-bottom: 5px;
}

.instruction-list {
  text-align: left;
  font-size: 0.85rem;
  color: rgba(255, 255, 255, 0.9);
  padding-left: 20px;
}

.instruction-list li {
  margin-bottom: 4px;
}

.account-selection {
  margin: 20px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
  padding: 0 10px;
  width: 100%;
}

.account-selection select {
  width: 100%;
  max-width: 300px;
  padding: 10px;
  border-radius: 4px;
  border: 1px solid #ddd;
  background-color: white;
  font-size: 16px;
  color: #222;
  margin-bottom: 5px;
}

.account-selection .green-btn {
  margin-top: 5px;
  min-width: 150px;
  padding: 10px 20px;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  margin: 20px auto;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #2196f3;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-message {
  color: #f44336;
  text-align: center;
  margin: 15px 0;
  font-weight: 600;
  background-color: rgba(244, 67, 54, 0.1);
  padding: 8px;
  border-radius: 4px;
}

/* Menu Screen */
.account-info {
  text-align: center;
  padding: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  margin-bottom: 10px;
}

.balance-display {
  margin: 10px 0;
  font-size: 18px;
}

.balance-label {
  color: rgba(255, 255, 255, 0.7);
  margin-right: 10px;
}

.balance-amount {
  font-weight: 600;
  color: white;
  font-size: 22px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.menu-options {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 30px 0;
  padding-bottom: 30px;
}

.menu-options h4 {
  margin-bottom: 20px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 16px;
}

.menu-buttons {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 15px;
  margin-top: 20px;
  padding-bottom: 20px;
}

.menu-spacer {
  flex-grow: 1;
}

.confirm-row, .action-row {
  display: flex;
  justify-content: space-around;
  align-items: center;
  gap: 15px;
  margin-top: 15px;
}

/* Amount Entry Screen */
.operation-type {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.amount-entry {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-grow: 1;
}

.amount-display {
  font-size: 36px;
  font-weight: 600;
  color: white;
  margin-bottom: 20px;
  min-height: 42px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.entry-instructions {
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 12px;
}

.amount-presets {
  margin: 15px 0;
}

.preset-row {
  display: flex;
  align-items: center;
  margin: 5px 0;
}

.preset-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 600;
  padding: 5px 10px;
  white-space: nowrap;
}

.preset-amounts {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-left: 12px;
}

.preset {
  background: linear-gradient(to bottom, #555, #444);
  border: 1px solid #333;
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 13px;
  color: white;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
  text-shadow: 0 1px 1px rgba(0, 0, 0, 0.5);
}

.preset:hover {
  background: linear-gradient(to bottom, #666, #555);
}

.preset:active {
  background: linear-gradient(to bottom, #444, #555);
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.3);
}

.confirm-buttons {
  display: flex;
  justify-content: space-between;
  padding: 0 20px 20px;
  flex-direction: column;
  gap: 10px;
}

/* Confirm Screen */
.confirmation-details, .receipt-details {
  margin: 15px 0;
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  padding: 12px;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.confirm-row, .receipt-row {
  display: flex;
  margin-bottom: 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  padding-bottom: 8px;
}

.confirm-label, .receipt-label {
  width: 40%;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 600;
}

.confirm-value, .receipt-value {
  width: 60%;
  color: white;
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
  width: 40px;
  height: 40px;
  border: 4px solid rgba(255, 255, 255, 0.2);
  border-top: 4px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

.processing-animation p {
  color: white;
  margin: 4px 0;
}

.processing-animation .small {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
}

/* Success Screen */
.screen-success {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 20px;
  text-align: center;
  animation: fadeIn 0.3s ease-in;
  background-color: #004080;
}

.screen-success .success-icon {
  width: 60px;
  height: 60px;
  background-color: #4caf50;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 36px;
  color: white;
  margin-bottom: 20px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
}

.screen-success h3 {
  font-size: 24px;
  margin-bottom: 20px;
  color: white;
}

.screen-success .transaction-details {
  background-color: rgba(255, 255, 255, 0.1);
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 25px;
  color: white;
  max-width: 80%;
}

.screen-success .amount,
.screen-success .balance {
  font-weight: bold;
  font-size: 1.1em;
}

.screen-success button {
  margin-top: 15px;
  min-width: 180px;
}

@keyframes fadeIn {
  0% { opacity: 0; }
  100% { opacity: 1; }
}

.transaction-success {
  text-align: center;
  margin-bottom: 30px;
}

.success-icon {
  width: 60px;
  height: 60px;
  margin: 0 auto 15px;
  background-color: #4caf50;
  color: white;
  font-size: 36px;
  font-weight: bold;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}

.transaction-details {
  background-color: rgba(255, 255, 255, 0.15);
  padding: 15px;
  border-radius: 8px;
  margin: 20px auto;
  max-width: 80%;
}

.transaction-details .amount,
.transaction-details .balance {
  font-weight: bold;
  color: white;
  font-size: 1.1em;
}

.prompt-question {
  text-align: center;
  margin-top: 20px;
}

.prompt-question h4 {
  font-size: 1.4em;
  margin-bottom: 20px;
  color: white;
}

.prompt-options {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 20px;
}

/* Enhanced Receipt Screen */
/* Removed receipt-related styling */

.action-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
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
  font-size: 40px;
  color: #f44336;
  margin-bottom: 16px;
}

.screen-error h3 {
  color: #f44336;
  margin-bottom: 12px;
}

.screen-error .error-message {
  text-align: center;
  margin-bottom: 20px;
  max-width: 80%;
}

/* Debug element for forcing receipt prompt */
.debug-force-receipt-prompt {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 9999;
  background: rgba(255, 0, 0, 0.9);
  padding: 8px;
  border-radius: 4px;
  border: 2px solid yellow;
  box-shadow: 0 0 10px rgba(0,0,0,0.5);
}

.debug-force-receipt-prompt button {
  background: #f44336;
  color: white;
  border: 2px solid white;
  padding: 8px 12px;
  cursor: pointer;
  font-size: 14px;
  font-weight: bold;
  border-radius: 4px;
}

/* Responsive Adjustments */
@media (max-width: 768px) {
  .atm-container {
    padding: 10px;
    min-height: auto;
  }
  
  .atm-machine {
    border-radius: 8px;
    margin-bottom: 30px;
    max-width: 100%;
    box-shadow: 
      0 5px 15px rgba(0, 0, 0, 0.2),
      0 0 0 5px #d0d0d0;
  }
  
  .atm-screen {
    height: auto;
    min-height: 320px;
  }
  
  .atm-controls {
    flex-direction: column;
    align-items: center;
    padding: 15px 10px;
    margin: 10px;
  }
  
  .keypad, .cash-slots {
    width: 100%;
    max-width: 350px;
    margin-bottom: 16px;
  }
  
  .menu-buttons {
    grid-template-columns: 1fr;
    gap: 8px;
  }
  
  .account-selection {
    gap: 10px;
  }
  
  .account-selection select, 
  .account-selection .green-btn {
    max-width: 100%;
  }
  
  .welcome-message h2 {
    font-size: 1.5rem;
  }
  
  .instruction-list {
    font-size: 0.8rem;
  }
  
  .keypad-btn {
    height: 40px;
  }
  
  /* Ensure all buttons are properly visible */
  .screen-content {
    padding: 15px 10px;
    overflow-y: auto;
  }
  
  .screen-idle, .screen-success, .screen-error {
    padding-bottom: 20px;
  }
}
</style>
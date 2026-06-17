package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

class VtuViewModel(private val repository: VtuRepository) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.allTransactions.first().let { currentList ->
                if (currentList.isEmpty()) {
                    // Seed initial transactions containing SUCCESS, PENDING, and FAILED states
                    val seeds = listOf(
                        Transaction(
                            serviceType = "Airtime",
                            recipient = "08031234567",
                            provider = "MTN",
                            amount = 500.0,
                            fee = 0.0,
                            reference = "VTU-MTN-98214",
                            status = "SUCCESS",
                            timestamp = System.currentTimeMillis() - 3600000 * 2, // 2 hours ago
                            details = "Airtime credit topup"
                        ),
                        Transaction(
                            serviceType = "Data",
                            recipient = "08051284931",
                            provider = "Glo",
                            amount = 2000.0,
                            fee = 0.0,
                            reference = "VTU-GLO-23194",
                            status = "PENDING",
                            timestamp = System.currentTimeMillis() - 1800000, // 30 mins ago
                            details = "Data Bundle: 6GB - 30 Days (₦2,000)"
                        ),
                        Transaction(
                            serviceType = "Airtime",
                            recipient = "09091234321",
                            provider = "Airtel",
                            amount = 1000.0,
                            fee = 0.0,
                            reference = "VTU-ART-00234",
                            status = "FAILED",
                            timestamp = System.currentTimeMillis() - 3600000 * 5, // 5 hours ago
                            details = "Failed: Network connection timeout"
                        ),
                        Transaction(
                            serviceType = "Wallet Fund",
                            recipient = "SwiftWallet",
                            provider = "Access Bank",
                            amount = 5000.0,
                            fee = 0.0,
                            reference = "FUND-928172",
                            status = "SUCCESS",
                            timestamp = System.currentTimeMillis() - 3600000 * 24, // 1 day ago
                            details = "Direct credit transfer"
                        ),
                        Transaction(
                            serviceType = "Cable TV",
                            recipient = "2019385712",
                            provider = "Gotv",
                            amount = 3950.0,
                            fee = 100.0,
                            reference = "VTU-GOT-55721",
                            status = "SUCCESS",
                            timestamp = System.currentTimeMillis() - 3600000 * 48, // 2 days ago
                            details = "TV Subscription bouquet: Jolli (₦3,950)"
                        )
                    )
                    for (seed in seeds) {
                        repository.saveTransaction(seed)
                    }

                    // Also seed some initial beneficiaries
                    val beneficiaries = listOf(
                        Beneficiary(
                            name = "My MTN Line",
                            alias = "MTN (4567)",
                            number = "08031234567",
                            type = "Airtime",
                            provider = "MTN"
                        ),
                        Beneficiary(
                            name = "Dad's Airtel Line",
                            alias = "Airtel (4321)",
                            number = "09091234321",
                            type = "Airtime",
                            provider = "Airtel"
                        )
                    )
                    for (bene in beneficiaries) {
                        repository.saveBeneficiary(bene)
                    }
                }
            }
        }
    }

    // Active Service Tab
    private val _selectedService = MutableStateFlow("Airtime")
    val selectedService: StateFlow<String> = _selectedService.asStateFlow()

    // Form states
    private val _recipientNumber = MutableStateFlow("")
    val recipientNumber: StateFlow<String> = _recipientNumber.asStateFlow()

    private val _selectedProvider = MutableStateFlow("MTN")
    val selectedProvider: StateFlow<String> = _selectedProvider.asStateFlow()

    private val _inputAmount = MutableStateFlow("")
    val inputAmount: StateFlow<String> = _inputAmount.asStateFlow()

    // Cable TV Specific state
    private val _selectedCablePackage = MutableStateFlow("DSTV Yanga (₦4,200)")
    val selectedCablePackage: StateFlow<String> = _selectedCablePackage.asStateFlow()

    // Internet Data Specific state
    private val _selectedDataPlan = MutableStateFlow("1.5GB - 30 Days (₦1,200)")
    val selectedDataPlan: StateFlow<String> = _selectedDataPlan.asStateFlow()

    // Query for transactions
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Wallet State: Ensure it's never null, default init if needed.
    val wallet: StateFlow<Wallet> = repository.wallet
        .onEach { existing ->
            if (existing == null) {
                // Initialize default wallet with ₦5,000.00
                viewModelScope.launch {
                    repository.saveWallet(Wallet(id = 1, balance = 5000.0))
                }
            }
        }
        .filterNotNull()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Wallet(id = 1, balance = 5000.0)
        )

    // Raw Transactions and filtered active transactions
    val transactions: StateFlow<List<Transaction>> = combine(
        repository.allTransactions,
        _searchQuery
    ) { txList, query ->
        if (query.isEmpty()) {
            txList
        } else {
            txList.filter {
                it.serviceType.contains(query, ignoreCase = true) ||
                it.recipient.contains(query) ||
                it.provider.contains(query, ignoreCase = true) ||
                it.reference.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Beneficiaries
    val beneficiaries: StateFlow<List<Beneficiary>> = repository.allBeneficiaries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectService(service: String) {
        _selectedService.value = service
        _recipientNumber.value = ""
        _inputAmount.value = ""
        // Reset defaults per service type
        when (service) {
            "Airtime" -> {
                _selectedProvider.value = "MTN"
            }
            "Data" -> {
                _selectedProvider.value = "MTN"
                _selectedDataPlan.value = "1.5GB - 30 Days (₦1,200)"
            }
            "Cable TV" -> {
                _selectedProvider.value = "DSTV"
                _selectedCablePackage.value = "Yanga (₦4,200)"
            }
            "Electricity" -> {
                _selectedProvider.value = "AEDC (Abuja)"
            }
        }
    }

    fun updateRecipient(number: String) {
        // Keep digits only or limit based on length
        _recipientNumber.value = number.filter { it.isDigit() }
    }

    fun updateProvider(prov: String) {
        _selectedProvider.value = prov
        // Reset plan values if provider changes
        if (_selectedService.value == "Data") {
            val plans = getDataPlansForProvider(prov)
            if (plans.isNotEmpty()) {
                _selectedDataPlan.value = plans.first()
            }
        } else if (_selectedService.value == "Cable TV") {
            val tvPlans = getTvPackagesForProvider(prov)
            if (tvPlans.isNotEmpty()) {
                _selectedCablePackage.value = tvPlans.first()
            }
        }
    }

    fun updateAmount(amt: String) {
        _inputAmount.value = amt.filter { it.isDigit() || it == '.' }
    }

    fun updateCablePackage(pkg: String) {
        _selectedCablePackage.value = pkg
    }

    fun updateDataPlan(plan: String) {
        _selectedDataPlan.value = plan
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Helper utilities for pricing
    fun getDataPlansForProvider(provider: String): List<String> = when (provider) {
        "MTN" -> listOf("1.5GB - 30 Days (₦1,200)", "3GB - 30 Days (₦1,600)", "10GB - 30 Days (₦3,500)", "20GB - 30 Days (₦6,000)")
        "Airtel" -> listOf("2GB - 30 Days (₦1,200)", "5GB - 30 Days (₦1,800)", "15GB - 30 Days (₦4,000)", "30GB - 30 Days (₦8,000)")
        "Glo" -> listOf("2.5GB - 30 Days (₦1,000)", "6GB - 30 Days (₦2,000)", "12GB - 30 Days (₦3,000)", "25GB - 30 Days (₦5,000)")
        "9mobile" -> listOf("2GB - 30 Days (₦1,000)", "7GB - 30 Days (₦2,500)", "15GB - 30 Days (₦4,000)")
        else -> emptyList()
    }

    fun getTvPackagesForProvider(provider: String): List<String> = when (provider) {
        "DSTV" -> listOf("Yanga (₦4,200)", "Confam (₦7,400)", "Compact (₦12,500)", "Premium (₦29,500)")
        "Gotv" -> listOf("Supa+ (₦12,500)", "Max (₦5,700)", "Jolli (₦3,950)", "Jinja (₦2,700)", "Lite (₦1,300)")
        "Startimes" -> listOf("Super (₦9,000)", "Classic (₦5,000)", "Basic (₦3,000)", "Nova (₦1,500)")
        else -> emptyList()
    }

    // Extraction helper
    fun extractPriceFromString(text: String): Double {
        val regex = "₦([\\d,]+)".toRegex()
        val match = regex.find(text)
        return if (match != null) {
            match.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
        } else {
            0.0
        }
    }

    fun fundWallet(amount: Double, sourceName: String) {
        viewModelScope.launch {
            val currentBal = wallet.value.balance
            val newBal = currentBal + amount
            repository.saveWallet(Wallet(id = 1, balance = newBal))

            val ref = "FUND-${System.currentTimeMillis() % 1000000}"
            repository.saveTransaction(
                Transaction(
                    serviceType = "Wallet Fund",
                    recipient = "SwiftWallet",
                    provider = sourceName,
                    amount = amount,
                    fee = 0.0,
                    reference = ref,
                    status = "SUCCESS",
                    details = "Wallet direct credit from Bank Transfer"
                )
            )
        }
    }

    fun executeTransaction(onSuccess: (Transaction) -> Unit, onError: (String) -> Unit) {
        val service = _selectedService.value
        val number = _recipientNumber.value
        val provider = _selectedProvider.value

        // Validate inputs
        if (number.length < 10) {
            onError("Please enter a valid phone/identification/smartcard number.")
            return
        }

        var amount = 0.0
        var serviceFee = 0.0

        when (service) {
            "Airtime" -> {
                amount = _inputAmount.value.toDoubleOrNull() ?: 0.0
                if (amount < 50.0 || amount > 50000.0) {
                    onError("Airtime buy amount must be between ₦50 and ₦50,000.")
                    return
                }
                serviceFee = 0.0
            }
            "Data" -> {
                amount = extractPriceFromString(_selectedDataPlan.value)
                serviceFee = 0.0
            }
            "Cable TV" -> {
                amount = extractPriceFromString(_selectedCablePackage.value)
                serviceFee = 100.0 // Standard bill fee
            }
            "Electricity" -> {
                amount = _inputAmount.value.toDoubleOrNull() ?: 0.0
                if (amount < 500.0 || amount > 100000.0) {
                    onError("Electricity purchase must be between ₦500 and ₦100,000.")
                    return
                }
                serviceFee = 100.0 // Standard meter fee
            }
        }

        val totalCost = amount + serviceFee
        val walletObj = wallet.value

        if (walletObj.balance < totalCost) {
            // Log as FAILED in history
            viewModelScope.launch {
                val ref = "TX-FAIL-${100000 + Random.nextInt(900000)}"
                val failedTx = Transaction(
                    serviceType = service,
                    recipient = number,
                    provider = provider,
                    amount = amount,
                    fee = serviceFee,
                    reference = ref,
                    status = "FAILED",
                    details = "Failed: Insufficient Wallet Balance"
                )
                repository.saveTransaction(failedTx)
            }
            onError("Insufficient wallet balance. Please fund your wallet first.")
            return
        }

        // Deduct balance and commit transaction
        viewModelScope.launch {
            val newBalance = walletObj.balance - totalCost
            repository.saveWallet(Wallet(id = 1, balance = newBalance))

            val ref = "VTU-${System.currentTimeMillis() % 1000000}"
            val txDetails = when (service) {
                "Airtime" -> "Airtime credit topup"
                "Data" -> "Data Bundle: ${_selectedDataPlan.value}"
                "Cable TV" -> "TV Subscription bouquet: ${_selectedCablePackage.value}"
                "Electricity" -> "Electricity smart token generated"
                else -> ""
            }

            val successTx = Transaction(
                serviceType = service,
                recipient = number,
                provider = provider,
                amount = amount,
                fee = serviceFee,
                reference = ref,
                status = "SUCCESS",
                details = txDetails
            )
            repository.saveTransaction(successTx)
            onSuccess(successTx)
        }
    }

    fun saveBeneficiary(name: String, alias: String, number: String, type: String, provider: String) {
        viewModelScope.launch {
            repository.saveBeneficiary(
                Beneficiary(
                    name = name,
                    alias = alias,
                    number = number,
                    type = type,
                    provider = provider
                )
            )
        }
    }

    fun deleteBeneficiary(beneficiary: Beneficiary) {
        viewModelScope.launch {
            repository.removeBeneficiary(beneficiary)
        }
    }
}

class VtuViewModelFactory(private val repository: VtuRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VtuViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VtuViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

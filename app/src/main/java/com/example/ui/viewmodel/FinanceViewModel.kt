package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.SecuritySettingsEntity
import com.example.data.Transaction
import com.example.pdf.PdfExportHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FinanceViewModel(
    private val repository: AppRepository,
    private val context: Context
) : ViewModel() {

    // Preferences Storage
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _appTheme = MutableStateFlow(prefs.getString("theme", "Sistem") ?: "Sistem")
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    private val _accentColor = MutableStateFlow(prefs.getString("accent", "Biru") ?: "Biru")
    val accentColor: StateFlow<String> = _accentColor.asStateFlow()

    private val _fontScale = MutableStateFlow(prefs.getString("fontScale", "Sedang") ?: "Sedang")
    val fontScale: StateFlow<String> = _fontScale.asStateFlow()

    private val _displayScale = MutableStateFlow(prefs.getString("displayScale", "Sedang") ?: "Sedang")
    val displayScale: StateFlow<String> = _displayScale.asStateFlow()

    fun updateTheme(newTheme: String) {
        _appTheme.value = newTheme
        prefs.edit().putString("theme", newTheme).apply()
    }

    fun updateAccentColor(newAccent: String) {
        _accentColor.value = newAccent
        prefs.edit().putString("accent", newAccent).apply()
    }

    fun updateFontScale(newScale: String) {
        _fontScale.value = newScale
        prefs.edit().putString("fontScale", newScale).apply()
    }

    fun updateDisplayScale(newScale: String) {
        _displayScale.value = newScale
        prefs.edit().putString("displayScale", newScale).apply()
    }

    // Main Transaction stream
    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Security settings
    val securitySettings: StateFlow<SecuritySettingsEntity?> = repository.securitySettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Lock screen state
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    // Currently selected month for reports (Format: "MMMM yyyy", e.g. "Mei 2026")
    private val _selectedMonth = MutableStateFlow("")
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    // PDF generation status or notice
    private val _pdfExportStatus = MutableStateFlow<String?>(null)
    val pdfExportStatus: StateFlow<String?> = _pdfExportStatus.asStateFlow()

    init {
        // Initialize lock state based on database preference
        viewModelScope.launch {
            val settings = repository.getSecuritySettings()
            if (settings != null && settings.isPasscodeEnabled && settings.encryptedPasscode.isNotEmpty()) {
                _isLocked.value = true
            }
        }

        // Initialize selectedMonth with current month
        val currentMonth = SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(Date())
        _selectedMonth.value = currentMonth
    }

    // List of months present in transactions to select from, always including current month
    val availableMonths: StateFlow<List<String>> = transactions.map { list ->
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
        val months = list.map { sdf.format(Date(it.timestamp)) }.toMutableSet()
        val currentMonth = sdf.format(Date())
        months.add(currentMonth)
        months.toList().sortedWith { m1, m2 ->
            try {
                val d1 = sdf.parse(m1) ?: Date(0)
                val d2 = sdf.parse(m2) ?: Date(0)
                d2.compareTo(d1) // Show newest months first
            } catch (e: Exception) {
                0
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf(SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(Date()))
    )

    // Filtered transaction list for the selected month
    val filteredTransactions: StateFlow<List<Transaction>> = combine(transactions, selectedMonth) { list, month ->
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
        list.filter { sdf.format(Date(it.timestamp)) == month }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Summary calculations for the selected month
    val monthlySummary: StateFlow<MonthlySummaryData> = filteredTransactions.map { list ->
        val income = list.filter { it.isIncome }.sumOf { it.amount }
        val expense = list.filter { !it.isIncome }.sumOf { it.amount }
        
        // Group expenses and incomes by category
        val categoryBreakdown = list.filter { !it.isIncome }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        MonthlySummaryData(
            totalIncome = income,
            totalExpense = expense,
            netBalance = income - expense,
            categoryBreakdown = categoryBreakdown
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MonthlySummaryData()
    )

    // Operations
    fun selectMonth(month: String) {
        _selectedMonth.value = month
    }

    fun addTransaction(title: String, amount: Double, category: String, note: String, isIncome: Boolean, paymentMethod: String = "Tunai", customTimestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            val newTxn = Transaction(
                title = title.ifBlank { if (isIncome) "Pemasukan Baru" else "Pengeluaran Baru" },
                amount = amount,
                category = category,
                note = note,
                timestamp = customTimestamp,
                isIncome = isIncome,
                paymentMethod = paymentMethod
            )
            repository.insertTransaction(newTxn)
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllTransactions()
        }
    }

    // Security actions
    fun setupPasscode(passcode: String) {
        viewModelScope.launch {
            // Securely hashed/stored on device using AES-256 via KeyStore
            val settings = SecuritySettingsEntity(
                id = 1,
                encryptedPasscode = com.example.security.KeystoreHelper.encrypt(passcode),
                isPasscodeEnabled = true
            )
            repository.saveSecuritySettings(settings)
            _isLocked.value = false
        }
    }

    fun disablePasscode() {
        viewModelScope.launch {
            val settings = SecuritySettingsEntity(
                id = 1,
                encryptedPasscode = "",
                isPasscodeEnabled = false
            )
            repository.saveSecuritySettings(settings)
            _isLocked.value = false
        }
    }

    fun unlockApp(passcode: String): Boolean {
        val settings = securitySettings.value
        return if (settings != null && settings.isPasscodeEnabled) {
            val decrypted = com.example.security.KeystoreHelper.decrypt(settings.encryptedPasscode)
            if (decrypted == passcode) {
                _isLocked.value = false
                true
            } else {
                false
            }
        } else {
            _isLocked.value = false
            true
        }
    }

    fun lockApp() {
        val settings = securitySettings.value
        if (settings != null && settings.isPasscodeEnabled) {
            _isLocked.value = true
        }
    }

    // Export PDF
    fun exportReport(context: Context, month: String, onShare: (File) -> Unit) {
        viewModelScope.launch {
            _pdfExportStatus.value = "Menghasilkan dokumen PDF..."
            val txns = filteredTransactions.value
            val file = PdfExportHelper.exportMonthlyReport(context, month, txns)
            if (file != null) {
                _pdfExportStatus.value = "Laporan PDF berhasil diunduh ke folder Download!"
                onShare(file)
            } else {
                _pdfExportStatus.value = "Gagal membuat laporan PDF."
            }
        }
    }

    fun clearPdfStatus() {
        _pdfExportStatus.value = null
    }
}

data class MonthlySummaryData(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netBalance: Double = 0.0,
    val categoryBreakdown: Map<String, Double> = emptyMap()
)

// ViewModel factory to inject repository and context
class FinanceViewModelFactory(
    private val repository: AppRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinanceViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

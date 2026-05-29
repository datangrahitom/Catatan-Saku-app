package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Transaction
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.MonthlySummaryData
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()
    val securitySettings by viewModel.securitySettings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val pdfStatus by viewModel.pdfExportStatus.collectAsStateWithLifecycle()

    // Show toast on PDF status change
    LaunchedEffect(pdfStatus) {
        pdfStatus?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearPdfStatus()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (isLocked) {
            // Lock overlay screen
            LockOverlay(
                onUnlock = { pin ->
                    val success = viewModel.unlockApp(pin)
                    if (!success) {
                        Toast.makeText(context, "PIN tidak cocok. Silakan coba lagi.", Toast.LENGTH_SHORT).show()
                    }
                },
                isSetupMode = securitySettings?.isPasscodeEnabled != true
            )
        } else {
            // Main Dashboard App
            MainContent(viewModel = viewModel)
        }
    }
}

@Composable
fun IosAmbientBackdrop() {
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0B0C10)
    val baseBgColor = if (isDark) Color(0xFF0C0E1A) else Color(0xFFEFF3F8)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(baseBgColor)
    )
}

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 28.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0B0C10)
    val containerBg = if (isDark) {
        Color(0xFF1E2030).copy(alpha = 0.55f)
    } else {
        Color.White
    }
    val borderCol = if (isDark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color(0xFFE2E8F0)
    }

    Card(
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
        modifier = modifier.border(0.75.dp, borderCol, RoundedCornerShape(cornerRadius))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            content()
        }
    }
}

@Composable
fun MainContent(viewModel: FinanceViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            CustomFloatingBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onAddClick = { showAddDialog = true }
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Ambient Backdrop covers the entire screen
            IosAmbientBackdrop()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (selectedTab) {
                    0 -> TransaksiTab(viewModel = viewModel, onAddTrigger = { showAddDialog = true })
                    1 -> LaporanTab(viewModel = viewModel)
                    2 -> KeamananTab(viewModel = viewModel)
                    3 -> SetelanTab(viewModel = viewModel)
                }
            }
        }

        if (showAddDialog) {
            AddTransactionDialog(
                onDismiss = { showAddDialog = false },
                onSave = { title, amount, category, note, isIncome, paymentMethod, customTimestamp ->
                    viewModel.addTransaction(title, amount, category, note, isIncome, paymentMethod, customTimestamp)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun CustomFloatingBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onAddClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0B0C10)
    val barContainerColor = if (isDark) {
        Color(0xFF1C1C1E).copy(alpha = 0.85f)
    } else {
        Color.White
    }
    val barBorderColor = if (isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color(0xFFE2E8F0)
    }

    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Card(
            shape = RoundedCornerShape(38.dp),
            colors = CardDefaults.cardColors(
                containerColor = barContainerColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp, top = 4.dp)
                .fillMaxWidth()
                .shadow(
                    elevation = if (isDark) 4.dp else 18.dp,
                    shape = RoundedCornerShape(38.dp),
                    clip = false,
                    ambientColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFF0F172A).copy(alpha = 0.12f),
                    spotColor = if (isDark) Color.Black.copy(alpha = 0.7f) else Color(0xFF0F172A).copy(alpha = 0.22f)
                )
                .height(82.dp)
                .border(0.5.dp, barBorderColor, RoundedCornerShape(38.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomBarItem(
                    selected = selectedTab == 0,
                    onClick = { onTabSelected(0) },
                    icon = Icons.Filled.AccountBalanceWallet,
                    label = "Transaksi",
                    tag = "nav_transactions"
                )

                BottomBarItem(
                    selected = selectedTab == 1,
                    onClick = { onTabSelected(1) },
                    icon = Icons.Filled.Assessment,
                    label = "Laporan",
                    tag = "nav_reports"
                )

                // Beautiful central interactive add button
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .shadow(
                                elevation = 6.dp,
                                shape = CircleShape,
                                clip = false,
                                ambientColor = MaterialTheme.colorScheme.primary,
                                spotColor = MaterialTheme.colorScheme.primary
                            )
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            )
                            .clickable { onAddClick() }
                            .testTag("nav_add"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Tambah Catatan",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                BottomBarItem(
                    selected = selectedTab == 2,
                    onClick = { onTabSelected(2) },
                    icon = Icons.Filled.Lock,
                    label = "Keamanan",
                    tag = "nav_security"
                )

                BottomBarItem(
                    selected = selectedTab == 3,
                    onClick = { onTabSelected(3) },
                    icon = Icons.Filled.Settings,
                    label = "Setelan",
                    tag = "nav_settings"
                )
            }
        }
    }
}

@Composable
fun RowScope.BottomBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    tag: String
) {
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0B0C10)
    
    // Smooth transition animations
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1.0f,
        animationSpec = tween(durationMillis = 200),
        label = "scale"
    )
    
    val activeBgColor = if (selected) {
        if (isDark) Color(0xFF2E2E32) else Color.White
    } else {
        Color.Transparent
    }
    
    val activeContentColor = MaterialTheme.colorScheme.primary
    val inactiveContentColor = if (isDark) Color(0xFFA0A0AB) else Color(0xFF6B6B75)
    
    val contentColor by animateColorAsState(
        targetValue = if (selected) activeContentColor else inactiveContentColor,
        animationSpec = tween(durationMillis = 200),
        label = "contentColor"
    )

    Box(
        modifier = Modifier
            .weight(1.3f)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .height(64.dp)
                .shadow(
                    elevation = if (selected && !isDark) 6.dp else 0.dp,
                    shape = RoundedCornerShape(32.dp),
                    clip = false,
                    ambientColor = if (isDark) Color.Transparent else Color(0xFF0F172A).copy(alpha = 0.08f),
                    spotColor = if (isDark) Color.Transparent else Color(0xFF0F172A).copy(alpha = 0.12f)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(activeBgColor)
                .clickable { onClick() }
                .testTag(tag)
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                color = contentColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---------------- TRANSAKSI TAB ----------------

@Composable
fun TransaksiTab(viewModel: FinanceViewModel, onAddTrigger: () -> Unit) {
    val transactionsStream by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val monthlySummary by viewModel.monthlySummary.collectAsStateWithLifecycle()
    val availableMonths by viewModel.availableMonths.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val currentTheme by viewModel.appTheme.collectAsStateWithLifecycle()

    var showMonthMenu by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    val transactions = remember(transactionsStream, searchQuery) {
        if (searchQuery.isBlank()) {
            transactionsStream
        } else {
            transactionsStream.filter { txn ->
                txn.title.contains(searchQuery, ignoreCase = true) ||
                txn.category.contains(searchQuery, ignoreCase = true) ||
                txn.note.contains(searchQuery, ignoreCase = true) ||
                txn.paymentMethod.contains(searchQuery, ignoreCase = true) ||
                txn.amount.toString().contains(searchQuery)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Title Branding & Dynamic Utility Row (Theme, Search, Month)
        item {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSearching) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari rincian, memo, nominal...", fontSize = 13.sp) },
                        leadingIcon = {
                            IconButton(onClick = {
                                isSearching = false
                                searchQuery = ""
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = "Kembali",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = "Kosongkan",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Catatan saku",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = "ENKRIPSI AKTIF",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // Top Action Bar Items: Theme toggle, Search icon, Month Switcher button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Light & Dark Mode Button
                        IconButton(
                            onClick = {
                                val nextTheme = if (currentTheme == "Gelap") "Terang" else "Gelap"
                                viewModel.updateTheme(nextTheme)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (currentTheme == "Gelap") Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = "Ubah Tema",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Search Icon Trigger Button
                        IconButton(
                            onClick = { isSearching = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Buka Pencarian",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Month Switcher
                        Box {
                            Button(
                                onClick = { showMonthMenu = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(selectedMonth, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Pilih Bulan", modifier = Modifier.size(14.dp))
                            }
                            DropdownMenu(
                                expanded = showMonthMenu,
                                onDismissRequest = { showMonthMenu = false }
                            ) {
                                availableMonths.forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(m, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                                        onClick = {
                                            viewModel.selectMonth(m)
                                            showMonthMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Summary Card
        item {
            FinancialSummaryCard(summary = monthlySummary)
        }

        // Quick Stats Row
        item {
            QuickStatsRow(transactions = transactions)
        }

        // Transactions List Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Catatan Transaksi",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${transactions.size} Item",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        if (transactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalanceWallet,
                            contentDescription = "Kosong",
                            tint = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Hasil pencarian tidak ditemukan." else "Belum ada transaksi di bulan ini.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Coba gunakan kata kunci pencarian yang lain." else "Tekan tombol Tambah di tengah bawah untuk menambah.",
                            color = Color.Gray.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(transactions, key = { it.id }) { txn ->
                TransactionRow(
                    transaction = txn,
                    onDelete = { viewModel.deleteTransaction(txn.id) }
                )
            }
        }
    }
}

@Composable
fun FinancialSummaryCard(summary: MonthlySummaryData) {
    var isHidden by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

    val balanceStr = if (isHidden) "••••••" else formatRupiah(summary.netBalance)
    val incomeStr = if (isHidden) "••••••" else formatRupiah(summary.totalIncome)
    val expenseStr = if (isHidden) "••••••" else formatRupiah(summary.totalExpense)
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0B0C10)
    // iOS Widget style Glassmorphism card container
    val glassBg = if (isDark) {
        Color(0xFF1E2135).copy(alpha = 0.65f)
    } else {
        Color.White
    }
    val glassBorderColor = if (isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color(0xFFE2E8F0)
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, glassBorderColor, RoundedCornerShape(28.dp)),
        colors = CardDefaults.cardColors(containerColor = glassBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ESTIMASI SALDO BERSIH",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White.copy(alpha = 0.65f) else Color.Black.copy(alpha = 0.55f),
                            letterSpacing = 0.5.sp
                        )
                    }
                    IconButton(
                        onClick = { isHidden = !isHidden },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isHidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = "Sembunyikan Saldo",
                            tint = if (isDark) Color.White.copy(alpha = 0.75f) else Color.Black.copy(alpha = 0.60f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = balanceStr,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Beautiful translucent summary capsules
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isDark) Color.Black.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.05f))
                        .border(
                            0.5.dp, 
                            if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f), 
                            RoundedCornerShape(20.dp)
                        )
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Income Summary details
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.ArrowUpward,
                                contentDescription = "Pemasukan",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Pemasukan",
                                fontSize = 10.sp,
                                color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = incomeStr,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }

                    // Divider line
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f))
                    )

                    // Expense Summary details
                    Row(
                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.ArrowDownward,
                                contentDescription = "Pengeluaran",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Pengeluaran",
                                fontSize = 10.sp,
                                color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = expenseStr,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionRow(
    transaction: Transaction,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDetailDialog by remember { mutableStateOf(false) }

    val iconColor = if (transaction.isIncome) Color(0xFF10B981) else Color(0xFFEF4444)
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0B0C10)
    val iconBg = if (isDark) {
        if (transaction.isIncome) Color(0xFF064E3B) else Color(0xFF7F1D1D)
    } else {
        if (transaction.isIncome) Color(0xFFE6F7F0) else Color(0xFFFEE2E2)
    }
    val iconVec = if (transaction.isIncome) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward

    val itemBg = if (isDark) {
        Color(0xFF1E2030).copy(alpha = 0.45f)
    } else {
        Color.White
    }
    val itemBorder = if (isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color(0xFFE2E8F0)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = itemBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.75.dp, itemBorder, RoundedCornerShape(20.dp))
            .clickable { showDetailDialog = true }
            .testTag("transaction_item_${transaction.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    iconVec,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${transaction.category} • ${formatShortDate(transaction.timestamp)}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
            }

            Text(
                text = (if (transaction.isIncome) "+" else "-") + formatRupiah(transaction.amount),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = iconColor
            )

            IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = "Hapus",
                    tint = Color.Gray.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Catatan") },
            text = { Text("Apakah Anda yakin ingin menghapus catatan keuangan \"${transaction.title}\" ini?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showDetailDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showDetailDialog = false }) {
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                cornerRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val isIncome = transaction.isIncome
                    val typeColor = if (isIncome) Color(0xFF10B981) else Color(0xFFEF4444)

                    Text(
                        text = if (isIncome) "Rincian Pemasukan" else "Rincian Pengeluaran",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = typeColor
                    )

                    Text(
                        text = (if (isIncome) "+" else "-") + formatRupiah(transaction.amount),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    DetailFieldRow(label = "Kategori", value = transaction.category)
                    DetailFieldRow(label = "Metode Pembayaran", value = transaction.paymentMethod)
                    
                    val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("id", "ID"))
                    val formattedTime = sdf.format(java.util.Date(transaction.timestamp))
                    DetailFieldRow(label = "Tanggal", value = formattedTime)

                    if (transaction.note.isNotBlank()) {
                         DetailFieldRow(label = "Catatan", value = transaction.note)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { showDetailDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Tutup", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailFieldRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ---------------- LAPORAN TAB ----------------

@Composable
fun LaporanTab(viewModel: FinanceViewModel) {
    val context = LocalContext.current
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val summary by viewModel.monthlySummary.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val availableMonths by viewModel.availableMonths.collectAsStateWithLifecycle()

    var showMonthMenu by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tab Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Analisis Bulanan",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Otomatis mengelompokkan pengeluaran Anda",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // Month Switcher
                Box {
                    Button(
                        onClick = { showMonthMenu = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(selectedMonth)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Pilih Laporan")
                    }
                    DropdownMenu(
                        expanded = showMonthMenu,
                        onDismissRequest = { showMonthMenu = false }
                    ) {
                        availableMonths.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m) },
                                onClick = {
                                    viewModel.selectMonth(m)
                                    showMonthMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Quick Stats Chart Area
        item {
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp
            ) {
                Text(
                    text = "Statistik Pengeluaran & Pemasukan",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                val total = summary.totalIncome + summary.totalExpense
                val incomePercent = if (total > 0) (summary.totalIncome / total).toFloat() else 0.5f
                val expensePercent = if (total > 0) (summary.totalExpense / total).toFloat() else 0.5f

                // Side by side Comparison Gauge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .weight(incomePercent.coerceAtLeast(0.05f))
                            .fillMaxHeight()
                            .background(Color(0xFF10B981))
                    )
                    Box(
                        modifier = Modifier
                            .weight(expensePercent.coerceAtLeast(0.05f))
                            .fillMaxHeight()
                            .background(Color(0xFFEF4444))
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Pemasukan (${(incomePercent * 100).toInt()}% )",
                        color = Color(0xFF10B981),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pengeluaran (${(expensePercent * 100).toInt()}% )",
                        color = Color(0xFFEF4444),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Category Breakdown graph section
        item {
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Text(
                    text = "Klasifikasi Kategori Pengeluaran",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (summary.categoryBreakdown.isEmpty()) {
                    Text(
                        text = "Tidak ada riwayat pengeluaran bulan ini.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    // Dynamic statistical donut chart representation
                    ExpenseDonutChart(categoryBreakdown = summary.categoryBreakdown)
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    val maxExpense = summary.categoryBreakdown.values.maxOrNull() ?: 1.0
                    summary.categoryBreakdown.entries.sortedByDescending { it.value }.forEach { entry ->
                        val progress = (entry.value / maxExpense).toFloat()
                        val color = getCategoryColor(entry.key)
                        CategoryReportRow(
                            title = entry.key,
                            amountStr = formatRupiah(entry.value),
                            progress = progress,
                            fraction = progress,
                            progressColor = color
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }
        }

        // Export PDF Section
        item {
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = "PDF Icon",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ekspor Laporan PDF",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Simpan laporan bulan $selectedMonth ke berkas PDF lokal Anda.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            viewModel.exportReport(context, selectedMonth) { file ->
                                // On complete write, launch share intent
                                try {
                                    val shareIntent = com.example.pdf.PdfExportHelper.getSharePdfIntent(context, file)
                                    val chooser = Intent.createChooser(shareIntent, "Bagikan Laporan PDF Keuangan")
                                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(chooser)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Berhasil diunduh! Tidak dapat meluncurkan share dialog.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.testTag("pdf_download_button")
                    ) {
                        Text("Unduh")
                    }
                }
            }
        }
    }
}

val categoryColors = mapOf(
    "Makanan & Minuman" to Color(0xFFF59E0B), // Amber
    "Transportasi" to Color(0xFF3B82F6),     // Blue
    "Belanja Bulanan" to Color(0xFFEC4899),  // Pink
    "Keperluan Rumah" to Color(0xFF10B981),  // Emerald Green
    "Hiburan" to Color(0xFF8B5CF6),          // Purple
    "Pendidikan" to Color(0xFF06B6D4),       // Teal
    "Kesehatan" to Color(0xFFEF4444),        // Red
    "Lainnya" to Color(0xFF6B7280)           // Slate Gray
)

fun getCategoryColor(category: String): Color {
    return categoryColors[category] ?: Color(0xFF84CC16) // Lime as default fallback
}

@Composable
fun ExpenseDonutChart(
    categoryBreakdown: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    val totalExpense = categoryBreakdown.values.sum()
    if (totalExpense <= 0.0) return

    val entries = categoryBreakdown.entries.sortedByDescending { it.value }
    var currentStartAngle = -90f // Start pointing up

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(170.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidthPx = 16.dp.toPx()
                val radius = (size.minDimension - strokeWidthPx) / 2f
                val centerOffset = Offset(size.width / 2f, size.height / 2f)

                entries.forEach { entry ->
                    val sweepAngle = ((entry.value / totalExpense) * 360f).toFloat()
                    val color = getCategoryColor(entry.key)
                    
                    val gap = if (entries.size > 1) 3f else 0f
                    val finalSweepAngle = (sweepAngle - gap).coerceAtLeast(1f)

                    drawArc(
                        color = color,
                        startAngle = currentStartAngle + (gap / 2f),
                        sweepAngle = finalSweepAngle,
                        useCenter = false,
                        style = Stroke(
                            width = strokeWidthPx,
                            cap = StrokeCap.Butt
                        ),
                        topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                        size = Size(radius * 2f, radius * 2f)
                    )
                    currentStartAngle += sweepAngle
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Total Pengeluaran",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatRupiah(totalExpense),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun CategoryReportRow(
    title: String,
    amountStr: String,
    progress: Float,
    fraction: Float,
    progressColor: Color = Color(0xFFEF4444)
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(progressColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = amountStr,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = progressColor
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

// ---------------- SETELAN TAB ----------------

@Composable
fun SetelanTab(viewModel: FinanceViewModel) {
    val context = LocalContext.current
    val currentTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val currentAccent by viewModel.accentColor.collectAsStateWithLifecycle()
    val currentFontScale by viewModel.fontScale.collectAsStateWithLifecycle()
    val currentDisplayScale by viewModel.displayScale.collectAsStateWithLifecycle()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var localFontScale by remember(currentFontScale) { mutableStateOf(currentFontScale) }
    var localDisplayScale by remember(currentDisplayScale) { mutableStateOf(currentDisplayScale) }
    
    val isScaleChanged = localFontScale != currentFontScale || localDisplayScale != currentDisplayScale

    val themes = listOf("Sistem", "Terang", "Gelap")
    val scaleOptions = listOf("Kecil", "Sedang", "Besar")
    val accents = listOf(
        Triple("Biru", "Biru Modern", Color(0xFF2563EB)),
        Triple("Teal", "Ocean Teal", Color(0xFF0D9488)),
        Triple("Hijau", "Emerald", Color(0xFF059669)),
        Triple("Emas", "Kuning Emas", Color(0xFFD97706)),
        Triple("Ungu", "Original Purple", Color(0xFF4F46E5))
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tab Header
        item {
            Column {
                Text(
                    text = "Aksen & Tema Aplikasi",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Personalitaskan tampilan sesuai kenyamanan Anda",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        // Card 1: Theme Selector
        item {
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.DarkMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tema Aplikasi",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                themes.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updateTheme(theme) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (currentTheme == theme),
                            onClick = { viewModel.updateTheme(theme) },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = theme,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Card 3: Custom Accent Palette Color Selector
        item {
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Aksen Warna Utama",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(accents) { (key, name, color) ->
                        val isSelected = currentAccent == key
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { viewModel.updateAccentColor(key) }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = key,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Card 4: Penyesuaian Ukuran
        item {
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.TextFormat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Penyesuaian Ukuran",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Ukuran Teks (Font)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    scaleOptions.forEach { scale ->
                        val isSelected = localFontScale == scale
                        Button(
                            onClick = { localFontScale = scale },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(scale, fontSize = 12.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Ukuran Tampilan (Elemen UI)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    scaleOptions.forEach { scale ->
                        val isSelected = localDisplayScale == scale
                        Button(
                            onClick = { localDisplayScale = scale },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(scale, fontSize = 12.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.updateFontScale(localFontScale)
                        viewModel.updateDisplayScale(localDisplayScale)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isScaleChanged,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Terapkan", color = if (isScaleChanged) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Card 4.5: Panduan Penggunaan
        item {
            var isGuideExpanded by remember { mutableStateOf(false) }
            val isDark = MaterialTheme.colorScheme.background == Color(0xFF0B0C10)
            val containerBg = if (isDark) {
                Color(0xFF1E2030).copy(alpha = 0.55f)
            } else {
                Color.White
            }
            val borderCol = if (isDark) {
                Color.White.copy(alpha = 0.10f)
            } else {
                Color(0xFFE2E8F0)
            }
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = containerBg),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isGuideExpanded = !isGuideExpanded }
                    .border(0.75.dp, borderCol, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Panduan Penggunaan",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = if (isGuideExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Expand/Collapse",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isGuideExpanded) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            GuideItem(
                                icon = Icons.Filled.AddCircle,
                                title = "Catat Transaksi",
                                desc = "Gunakan tombol + di halaman utama (Beranda) untuk mencatat Pemasukan atau Pengeluaran baru."
                            )
                            GuideItem(
                                icon = Icons.Filled.Insights,
                                title = "Pantau Grafik",
                                desc = "Di tab Grafik (icon bar chart), Anda dapat memantau perbandingan pemasukan bulanan Anda dengan mudah."
                            )
                            GuideItem(
                                icon = Icons.Filled.PictureAsPdf,
                                title = "Ekspor PDF",
                                desc = "Gunakan tab Laporan (PDF) untuk mencetak atau mengekspor catatan keuangan Anda per bulan secara rapi."
                            )
                            GuideItem(
                                icon = Icons.Filled.Delete,
                                title = "Hapus Transaksi",
                                desc = "Pada daftar transaksi terbaru di Beranda, klik tombol tong sampah berwarna merah pada item untuk menghapusnya."
                            )
                        }
                    }
                }
            }
        }

        // Card 5: Data Management & Reset
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.DeleteForever,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Manajemen Data",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tindakan di bawah ini tidak dapat dibatalkan. Semua catatan keuangan terenkripsi Anda akan dihapus secara permanen.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Kosongkan Semua Catatan", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Decorative Credits & Icon
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = "Catatan saku v1.2",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Text(
                    text = "Aplikasi Keuangan Pribadi Terenkripsi",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Hapus Semua Data?", fontWeight = FontWeight.Bold) },
            text = { Text("Semua transaksi masuk dan keluar Anda akan dihapus secara permanen dari penyimpanan terenkripsi perangkat ini.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text("Ya, Hapus", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun GuideItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

// ---------------- KEAMANAN TAB ----------------

@Composable
fun KeamananTab(viewModel: FinanceViewModel) {
    val context = LocalContext.current
    val securitySettings by viewModel.securitySettings.collectAsStateWithLifecycle()
    var pinText by remember { mutableStateOf("") }
    var showSetupDialog by remember { mutableStateOf(false) }
    var showDisableConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core Branding Title
        item {
            Column {
                Text(
                    text = "Keamanan & Privasi Data",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Konfigurasi enkripsi hardware-backed KeyStore",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        // Encryption Status Banner (Hardware-backed AES proof)
        item {
            val isDark = MaterialTheme.colorScheme.background == Color(0xFF0B0C10)
            val bannerBg = if (isDark) Color(0xFF1E2135).copy(alpha = 0.65f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            val titleColor = if (isDark) Color.White else MaterialTheme.colorScheme.primary
            val descColor = if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            val borderCol = if (isDark) Color.White.copy(alpha = 0.10f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().border(0.75.dp, borderCol, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = bannerBg
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4F46E5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Shield,
                            contentDescription = "Protected",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Enkripsi AES-256 Aktif",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = titleColor
                        )
                        Text(
                            text = "Seluruh field judul, nominal, kategori, dan memo disimpan dalam database local sebagai string acak acipher text. Hanya bisa dibuka oleh kunci rahasia perangkat Anda.",
                            fontSize = 11.sp,
                            color = descColor
                        )
                    }
                }
            }
        }

        // Toggle PIN Lock system
        item {
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kunci Aplikasi (PIN Paascod)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Minta PIN keamanan setiap kali aplikasi Pencatat Uang dibuka.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    Switch(
                        checked = securitySettings?.isPasscodeEnabled == true,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                showSetupDialog = true
                            } else {
                                showDisableConfirm = true
                            }
                        },
                        modifier = Modifier.testTag("pin_lock_switch")
                    )
                }

                if (securitySettings?.isPasscodeEnabled == true) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.LockOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Status PIN: AKTIF (PIN 4 Digit Terenkripsi)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Educational note explaining private database structure
        item {
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Text(
                    text = "Cara Kerja Privasi Sepenuhnya",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "1. Tidak ada server cloud: Kunci enkripsi di-generate langsung di dalam hardware prosesor (TEE/SE) menggunakan Android KeyStore API.\n" +
                           "2. Kebal Ekstraksi: Jika handphone Anda hilang atau data folder di-root, database SQLite yang ditinggalkan berisi data biner terenkripsi yang mustahil dibaca tanpa kunci hardware hardware-bound.\n" +
                           "3. Kontrol Mandiri: Anda dapat menghapus atau mengekspor laporan keuangan secara offline langsung sesuai keinginan Anda.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )
            }
        }

        // Clear All Data
        item {
            var showClearConfirm by remember { mutableStateOf(false) }

            Button(
                onClick = { showClearConfirm = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("clear_all_data_button")
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Hapus Seluruh Data Aplikasi")
            }

            if (showClearConfirm) {
                AlertDialog(
                    onDismissRequest = { showClearConfirm = false },
                    title = { Text("Hapus Semua Data?") },
                    text = { Text("Tindakan ini akan menghapus semua catatan keuangan secara permanen dari perangkat ini. Tindakan ini tidak dapat dibatalkan.") },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.clearAllData()
                            showClearConfirm = false
                            Toast.makeText(context, "Semua data berhasil dibersihkan.", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Ya, Hapus Semua", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearConfirm = false }) {
                            Text("Batal")
                        }
                    }
                )
            }
        }
    }

    // Set PIN Dialog
    if (showSetupDialog) {
        Dialog(onDismissRequest = { showSetupDialog = false }) {
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                cornerRadius = 24.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Buat PIN Baru",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Masukkan 4 digit PIN angka untuk mengunci aplikasi.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = pinText,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                pinText = it
                            }
                        },
                        label = { Text("Sandi PIN 4-Angka") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pin_setup_input")
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            showSetupDialog = false
                            pinText = ""
                        }) {
                            Text("Batal")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (pinText.length == 4) {
                                    viewModel.setupPasscode(pinText)
                                    showSetupDialog = false
                                    pinText = ""
                                    Toast.makeText(context, "Sandi PIN berhasil diaktifkan!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "PIN harus berupa 4 digit angka.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("pin_setup_save")
                        ) {
                            Text("Simpan PIN")
                        }
                    }
                }
            }
        }
    }

    // Disable PIN Dialog
    if (showDisableConfirm) {
        AlertDialog(
            onDismissRequest = { showDisableConfirm = false },
            title = { Text("Nonaktifkan Pengunci PIN?") },
            text = { Text("Apakah Anda yakin ingin melepas kunci pengaman? Data Anda akan tetap terenkripsi secara hardware, namun aplikasi dapat diakses tanpa PIN.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.disablePasscode()
                    showDisableConfirm = false
                    Toast.makeText(context, "PIN Pengunci dinonaktifkan.", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Ya, Lepas PIN", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

// ---------------- LOCK SCREEN OVERLAY ----------------

@Composable
fun LockOverlay(
    onUnlock: (String) -> Unit,
    isSetupMode: Boolean
) {
    var pinText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0B0C10), Color(0xFF1E1E2F))
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "🔒 KeyStore Encrypted",
                tint = Color(0xFF818CF8),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "DATABASE TERKUNCI",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Silakan masukkan PIN 4 digit untuk medekripsi data lokal Anda.",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Pin Visual Dots Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val active = i < pinText.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (active) Color(0xFF818CF8) else Color.White.copy(alpha = 0.2f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Easy custom on-screen keypad to avoid keyboard annoyities
            NumericKeypad(
                onNumberPress = { num ->
                    if (pinText.length < 4) {
                        pinText += num
                        if (pinText.length == 4) {
                            onUnlock(pinText)
                            pinText = "" // Redo clear
                        }
                    }
                },
                onBackspace = {
                    if (pinText.isNotEmpty()) {
                        pinText = pinText.dropLast(1)
                    }
                }
            )
        }
    }
}

@Composable
fun NumericKeypad(
    onNumberPress: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val buttons = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        buttons.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { label ->
                    if (label.isEmpty()) {
                        Spacer(modifier = Modifier.size(64.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable {
                                    if (label == "⌫") {
                                        onBackspace()
                                    } else {
                                        onNumberPress(label)
                                    }
                                }
                                .testTag("keypad_$label"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------- ADD TRANSACTION DIALOG ----------------

@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, category: String, note: String, isIncome: Boolean, paymentMethod: String, customTimestamp: Long) -> Unit
) {
    var isIncome by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val expenseCategories = listOf("Makanan & Minuman", "Transportasi", "Belanja Bulanan", "Keperluan Rumah", "Hiburan", "Pendidikan", "Kesehatan", "Lainnya")
    val incomeCategories = listOf("Gaji & Upah", "Investasi", "Bonus & Kas", "Transfer Masuk", "Lainnya")

    var selectedCategory by remember(isIncome) {
        mutableStateOf(if (isIncome) incomeCategories.first() else expenseCategories.first())
    }

    var categoryExpanded by remember { mutableStateOf(false) }

    val paymentMethods = listOf("Tunai", "Transfer Bank", "Kartu Kredit/Debit", "E-Wallet (GoPay/Dana/OVO)", "Lainnya")
    var selectedPaymentMethod by remember { mutableStateOf(paymentMethods.first()) }
    var paymentMethodExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        GlassmorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("add_transaction_dialog"),
            cornerRadius = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Tambah Catatan",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // Type Tab Row selector (Income vs Expense)
                val isDark = MaterialTheme.colorScheme.background == Color(0xFF0B0C10)
                val expenseActiveBg = if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2)
                val expenseActiveText = if (isDark) Color(0xFFFCA5A5) else Color(0xFFEF4444)
                val incomeActiveBg = if (isDark) Color(0xFF064E3B) else Color(0xFFE6F7F0)
                val incomeActiveText = if (isDark) Color(0xFFA7F3D0) else Color(0xFF10B981)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isIncome) expenseActiveBg else Color.Transparent)
                            .clickable { isIncome = false }
                            .padding(vertical = 10.dp)
                            .testTag("type_expense"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Pengeluaran",
                            color = if (!isIncome) expenseActiveText else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isIncome) incomeActiveBg else Color.Transparent)
                            .clickable { isIncome = true }
                            .padding(vertical = 10.dp)
                            .testTag("type_income"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Pemasukan",
                            color = if (isIncome) incomeActiveText else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Nominal input (Amount of transaction)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() || char == '.' }) {
                                amountStr = it
                            }
                        },
                        label = { Text("Jumlah Nominal (Rp) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        prefix = { Text("Rp ") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("amount_input")
                    )
                }

                // Description Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Deskripsi Singkat (Opsional)") },
                    placeholder = { Text("misal: Nasi Padang, Gaji Bulanan") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("title_input")
                )

                // Category dropdown picker
                @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategori") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("category_selector")
                    )

                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        val activeCategories = if (isIncome) incomeCategories else expenseCategories
                        activeCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Payment Method dropdown picker
                @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
                ExposedDropdownMenuBox(
                    expanded = paymentMethodExpanded,
                    onExpandedChange = { paymentMethodExpanded = !paymentMethodExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedPaymentMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Metode Pembayaran *") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentMethodExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("payment_method_selector")
                    )

                    ExposedDropdownMenu(
                        expanded = paymentMethodExpanded,
                        onDismissRequest = { paymentMethodExpanded = false }
                    ) {
                        paymentMethods.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method) },
                                onClick = {
                                    selectedPaymentMethod = method
                                    paymentMethodExpanded = false
                                }
                            )
                        }
                    }
                }

                // Extra Memopadh Notes input
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan Tambahan / Memo (Opsional)") },
                    placeholder = { Text("misal: Biaya bulanan kos") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_input")
                )

                // Buttons action row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amount = amountStr.toDoubleOrNull() ?: 0.0
                            if (amount > 0.0) {
                                val finalTitle = title.trim().ifBlank {
                                    val genericCategory = selectedCategory
                                    if (isIncome) "Pemasukan ($genericCategory)" else "Pengeluaran ($genericCategory)"
                                }
                                onSave(finalTitle, amount, selectedCategory, note, isIncome, selectedPaymentMethod, System.currentTimeMillis())
                            }
                        },
                        modifier = Modifier.testTag("save_transaction_button")
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

@Composable
fun QuickStatsRow(transactions: List<Transaction>) {
    if (transactions.isEmpty()) return
    
    val totalCount = transactions.size
    val incomeCount = transactions.count { it.isIncome }
    val expenseCount = totalCount - incomeCount
    
    // Most used payment method
    val paymentMethodCounts = transactions.groupBy { it.paymentMethod }.mapValues { it.value.size }
    val favPaymentMethod = paymentMethodCounts.maxByOrNull { it.value }?.key ?: "-"
    
    // Average transaction amount
    val totalExpenseValue = transactions.filter { !it.isIncome }.sumOf { it.amount }
    val averageExpense = if (expenseCount > 0) totalExpenseValue / expenseCount else 0.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Statistik Bulan Ini",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // First mini-card: Activity counters
            GlassmorphicCard(
                modifier = Modifier.weight(1f),
                cornerRadius = 20.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Analytics,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Frekuensi", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$totalCount Transaksi",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$incomeCount masuk • $expenseCount keluar",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }

            // Second mini-card: Payment method favorite
            GlassmorphicCard(
                modifier = Modifier.weight(1f),
                cornerRadius = 20.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Payment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Metode Favorit", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = favPaymentMethod,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val count = paymentMethodCounts[favPaymentMethod] ?: 0
                Text(
                    text = "Digunakan sebanyak $count kali",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Third mini-card: Average expense
            GlassmorphicCard(
                modifier = Modifier.weight(1f),
                cornerRadius = 20.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Calculate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Rata-rata Belanja", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatRupiahSimple(averageExpense),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Per transaksi keluar",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

// ---------------- LOCAL FORMATTING HELPERS ----------------

fun formatRupiah(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return format.format(amount).replace("Rp", "Rp ").replace(",00", "")
}

fun formatRupiahSimple(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))
    return "Rp ${formatter.format(amount)}"
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
    return sdf.format(Date(timestamp))
}

fun formatShortDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yy", Locale("id", "ID"))
    return sdf.format(Date(timestamp))
}

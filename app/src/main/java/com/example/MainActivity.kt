package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.VtuViewModel
import com.example.viewmodel.VtuViewModelFactory
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Init room components
        val database = VtuDatabase.getDatabase(this)
        val repository = VtuRepository(database.vtuDao())
        val viewModelFactory = VtuViewModelFactory(repository)

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold")
                ) { innerPadding ->
                    HomeScreen(
                        factory = viewModelFactory,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    factory: VtuViewModelFactory,
    modifier: Modifier = Modifier
) {
    val viewModel: VtuViewModel = viewModel(factory = factory)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Database state flows
    val wallet by viewModel.wallet.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val beneficiaries by viewModel.beneficiaries.collectAsStateWithLifecycle()

    // Form settings state flows
    val selectedService by viewModel.selectedService.collectAsStateWithLifecycle()
    val recipientNumber by viewModel.recipientNumber.collectAsStateWithLifecycle()
    val selectedProvider by viewModel.selectedProvider.collectAsStateWithLifecycle()
    val inputAmount by viewModel.inputAmount.collectAsStateWithLifecycle()
    val selectedCablePackage by viewModel.selectedCablePackage.collectAsStateWithLifecycle()
    val selectedDataPlan by viewModel.selectedDataPlan.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    // View interaction states
    var isWalletHidden by remember { mutableStateOf(false) }
    var activeHistoryTab by remember { mutableStateOf("All") }
    var showFundWalletDialog by remember { mutableStateOf(false) }
    var fundAmountInput by remember { mutableStateOf("") }
    var fundSourceInput by remember { mutableStateOf("GTBank Transfer") }

    // Beneficiary Form Dialogue states
    var showAddBeneficiaryDialog by remember { mutableStateOf(false) }
    var saveRecipientChecked by remember { mutableStateOf(false) }

    // Receipt Dialog state
    var showReceiptDialog by remember { mutableStateOf(false) }
    var activeReceiptTx by remember { mutableStateOf<Transaction?>(null) }
    var generatedTokenPin by remember { mutableStateOf("") }

    // Filter list helper
    val filteredTransactions = remember(transactions, activeHistoryTab) {
        if (activeHistoryTab == "All") {
            transactions
        } else {
            transactions.filter {
                if (activeHistoryTab == "Data Bundle") {
                    it.serviceType == "Data"
                } else {
                    it.serviceType.equals(activeHistoryTab, ignoreCase = true)
                }
            }
        }
    }

    val currencyFormatter = remember { DecimalFormat("#,##0.00") }
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    // Standard Quick Amount Buttons
    val quickAmounts = listOf("200", "500", "1000", "2000", "5000", "10000")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header spacing
            item { Spacer(modifier = Modifier.height(12.dp)) }

            // Brand Header Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_vtu_logo_1781688737019),
                            contentDescription = "VTU Pay Logo",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Column {
                            Text(
                                text = "VTU Pay",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Secure Micro-Payments",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Network status indicator badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00C853))
                            )
                            Text(
                                text = "Server Online",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Wallet Balance Hero Card (combines generated background and premium design)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { isWalletHidden = !isWalletHidden }
                ) {
                    // Decorative Abstract Background Banner
                    Image(
                        painter = painterResource(id = R.drawable.img_vtu_banner_1781688755053),
                        contentDescription = "Wallet Graphic",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.28f
                    )

                    // Card Content Overlay
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "💳",
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = "AVAILABLE BALANCE",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        letterSpacing = 1.2.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }

                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = if (isWalletHidden) "Show Balance" else "Hide Balance",
                                tint = if (isWalletHidden) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Big Balance Text Display
                        AnimatedContent(
                            targetState = isWalletHidden,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "balanceAnim"
                        ) { hidden ->
                            if (hidden) {
                                Text(
                                    text = "₦ • • • • • •",
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 32.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                Text(
                                    text = "₦${currencyFormatter.format(wallet.balance)}",
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.SansSerif,
                                        fontSize = 32.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("wallet_balance_text")
                                )
                            }
                        }

                        // Bottom Actions (Fund Wallet, Add Bene)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Wallet Ref: VTU-U-${wallet.id}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )

                            Button(
                                onClick = { showFundWalletDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("fund_wallet_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Text(
                                        text = "Fund Wallet",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Services Selection Segment Category Tabs
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "VTU Billing Services",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Airtime", "Data", "Cable TV", "Electricity").forEach { serviceName ->
                            val isSelected = selectedService == serviceName
                            val emoji = when (serviceName) {
                                "Airtime" -> "📱"
                                "Data" -> "📶"
                                "Cable TV" -> "📺"
                                "Electricity" -> "⚡"
                                else -> "💳"
                            }

                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectService(serviceName) },
                                label = { Text(text = "$emoji $serviceName") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(40.dp)
                                    .testTag("vtu_service_${serviceName.lowercase().replace(" ", "_")}")
                            )
                        }
                    }
                }
            }

            // Interactive Form inputs card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = when (selectedService) {
                                "Airtime" -> "Top up Airtime Instantly"
                                "Data" -> "Purchase High-Speed Internet Bundle"
                                "Cable TV" -> "Renew Cable Television"
                                else -> "Generate Prepaid Power Token"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Beneficiaries horizontal list helper if any exist
                        val serviceBeneficiaries = beneficiaries.filter { it.type == selectedService }
                        if (serviceBeneficiaries.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Quick Select Saved Beneficiary",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(serviceBeneficiaries) { bene ->
                                        SuggestionChip(
                                            onClick = {
                                                viewModel.updateRecipient(bene.number)
                                                viewModel.updateProvider(bene.provider)
                                            },
                                            label = {
                                                Column {
                                                    Text(
                                                        text = bene.alias,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                                    )
                                                    Text(
                                                        text = "${bene.provider} (${bene.number.takeLast(4)})",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontSize = 9.sp
                                                    )
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Provider Select Segment
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Select Provider:",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            // Provider Picker dialog / row
                            val providers = when (selectedService) {
                                "Airtime", "Data" -> listOf("MTN", "Airtel", "Glo", "9mobile")
                                "Cable TV" -> listOf("DSTV", "Gotv", "Startimes")
                                else -> listOf("AEDC (Abuja)", "EKEDC (Eko)", "IKEDC (Ikeja)", "KEDCO (Kano)", "IBEDC (Ibadan)")
                            }

                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                providers.forEach { prov ->
                                    val isProvSelected = selectedProvider == prov || selectedProvider.startsWith(prov.take(3))
                                    ElevatedFilterChip(
                                        selected = isProvSelected,
                                        onClick = { viewModel.updateProvider(prov) },
                                        label = { Text(text = prov, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.elevatedFilterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            selectedLabelColor = MaterialTheme.colorScheme.primary
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }

                        // Destination Recipient Input Layout
                        val recipientLabel = when (selectedService) {
                            "Airtime", "Data" -> "Phone Number"
                            "Cable TV" -> "SmartCard / IUC ID"
                            else -> "Meter ID Number"
                        }

                        OutlinedTextField(
                            value = recipientNumber,
                            onValueChange = { viewModel.updateRecipient(it) },
                            label = { Text(recipientLabel) },
                            placeholder = {
                                Text(
                                    when (selectedService) {
                                        "Airtime", "Data" -> "e.g. 08012345678"
                                        "Cable TV" -> "e.g. 1029384756"
                                        else -> "e.g. 45293817293"
                                    }
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (selectedService) {
                                        "Airtime", "Data" -> Icons.Filled.Person
                                        "Cable TV" -> Icons.Filled.Settings
                                        else -> Icons.Filled.Warning
                                    },
                                    contentDescription = null
                                )
                            },
                            trailingIcon = {
                                if (recipientNumber.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.updateRecipient("") }) {
                                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recipient_input")
                        )

                        // Custom Bundle Lists/Packages Dropdown Selectors
                        if (selectedService == "Data") {
                            val dataPlans = viewModel.getDataPlansForProvider(selectedProvider)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Select Data Bundle Plan:",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                dataPlans.forEach { plan ->
                                    val isPlanSelected = selectedDataPlan == plan
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isPlanSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                else MaterialTheme.colorScheme.surface
                                            )
                                            .border(
                                                1.dp,
                                                if (isPlanSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { viewModel.updateDataPlan(plan) }
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            RadioButton(
                                                selected = isPlanSelected,
                                                onClick = { viewModel.updateDataPlan(plan) }
                                            )
                                            Text(
                                                text = plan.substringBefore(" ("),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = "₦" + plan.substringAfter("(₦").substringBefore(")"),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        } else if (selectedService == "Cable TV") {
                            val tvPlans = viewModel.getTvPackagesForProvider(selectedProvider)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Select Subscription Bouquet:",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                tvPlans.forEach { tvPackage ->
                                    val fullPkgName = "$selectedProvider $tvPackage"
                                    val isTvSelected = selectedCablePackage.contains(tvPackage.substringBefore(" ("))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isTvSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                else MaterialTheme.colorScheme.surface
                                            )
                                            .border(
                                                1.dp,
                                                if (isTvSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { viewModel.updateCablePackage(fullPkgName) }
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            RadioButton(
                                                selected = isTvSelected,
                                                onClick = { viewModel.updateCablePackage(fullPkgName) }
                                            )
                                            Text(
                                                text = tvPackage.substringBefore(" ("),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = "₦" + tvPackage.substringAfter("(₦").substringBefore(")"),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Portal Convenience Fee:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "₦100.00",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        } else if (selectedService == "Electricity" || selectedService == "Airtime") {
                            // Manual Amount input for Airtime / Electricity
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Enter Top Up Amount:",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                OutlinedTextField(
                                    value = inputAmount,
                                    onValueChange = { viewModel.updateAmount(it) },
                                    label = { Text("Amount (₦)") },
                                    leadingIcon = {
                                        Text(
                                            text = "₦",
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("amount_input")
                                )

                                // Quick Amount Chips row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    quickAmounts.forEach { qAmt ->
                                        SuggestionChip(
                                            onClick = { viewModel.updateAmount(qAmt) },
                                            label = { Text("₦$qAmt") },
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }

                                if (selectedService == "Electricity") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Meter Token Printing Fee:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = "₦100.00",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Save Beneficiary Checkbox Option
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = saveRecipientChecked,
                                onCheckedChange = { saveRecipientChecked = it }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = "Save details as beneficiary",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Auto-save details for rapid speed checkout",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }

                        // Main action submit button
                        Button(
                            onClick = {
                                viewModel.executeTransaction(
                                    onSuccess = { tx ->
                                        activeReceiptTx = tx
                                        showReceiptDialog = true
                                        // Auto-generate token if Cable TV or Electricity
                                        generatedTokenPin = when (selectedService) {
                                            "Electricity" -> {
                                                // Generate 20 digit electrical prepaid pin
                                                val r = java.util.Random()
                                                "${1000 + r.nextInt(9000)}-${1000 + r.nextInt(9000)}-${1000 + r.nextInt(9000)}-${1000 + r.nextInt(9000)}-${1000 + r.nextInt(9000)}"
                                            }
                                            "Cable TV" -> {
                                                // Generate 12 digit cable pin code
                                                val r = java.util.Random()
                                                "${1000 + r.nextInt(9000)}-${1000 + r.nextInt(9000)}-${1000 + r.nextInt(9000)}"
                                            }
                                            else -> ""
                                        }

                                        // Save beneficiary if checked
                                        if (saveRecipientChecked) {
                                            viewModel.saveBeneficiary(
                                                name = "My $selectedProvider ${tx.serviceType}",
                                                alias = "${tx.provider} (${tx.recipient.takeLast(4)})",
                                                number = tx.recipient,
                                                type = tx.serviceType,
                                                provider = tx.provider
                                            )
                                        }

                                        Toast.makeText(context, "Transaction successfully processed!", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("purchase_submit_button")
                        ) {
                            Text(
                                text = "Proceed to Pay Securely",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            // Transaction History Module (Header and Log search)
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Transaction Records",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = "${filteredTransactions.size} logs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }

                    // Search input bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Search logs by Recipient, Ref or Provider...", fontSize = 13.sp) },
                        leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = "Search") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    )

                    // Historic log filtering categories chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "Airtime", "Data Bundle", "Cable TV", "Electricity", "Wallet Fund").forEach { tab ->
                            val isTabSelected = activeHistoryTab == tab
                            SuggestionChip(
                                onClick = { activeHistoryTab = tab },
                                label = { Text(tab, fontSize = 11.sp) },
                                border = if (isTabSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // List of filtered historic transactions
            if (filteredTransactions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.List,
                            contentDescription = "Empty History",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No recorded transactions found",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Complete your first utility bill or top up transaction above to generate records.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                items(filteredTransactions, key = { it.id }) { tx ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                activeReceiptTx = tx
                                generatedTokenPin = when (tx.serviceType) {
                                    "Electricity" -> "5812-4019-2183-1293-8419"
                                    "Cable TV" -> "4912-8812-9023"
                                    else -> ""
                                }
                                showReceiptDialog = true
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Service styled rounded logo icon wrapping Emojis!
                                val emojiSymbol = when (tx.serviceType) {
                                    "Wallet Fund" -> "💰"
                                    "Airtime" -> "📱"
                                    "Data" -> "📶"
                                    "Cable TV" -> "📺"
                                    else -> "⚡"
                                }

                                val iconBg = when (tx.serviceType) {
                                    "Wallet Fund" -> Color(0xFF10B981).copy(alpha = 0.15f)
                                    "Airtime" -> Color(0xFF00B0FF).copy(alpha = 0.15f)
                                    "Data" -> Color(0xFF7C4DFF).copy(alpha = 0.15f)
                                    "Cable TV" -> Color(0xFFE11D48).copy(alpha = 0.15f)
                                    else -> Color(0xFFEAB308).copy(alpha = 0.15f)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(iconBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = emojiSymbol,
                                        fontSize = 18.sp
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = if (tx.serviceType == "Wallet Fund") "Fund: ${tx.provider}"
                                        else "${tx.provider} ${tx.serviceType}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${tx.recipient} • ${dateFormatter.format(tx.timestamp)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val textSign = if (tx.serviceType == "Wallet Fund" || tx.status == "FAILED") "" else "-"
                                val valueColor = when (tx.status.uppercase(Locale.getDefault())) {
                                    "FAILED" -> Color(0xFFEF4444)
                                    "PENDING" -> Color(0xFFD97706)
                                    else -> {
                                        if (tx.serviceType == "Wallet Fund") Color(0xFF10B981)
                                        else MaterialTheme.colorScheme.onSurface
                                    }
                                }

                                Text(
                                    text = "$textSign₦${currencyFormatter.format(tx.amount)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = valueColor
                                )

                                val statusBgColor = when (tx.status.uppercase(Locale.getDefault())) {
                                    "SUCCESS" -> Color(0xFF10B981).copy(alpha = 0.12f)
                                    "PENDING" -> Color(0xFFF59E0B).copy(alpha = 0.12f)
                                    else -> Color(0xFFEF4444).copy(alpha = 0.12f)
                                }
                                val statusTextColor = when (tx.status.uppercase(Locale.getDefault())) {
                                    "SUCCESS" -> Color(0xFF10B981)
                                    "PENDING" -> Color(0xFFD97706)
                                    else -> Color(0xFFEF4444)
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = statusBgColor
                                ) {
                                    Text(
                                        text = tx.status,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        ),
                                        color = statusTextColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Spacing
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        // Fund Wallet Floating Dialog Sheet
        if (showFundWalletDialog) {
            Dialog(onDismissRequest = { showFundWalletDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Add Funds to Wallet",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = { showFundWalletDialog = false }) {
                                Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
                            }
                        }

                        Text(
                            text = "To fund your local wallet instantly, make a test bank transfer to the virtual account below or type an amount to fund simulatedly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        // Bank Account Info Column mockup
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Virtual Bank Name:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    Text(text = "Providus Bank (Demo)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Account Number:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(text = "9902381273", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString("9902381273"))
                                                Toast.makeText(context, "Virtual Account Number Copied!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Icon(imageVector = Icons.Filled.Share, contentDescription = "Copy Account Nunber", modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Account Name:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    Text(text = "VTU PAY - WALLET OWNER", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }

                        // Funding Manual Entry input
                        OutlinedTextField(
                            value = fundAmountInput,
                            onValueChange = { fundAmountInput = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Simulated Funding Amount") },
                            leadingIcon = {
                                Text(
                                    text = "₦",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = fundSourceInput,
                            onValueChange = { fundSourceInput = it },
                            label = { Text("Transfer Bank Name / Source") },
                            placeholder = { Text("GTBank, Zenith Bank, etc.") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                val amtNum = fundAmountInput.toDoubleOrNull() ?: 0.0
                                if (amtNum > 10.0) {
                                    viewModel.fundWallet(amtNum, fundSourceInput)
                                    showFundWalletDialog = false
                                    fundAmountInput = ""
                                    Toast.makeText(context, "Wallet successfully funded with ₦${currencyFormatter.format(amtNum)}!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please enter a funding amount greater than ₦10.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = "Simulate Credit Deposit",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }

        // Custom Styled Receipt Screen Dialog (displays details of successful payments)
        if (showReceiptDialog && activeReceiptTx != null) {
            val rx = activeReceiptTx!!
            Dialog(onDismissRequest = { showReceiptDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header Status Icon Stamp
                        val statusStampColor = when (rx.status.uppercase(Locale.getDefault())) {
                            "SUCCESS" -> Color(0xFF10B981)
                            "PENDING" -> Color(0xFFD97706)
                            else -> Color(0xFFEF4444)
                        }
                        val statusStampIcon = when (rx.status.uppercase(Locale.getDefault())) {
                            "SUCCESS" -> Icons.Filled.CheckCircle
                            "PENDING" -> Icons.Filled.Refresh
                            else -> Icons.Filled.Warning
                        }
                        val statusStampTitle = when (rx.status.uppercase(Locale.getDefault())) {
                            "SUCCESS" -> "Payment Successful"
                            "PENDING" -> "Payment Processing"
                            else -> "Transaction Failed"
                        }

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(statusStampColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = statusStampIcon,
                                tint = statusStampColor,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = statusStampTitle,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = statusStampColor
                            )
                            Text(
                                text = rx.serviceType + " Service Payment",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        // Big readable Transaction Amount
                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        Text(
                            text = "₦${currencyFormatter.format(rx.amount)}",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            fontSize = 32.sp
                        )

                        // If Electricity / Cable TV, show dynamic generated PIN token securely!
                        if (rx.status == "SUCCESS" && generatedTokenPin.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (rx.serviceType == "Electricity") "PREPAID METER PIN TOKEN" else "CABLE TV RECHARGE PIN",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = generatedTokenPin,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Button(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(generatedTokenPin))
                                            Toast.makeText(context, "Utility Pin Copied to Clipboard!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text(text = "Copy Pin Key", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }

                        // Transaction Info list details
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ReceiptDetailItem("Recipient Number", rx.recipient)
                            ReceiptDetailItem("Provider / Disco", rx.provider)
                            ReceiptDetailItem("Transaction ID", rx.reference)
                            ReceiptDetailItem("Timestamp", dateFormatter.format(rx.timestamp))
                            if (rx.fee > 0.0) {
                                ReceiptDetailItem("System Service Fee", "₦${currencyFormatter.format(rx.fee)}")
                            }
                            if (rx.details.isNotEmpty()) {
                                ReceiptDetailItem("Details", rx.details)
                            }
                        }

                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    showReceiptDialog = false
                                    // Prepopulate fields for dynamic replay transaction
                                    viewModel.selectService(rx.serviceType)
                                    viewModel.updateRecipient(rx.recipient)
                                    viewModel.updateProvider(rx.provider)
                                    viewModel.updateAmount(rx.amount.toString())
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Replay Pay", color = MaterialTheme.colorScheme.primary)
                            }

                            Button(
                                onClick = { showReceiptDialog = false },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Dismiss", color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptDetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

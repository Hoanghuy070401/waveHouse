package com.wavehouse.presentation.stock.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wavehouse.core.ui.components.WaveAppBar
import com.wavehouse.core.ui.components.searchAction
import com.wavehouse.core.ui.theme.ChartIn
import com.wavehouse.core.ui.theme.ChartOut
import com.wavehouse.core.ui.theme.PrimaryGreen
import com.wavehouse.core.ui.theme.StockLow
import com.wavehouse.core.utils.toDateString
import com.wavehouse.core.utils.toVndString
import com.wavehouse.core.utils.todayStartMillis
import com.wavehouse.domain.model.StockEntry
import com.wavehouse.domain.model.StockEntryType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.wavehouse.presentation.order.history.OrderHistoryCard
import com.wavehouse.core.utils.todayStartMillis
import com.wavehouse.core.utils.weekStartMillis
import com.wavehouse.core.utils.monthStartMillis
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

// ─── Design tokens ────────────────────────────────────────────────────────────
private val SurfaceBase         = Color(0xFFF4FBF1)
private val SurfaceContainerLow = Color(0xFFEFF6EC)
private val SurfaceContainerHigh = Color(0xFFDDE5DB)
private val SurfaceWarm         = Color(0xFFF5EFE0)   // kem — card doanh thu
private val OnSurface           = Color(0xFF171D17)
private val OnSurfaceVariant    = Color(0xFF3D4E39)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockHistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToOrder: (String) -> Unit = {},
    viewModel: StockHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var periodIndex by remember { mutableIntStateOf(0) }   // 0=Hôm nay, 1=Tuần này, 2=Tháng này

    Scaffold(
        containerColor = SurfaceBase,
        topBar = {
            WaveAppBar(
                title = "Lịch sử & Báo cáo",
                onBack = onNavigateBack,
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        
            val visibleOrders by remember(uiState.orders, searchQuery, periodIndex) {
                derivedStateOf {
                    val minDate = when (periodIndex) {
                        0 -> todayStartMillis()
                        1 -> weekStartMillis()
                        else -> monthStartMillis()
                    }
                    var list = uiState.orders.filter { it.createdAt >= minDate }
                    if (searchQuery.isNotBlank()) {
                        list = list.filter { it.id.contains(searchQuery, ignoreCase = true) }
                    }
                    list
                }
            }

            val visibleEntries by remember(uiState.entries, searchQuery, periodIndex) {
                derivedStateOf {
                    val minDate = when (periodIndex) {
                        0 -> todayStartMillis()
                        1 -> weekStartMillis()
                        else -> monthStartMillis()
                    }
                    var list = uiState.entries.filter { it.createdAt >= minDate }
                    if (searchQuery.isNotBlank()) {
                        list = list.filter {
                            it.id.contains(searchQuery, ignoreCase = true) ||
                            it.productName.contains(searchQuery, ignoreCase = true)
                        }
                    }
                    list
                }
            }

            // Scrollable content
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // ── Search bar ────────────────────────────────────────
                item {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }

                // ── Period filter pills ───────────────────────────────
                item {
                    PeriodFilterRow(
                        selected = periodIndex,
                        onSelected = { periodIndex = it },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // ── Category tabs ─────────────────────────────────────
                item {
                    CategoryTabRow(
                        selected = uiState.filterIndex,
                        onSelected = viewModel::setFilter,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // ── Summary Cards ──────────────────────────────────────────
                item {
                    StatsCardsRow(
                        orders = visibleOrders,
                        entries = visibleEntries,
                        filterIndex = uiState.filterIndex,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                item { Spacer(Modifier.height(20.dp)) }

                // ── Transactions list ──────────────────────────────────────
                when {
                    uiState.isLoading -> item {
                        Box(
                            Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator(color = PrimaryGreen) }
                    }

                    (uiState.filterIndex == 0 && visibleOrders.isEmpty()) || 
                    (uiState.filterIndex != 0 && visibleEntries.isEmpty()) -> item {
                        Box(
                            Modifier.fillMaxWidth().height(160.dp).padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("📋", fontSize = 40.sp)
                                Text("Chưa có giao dịch nào phù hợp", color = OnSurfaceVariant)
                            }
                        }
                    }

                    else -> {
                        if (uiState.filterIndex == 0) { // Bán Lẻ (Orders)
                            val groupedOrders = visibleOrders.groupBy {
                                val cal = java.util.Calendar.getInstance().apply { timeInMillis = it.createdAt }
                                "${cal.get(java.util.Calendar.DAY_OF_MONTH)}/${cal.get(java.util.Calendar.MONTH) + 1}/${cal.get(java.util.Calendar.YEAR)}"
                            }
                            groupedOrders.forEach { (header, dayOrders) ->
                                item(key = "ho-$header") {
                                    DayHeader(label = header, count = dayOrders.size, modifier = Modifier.padding(horizontal = 16.dp))
                                }
                                items(dayOrders, key = { "o-${it.id}" }) { order ->
                                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                                        OrderHistoryCard(
                                            order = order,
                                            onClick = { onNavigateToOrder(order.id) },
                                        )
                                    }
                                }
                            }
                        } else { // Nhập / Xuất
                            val groupedEntries = visibleEntries.groupBy {
                                val cal = java.util.Calendar.getInstance().apply { timeInMillis = it.createdAt }
                                "${cal.get(java.util.Calendar.DAY_OF_MONTH)}/${cal.get(java.util.Calendar.MONTH) + 1}/${cal.get(java.util.Calendar.YEAR)}"
                            }
                            groupedEntries.forEach { (header, dayEntries) ->
                                item(key = "he-$header") {
                                    DayHeader(label = header, count = dayEntries.size, modifier = Modifier.padding(horizontal = 16.dp))
                                }
                                items(dayEntries, key = { "e-${it.id}" }) { entry ->
                                    StockEntryCard(
                                        entry = entry,
                                        onClick = { onNavigateToDetail(entry.id) },
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Remove status footer from scrollable list
            } // END LazyColumn

            // ── Warehouse status footer (Pinned to bottom) ─────────────────
            Surface(
                color = SurfaceBase,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                WarehouseStatusCard(
                    inStockPercentage = uiState.inStockPercentage,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// HistoryHeader removed — replaced by WaveAppBar in Scaffold topBar

// ══════════════════════════════════════════════════════════════════════════════
// Search bar
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = Color.White,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.Search, null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("Tìm kiếm mã đơn hàng...", color = OnSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium)
                    inner()
                }
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Period filter pills — Hôm nay | Tuần này | Tháng này
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PeriodFilterRow(selected: Int, onSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    val labels = listOf("Hôm nay", "Tuần này", "Tháng này")
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        labels.forEachIndexed { i, label ->
            val active = selected == i
            Surface(
                onClick = { onSelected(i) },
                shape = CircleShape,
                color = if (active) Color(0xFF006D37) else SurfaceContainerHigh,
                contentColor = if (active) Color.White else OnSurfaceVariant
            ) {
                Text(
                    label,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Category tabs — Bán lẻ | Nhập kho | Xuất kho
// Active tab = white card elevated inside gray container
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CategoryTabRow(selected: Int, onSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    // filterIndex: 0=Tất cả→map to "Bán lẻ", 1=Nhập kho, 2=Xuất kho
    val tabs = listOf("Bán lẻ", "Nhập kho", "Xuất kho")
    // Map viewmodel filterIndex (0=all, 1=in, 2=out) → tab index (0,1,2)
    val tabIndex = when (selected) { 1 -> 1; 2 -> 2; else -> 0 }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceContainerLow,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            tabs.forEachIndexed { i, label ->
                val active = tabIndex == i
                Surface(
                    onClick = {
                        val vmIndex = when (i) { 1 -> 1; 2 -> 2; else -> 0 }
                        onSelected(vmIndex)
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (active) Color.White else Color.Transparent,
                    contentColor = if (active) PrimaryGreen else OnSurfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .wrapContentWidth(Alignment.CenterHorizontally),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Stats cards — 2 cards side by side
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StatsCardsRow(
    orders: List<com.wavehouse.domain.model.Order>,
    entries: List<StockEntry>,
    filterIndex: Int,
    modifier: Modifier = Modifier
) {
    val totalCount = if (filterIndex == 0) orders.size else entries.size
    val totalValue = if (filterIndex == 0) {
        orders.sumOf { it.paidAmount }
    } else {
        entries.sumOf { (it.unitCostPrice ?: 0.0) * it.quantity }
    }

    val (countLabel, valueLabel) = when (filterIndex) {
        1 -> "TỔNG LƯỢT NHẬP" to "TỔNG CHI PHÍ"
        2 -> "TỔNG XUẤT" to "HAO HỤT"
        else -> "TỔNG ĐƠN" to "DOANH THU"
    }

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // Left card — count (gray)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SurfaceContainerHigh,
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(countLabel, style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant, letterSpacing = 0.8.sp)
                Text(
                    text = "$totalCount",
                    fontWeight = FontWeight.Black,
                    fontSize = 34.sp,
                    color = PrimaryGreen
                )
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.KeyboardArrowUp, null, tint = PrimaryGreen,
                        modifier = Modifier.size(14.dp))
                    Text("+12% vs h.qua", style = MaterialTheme.typography.labelSmall,
                        color = PrimaryGreen)
                }
            }
        }

        // Right card — value (warm/cream)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SurfaceWarm,
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(valueLabel, style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF7B5E2A), letterSpacing = 0.8.sp)
                Text(
                    text = formatMillions(totalValue),
                    fontWeight = FontWeight.Black,
                    fontSize = 30.sp,
                    color = Color(0xFF5C3D0A)
                )
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.AccountBalanceWallet, null, tint = Color(0xFF7B5E2A),
                        modifier = Modifier.size(14.dp))
                    Text("VNĐ", style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF7B5E2A))
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Section header — title + "Xem tất cả" link
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = OnSurface)
        Text(
            "Xem tất cả",
            style = MaterialTheme.typography.labelLarge,
            color = PrimaryGreen,
            fontWeight = FontWeight.SemiBold,
            textDecoration = TextDecoration.Underline
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Day header
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun DayHeader(label: String, count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold, color = OnSurface)
        Surface(shape = CircleShape, color = SurfaceContainerHigh) {
            Text("$count",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Entry card — matches design: icon • title + subtitle • price + badge
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StockEntryCard(entry: StockEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val (iconVector, iconColor) = entryIconAndColor(entry.type)
    val isOut = !entry.type.isIncoming
    val value = (entry.unitCostPrice ?: 0.0) * entry.quantity
    val valueText = if (isOut) "- ${value.toVndString()}" else value.toVndString()
    val valueColor = if (isOut) ChartOut else OnSurface

    // Title: supplier name for IN, product name for others
    val title = when (entry.type) {
        StockEntryType.IN -> entry.supplierName?.takeIf { it.isNotBlank() }
            ?: entry.productName.ifBlank { "Nhập kho" }
        else -> entry.productName.ifBlank { entry.type.label }
    }

    // Subtitle: #ID · time · qty
    val shortId = "#${entry.type.name.take(3)}-${entry.id.takeLast(8).uppercase()}"
    val time = entry.createdAt.toHourMinute()
    val qtyStr = "${formatQty(entry.quantity)}${if (entry.productSku.isNotBlank()) " ${entry.productSku}" else " đv"}"
    val subtitle = "$shortId • $time • $qtyStr"

    val badgeLabel = when (entry.type) {
        StockEntryType.IN -> "XEM CHI TIẾT"
        else -> "CHI TIẾT"
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SurfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Product image or icon circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                if (!entry.productImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(entry.productImageUrl).crossfade(true).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(iconVector, null, tint = iconColor, modifier = Modifier.size(24.dp))
                }
            }

            // Title + subtitle
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text(subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }

            // Price + badge — stacked
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (value > 0) {
                    Text(valueText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = valueColor)
                }
                // Action badge — green outline pill
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen)
                ) {
                    Text(
                        badgeLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryGreen,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Warehouse status footer card
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun WarehouseStatusCard(inStockPercentage: Float, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SurfaceContainerLow,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(PrimaryGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.CheckCircle, null, tint = PrimaryGreen,
                    modifier = Modifier.size(24.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("Trạng thái kho", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, color = OnSurface)
                val percentageStr = (inStockPercentage * 100).toInt()
                Text("$percentageStr% mặt hàng còn hàng", style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant)
            }

            // Progress bar
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerHigh)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(inStockPercentage)
                        .clip(CircleShape)
                        .background(PrimaryGreen)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Helpers
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun entryIconAndColor(type: StockEntryType): Pair<ImageVector, Color> = when (type) {
    StockEntryType.IN        -> Icons.Filled.LocalShipping to ChartIn
    StockEntryType.OUT       -> Icons.Filled.ArrowUpward to ChartOut
    StockEntryType.ADJUST    -> Icons.Filled.Tune to StockLow
    StockEntryType.TRANSFER  -> Icons.Filled.SwapHoriz to MaterialTheme.colorScheme.primary
    StockEntryType.SHRINKAGE -> Icons.Filled.DeleteForever to ChartOut
}

private val timeFormatter = SimpleDateFormat("HH:mm", Locale("vi", "VN"))
private fun Long.toHourMinute(): String = timeFormatter.format(Date(this))

private fun List<StockEntry>.groupByDay(): List<Pair<String, List<StockEntry>>> {
    val today = todayStartMillis()
    val yesterday = today - 86_400_000L
    return groupBy { entry ->
        when {
            entry.createdAt >= today     -> "Hôm nay"
            entry.createdAt >= yesterday -> "Hôm qua"
            else                         -> entry.createdAt.toDateString()
        }
    }.toList()
}

private fun formatQty(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else "%.1f".format(value)

/** Format large numbers as "12.5M", "850K", etc. */
private fun formatMillions(value: Double): String = when {
    value >= 1_000_000 -> "${"%.1f".format(value / 1_000_000)}M"
    value >= 1_000     -> "${"%.0f".format(value / 1_000)}K"
    else               -> value.toVndString()
}

private fun sectionTitle(filterIndex: Int) = when (filterIndex) {
    1    -> "Lịch nhập kho gần đây"
    2    -> "Xuất kho gần đây"
    else -> "Giao dịch gần đây"
}

// BasicTextField typealias moved to imports section

package com.wavehouse.presentation.stock.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    viewModel: StockHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var periodIndex by remember { mutableIntStateOf(0) }   // 0=Hôm nay, 1=Tuần này, 2=Tháng này

    Scaffold(containerColor = SurfaceBase) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Header bar ────────────────────────────────────────────
            HistoryHeader(onBack = onNavigateBack)

            // Pre-compute filtered + grouped BEFORE LazyColumn (must be in @Composable scope)
            val visible by remember(uiState.entries, searchQuery) {
                derivedStateOf {
                    if (searchQuery.isBlank()) uiState.entries
                    else uiState.entries.filter {
                        it.productName.contains(searchQuery, ignoreCase = true) ||
                                it.id.contains(searchQuery, ignoreCase = true)
                    }
                }
            }
            val grouped by remember(visible) { derivedStateOf { visible.groupByDay() } }

            // Scrollable content
            LazyColumn(
                contentPadding = PaddingValues(bottom = 32.dp),
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

                // ── Stats cards row ───────────────────────────────────
                item {
                    StatsCardsRow(
                        entries = uiState.entries,
                        filterIndex = uiState.filterIndex,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(20.dp))
                }

                // ── Section header ────────────────────────────────────
                item {
                    SectionHeader(
                        title = sectionTitle(uiState.filterIndex),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                }

                // ── Loading / empty states ────────────────────────────
                when {
                    uiState.isLoading -> item {
                        Box(
                            Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator(color = PrimaryGreen) }
                    }

                    uiState.entries.isEmpty() -> item {
                        Box(
                            Modifier.fillMaxWidth().height(160.dp).padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("📋", fontSize = 40.sp)
                                Text("Chưa có giao dịch nào", color = OnSurfaceVariant)
                            }
                        }
                    }

                    else -> {
                        grouped.forEach { (header, dayEntries) ->
                            item(key = "h-$header") {
                                DayHeader(
                                    label = header,
                                    count = dayEntries.size,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                            items(dayEntries, key = { it.id }) { entry ->
                                StockEntryCard(
                                    entry = entry,
                                    onClick = { onNavigateToDetail(entry.id) },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                // ── Warehouse status footer ───────────────────────────
                item {
                    Spacer(Modifier.height(16.dp))
                    WarehouseStatusCard(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Header — hamburger + title + search icon (no back arrow per design)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun HistoryHeader(onBack: () -> Unit) {
    Surface(color = SurfaceBase, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back arrow (navigate-back exists in nav stack)
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại",
                    tint = PrimaryGreen, modifier = Modifier.size(22.dp))
            }
            Text(
                text = "Lịch sử & Báo cáo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
            IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Search, "Tìm kiếm", tint = PrimaryGreen,
                    modifier = Modifier.size(22.dp))
            }
        }
    }
}

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
private fun StatsCardsRow(entries: List<StockEntry>, filterIndex: Int, modifier: Modifier = Modifier) {
    val totalCount = entries.size
    val totalValue = entries.sumOf { (it.unitCostPrice ?: 0.0) * it.quantity }

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
            // Icon circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconVector, null, tint = iconColor, modifier = Modifier.size(24.dp))
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
private fun WarehouseStatusCard(modifier: Modifier = Modifier) {
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
                Text("92% mặt hàng còn hàng", style = MaterialTheme.typography.labelSmall,
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
                        .fillMaxWidth(0.92f)
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

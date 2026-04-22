package com.wavehouse.presentation.stock.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.wavehouse.core.ui.components.WaveAppBar
import com.wavehouse.core.ui.theme.ChartIn
import com.wavehouse.core.ui.theme.ChartOut
import com.wavehouse.core.ui.theme.StockLow
import com.wavehouse.core.utils.toDateTimeString
import com.wavehouse.core.utils.toVndString
import com.wavehouse.domain.model.StockEntry
import com.wavehouse.domain.model.StockEntryType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── Design tokens (The Organic Grid) ────────────────────────────────────────
private val SurfaceBase          = Color(0xFFF4FBF1)
private val SurfaceContainerLow  = Color(0xFFEFF6EC)
private val SurfaceContainerHigh = Color(0xFFDDE5DB)
private val OnSurface            = Color(0xFF171D17)
private val OnSurfaceVariant     = Color(0xFF3D4E39)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockEntryDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToOrder: (String) -> Unit = {},
    viewModel: StockEntryDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Redirect sang OrderDetail khi entry là SALE
    LaunchedEffect(Unit) {
        viewModel.navigateToOrder.collect { orderId ->
            onNavigateToOrder(orderId)
        }
    }

    Scaffold(
        topBar = {
            WaveAppBar(
                title = "Chi tiết giao dịch",
                onBack = onNavigateBack
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0)
    ) { padding ->
        val bgMod = Modifier.background(SurfaceBase)
        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxSize().then(bgMod).padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = ChartIn) }

            uiState.entry == null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    uiState.errorMessage ?: "Không tìm thấy giao dịch",
                    color = MaterialTheme.colorScheme.error
                )
            }

            else -> StockEntryDetailContent(
                entry = uiState.entry!!,
                onOpenOrder = { orderId -> viewModel.openSourceOrder(orderId) },
                modifier = Modifier
                    .fillMaxSize()
                    .then(bgMod)
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}

@Composable
private fun StockEntryDetailContent(
    entry: StockEntry,
    onOpenOrder: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val (icon, accent) = entry.typeIconAndColor()
    when {
        entry.type == StockEntryType.IN        -> StockInDetailContent(entry, icon, accent, modifier)
        entry.type == StockEntryType.SHRINKAGE -> StockLossDetailContent(entry, accent, modifier)
        entry.type == StockEntryType.OUT       -> StockLossDetailContent(entry, accent, modifier)
        entry.source == "SALE"                 -> SaleEntryRedirectContent(entry, accent, onOpenOrder, modifier)
        else                                   -> GenericStockEntryDetailContent(entry, icon, accent, modifier)
    }
}

@Composable
private fun StockInDetailContent(
    entry: StockEntry,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val totalValue = entry.displayValue()
    val supplierName = entry.supplierName?.takeIf { it.isNotBlank() }
    val qtyText = "+${formatQty(entry.quantity)}"

    Column(
        modifier = modifier.padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Title block ────────────────────────────────────────
        Text(
            "Chi tiết nhập hàng",
            style = MaterialTheme.typography.labelLarge,
            color = OnSurfaceVariant
        )
        Text(
            text = "Order ID #${entry.id.takeLast(6).uppercase()}",
            fontWeight = FontWeight.Black,
            fontSize = 28.sp,
            color = OnSurface
        )
        StatusChip(label = "● Đã nhập", color = accent)

        Spacer(Modifier.height(4.dp))

        // ── Supplier card ──────────────────────────────────────
        SupplierHighlightCard(
            supplierName = supplierName,
            supplierId = entry.supplierId,
            accent = accent
        )

        // ── Date card ──────────────────────────────────────────
        ScheduleHighlightCard(timestamp = entry.createdAt, accent = accent)

        // ── Product list ───────────────────────────────────────
        Text(
            "Danh sách sản phẩm",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
            color = OnSurface
        )

        StockInProductCard(
            productName = entry.productName,
            sku = entry.productSku,
            quantity = qtyText,
            unitCost = entry.unitCostPrice,
            accent = accent,
            icon = icon,
            imageUrl = entry.productImageUrl
        )

        // ── Note ───────────────────────────────────────────────
        if (!entry.note.isNullOrBlank()) {
            StockInNoteCard(entry.note!!)
        }

        // ── Summary bar ────────────────────────────────────────
        StockInSummaryCard(
            label = "Tổng cộng đơn hàng",
            valueText = totalValue?.toVndString() ?: "${formatQty(entry.quantity)} đv",
            paymentLabel = null  // StockEntry không có paymentMethod
        )
    }
}

/**
 * Entry được tạo tự động bởi POS khi bán hàng.
 * Không hiển thị như một phiếu xuất kho — thay vào đó hiển thị banner
 * và nút "Xem đơn hàng" để redirect sang OrderDetailScreen.
 */
@Composable
private fun SaleEntryRedirectContent(
    entry: StockEntry,
    accent: Color,
    onOpenOrder: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon + label
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Receipt,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(36.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Đây là giao dịch bán hàng",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "Mục này thuộc về đơn hàng POS, không phải phiếu xuất kho thủ công.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // Info tóm tắt
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoRow(
                    icon = Icons.Filled.Person,
                    label = "Người bán",
                    value = entry.createdByName.ifBlank { "—" },
                    accent = accent
                )
                InfoRow(
                    icon = Icons.Filled.Receipt,
                    label = "Sản phẩm",
                    value = "${entry.productName} · ${formatQty(entry.quantity)} đv",
                    accent = accent
                )
                InfoRow(
                    icon = Icons.Filled.Description,
                    label = "Ghi chú",
                    value = entry.note ?: "—",
                    accent = accent
                )
            }
        }

        // Nút CTA điều hướng
        val orderId = entry.orderId
        if (orderId != null) {
            Button(
                onClick = { onOpenOrder(orderId) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Xem chi tiết đơn hàng", fontWeight = FontWeight.SemiBold)
            }
        } else {
            // Entry cũ (trước khi thêm orderId) — hiển thông báo thay thế
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Đơn hàng này được tạo trước khi có tính năng liên kết. Vui lòng xem trong Lịch sử Đơn Hàng.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}


@Composable
private fun GenericStockEntryDetailContent(
    entry: StockEntry,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    // Show positive values with color coding instead of negative signs
    val qtyPrefix = if (entry.type.isIncoming) "+" else ""

    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusChip(label = "Đã xác nhận", color = accent)
            Text(
                entry.createdAt.toDateTimeString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = entry.displayTitle(),
                fontWeight = FontWeight.Black,
                fontSize = 26.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (entry.type == StockEntryType.SHRINKAGE && entry.shrinkageReason != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Lý do:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        entry.shrinkageReason.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = accent
                    )
                }
            }
            Text(
                text = "#${entry.id.takeLast(6).uppercase()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        InfoCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (entry.type == StockEntryType.IN && !entry.supplierName.isNullOrBlank()) {
                    InfoRow(
                        icon = Icons.Filled.Storefront,
                        label = "Nhà cung cấp",
                        value = entry.supplierName,
                        accent = accent
                    )
                }
                InfoRow(
                    icon = Icons.Filled.Person,
                    label = "Người thực hiện",
                    value = entry.createdByName.ifBlank { "—" },
                    accent = accent
                )
            }
        }

        val totalValue = entry.displayValue()
        if (totalValue != null) {
            InfoCard(containerColor = accent.copy(alpha = 0.10f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = entry.valueLabel(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = totalValue.toVndString(),
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        color = accent
                    )
                }
            }
        }

        if (!entry.note.isNullOrBlank()) {
            InfoCard {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    IconCircle(icon = Icons.Filled.Description, color = accent)
                    Column {
                        Text(
                            "Ghi chú",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            entry.note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Text(
            "Danh sách sản phẩm",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
        InfoCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        entry.productName.ifBlank { "Sản phẩm không xác định" },
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                    if (entry.productSku.isNotBlank()) {
                        Text(
                            "SKU ${entry.productSku}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (entry.unitCostPrice != null && entry.unitCostPrice > 0.0) {
                        Text(
                            "${entry.unitCostPrice.toVndString()} / đv",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "$qtyPrefix${formatQty(entry.quantity)}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = accent
                )
            }
        }
    }
}


// ══════════════════════════════════════════════════════════════════
// Chi tiết xuất kho / hao hụt — Shared Stitch layout
// ══════════════════════════════════════════════════════════════════

/**
 * Shared layout for OUT (manual stock-out) and SHRINKAGE entries.
 *
 * Both types carry a quantity deduction and a monetary cost; the only
 * differences are the title text, the value-card label, the qty label,
 * and whether a shrinkage-reason row is shown.
 */
@Composable
private fun StockLossDetailContent(
    entry: StockEntry,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val isShrinkage = entry.type == StockEntryType.SHRINKAGE
    val costPerUnit = entry.unitCostPrice ?: entry.macAfter ?: 0.0
    val lossValue   = costPerUnit * entry.quantity
    val hasCost     = costPerUnit > 0.0 && entry.quantity > 0.0
    val dateStr = remember(entry.createdAt) {
        SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN")).format(Date(entry.createdAt))
    }
    val titleText  = if (isShrinkage) "Chi tiết hao hụt" else "Chi tiết xuất kho"
    val valueLabel = if (isShrinkage) "TỔNG GIÁ TRỊ THẤT THOÁT" else "TỔNG GIÁ TRỊ XUẤT"
    val qtyLabel   = if (isShrinkage) "Hao hụt" else "Xuất"
    val qtyIcon    = if (isShrinkage) Icons.Filled.Warning else Icons.Filled.ArrowUpward

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Title block ────────────────────────────────────────────
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusChip(label = "✓ Đã xác nhận", color = Color(0xFFB8860B))
                    Text(
                        dateStr,
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant
                    )
                }
                Text(
                    titleText,
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp,
                    color = Color(0xFF006D37)
                )
                // Shrinkage-only: lý do
                if (isShrinkage && entry.shrinkageReason != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Lý do:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                        Text(
                            entry.shrinkageReason.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = accent,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
                // OUT-only: hiển thị ID phiếu xuất
                if (!isShrinkage) {
                    Text(
                        "#${entry.id.takeLast(6).uppercase()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant
                    )
                }
            }
        }

        // ── Staff card ─────────────────────────────────────────────
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF006D37).copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Person, null,
                        tint = Color(0xFF006D37),
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    "NGƯỜI THỰC HIỆN",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp
                )
                Text(
                    entry.createdByName.ifBlank { "—" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
            }
        }

        // ── Value / loss card ──────────────────────────────────────
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    valueLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    letterSpacing = 0.8.sp
                )
                if (hasCost) {
                    Text(
                        "-${lossValue.toVndString()}",
                        fontWeight = FontWeight.Black,
                        fontSize = 34.sp,
                        color = accent
                    )
                    val priceLabel = if (entry.unitCostPrice != null) "Giá nhập" else "Giá vốn (MAC)"
                    Text(
                        "$priceLabel: ${costPerUnit.toVndString()}/đv × ${formatQty(entry.quantity)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant
                    )
                } else {
                    Text(
                        "-${formatQty(entry.quantity)} đv",
                        fontWeight = FontWeight.Black,
                        fontSize = 34.sp,
                        color = accent
                    )
                    Text(
                        "Chưa có thông tin giá vốn",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant
                    )
                }
                // Ghi chú — embedded inside value card
                if (!entry.note.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Menu, null,
                                    tint = OnSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    "GHI CHÚ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.8.sp
                                )
                            }
                            Text(
                                entry.note,
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnSurface
                            )
                        }
                    }
                }
            }
        }

        // ── Product list header ────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Danh sách sản phẩm",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF006D37)
            )
            Surface(shape = CircleShape, color = SurfaceContainerHigh) {
                Text(
                    "1 item",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        }

        // ── Product row card ───────────────────────────────────────
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SurfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!entry.productImageUrl.isNullOrBlank()) {
                        coil3.compose.AsyncImage(
                            model = entry.productImageUrl,
                            contentDescription = entry.productName,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            entry.productName.takeIf { it.isNotBlank() }?.firstOrNull()?.uppercase() ?: "",
                            fontWeight = FontWeight.Bold, fontSize = 24.sp,
                            color = accent
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        entry.productName.ifBlank { "Sản phẩm" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurface
                    )
                    if (entry.productSku.isNotBlank()) {
                        Text(
                            "SKU: ${entry.productSku}",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant
                        )
                    }
                    if (hasCost) {
                        Text(
                            "Giá vốn: ${costPerUnit.toVndString()}/đv",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        "-${formatQty(entry.quantity)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                    Text(
                        qtyLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// Small UI building blocks
// ══════════════════════════════════════════════════════════════════

@Composable
private fun InfoCard(
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconCircle(icon = icon, color = accent)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun IconCircle(icon: ImageVector, color: Color, size: Int = 40) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size((size * 0.5).dp))
    }
}

@Composable
private fun StatusChip(label: String, color: Color) {
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.10f),
        contentColor = color,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            color = color.copy(alpha = 0.50f)
        ),
        modifier = Modifier.padding(0.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

// ══════════════════════════════════════════════════════════════════
// Entry-type presentation helpers
// ══════════════════════════════════════════════════════════════════

@Composable
private fun StockEntry.typeIconAndColor(): Pair<ImageVector, Color> = when (type) {
    StockEntryType.IN -> Icons.Filled.ArrowDownward to ChartIn
    StockEntryType.OUT -> Icons.Filled.ArrowUpward to ChartOut
    StockEntryType.ADJUST -> Icons.Filled.Tune to StockLow
    StockEntryType.TRANSFER -> Icons.Filled.SwapHoriz to MaterialTheme.colorScheme.primary
    StockEntryType.SHRINKAGE -> Icons.Filled.DeleteForever to MaterialTheme.colorScheme.error
}

private fun StockEntry.displayTitle(): String = when (type) {
    StockEntryType.IN -> "Chi tiết nhập kho"
    StockEntryType.OUT -> "Chi tiết xuất kho"
    StockEntryType.SHRINKAGE -> "Chi tiết hao hụt"
    StockEntryType.ADJUST -> "Chi tiết điều chỉnh"
    StockEntryType.TRANSFER -> "Chi tiết chuyển kho"
}

private fun StockEntry.valueLabel(): String = when (type) {
    StockEntryType.IN -> "Tổng giá trị nhập hàng"
    StockEntryType.OUT -> "Tổng giá trị xuất"
    StockEntryType.SHRINKAGE -> "Tổng giá trị thất thoát"
    else -> "Tổng giá trị"
}

/** Giá trị của phiếu:
 *  - IN/OUT/ADJUST: unitCostPrice × quantity (giá lô nhập)
 *  - SHRINKAGE: unitCostPrice (giá nhập sản phẩm) hoặc macAfter (giá vốn bình quân) × quantity
 *  Trả null nếu không có thông tin giá.
 */
private fun StockEntry.displayValue(): Double? {
    val priceEach = unitCostPrice ?: macAfter
    return if (priceEach != null && priceEach > 0.0) priceEach * quantity else null
}

private fun formatQty(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else "%.2f".format(value)

@Composable
private fun SupplierHighlightCard(supplierName: String?, supplierId: String?, accent: Color) {
    // Nền SurfaceContainerLow, không shadow — background shift
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = SurfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Uppercase gray label
                Text(
                    "NHÀ CUNG CẤP",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp
                )
                Text(
                    supplierName ?: "Chưa có nhà cung cấp",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
                if (supplierId != null) {
                    Text(
                        "Chuyên cung cấp rau củ quả hữu cơ tiêu chuẩn VietGAP từ cao nguyên Đà Lạt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
            // Icon cây/thực vật góc phải
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = accent.copy(alpha = 0.12f)
            ) {
                Icon(
                    Icons.Filled.Storefront, null,
                    tint = accent,
                    modifier = Modifier.padding(10.dp).size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ScheduleHighlightCard(timestamp: Long, accent: Color) {
    val fullDate = remember(timestamp) { timestamp.toFullDateString() }
    val dayMonth = remember(timestamp) { timestamp.toDayOfMonth() }  // "24/10"
    val year = remember(timestamp) { SimpleDateFormat("yyyy", locale).format(Date(timestamp)) }

    // Nền xanh đậm — khớp design
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF006D37),
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Calendar icon
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.18f)) {
                Icon(
                    Icons.Filled.CalendarMonth, null,
                    modifier = Modifier.padding(10.dp).size(20.dp)
                )
            }
            // Label + date
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Ngày nhập kho", style = MaterialTheme.typography.labelMedium)
                Text(
                    dayMonth,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp
                )
            }
            // Year badge
            Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.18f)) {
                Text(
                    year,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun StockInProductCard(
    productName: String,
    sku: String,
    quantity: String,
    unitCost: Double?,
    accent: Color,
    icon: ImageVector,
    imageUrl: String? = null
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = SurfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            // ── Image header — full width, ~140dp tall ─────────────────
            val context = LocalContext.current
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(accent.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    coil3.compose.AsyncImage(
                        model = coil3.request.ImageRequest.Builder(context)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = productName,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        productName.takeIf { it.isNotBlank() }?.firstOrNull()?.uppercase() ?: "",
                        fontWeight = FontWeight.Bold, fontSize = 64.sp,
                        color = accent,
                        style = MaterialTheme.typography.displayLarge
                    )
                }
            }

            // ── Product info ──────────────────────────────────────────
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tên sản phẩm + đơn giá
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        productName.ifBlank { "Sản phẩm không xác định" },
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurface
                    )
                    if (unitCost != null && unitCost > 0.0) {
                        Text(
                            "Đơn giá: ${unitCost.toVndString()} / ${if (sku.isNotBlank()) sku else "kg"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }

                // SỐ LƯỢNG + THÀNH TIỀN — 2 cột
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "SỐ LƯỢNG",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            quantity,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = accent
                        )
                    }
                    if (unitCost != null && unitCost > 0.0) {
                        val rawQty = quantity.trimStart('+').toDoubleOrNull() ?: 0.0
                        val total = unitCost * rawQty
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "THÀNH TIỀN",
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariant,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                total.toVndString(),
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = accent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StockInNoteCard(note: String) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFFFF3E6),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.7f)) {
                Icon(
                    Icons.Filled.Description, null,
                    tint = Color(0xFFFF8A4A),
                    modifier = Modifier.padding(10.dp).size(20.dp)
                )
            }
            Column {
                Text(
                    "GHI CHÚ",  // uppercase — khớp design
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8A4F31),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "\"$note\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5E331F),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun StockInSummaryCard(label: String, valueText: String, paymentLabel: String?) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF0B3B29),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Total row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
            Text(
                valueText,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                color = ChartIn  // xanh sáng — giá trị nổi bật
            )
            // Payment method row
            if (!paymentLabel.isNullOrBlank()) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "THANH TOÁN",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        paymentLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = ChartIn
                    )
                }
            }
        }
    }
}

@Composable
private fun LightStatusChip(label: String) {
    Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.18f),
        contentColor = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

private val locale = Locale("vi", "VN")
private val fullDateFormatter = SimpleDateFormat("dd/MM/yyyy", locale)
private val dayFormatter = SimpleDateFormat("dd/MM", locale)
private val timeFormatter = SimpleDateFormat("HH:mm", locale)

private fun Long.toFullDateString(): String = fullDateFormatter.format(Date(this))
private fun Long.toDayOfMonth(): String = dayFormatter.format(Date(this))
private fun Long.toHourMinuteString(): String = timeFormatter.format(Date(this))

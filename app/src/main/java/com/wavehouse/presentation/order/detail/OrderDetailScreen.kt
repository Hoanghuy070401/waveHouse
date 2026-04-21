package com.wavehouse.presentation.order.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.wavehouse.core.ui.theme.PrimaryGreen
import com.wavehouse.core.utils.toDateTimeString
import com.wavehouse.core.utils.toVndString
import com.wavehouse.domain.model.Order
import com.wavehouse.domain.model.OrderItem
import com.wavehouse.domain.model.OrderStatus

// ─── Design tokens (từ DESIGN.md "The Organic Grid") ────────────────────────
private val SurfaceBase           = Color(0xFFF4FBF1) // nền trang
private val SurfaceContainerLow   = Color(0xFFEFF6EC) // nền info cards
private val SurfaceContainerHigh  = Color(0xFFDDE5DB) // zebra-even item bg
private val OnSurface             = Color(0xFF171D17) // text đen hữu cơ
private val OnSurfaceVariant      = Color(0xFF3D4E39) // label gray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: OrderDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // topBar cố định — KHÔNG cuộn cùng content
    Scaffold(
        containerColor = SurfaceBase,
        topBar = {
            val order = uiState.order
            if (order != null) {
                OrderDetailHeader(order = order, onBack = onNavigateBack)
            } else {
                // Placeholder khi đang load
                Surface(color = SurfaceBase) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại",
                                tint = OnSurface)
                        }
                    }
                }
            }
        }
    ) { padding ->
        val order = uiState.order
        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = PrimaryGreen) }

            order == null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    uiState.errorMessage ?: "Không tìm thấy đơn hàng",
                    color = MaterialTheme.colorScheme.error
                )
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(SurfaceBase)
                    .verticalScroll(rememberScrollState())
            ) {
                OrderDetailBody(order = order, itemImages = uiState.itemImages)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// Fixed TopAppBar — nền SurfaceBase, không có shadow
// ══════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderDetailHeader(order: Order, onBack: () -> Unit) {
    val accent = if (order.status == OrderStatus.CANCELLED)
        MaterialTheme.colorScheme.error else PrimaryGreen

    // Dùng nền SurfaceBase giống trang — không tạo "bar" nổi bật
    Surface(color = SurfaceBase, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Back arrow
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = OnSurface,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Title + order ID
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Order Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
                Text(
                    text = "#ORD-${order.id.takeLast(8).uppercase()}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = accent
                )
            }

            StatusPill(status = order.status, accent = accent)
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// Scrollable body
// ══════════════════════════════════════════════════════════════════

@Composable
private fun OrderDetailBody(order: Order, itemImages: Map<String, String?>) {
    val accent = if (order.status == OrderStatus.CANCELLED)
        MaterialTheme.colorScheme.error else PrimaryGreen

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ─ Info cards ───────────────────────────────────────────
        HighlightInfoCard(label = "CUSTOMER", value = "Khách lẻ",
            imageUrl = CUSTOMER_IMAGE_URL)
        HighlightInfoCard(label = "PAYMENT METHOD", value = order.paymentMethod.label,
            imageUrl = PAYMENT_IMAGE_URL)
        HighlightInfoCard(label = "ORDER TIME", value = order.createdAt.toDateTimeString(),
            imageUrl = TIME_IMAGE_URL)
        if (order.createdByName.isNotBlank()) {
            HighlightInfoCard(label = "SALES STAFF", value = order.createdByName,
                imageUrl = STAFF_IMAGE_URL)
        }

        // ─ Spacing trước product list ────────────────────────────
        Spacer(Modifier.height(4.dp))

        // ─ Product section ───────────────────────────────────────
        ProductListSection(order = order, accent = accent, itemImages = itemImages)

        // ─ Spacing trước totals ──────────────────────────────────
        Spacer(Modifier.height(4.dp))

        // ─ Totals ────────────────────────────────────────────────
        OrderTotalsSection(order = order, accent = accent)
    }
}

// ══════════════════════════════════════════════════════════════════
// Info cards — nền SurfaceContainerLow, không có border/shadow
// ══════════════════════════════════════════════════════════════════

@Composable
private fun HighlightInfoCard(label: String, value: String, imageUrl: String?) {
    Surface(
        shape = RoundedCornerShape(24.dp),   // xl radius (1.5rem)
        color = SurfaceContainerLow,          // background shift thay thế border
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Label: uppercase tiếng Anh, nhỏ, gray
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp
                )
                // Value: bold, dark
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
            }
            ThumbnailImage(imageUrl = imageUrl, size = 68.dp, cornerRadius = 18.dp)
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// Product list — header + zebra items
// ══════════════════════════════════════════════════════════════════

@Composable
private fun ProductListSection(order: Order, accent: Color, itemImages: Map<String, String?>) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Danh sách sản phẩm",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = OnSurface
            )
            // Badge "3 Items" — xanh nhạt
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.14f),
                contentColor = accent
            ) {
                Text(
                    text = "${order.items.size} Items",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        }

        // Zebra-stripe items — 12px gap
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            order.items.forEachIndexed { index, item ->
                ProductListItem(
                    item = item,
                    index = index,
                    imageUrl = itemImages[item.productId]
                )
            }
        }
    }
}

@Composable
private fun ProductListItem(item: OrderItem, index: Int, imageUrl: String?) {
    // Zebra: item chẵn (0,2,4...) = SurfaceContainerHigh (hơi tối), lẻ = SurfaceContainerLow
    val bg = if (index % 2 == 0) SurfaceContainerHigh else SurfaceContainerLow

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = bg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Product image
            ThumbnailImage(
                imageUrl = imageUrl,
                size = 60.dp,
                cornerRadius = 16.dp,
                placeholder = {
                    Icon(Icons.Filled.Inventory2, null,
                        tint = PrimaryGreen, modifier = Modifier.size(28.dp))
                }
            )

            // Name + qty × price
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = item.productName.ifBlank { "Sản phẩm" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val qtyLine = "${formatQty(item.quantity)} x ${item.unitPrice.toVndString()}"
                Text(qtyLine, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
            }

            // Line total — đen đậm (KHÔNG xanh)
            Text(
                text = item.lineTotal.toVndString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OnSurface
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// Totals — không dùng card, chỉ dùng nền trang + negative space
// ══════════════════════════════════════════════════════════════════

@Composable
private fun OrderTotalsSection(order: Order, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SummaryRow("Tạm tính (Subtotal)", order.items.sumOf { it.lineTotal }.toVndString())
        SummaryRow("Thuế (VAT 0%)", "0đ")

        HorizontalDivider(
            color = Color(0xFFBCCABC).copy(alpha = 0.4f),  // outline-variant @20%
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // Grand total — xanh, lớn, đậm
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tổng cộng",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = OnSurface
            )
            Text(
                text = order.totalAmount.toVndString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = accent
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════
// Small building blocks
// ══════════════════════════════════════════════════════════════════

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
            color = OnSurface)
    }
}

@Composable
private fun StatusPill(status: OrderStatus, accent: Color) {
    val label = when (status) {
        OrderStatus.PAID      -> "THÀNH CÔNG"
        OrderStatus.CANCELLED -> "ĐÃ HUỶ"
        OrderStatus.PENDING   -> "CHỜ TT"
    }
    // Solid fill + white text
    Surface(shape = CircleShape, color = accent, contentColor = Color.White) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(14.dp))
            Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
private fun ThumbnailImage(
    imageUrl: String?,
    size: Dp,
    cornerRadius: Dp,
    placeholder: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(SurfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNullOrBlank()) {
            placeholder?.invoke()
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl).crossfade(true).build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

private fun formatQty(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else "%.2f".format(value)

private const val CUSTOMER_IMAGE_URL = "https://lh3.googleusercontent.com/aida-public/AB6AXuCQZCQ37U1Ou14CD_jXrcXaz7UNTm-bvpho-RWahjJMRdh7mkSWW8E754cArrVgB2rGa1dzREw1n1zB-gFLltoXedfMFdt4cqW5xlxqr4M-4NpHQShyOLCjE9sch6TVFHCsizFtqWQG-6cFswNnHlT6GsiwXTwSDQhZz-mbB7Tdt4_qLH-Bl4Cn0W2OtVZaYR2F6zS6sfduBchRv0BNElL7-BLTpIRZztnwZ3oqGqUfIXFld9vkPseSxk_hYgRwGfuZcLvGRgUQPNk"
private const val PAYMENT_IMAGE_URL  = "https://lh3.googleusercontent.com/aida-public/AB6AXuAhdQnWKl03HyXMjyjmMSm6_S5Ns1q3pPLNuszdGOAaYYs31v9BDyeliq8F7OQNHo_GBqTAheGLhRHv8J6qtJ7lc2l0BIo2rVA2DD-ou5ck5-APuXRlU3Yhguf7a19EciK_iXw2P4_C0vhBlDKFPZ0KI-hZyMuaXG2S5VMvOArDCSmPMAU3Gl_3C9pviaVfBXQF7k2QEXYWKE_V0V1CsET8xulnL7MBGx9NCnWqqPb6Y1H9rLfZsg3bwu5FozUKpA_hIMi34Y3jh0M"
private const val TIME_IMAGE_URL     = "https://lh3.googleusercontent.com/aida-public/AB6AXuDQkJ3T87Irallo1APDmZ3s4KCqHOOYPzYDyIpZMszrtWYy-iWHlYZZl3mSqwu_iNwOdK8u88Tu3fKOF6yjaAGUqIT_m2U2dVKvPKy3YAJESoS4jP0Zpv7zKyVp0YNi8qla_TLRL2Ol03J_klpDD3_x19AY1A-V_OIu0Wogf3eoKLX4V1nJpVNeNw3X_L96vvjdq1j7mfm6x6aDufDX0XV1yXiQ7er0-6u5L4S4O6wQXlMqZaGGJCyLMVjfbbtiRAuT0JBvpPg8bIY"
private const val STAFF_IMAGE_URL    = "https://lh3.googleusercontent.com/aida-public/AB6AXuDppUNwCXaA9pDGm8hSrdFJhFJX5brS7MwptdTU0HLcz382GL4FejUkQyS_vYswjnZ-iAjY8R0-JL0SHvYfjhuqv2gTkbFBLfw7Dry8foyO6JVGHdmCTdIb5bqGph0L_kBl1J16ERZFzIYjcTXciZqp4Xoh1_u24wL7Nku7e7dZVG-5Iq0Mne_rj7zkCVsvR_s62b6_z2kgH1E"

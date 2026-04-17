package com.wavehouse.presentation.pos

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.wavehouse.domain.model.CartItem
import com.wavehouse.domain.model.PaymentMethod
import com.wavehouse.domain.model.Product
import java.text.NumberFormat
import java.util.Locale

// ── Design Tokens (Organic Ledger) ──────────────────────────────────────────
private val BgSurface           = Color(0xFFF4FBF1)
private val SurfaceContainerLow = Color(0xFFEFF6EC)
private val SurfaceContainer    = Color(0xFFE9F0E6)
private val SurfaceHigh         = Color(0xFFE3EAE0)
private val SurfaceHighest      = Color(0xFFDDE5DB)
private val PrimaryGreen        = Color(0xFF006D37)
private val PrimaryContainer    = Color(0xFF27AE60)
private val OnSurface           = Color(0xFF171D17)
private val OnSurfaceVariant    = Color(0xFF3D4A3F)
private val Outline             = Color(0xFF6D7A6E)
private val OutlineVariant      = Color(0xFFBCCABC)

private val vndFmt = NumberFormat.getNumberInstance(Locale("vi", "VN"))
private fun Long.toVnd() = "${vndFmt.format(this)}đ"
private fun Double.toVnd() = toLong().toVnd()

// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    navController: NavController,
    viewModel: PosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // ── Checkout success dialog ──────────────────────────────────────────────
    if (uiState.checkoutSuccess) {
        val isQr = uiState.selectedPaymentMethod == PaymentMethod.QR
        AlertDialog(
            onDismissRequest = { viewModel.resetCheckoutState() },
            icon = { Icon(Icons.Filled.CheckCircle, null, tint = PrimaryGreen) },
            title = { Text(if (isQr) "Chờ xác nhận thanh toán" else "Thanh toán thành công!") },
            text = {
                Text(
                    if (isQr) "Đơn hàng đã tạo. Xác nhận khi nhận được tiền."
                    else "Đơn hàng đã ghi nhận và tồn kho đã cập nhật."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.resetCheckoutState() }) { Text("Đóng") }
            }
        )
    }

    Scaffold(
        topBar = { PosTopBar(navController) },
        containerColor = BgSurface
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = 8.dp,
                    // Extra bottom padding so last item isn't hidden behind the floating bar
                    bottom = if (uiState.isCartEmpty) 16.dp else 120.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ── Search ────────────────────────────────────────────────
                item {
                    SearchField(
                        query = uiState.searchQuery,
                        onQueryChange = viewModel::onSearchQueryChange
                    )
                }

                // ── Quick Add Product grid (4 cols) ───────────────────────
                if (uiState.products.isNotEmpty()) {
                    item {
                        ProductQuickGrid(
                            products = uiState.products.take(8),
                            onAdd = { viewModel.addToCart(it) }
                        )
                    }
                }

                // ── Cart section ──────────────────────────────────────────
                if (!uiState.isCartEmpty) {
                    item {
                        CartSection(
                            cartItems = uiState.cartItems,
                            onIncrease = { viewModel.increaseQuantity(it) },
                            onDecrease = { viewModel.decreaseQuantity(it) },
                            onRemove   = { viewModel.removeFromCart(it) },
                            onSetQty   = { id, qty -> viewModel.setQuantity(id, qty) },
                            onSetQtyDecimal = { id, qty -> viewModel.setQuantityDecimal(id, qty) },
                            onClear    = viewModel::clearCart
                        )
                    }
                }

                // ── Error ─────────────────────────────────────────────────
                uiState.error?.let { err ->
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                err, modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Loading overlay
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = PrimaryGreen
                )
            }

            // ── Floating Checkout Bar ─────────────────────────────────────
            AnimatedVisibility(
                visible = !uiState.isCartEmpty,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically { it } + fadeIn(),
                exit  = slideOutVertically { it } + fadeOut()
            ) {
                CheckoutBar(
                    total      = uiState.cartTotal,
                    itemCount  = uiState.cartItems.size,
                    isLoading  = uiState.isCheckingOut,
                    onCheckout = viewModel::checkout
                )
            }
        }
    }
}

// ─── Top Bar ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PosTopBar(navController: NavController) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceHighest)
                ) {
                    AsyncImage(
                        model = "https://lh3.googleusercontent.com/aida-public/AB6AXuCg6Sx_s2wsnXFYcoEhCUhZMB9PJT7BRAZn1Pv7dUFx-rie4xmR-fbW6DMvcpGXyxzf9FvvCYkCuKpuz4wHnoJKsHer-p4qw8ihioXKdc5GlwmVMZ7Y35vAacLau-E592QJpFRQ5-zkd_5D1Nf6RKYUJgS22h6yL_KcUqYf9A4qrU_aWWGsxkejHHWbmj4nbxWC1PEiPdwHuXR54TOrKe11rzN5m6GU3BUz-7igYL3vYfOksJYKcLopRjlOZgu50EJxJ6IGHv6RsG8",
                        contentDescription = "User",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Text(
                    "FreshStock",
                    fontWeight = FontWeight.Black,
                    color = PrimaryGreen,
                    fontSize = 24.sp,
                    letterSpacing = (-0.5).sp
                )
            }
        },
        actions = {
            IconButton(onClick = { /* Notifications */ }) {
                Icon(Icons.Filled.Notifications, "Thông báo", tint = OnSurface)
            }
            IconButton(onClick = { navController.navigate(com.wavehouse.core.ui.navigation.Routes.OrderHistory.route) }) {
                Icon(Icons.Filled.Receipt, "Lịch sử bán hàng", tint = OnSurface)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = BgSurface)
    )
}

// ─── Search Field ────────────────────────────────────────────────────────────

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, null, tint = Outline, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Tìm sản phẩm (Tên, mã vạch...)", color = OutlineVariant, fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                )
            )
        }
    }
}

// ─── Quick-Add Product Grid (horizontal 2-row scroll) ──────────────────────

@Composable
private fun ProductQuickGrid(products: List<Product>, onAdd: (Product) -> Unit) {
    // Split into 2 rows and scroll horizontally — avoids LazyGrid-in-LazyColumn nesting
    val row1 = products.filterIndexed { i, _ -> i % 2 == 0 }
    val row2 = products.filterIndexed { i, _ -> i % 2 == 1 }
    val maxCols = maxOf(row1.size, row2.size)

    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(maxCols) { col ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                row1.getOrNull(col)?.let { product ->
                    ProductChip(product = product, onClick = { onAdd(product) }, modifier = Modifier.width(80.dp))
                } ?: Spacer(Modifier.width(80.dp).height(94.dp))

                row2.getOrNull(col)?.let { product ->
                    ProductChip(product = product, onClick = { onAdd(product) }, modifier = Modifier.width(80.dp))
                }
            }
        }
    }
}

@Composable
private fun ProductChip(product: Product, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceHighest)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            if (product.imageUrl != null) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    product.name.first().toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = PrimaryGreen
                )
            }
        }
        Text(
            product.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = OnSurface,
            lineHeight = 14.sp
        )
    }
}

// ─── Cart Section ────────────────────────────────────────────────────────────

@Composable
private fun CartSection(
    cartItems: List<CartItem>,
    onIncrease: (String) -> Unit,
    onDecrease: (String) -> Unit,
    onRemove: (String) -> Unit,
    onSetQty: (String, Int) -> Unit,
    onSetQtyDecimal: (String, Double) -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceContainerLow)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Giỏ hàng (${cartItems.size.toString().padStart(2, '0')})",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = OnSurface
            )
            TextButton(onClick = onClear) {
                Text("Xóa tất cả", color = PrimaryGreen, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
        }

        // 2-column grid of cart cards
        val rows = cartItems.chunked(2)
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { cartItem ->
                    CartItemCard(
                        cartItem = cartItem,
                        onIncrease = { onIncrease(cartItem.product.id) },
                        onDecrease = { onDecrease(cartItem.product.id) },
                        onRemove   = { onRemove(cartItem.product.id) },
                        onSetQty   = { qty -> onSetQty(cartItem.product.id, qty) },
                        onSetQtyDecimal = { qty -> onSetQtyDecimal(cartItem.product.id, qty) },
                        modifier   = Modifier.weight(1f)
                    )
                }
                // Placeholder "Thêm mới" card in last row empty slot
                if (row.size == 1) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.4f))
                            .drawBehind {
                                drawRoundRect(
                                    color = OutlineVariant,
                                    style = Stroke(
                                        width = 2.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f))
                                    ),
                                    cornerRadius = CornerRadius(18.dp.toPx())
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.AddShoppingCart, null, tint = Outline, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(4.dp))
                            Text("THÊM MỚI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Outline, letterSpacing = 1.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartItemCard(
    cartItem: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    onSetQty: (Int) -> Unit,
    onSetQtyDecimal: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val allowDecimal = cartItem.product.allowDecimal

    // Hiển thị số lượng: ẩn .0 cho số nguyên, dùng dấu phẩy cho số lẻ
    fun formatQtyDisplay(q: Double): String =
        if (q % 1.0 == 0.0) q.toLong().toString()
        else "%.1f".format(q).replace('.', ',')

    // Local text state — key theo quantity để sync khi stepper btn thay đổi từ ngoài
    var qtyText by remember(cartItem.quantity) {
        mutableStateOf(formatQtyDisplay(cartItem.quantity))
    }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: name + delete
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        cartItem.product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 17.sp,
                        color = OnSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${cartItem.product.salePrice.toVnd()} / ${cartItem.product.unitName ?: "sp"}",
                        fontSize = 10.sp,
                        color = Outline,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Filled.Delete, "Xóa",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Middle: line total
            Text(
                cartItem.lineTotal.toVnd(),
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                color = PrimaryGreen,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            // Bottom: stepper + editable qty input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceContainerLow)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Decrease button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceHighest)
                        .clickable {
                            onDecrease()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Remove, null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                }

                // Editable qty — thập phân nếu allowDecimal=true
                BasicTextField(
                    value = qtyText,
                    onValueChange = { v ->
                        val filtered = if (allowDecimal)
                            v.filter { it.isDigit() || it == ',' || it == '.' }.let { s ->
                                val normalized = s.replace('.', ',')
                                val parts = normalized.split(',')
                                if (parts.size > 2) parts[0] + ',' + parts[1] else normalized
                            }
                        else
                            v.filter { it.isDigit() }
                        qtyText = filtered
                        // ── Cập nhật tổng ngay khi gõ (realtime) ─────────────────────────────
                        if (allowDecimal) {
                            val live = filtered.replace(',', '.').toDoubleOrNull()
                            if (live != null && live > 0.0)
                                onSetQtyDecimal(live.coerceAtMost(cartItem.product.currentStock))
                        } else {
                            val live = filtered.toIntOrNull()
                            if (live != null && live > 0)
                                onSetQty(live.coerceAtMost(cartItem.product.currentStock.toInt()))
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { fs ->
                            if (!fs.isFocused) {
                                // Commit khi mất focus
                                if (allowDecimal) {
                                    val parsed = qtyText.replace(',', '.').toDoubleOrNull() ?: 0.0
                                    if (parsed <= 0.0) {
                                        qtyText = "0,5"
                                        onSetQtyDecimal(0.5)
                                    } else {
                                        val clamped = parsed.coerceAtMost(cartItem.product.currentStock)
                                        qtyText = formatQtyDisplay(clamped)
                                        onSetQtyDecimal(clamped)
                                    }
                                } else {
                                    val parsed = qtyText.toIntOrNull() ?: 0
                                    if (parsed <= 0) {
                                        qtyText = "1"
                                        onSetQty(1)
                                    } else {
                                        val max = cartItem.product.currentStock.toInt()
                                        val clamped = parsed.coerceAtMost(max)
                                        qtyText = clamped.toString()
                                        onSetQty(clamped)
                                    }
                                }
                            }
                        },
                    textStyle = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = OnSurface,
                        textAlign = TextAlign.Center
                    ),
                    cursorBrush = SolidColor(PrimaryGreen),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (allowDecimal) KeyboardType.Decimal else KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (allowDecimal) {
                                val parsed = qtyText.replace(',', '.').toDoubleOrNull() ?: 0.5
                                val clamped = parsed.coerceIn(0.5, cartItem.product.currentStock)
                                qtyText = formatQtyDisplay(clamped)
                                onSetQtyDecimal(clamped)
                            } else {
                                val parsed = qtyText.toIntOrNull() ?: 1
                                val max = cartItem.product.currentStock.toInt()
                                val clamped = parsed.coerceIn(1, max)
                                qtyText = clamped.toString()
                                onSetQty(clamped)
                            }
                            focusManager.clearFocus()
                        }
                    ),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) { inner() }
                    }
                )

                // Increase button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceHighest)
                        .clickable {
                            onIncrease()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ─── Floating Checkout Bar ───────────────────────────────────────────────────

@Composable
private fun CheckoutBar(
    total: Double,
    itemCount: Int,
    isLoading: Boolean,
    onCheckout: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left: total amount + badge
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "TỔNG THANH TOÁN",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Outline,
                    letterSpacing = 1.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        total.toVnd(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = PrimaryGreen,
                        letterSpacing = (-0.5).sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(SurfaceContainerLow)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "$itemCount sp",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }

            // Right: checkout button (fixed width)
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(listOf(PrimaryGreen, PrimaryContainer))
                    )
                    .clickable(enabled = !isLoading, onClick = onCheckout),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Filled.ShoppingCartCheckout,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "THANH TOÁN",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import android.widget.Toast
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
    val context = LocalContext.current

    // ── Stock-limit toast (shown outside the screen content) ─────────────────────
    LaunchedEffect(Unit) {
        viewModel.limitEvents.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    // ── Checkout success dialog ──────────────────────────────────────────────
    if (uiState.checkoutSuccess) {
        val successMethod = uiState.lastCheckoutMethod ?: uiState.selectedPaymentMethod
        val isQr = successMethod == PaymentMethod.QR
        AlertDialog(
            onDismissRequest = { viewModel.resetCheckoutState() },
            icon = { Icon(Icons.Filled.CheckCircle, null, tint = PrimaryGreen) },
            title = { Text("Thanh toán thành công!") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Đơn hàng đã ghi nhận và tồn kho đã cập nhật.")
                    if (isQr) {
                        Text("Thanh toán QR đã được xác nhận.")
                    }
                    if (uiState.pendingOrderAmount > 0) {
                        Text(
                            "Tổng thanh toán: ${uiState.pendingOrderAmount.toVnd()}",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.resetCheckoutState() }) { Text("Đóng") }
            }
        )
    }

    if (uiState.showCashConfirmDialog) {
        var inputStr by remember { mutableStateOf("") }
        val isQr = uiState.selectedPaymentMethod == PaymentMethod.QR
        val total = uiState.cartTotal
        val typedValue = inputStr.replace(Regex("[^0-9]"), "").toDoubleOrNull() ?: total
        val debt = (total - typedValue).coerceAtLeast(0.0)

        AlertDialog(
            onDismissRequest = { viewModel.dismissCashConfirmDialog() },
            title = { Text(if (isQr) "Xác nhận thanh toán QR" else "Thanh toán Tiền mặt", color = PrimaryGreen) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tổng hóa đơn:", style = MaterialTheme.typography.bodyLarge)
                        Text(total.toVnd(), fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(
                        value = inputStr,
                        onValueChange = { inputStr = it },
                        label = { Text("Khách thanh toán") },
                        placeholder = { Text(total.toVnd()) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            focusedLabelColor = PrimaryGreen
                        )
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Ghi nợ:", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                        Text(debt.toVnd(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissCashConfirmDialog() }) {
                    Text("Hủy", color = OnSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isQr) viewModel.startQrCheckout(typedValue)
                        else viewModel.confirmCashCheckout(typedValue)
                    },
                    enabled = !uiState.isCheckingOut,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    if (uiState.isCheckingOut) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(if (isQr) "Lấy mã QR" else "Hoàn tất")
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = { PosTopBar(navController) },
        bottomBar = {
            AnimatedVisibility(
                visible = !uiState.isCartEmpty,
                enter = slideInVertically { it } + fadeIn(),
                exit  = slideOutVertically { it } + fadeOut()
            ) {
                CheckoutBar(
                    total = uiState.cartTotal,
                    itemCount = uiState.cartItems.size,
                    selectedMethod = uiState.selectedPaymentMethod,
                    qrEnabled = uiState.qrEnabled,
                    isLoading = uiState.isCheckingOut,
                    onSelectMethod = { viewModel.setPaymentMethod(it) },
                    onCheckout = viewModel::requestCheckout
                )
            }
        },
        containerColor = BgSurface,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0)
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp,
                        top = 8.dp,
                        bottom = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                products = uiState.products.take(20),
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
            }

            // Loading overlay
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = PrimaryGreen
                )
            }
        }
    }

    if (uiState.showQrSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.cancelQrCheckout() },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Thanh toán QR",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
                Text(
                    "Khách quét mã QR bên dưới, xác nhận khi đã nhận tiền.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (uiState.qrImageUrl != null) {
                    AsyncImage(
                        model = uiState.qrImageUrl,
                        contentDescription = "QR thanh toán",
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White),
                        contentScale = ContentScale.FillBounds
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Chưa có mã QR", color = OnSurfaceVariant)
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        uiState.pendingOrderAmount.toVnd(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = PrimaryGreen
                    )
                    Text(
                        "${uiState.pendingOrderItemCount} sản phẩm",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }

                uiState.paymentFlowError?.let { errorMsg ->
                    Text(
                        errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.cancelQrCheckout() },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isConfirmingPayment
                    ) {
                        Text("Hủy")
                    }
                    Button(
                        onClick = { viewModel.confirmQrPayment() },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isConfirmingPayment
                    ) {
                        if (uiState.isConfirmingPayment) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text("Đã nhận tiền")
                        }
                    }
                }
            }
        }
    }
}

// ─── Top Bar ────────────────────────────────────────────────────────────────

@Composable
private fun PosTopBar(navController: NavController) {
    Surface(
        color = BgSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Avatar
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

            // Brand name
            Text(
                "FreshStock",
                fontWeight = FontWeight.Black,
                color = PrimaryGreen,
                fontSize = 24.sp,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.weight(1f)
            )

            // Actions
            IconButton(onClick = { /* Notifications */ }) {
                Icon(Icons.Filled.Notifications, "Thông báo", tint = OnSurface)
            }
            IconButton(onClick = {
                navController.navigate(com.wavehouse.core.ui.navigation.Routes.DebtList.route)
            }) {
                Icon(Icons.Filled.AccountBalanceWallet, "Công nợ", tint = OnSurface)
            }
        }
    }
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

// ─── Quick-Add Product Row (single horizontal scroll) ──────────────────────

@Composable
private fun ProductQuickGrid(products: List<Product>, onAdd: (Product) -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(products.size) { index ->
            val product = products[index]
            ProductChip(
                product = product,
                onClick = { onAdd(product) },
                modifier = Modifier.width(80.dp)
            )
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
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLow)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                fontSize = 16.sp,
                color = OnSurface
            )
            TextButton(onClick = onClear, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                Text("Xóa tất cả", color = PrimaryGreen, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            }
        }

        // Single-column compact row list
        cartItems.forEach { cartItem ->
            CartItemRow(
                cartItem = cartItem,
                onIncrease = { onIncrease(cartItem.product.id) },
                onDecrease = { onDecrease(cartItem.product.id) },
                onRemove   = { onRemove(cartItem.product.id) },
                onSetQty   = { qty -> onSetQty(cartItem.product.id, qty) },
                onSetQtyDecimal = { qty -> onSetQtyDecimal(cartItem.product.id, qty) }
            )
        }
    }
}

@Composable
private fun CartItemRow(
    cartItem: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    onSetQty: (Int) -> Unit,
    onSetQtyDecimal: (Double) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PosViewModel = hiltViewModel()
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Thumbnail Image
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(OutlineVariant.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            if (!cartItem.product.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = cartItem.product.imageUrl,
                    contentDescription = cartItem.product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    cartItem.product.name.firstOrNull()?.uppercase() ?: "",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = PrimaryGreen.copy(alpha = 0.6f)
                )
            }
        }

        // Left: name + unit price + line total
        Column(modifier = Modifier.weight(1f)) {
            Text(
                cartItem.product.name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = OnSurface
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "${cartItem.product.salePrice.toVnd()}/${cartItem.product.unitName ?: "sp"}",
                    fontSize = 10.sp,
                    color = Outline,
                    fontWeight = FontWeight.Medium
                )
                Text("•", fontSize = 10.sp, color = Outline)
                Text(
                    cartItem.lineTotal.toVnd(),
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = PrimaryGreen
                )
            }
        }

        // Compact stepper
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceContainerLow)
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(SurfaceHighest)
                    .clickable { onDecrease() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Remove, null, tint = PrimaryGreen, modifier = Modifier.size(14.dp))
            }

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
                    if (allowDecimal) {
                        val live = filtered.replace(',', '.').toDoubleOrNull()
                        if (live != null) {
                            val clamped = live.coerceIn(0.0, cartItem.product.currentStock)
                            if (clamped <= 0.0) {
                                qtyText = filtered
                            } else {
                                val display = if (clamped == live) filtered else formatQtyDisplay(clamped)
                                qtyText = display
                                if (clamped != live) {
                                    viewModel.notifyStockLimit()
                                }
                                onSetQtyDecimal(clamped)
                            }
                        } else {
                            qtyText = filtered
                        }
                    } else {
                        val live = filtered.toIntOrNull()
                        if (live != null) {
                            val maxQty = cartItem.product.currentStock.toInt().coerceAtLeast(1)
                            val clamped = live.coerceIn(1, maxQty)
                            if (clamped != live) {
                                viewModel.notifyStockLimit()
                            }
                            qtyText = clamped.toString()
                            onSetQty(clamped)
                        } else {
                            qtyText = filtered
                        }
                    }
                },
                modifier = Modifier
                    .widthIn(min = 32.dp, max = 44.dp)
                    .onFocusChanged { fs ->
                        if (!fs.isFocused) {
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
                    fontSize = 14.sp,
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

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(SurfaceHighest)
                    .clickable { onIncrease() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, null, tint = PrimaryGreen, modifier = Modifier.size(14.dp))
            }
        }

        // Delete
        IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Filled.Delete, "Xóa",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.45f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ─── Floating Checkout Bar ───────────────────────────────────────────────────

@Composable
private fun CheckoutBar(
    total: Double,
    itemCount: Int,
    selectedMethod: PaymentMethod,
    qrEnabled: Boolean,
    isLoading: Boolean,
    onSelectMethod: (PaymentMethod) -> Unit,
    onCheckout: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PaymentMethodChip(
                    label = "Tiền mặt",
                    selected = selectedMethod == PaymentMethod.CASH,
                    enabled = true,
                    onClick = { onSelectMethod(PaymentMethod.CASH) }
                )
                PaymentMethodChip(
                    label = "Mã QR",
                    selected = selectedMethod == PaymentMethod.QR,
                    enabled = qrEnabled,
                    onClick = { onSelectMethod(PaymentMethod.QR) }
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
}

@Composable
private fun PaymentMethodChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = {
            Text(
                label,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        },
        leadingIcon = if (selected) {
            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PrimaryGreen.copy(alpha = 0.15f),
            selectedLabelColor = PrimaryGreen,
            selectedLeadingIconColor = PrimaryGreen
        )
    )
}

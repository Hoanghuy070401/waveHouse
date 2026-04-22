package com.wavehouse.presentation.product.addedit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.wavehouse.core.ui.components.WaveAppBar
import com.wavehouse.core.utils.formatThousands
import com.wavehouse.core.utils.stripFormat

// Organic Ledger Design Tokens
private val BackgroundColor = Color(0xFFF4FBF1)
private val SurfaceContainerLowest = Color(0xFFFFFFFF)
private val SurfaceContainerLow = Color(0xFFEFF6EC)
private val SurfaceContainer = Color(0xFFE9F0E6)
private val SurfaceContainerHigh = Color(0xFFE3EAE0)
private val SurfaceContainerHighest = Color(0xFFDDE5DB)
private val PrimaryColor = Color(0xFF006D37)
private val PrimaryContainer = Color(0xFF27AE60)
private val TertiaryFixed = Color(0xFFFFDDB9)
private val TertiaryColor = Color(0xFF865300)
private val OnTertiaryVariant = Color(0xFF663E00)
private val OnSurface = Color(0xFF171D17)
private val OnSurfaceVariant = Color(0xFF3D4A3F)
private val OutlineVariant = Color(0xFFBCCABC)
private val Outline = Color(0xFF6D7A6E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    onNavigateBack: () -> Unit,
    onNavigateToScanner: () -> Unit,
    viewModel: AddEditProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    var saveHistory by remember { mutableStateOf(true) }

    // ── Local TextFieldValue cho giá tiền — tránh cursor jump khi format ────────
    // Khởi tạo từ VM state (hỗ trợ edit mode: sản phẩm có sẵn giá)
    var costFieldValue by remember {
        val f = formatThousands(uiState.costPrice)
        mutableStateOf(TextFieldValue(text = f, selection = TextRange(f.length)))
    }
    var saleFieldValue by remember {
        val f = formatThousands(uiState.salePrice)
        mutableStateOf(TextFieldValue(text = f, selection = TextRange(f.length)))
    }
    // Sync khi VM nạp dữ liệu sản phẩm bất đồng bộ (chế độ edit)
    LaunchedEffect(uiState.costPrice) {
        if (stripFormat(costFieldValue.text) != uiState.costPrice) {
            val f = formatThousands(uiState.costPrice)
            costFieldValue = TextFieldValue(text = f, selection = TextRange(f.length))
        }
    }
    LaunchedEffect(uiState.salePrice) {
        if (stripFormat(saleFieldValue.text) != uiState.salePrice) {
            val f = formatThousands(uiState.salePrice)
            saleFieldValue = TextFieldValue(text = f, selection = TextRange(f.length))
        }
    }

    if (uiState.isAccessDenied) {
        AccessDeniedContent(onNavigateBack)
        return
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) onNavigateBack()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            WaveAppBar(
                title = if (uiState.isEditMode) "Chỉnh sửa sản phẩm" else "Thêm hàng mới",
                onBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = BackgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Product Image Picker Section
            val imagePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri -> uri?.let(viewModel::onPickImage) }

            val currentPreview = uiState.pendingImageUri ?: uiState.imageUrl
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceContainerLow)
                    .clickable(enabled = !uiState.isUploadingImage) {
                        imagePickerLauncher.launch("image/*")
                    }
            ) {
                if (currentPreview != null) {
                    AsyncImage(
                        model = currentPreview,
                        contentDescription = "Ảnh sản phẩm",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0x9900391A))
                                )
                            )
                    )
                    // Nút xoá ảnh
                    IconButton(
                        onClick = { viewModel.onRemoveImage() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        Icon(Icons.Filled.Close, "Xoá ảnh", tint = Color.White)
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.AddPhotoAlternate,
                            contentDescription = null,
                            tint = PrimaryColor,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Chạm để thêm ảnh sản phẩm",
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryColor
                        )
                        Text(
                            "(tỷ lệ 16:9 đẹp nhất)",
                            fontSize = 12.sp,
                            color = OnSurfaceVariant
                        )
                    }
                }

                if (uiState.isUploadingImage) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }

            // Basic Info Wrapper
            Box(
                Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(SurfaceContainerLow)
                    .padding(4.dp)
            ) {
                Column(
                    Modifier
                        .clip(RoundedCornerShape(28.dp))
                        .background(SurfaceContainerLowest)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Tên sản phẩm
                    Column {
                        Text(
                            "Tên sản phẩm",
                            fontWeight = FontWeight.SemiBold,
                            color = OnSurfaceVariant,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        )
                        OutlinedTextField(
                            value = uiState.name,
                            onValueChange = viewModel::onNameChange,
                            placeholder = { Text("Ví dụ: Cải bẹ xanh, Táo Rockit...", color = OutlineVariant) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceContainer,
                                unfocusedContainerColor = SurfaceContainer,
                                focusedBorderColor = PrimaryColor,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    }

                    // Đơn vị
                    Column {
                        Text(
                            "Đơn vị",
                            fontWeight = FontWeight.SemiBold,
                            color = OnSurfaceVariant,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        )
                        val units = listOf("kg", "bó", "thùng", "túi")
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            units.forEach { unit ->
                                val isSelected = uiState.selectedUnitName == unit
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isSelected) PrimaryColor else SurfaceContainer)
                                        .clickable { viewModel.onUnitSelected("", unit) }
                                        .padding(horizontal = 24.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        text = unit,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) Color.White else OnSurfaceVariant
                                    )
                                }
                            }
                            // Nút thêm đơn vị mới
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(SurfaceContainer)
                                    .clickable { /* Mở popup thêm */ }
                                    .padding(12.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Add unit", tint = OnSurfaceVariant)
                            }
                        }

                        // ── Trọng lượng quy đổi — chỉ hiện khi KHÔNG phải kg/gr ──────
                        if (!uiState.isWeightUnit && uiState.selectedUnitName.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("⚖️", fontSize = 16.sp)
                                    OutlinedTextField(
                                        value = uiState.weightPerUnit,
                                        onValueChange = viewModel::onWeightPerUnitChange,
                                        placeholder = {
                                            Text(
                                                "1 ${uiState.selectedUnitName} = ? kg",
                                                color = OutlineVariant,
                                                fontSize = 13.sp
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent
                                        )
                                    )
                                    Text(
                                        "kg/${uiState.selectedUnitName}",
                                        fontSize = 13.sp,
                                        color = OnSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // ── Cho phép bán lẻ toggle ──────────────────────────────────────
                    // Đặt sau Đơn vị vì liên quan trực tiếp đến cách bán
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (uiState.allowDecimal)
                            SurfaceContainerLow
                        else
                            SurfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Cho phép bán lẻ",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = OnSurface
                                    )
                                    Text(
                                        if (uiState.allowDecimal)
                                            "Có thể nhập số lẻ (ví dụ: 0,5 · 1,25)"
                                        else
                                            "Chỉ nhập số nguyên (ví dụ: 1 · 2 · 3)",
                                        fontSize = 12.sp,
                                        color = OnSurfaceVariant,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                Switch(
                                    checked = uiState.allowDecimal,
                                    onCheckedChange = viewModel::onAllowDecimalChange,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = PrimaryColor,
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = OutlineVariant
                                    )
                                )
                            }
                        }
                    }

                    // SKU
                    Column {
                        Text(
                            "Mã định danh (SKU / Barcode)",
                            fontWeight = FontWeight.SemiBold,
                            color = OnSurfaceVariant,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        )
                        OutlinedTextField(
                            value = uiState.sku,
                            onValueChange = viewModel::onSkuChange,
                            placeholder = { Text("SKU...", color = OutlineVariant) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceContainer,
                                unfocusedContainerColor = SurfaceContainer,
                                focusedBorderColor = PrimaryColor,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    }
                }
            }

            // Dynamic Pricing Section
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💰", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("Giá hôm nay", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Giá nhập
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceContainerLow)
                            .padding(24.dp)
                    ) {
                        Column {
                            Text(
                                "GIÁ NHẬP HÔM NAY",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            // Font co lại theo độ dài (tránh tran khi nhập giá lớn)
                            val costFontSize = when {
                                costFieldValue.text.length <= 5  -> 30.sp
                                costFieldValue.text.length <= 8  -> 22.sp
                                costFieldValue.text.length <= 11 -> 17.sp
                                else                             -> 14.sp
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = costFieldValue,
                                    onValueChange = { newVal ->
                                        val raw = stripFormat(newVal.text)
                                        val formatted = formatThousands(raw)
                                        costFieldValue = TextFieldValue(
                                            text = formatted,
                                            selection = TextRange(formatted.length)
                                        )
                                        viewModel.onCostPriceChange(raw)
                                    },
                                    placeholder = { Text("0", fontSize = costFontSize, fontWeight = FontWeight.Bold) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(
                                        fontSize = costFontSize, fontWeight = FontWeight.Bold, color = PrimaryColor
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    modifier = Modifier.weight(1f).padding(0.dp)
                                )
                                Text("đ", fontSize = costFontSize * 0.6f, fontWeight = FontWeight.SemiBold, color = Outline)
                            }
                            Text("Gợi ý: 15.000đ/kg", fontSize = 10.sp, color = Outline)
                        }
                    }

                    // Giá bán
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(TertiaryFixed)
                            .padding(24.dp)
                    ) {
                        Column {
                            Text(
                                "GIÁ BÁN HÔM NAY",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnTertiaryVariant,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            val saleFontSize = when {
                                saleFieldValue.text.length <= 5  -> 30.sp
                                saleFieldValue.text.length <= 8  -> 22.sp
                                saleFieldValue.text.length <= 11 -> 17.sp
                                else                             -> 14.sp
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = saleFieldValue,
                                    onValueChange = { newVal ->
                                        val raw = stripFormat(newVal.text)
                                        val formatted = formatThousands(raw)
                                        saleFieldValue = TextFieldValue(
                                            text = formatted,
                                            selection = TextRange(formatted.length)
                                        )
                                        viewModel.onSalePriceChange(raw)
                                    },
                                    placeholder = { Text("0", fontSize = saleFontSize, fontWeight = FontWeight.Bold) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(
                                        fontSize = saleFontSize, fontWeight = FontWeight.Bold, color = TertiaryColor
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    modifier = Modifier.weight(1f).padding(0.dp)
                                )
                                Text("đ", fontSize = saleFontSize * 0.6f, fontWeight = FontWeight.SemiBold, color = OnTertiaryVariant)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val c = stripFormat(costFieldValue.text).toDoubleOrNull() ?: 0.0
                                val s = stripFormat(saleFieldValue.text).toDoubleOrNull() ?: 0.0
                                val margin = if (c > 0) ((s - c) / c * 100).toInt() else 0
                                Text("Lợi nhuận: ${if(margin >= 0) "+" else ""}$margin%", fontSize = 10.sp, color = OnTertiaryVariant)
                                Icon(Icons.Filled.TrendingUp, null, tint = TertiaryColor, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Options Section
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerHigh)
                    .clickable { saveHistory = !saveHistory }
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Checkbox(
                        checked = saveHistory,
                        onCheckedChange = { saveHistory = it },
                        colors = CheckboxDefaults.colors(checkedColor = PrimaryColor)
                    )
                    Text("Lưu vào lịch sử giá", fontWeight = FontWeight.SemiBold, color = OnSurface)
                }
                Text("🕒", fontSize = 20.sp)
            }

            // Primary CTA
            Button(
                onClick = viewModel::save,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(50)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                enabled = !uiState.isLoading
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(PrimaryColor, PrimaryContainer))),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Filled.AddCircle, null, tint = Color.White)
                            Text(
                                if(uiState.isEditMode) "Cập nhật sản phẩm" else "Thêm sản phẩm",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun AccessDeniedContent(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            WaveAppBar(title = "Không có quyền", onBack = onNavigateBack)
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    "Không có quyền thực hiện",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Chỉ Admin và Thủ kho mới có thể thực hiện.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onNavigateBack) {
                    Text("Quay lại")
                }
            }
        }
    }
}

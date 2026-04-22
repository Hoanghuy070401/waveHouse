package com.wavehouse.presentation.product.scanner

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.wavehouse.domain.model.Product
import com.wavehouse.presentation.product.list.ProductCard
import java.util.concurrent.Executors

/**
 * Barcode scanner screen với hai chế độ:
 * 1. Quét barcode/QR bằng camera → tìm sản phẩm ngay
 * 2. Tìm theo tên trong danh sách theo danh mục (như yêu cầu của người dùng)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun BarcodeScanScreen(
    onNavigateBack: () -> Unit,
    onProductFound: (String) -> Unit,
    viewModel: BarcodeScanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    // Request camera permission on launch
    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    // Navigate when product found by barcode
    LaunchedEffect(uiState.foundProductId) {
        uiState.foundProductId?.let { onProductFound(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tìm sản phẩm", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = 0.6f))
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // === Search by name tab ===
            SearchModeSection(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                products = uiState.searchResults,
                isSearching = uiState.isSearching,
                onProductClick = onProductFound
            )

            // === OR divider ===
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.Gray)
                Text("  HOẶC QUÉT BARCODE  ", color = Color.Gray,
                    style = MaterialTheme.typography.labelSmall)
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.Gray)
            }

            // === Camera scanner ===
            Box(modifier = Modifier.weight(1f)) {
                if (cameraPermission.status.isGranted) {
                    CameraPreview(
                        onBarcodeDetected = viewModel::onBarcodeDetected
                    )
                    // Viewfinder overlay
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(240.dp, 120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Transparent)
                        ) {
                            // Corner markers drawn via border decoration
                        }
                        Text(
                            "Đưa barcode vào khung để quét",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
                        )
                    }

                    // Scanned result feedback
                    if (uiState.isLoadingBarcode) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                    uiState.barcodeError?.let { error ->
                        Snackbar(
                            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                        ) { Text(error) }
                    }
                } else {
                    // Permission denied state
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📷", style = MaterialTheme.typography.displayMedium)
                            Spacer(Modifier.height(8.dp))
                            Text("Cần quyền camera để quét barcode", color = Color.White)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { cameraPermission.launchPermissionRequest() }) {
                                Text("Cấp quyền")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchModeSection(
    query: String,
    onQueryChange: (String) -> Unit,
    products: List<Product>,
    isSearching: Boolean,
    onProductClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Tìm theo tên, danh mục...") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        if (query.isNotBlank()) {
            if (isSearching) {
                Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                }
            } else if (products.isEmpty()) {
                Text(
                    "Không tìm thấy sản phẩm",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 260.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(products.take(5), key = { it.id }) { product ->
                        ProductCard(product = product, onClick = { onProductClick(product.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(onBarcodeDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build().also { analysis ->
                        analysis.setAnalyzer(executor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val inputImage = InputImage.fromMediaImage(
                                    mediaImage, imageProxy.imageInfo.rotationDegrees
                                )
                                BarcodeScanning.getClient().process(inputImage)
                                    .addOnSuccessListener { barcodes ->
                                        barcodes.firstOrNull()?.rawValue?.let {
                                            onBarcodeDetected(it)
                                        }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else {
                                imageProxy.close()
                            }
                        }
                    }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

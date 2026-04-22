package com.wavehouse.presentation.debt.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wavehouse.core.utils.toVndString

@Composable
fun DebtListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCustomerDebt: (String) -> Unit, // phone
    viewModel: DebtListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { DebtTopBar(onNavigateBack) },
        containerColor = Color(0xFFF4FBF1)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search and Add
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = Color.Gray) },
                    placeholder = { Text("Tìm tên, SĐT...", color = Color.Gray, fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF006D37),
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                    ),
                    singleLine = true
                )
                Button(
                    onClick = { /* TODO Add Debt */ },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006D37)),
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(Icons.Filled.Add, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Thêm", fontWeight = FontWeight.SemiBold)
                }
            }

            // Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                val tabs = listOf("Tất cả", "Quá hạn", "Đã thu xong")
                tabs.forEachIndexed { index, title ->
                    val isSelected = uiState.filterTab == index
                    Column(
                        modifier = Modifier.clickable { viewModel.onFilterTabChanged(index) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isSelected) Color(0xFF006D37) else Color.Gray,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        if (isSelected) {
                            Box(modifier = Modifier
                                .height(4.dp)
                                .width(32.dp)
                                .background(Color(0xFF006D37), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                        }
                    }
                }
            }

            // Overview 4 Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OverviewCard(
                    title = "Tổng nợ",
                    value = uiState.totalDebt.toVndString(),
                    bgColor = Color(0xFFEFF6EC),
                    titleColor = Color.Gray,
                    valueColor = Color(0xFF171D17),
                    modifier = Modifier.weight(1f)
                )
                OverviewCard(
                    title = "Quá hạn",
                    value = uiState.overdueDebt.toVndString(),
                    bgColor = Color(0xFFFFDAD6).copy(alpha = 0.2f),
                    borderColor = Color(0xFFFFDAD6).copy(alpha = 0.3f),
                    titleColor = Color(0xFFBA1A1A),
                    valueColor = Color(0xFFBA1A1A),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OverviewCard(
                    title = "Trong hạn",
                    value = uiState.inTermDebt.toVndString(),
                    bgColor = Color(0xFFD58700).copy(alpha = 0.1f),
                    titleColor = Color(0xFF865300),
                    valueColor = Color(0xFF865300),
                    modifier = Modifier.weight(1f)
                )
                OverviewCard(
                    title = "Đã thu (Tháng)",
                    value = "0đ", // Placeholder 
                    bgColor = Color(0xFFCFE2F9).copy(alpha = 0.2f),
                    titleColor = Color(0xFF4E6073),
                    valueColor = Color(0xFF526478),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Debt List
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF006D37))
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.customers, key = { it.phone }) { customer ->
                        CustomerDebtCard(
                            customer = customer,
                            onClick = { onNavigateToCustomerDebt(customer.phone) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DebtTopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color(0xFF006D37))
            }
            Text("Quản lý công nợ", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF006D37))
        }
    }
}

@Composable
private fun OverviewCard(
    title: String,
    value: String,
    bgColor: Color,
    titleColor: Color,
    valueColor: Color,
    modifier: Modifier = Modifier,
    borderColor: Color = Color.Transparent
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = titleColor, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
        }
    }
}

@Composable
private fun CustomerDebtCard(
    customer: CustomerDebt,
    onClick: () -> Unit
) {
    val bgColor = if (customer.isOverdue) Color(0xFFDDE5DB) else Color(0xFFEFF6EC)
    val avatarBg = if (customer.isOverdue) Color(0xFFFFDAD6) else Color(0xFFD58700).copy(alpha = 0.2f)
    val avatarColor = if (customer.isOverdue) Color(0xFFBA1A1A) else Color(0xFF865300)
    val badgeBg = if (customer.isOverdue) Color(0xFFBA1A1A) else Color(0xFFD58700)
    val badgeColor = if (customer.isOverdue) Color.White else Color(0xFF472A00)
    val badgeText = if (customer.isOverdue) "QUÁ HẠN" else "TRONG HẠN"
    
    val timePassedMs = System.currentTimeMillis() - customer.lastUpdate
    val hours = timePassedMs / (1000 * 60 * 60)
    val updateText = if (hours < 24) "Cập nhật: $hours giờ trước" else "Cập nhật: ${hours / 24} ngày trước"

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier
                        .size(48.dp)
                        .background(avatarBg, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Person, null, tint = avatarColor)
                    }
                    Column {
                        Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF171D17))
                        Text(updateText, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
                    }
                }
                Surface(color = badgeBg, shape = RoundedCornerShape(8.dp)) {
                    Text(badgeText, color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Black, 
                         letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text(customer.totalDebt.toVndString(), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF171D17))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .border(1.dp, Color.LightGray.copy(alpha=0.5f), RoundedCornerShape(16.dp))
                    ) {
                        Icon(Icons.Filled.Call, null, tint = Color(0xFF006D37))
                    }
                    IconButton(
                        onClick = onClick,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .border(1.dp, Color.LightGray.copy(alpha=0.5f), RoundedCornerShape(16.dp))
                    ) {
                        Icon(Icons.Filled.ReceiptLong, null, tint = Color(0xFF006D37))
                    }
                }
            }
        }
    }
}

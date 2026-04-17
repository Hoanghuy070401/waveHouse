package com.wavehouse.presentation.auth.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

// ── Design tokens (theo Stitch design) ──────────────────────────────
private val BgColor      = Color(0xFFEEF4EF)   // Light green tint
private val CardColor    = Color.White
private val GreenPrimary = Color(0xFF1E5631)   // Dark forest green (header bar + button)
private val IconOrangeBg = Color(0xFFFFF0E0)   // Soft amber for hourglass bg
private val IconOrange   = Color(0xFFE07000)   // Hourglass icon color
private val DotAmber     = Color(0xFFF59E0B)   // Trạng thái bullet
private val TextGray     = Color(0xFF6B7280)

@Composable
fun PendingApprovalScreen(
    onLogout: () -> Unit,
    onApproved: () -> Unit = {},
    viewModel: PendingApprovalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top Bar ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GreenPrimary)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.LocalFlorist,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Warehouse Access",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            // ── Main Content ──────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Hourglass Icon trong vòng tròn amber
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(IconOrangeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.HourglassTop,
                        contentDescription = null,
                        tint = IconOrange,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(Modifier.height(28.dp))

                // ── Card nội dung ─────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Đang chờ duyệt",
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827),
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "Bạn đã được mời tham gia kho:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextGray,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(12.dp))

                        // Tên kho highlight
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.LocalFlorist,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (uiState.warehouseName.isNotBlank())
                                    uiState.warehouseName else "...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = GreenPrimary
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        // Trạng thái bullet
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(DotAmber)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Trạng thái: Đang chờ chủ kho xác nhận",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF111827)
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Vui lòng đợi hoặc liên hệ chủ kho để được duyệt.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextGray
                                )
                            }
                        }

                        Spacer(Modifier.height(28.dp))

                        // Nút Kiểm tra lại
                        Button(
                            onClick = { viewModel.recheckStatus(onApproved) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                            enabled = !uiState.isChecking
                        ) {
                            if (uiState.isChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Filled.Refresh,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Kiểm tra lại",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Nút Đăng xuất
                        OutlinedButton(
                            onClick = { viewModel.logout(onLogout) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF374151)
                            )
                        ) {
                            Icon(
                                Icons.Filled.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Đăng xuất",
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            // ── Footer ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Support",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray
                )
                Text(
                    "  •  ",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray
                )
                Text(
                    "Harvest Flow v2.4.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray
                )
            }
        }
    }
}

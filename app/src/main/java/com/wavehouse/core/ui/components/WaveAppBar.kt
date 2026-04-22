package com.wavehouse.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wavehouse.core.ui.theme.PrimaryGreen

/** Màu nền chuẩn cho toàn bộ app bar — khớp token SurfaceBase */
private val AppBarBg = androidx.compose.ui.graphics.Color(0xFFF4FBF1)

/**
 * Thanh toolbar chuẩn toàn ứng dụng WaveHouse.
 *
 * Layout: [←] [   Title (centered)   ] [action1?] [action2?]
 * Title luôn căn giữa tuyệt đối trong bar, bất kể số action.
 *
 * @param title         Tiêu đề hiển thị (ngôn ngữ Việt)
 * @param onBack        Callback nút quay lại. null = ẩn nút back (dùng cho rootScreens)
 * @param actions       Tối đa 2 action icon button ở phải. Dùng [WaveAppBarAction].
 * @param subtitle      Dòng phụ nhỏ bên dưới title (vd. "Mã: #ABC123")
 */
@Composable
fun WaveAppBar(
    title: String,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    actions: List<WaveAppBarAction> = emptyList()
) {
    Surface(
        color = AppBarBg,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .height(IntrinsicSize.Min),
            contentAlignment = Alignment.Center
        ) {
            // ── Centered title (always truly centered in bar) ─────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 56.dp)   // reserve space for icons both sides
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen,
                    maxLines = 1,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── Back button pinned to start ───────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    Spacer(Modifier.width(48.dp))
                }

                // ── Actions pinned to end ─────────────────────────────
                Row {
                    actions.take(2).forEach { action ->
                        IconButton(onClick = action.onClick) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = action.contentDescription,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    // Balance spacer when no actions (keeps title centered)
                    if (actions.isEmpty() && onBack != null) {
                        Spacer(Modifier.width(48.dp))
                    }
                }
            }
        }
    }
}

/** Định nghĩa một action icon button trên WaveAppBar */
data class WaveAppBarAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit
)

// ── Preset helpers ─────────────────────────────────────────────────────────────

/** Action tìm kiếm chuẩn */
fun searchAction(onClick: () -> Unit) = WaveAppBarAction(
    icon = Icons.Filled.Search,
    contentDescription = "Tìm kiếm",
    onClick = onClick
)

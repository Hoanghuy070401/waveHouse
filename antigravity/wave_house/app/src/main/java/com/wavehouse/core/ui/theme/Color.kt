package com.wavehouse.core.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════
// Organic Ledger — Color Palette
// Based on DESIGN.md "The Living Ledger"
// Primary: Harvest Green (#0d631b)
// ═══════════════════════════════════════════════════════════════

// === Primary — Harvest Green ===
val Primary10 = Color(0xFF002106)
val Primary20 = Color(0xFF00390D)
val Primary30 = Color(0xFF055215)
val Primary40 = Color(0xFF0D631B)       // 🟢 Main primary
val Primary80 = Color(0xFF72DA7F)
val Primary90 = Color(0xFFA8F5AB)
val Primary95 = Color(0xFFC3FAC4)
val Primary100 = Color(0xFFFFFFFF)

// === Secondary — Forest Sage ===
val Secondary10 = Color(0xFF0E1F0E)
val Secondary20 = Color(0xFF1E3620)
val Secondary30 = Color(0xFF2E4D32)
val Secondary40 = Color(0xFF3E6644)
val Secondary80 = Color(0xFF99C49E)
val Secondary90 = Color(0xFFBDE1BE)

// === Tertiary — Earth Amber ===
val Tertiary10 = Color(0xFF2A1800)
val Tertiary20 = Color(0xFF452B00)
val Tertiary30 = Color(0xFF624000)
val Tertiary40 = Color(0xFF815600)
val Tertiary80 = Color(0xFFFFB74D)
val Tertiary90 = Color(0xFFFFE0B2)

// === Error — Warm Red ===
val Error10 = Color(0xFF410002)
val Error20 = Color(0xFF690005)
val Error30 = Color(0xFF93000A)
val Error40 = Color(0xFFBA1A1A)
val Error80 = Color(0xFFFFB4AB)
val Error90 = Color(0xFFFFDAD6)

// === Surface Hierarchy (Organic Ledger "No-Line" tonal layering) ===
// Base Layer → Mid Layer → Top Layer
val Surface = Color(0xFFF7FBF0)           // "Paper" — base layer
val SurfaceContainer = Color(0xFFEBEFE5)  // Grouped content blocks
val SurfaceContainerLow = Color(0xFFF1F5EB)
val SurfaceContainerLowest = Color(0xFFFFFFFF)  // Interactive cards — "pop"
val SurfaceContainerHigh = Color(0xFFE4E8DE)
val SurfaceContainerHighest = Color(0xFFDDE1D7)

// === Neutral — Ink on Paper ===
val OnSurface = Color(0xFF181D17)         // NEVER use #000000, always this
val OnSurfaceVariant = Color(0xFF40493D)  // Icons default, metadata
val Outline = Color(0xFF70796D)
val OutlineVariant = Color(0xFFC0C9BC)    // Ghost Borders: 20% opacity

// === Dark Mode ===
val DarkSurface = Color(0xFF101410)
val DarkSurfaceContainer = Color(0xFF1A1E1A)
val DarkSurfaceContainerHigh = Color(0xFF242824)
val DarkOnSurface = Color(0xFFDFE4D9)
val DarkOnSurfaceVariant = Color(0xFFC0C9BC)

// === Stock Status Colors ===
val StockGood = Color(0xFF2E7D32)         // Tồn kho đủ — darker green
val StockLow = Color(0xFFE65100)          // Tồn kho thấp — deep orange
val StockOut = Color(0xFFC62828)          // Hết hàng — deep red
val StockNewBadge = Color(0xFF0D631B)     // Sản phẩm mới — primary green

// === Chart Colors ===
val ChartIn = Color(0xFF2E7D32)           // Đường nhập kho — green
val ChartOut = Color(0xFFC62828)          // Đường xuất kho — red
val ChartAdjust = Color(0xFF6A1B9A)       // Điều chỉnh — purple
val ChartShrinkage = Color(0xFFE65100)    // Hao hụt — orange

package com.bacos.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// ─── Spacing tokens — never arbitrary paddings ───
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val huge = 48.dp
}

object Radius {
    val sm = RoundedCornerShape(10.dp)
    val md = RoundedCornerShape(14.dp)
    val lg = RoundedCornerShape(20.dp)
    val xl = RoundedCornerShape(28.dp)
    val pill = RoundedCornerShape(50)
}

package io.github.sdsd08013.viewmarkerkit.compose.sample

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sdsd08013.viewmarkerkit.compose.MarkerPlacement
import kotlinx.coroutines.delay

/** The composable rendered for a [Pin]: a colored circle that bounces when tapped and fades on a screen edge. */
@Composable
fun PinContent(pin: Pin, placement: MarkerPlacement) {
    var taps by remember { mutableIntStateOf(0) }
    var bouncing by remember { mutableStateOf(false) }
    LaunchedEffect(taps) {
        if (taps == 0) return@LaunchedEffect
        bouncing = true
        delay(120)
        bouncing = false
    }
    val scale by animateFloatAsState(if (bouncing) 1.4f else 1f, spring(), label = "bounce")
    val atEdge = placement.descriptor?.isAtEdge == true

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(pin.color.copy(alpha = if (atEdge) 0.6f else 1f))
                .border(3.dp, Color.White, CircleShape)
                .clickable { taps++ },
            contentAlignment = Alignment.Center,
        ) {
            Text(pin.label, color = Color.White, fontSize = 11.sp, maxLines = 1)
        }
    }
}

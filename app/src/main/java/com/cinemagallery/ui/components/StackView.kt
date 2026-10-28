package com.cinemagallery.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cinemagallery.domain.model.MediaItem
import com.cinemagallery.ui.theme.WatchedGreen
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun StackView(
    items: List<MediaItem>,
    selectedId: String?,
    onMediaClick: (MediaItem) -> Unit,
    onIndexChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No titles match your filters",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val scope = rememberCoroutineScope()

    var currentIndex by remember(items) {
        val initialIdx = if (selectedId != null) {
            items.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
        } else 0
        mutableIntStateOf(initialIdx)
    }

    LaunchedEffect(currentIndex, items) {
        if (currentIndex in items.indices) {
            onIndexChanged(items[currentIndex].id)
        }
    }

    var isFlipped by remember(currentIndex) { mutableStateOf(false) }
    val flipRotation = remember { Animatable(0f) }

    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    fun resetFlip() {
        if (isFlipped) {
            isFlipped = false
            scope.launch {
                flipRotation.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
            }
        }
    }

    fun animateFlip() {
        val target = if (isFlipped) 0f else 180f
        isFlipped = !isFlipped
        scope.launch {
            flipRotation.animateTo(target, tween(450, easing = FastOutSlowInEasing))
        }
    }

    fun nextCard() {
        if (currentIndex < items.size - 1) {
            resetFlip()
            scope.launch {
                offsetX.animateTo(-1200f, tween(250))
                currentIndex++
                offsetX.snapTo(0f)
                offsetY.snapTo(0f)
            }
        }
    }

    fun prevCard() {
        if (currentIndex > 0) {
            resetFlip()
            scope.launch {
                offsetX.animateTo(1200f, tween(250))
                currentIndex--
                offsetX.snapTo(0f)
                offsetY.snapTo(0f)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Counter Pill Top-Left
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = "${currentIndex + 1} of ${items.size}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Center Card Stack Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Card 3 (2 behind)
            if (currentIndex + 2 < items.size) {
                StackCardBackground(
                    item = items[currentIndex + 2],
                    scale = 0.88f,
                    offsetYDp = 32.dp
                )
            }

            // Card 2 (1 behind)
            if (currentIndex + 1 < items.size) {
                StackCardBackground(
                    item = items[currentIndex + 1],
                    scale = 0.94f,
                    offsetYDp = 16.dp
                )
            }

            // Front Main Interactive Card
            val item = items[currentIndex]
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.88f)
                    .aspectRatio(0.68f)
                    .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                    .graphicsLayer {
                        rotationZ = (offsetX.value / 40f).coerceIn(-15f, 15f)
                    }
                    .pointerInput(currentIndex) {
                        var totalDragX = 0f
                        var totalDragY = 0f
                        detectDragGestures(
                            onDragStart = {
                                totalDragX = 0f
                                totalDragY = 0f
                            },
                            onDragEnd = {
                                if (totalDragX < -200f || totalDragY < -200f) {
                                    nextCard()
                                } else if (totalDragX > 200f || totalDragY > 200f) {
                                    prevCard()
                                } else {
                                    scope.launch {
                                        offsetX.animateTo(0f, tween(200))
                                        offsetY.animateTo(0f, tween(200))
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    offsetX.animateTo(0f, tween(200))
                                    offsetY.animateTo(0f, tween(200))
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalDragX += dragAmount.x
                                totalDragY += dragAmount.y
                                scope.launch {
                                    offsetX.snapTo(offsetX.value + dragAmount.x)
                                    offsetY.snapTo(offsetY.value + dragAmount.y)
                                }
                            }
                        )
                    }
                    .clickable { animateFlip() }
            ) {
                val rotation = flipRotation.value
                val isBackVisible = rotation > 90f

                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 12f * density
                        },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    if (isBackVisible) {
                        // Card Back (rotation 180)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { rotationY = 180f }
                                .padding(20.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                val subText = buildString {
                                    item.year?.let { append("$it · ") }
                                    if (item.genres.isNotEmpty()) append("${item.genres.first()} · ")
                                    if (item is MediaItem.Movie) append("${item.runtimeMinutes} min")
                                    else if (item is MediaItem.TvShow) append("${item.totalEpisodesCount} episodes")
                                    append(" · ★ %.1f".format(item.rating))
                                }

                                Text(
                                    text = subText,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = item.overview.ifBlank { "No plot overview available." },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    maxLines = 8,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                val filePath = when (item) {
                                    is MediaItem.Movie -> item.filePath
                                    is MediaItem.TvShow -> item.folderPath
                                }

                                Text(
                                    text = filePath.substringAfterLast('/'),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Play Button / Details Button
                                Button(
                                    onClick = {
                                        onMediaClick(item)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (item is MediaItem.Movie) Icons.Default.PlayArrow else Icons.Default.Folder,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (item is MediaItem.Movie) "Play Movie" else "View Episodes",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        // Card Front
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (!item.posterPath.isNullOrBlank()) {
                                AsyncImage(
                                    model = item.posterPath,
                                    contentDescription = item.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                    MaterialTheme.colorScheme.surface
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.title.take(1).uppercase(),
                                        style = MaterialTheme.typography.displayLarge.copy(
                                            fontSize = 90.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                        )
                                    )
                                }
                            }

                            // Dark Gradient Bottom Overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.6f)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.7f),
                                                Color.Black.copy(alpha = 0.95f)
                                            )
                                        )
                                    )
                            )

                            // Rating Badge Top-Left
                            if (item.rating > 0) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Black.copy(alpha = 0.8f),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "%.1f".format(item.rating),
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        )
                                    }
                                }
                            }

                            // Watched Check Top-Right
                            val isWatched = when (item) {
                                is MediaItem.Movie -> item.isWatched
                                is MediaItem.TvShow -> item.isFullyWatched
                            }

                            if (isWatched) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(14.dp)
                                        .size(28.dp)
                                        .background(WatchedGreen, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Watched",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Bottom Details Text
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(18.dp)
                            ) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 24.sp,
                                        color = Color.White
                                    ),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                val subInfo = buildString {
                                    item.year?.let { append("$it · ") }
                                    if (item.genres.isNotEmpty()) append("${item.genres.first()} · ")
                                    if (item is MediaItem.Movie) append("${item.runtimeMinutes} min")
                                    else if (item is MediaItem.TvShow) append("${item.totalEpisodesCount} episodes")
                                }

                                Text(
                                    text = subInfo,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Prev / Next Round Navigation Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { prevCard() },
                enabled = currentIndex > 0,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (currentIndex > 0) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Card",
                    tint = if (currentIndex > 0) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }

            IconButton(
                onClick = { nextCard() },
                enabled = currentIndex < items.size - 1,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (currentIndex < items.size - 1) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Card",
                    tint = if (currentIndex < items.size - 1) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        }
    }
}

@Composable
private fun StackCardBackground(
    item: MediaItem,
    scale: Float,
    offsetYDp: androidx.compose.ui.unit.Dp
) {
    Card(
        modifier = Modifier
            .fillMaxHeight(0.88f)
            .aspectRatio(0.68f)
            .offset(y = offsetYDp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!item.posterPath.isNullOrBlank()) {
                AsyncImage(
                    model = item.posterPath,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.6f
                )
            }
        }
    }
}

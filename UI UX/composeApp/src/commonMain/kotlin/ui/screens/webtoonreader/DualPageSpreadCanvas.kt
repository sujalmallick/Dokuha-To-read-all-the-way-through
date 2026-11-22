package ui.screens.webtoonreader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.reader.DualPageSpread
import data.reader.ReaderColorFilters
import data.settings.ReaderColorFilter

private val MangaPaperBg = Color(0xFFF9F7F1)

/**
 * Renders facing pages side-by-side with an authentic physical book spine crease,
 * center gutter curvature shadow, and tankōbon margin binding.
 */
@Composable
fun DualPageSpreadCanvas(
    spread: DualPageSpread,
    totalPages: Int,
    chapterTitle: String = "Chapter 42",
    isRtl: Boolean = true,
    cropBorders: Boolean = false,
    colorFilter: ReaderColorFilter = ReaderColorFilter.DEFAULT,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ReaderColorFilters.getBackplateColor(colorFilter))
    ) {
        if (spread.isDoubleSplash && spread.leftPage != null) {
            // Wide Double-Spread Splash Art spanning both pages
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (cropBorders) 0.dp else 6.dp)
            ) {
                MangaPageCanvas(
                    pageIndex = spread.leftPage,
                    totalPages = totalPages,
                    chapterTitle = chapterTitle,
                    isRtl = isRtl,
                    cropBorders = cropBorders,
                    colorFilter = colorFilter,
                    modifier = Modifier.fillMaxSize()
                )

                // Center Binding Crease running down the middle of the splash
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerX = size.width / 2f
                    val creaseWidth = 32.dp.toPx()

                    // Left-half gradient shadow fading to center
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color(0x38000000)),
                            startX = centerX - creaseWidth,
                            endX = centerX
                        ),
                        topLeft = Offset(centerX - creaseWidth, 0f),
                        size = Size(creaseWidth, size.height)
                    )

                    // Right-half gradient shadow fading from center
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0x38000000), Color.Transparent),
                            startX = centerX,
                            endX = centerX + creaseWidth
                        ),
                        topLeft = Offset(centerX, 0f),
                        size = Size(creaseWidth, size.height)
                    )

                    // Center hairline book seam
                    drawLine(
                        color = Color(0x40000000),
                        start = Offset(centerX, 0f),
                        end = Offset(centerX, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        } else {
            // Facing Pages (Verso on Left, Recto on Right)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (cropBorders) 0.dp else 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT PAGE CONTAINER (Verso)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    if (spread.leftPage != null) {
                        MangaPageCanvas(
                            pageIndex = spread.leftPage,
                            totalPages = totalPages,
                            chapterTitle = chapterTitle,
                            isRtl = isRtl,
                            cropBorders = cropBorders,
                            colorFilter = colorFilter,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Empty Verso (e.g. blank page before cover in Japanese reading order)
                        BlankMangaPage(
                            label = if (isRtl) "Cover Verso" else "End of Chapter",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Left page gutter binding shadow (right edge)
                    Canvas(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(28.dp)
                    ) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, Color(0x38000000))
                            )
                        )
                    }
                }

                // CENTER BOOK SPINE SEAM
                Canvas(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.5.dp)
                ) {
                    drawLine(
                        color = Color(0x55000000),
                        start = Offset(size.width / 2f, 0f),
                        end = Offset(size.width / 2f, size.height),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }

                // RIGHT PAGE CONTAINER (Recto)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    if (spread.rightPage != null) {
                        MangaPageCanvas(
                            pageIndex = spread.rightPage,
                            totalPages = totalPages,
                            chapterTitle = chapterTitle,
                            isRtl = isRtl,
                            cropBorders = cropBorders,
                            colorFilter = colorFilter,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Empty Recto
                        BlankMangaPage(
                            label = if (!isRtl) "Cover Recto" else "End of Chapter",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Right page gutter binding shadow (left edge)
                    Canvas(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .width(28.dp)
                    ) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0x38000000), Color.Transparent)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BlankMangaPage(
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(MangaPaperBg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = Color(0x40000000)
        )
    }
}

package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.DateFilterPeriod
import com.example.domain.CategoryBreakdownItem
import com.example.domain.DailyBalancePoint
import com.example.domain.FinanceCalculator
import com.example.domain.MonthlyComparisonPoint
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.DimGray
import com.example.ui.theme.ElevatedCharcoal
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GlassBorderWhite15
import com.example.ui.theme.GlassBorderWhite28
import com.example.ui.theme.GlassSurfaceWhite12
import com.example.ui.theme.GlassSurfaceWhite8
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SurfaceCharcoal
import com.example.ui.theme.TransferBlue
import com.example.ui.theme.WarningAmber

@Composable
fun LeovaneyLogoBadge(
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(ObsidianBlack)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(GlassBorderWhite28, GlassBorderWhite15)
                ),
                shape = RoundedCornerShape(size * 0.28f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.leovaney_logo),
            contentDescription = stringResource(id = R.string.logo_content_desc),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(size * 0.12f)
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    borderColor: Color = GlassBorderWhite15,
    containerColor: Color = SurfaceCharcoal.copy(alpha = 0.86f),
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val baseModifier = modifier
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    ElevatedCharcoal.copy(alpha = 0.92f),
                    containerColor
                )
            )
        )
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    borderColor,
                    borderColor.copy(alpha = 0.08f)
                )
            ),
            shape = shape
        )
        .let { mod ->
            if (onClick != null) mod.clickable(onClick = onClick) else mod
        }
        .padding(contentPadding)

    Column(
        modifier = baseModifier,
        content = content
    )
}

@Composable
fun GlassTopBar(
    title: String,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(GlassSurfaceWhite8)
                        .border(1.dp, GlassBorderWhite15, CircleShape)
                        .testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = PureWhite
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                LeovaneyLogoBadge(size = 42.dp)
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = PureWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSilver,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingContent()
        }
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isNumeric: Boolean = false,
    singleLine: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    testTag: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = if (placeholder.isNotEmpty()) {
                { Text(placeholder, color = DimGray) }
            } else null,
            singleLine = singleLine,
            isError = isError,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isNumeric) KeyboardType.Number else KeyboardType.Text
            ),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = PureWhite,
                unfocusedTextColor = PureWhite,
                focusedContainerColor = DeepCharcoal,
                unfocusedContainerColor = DeepCharcoal,
                focusedBorderColor = PureWhite,
                unfocusedBorderColor = GlassBorderWhite28,
                focusedLabelColor = PureWhite,
                unfocusedLabelColor = MutedSilver,
                cursorColor = PureWhite,
                errorBorderColor = ExpenseRed,
                errorLabelColor = ExpenseRed
            ),
            modifier = Modifier
                .fillMaxWidth()
                .let { if (testTag != null) it.testTag(testTag) else it }
        )
        if (!supportingText.isNullOrBlank()) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) ExpenseRed else MutedSilver,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun GlassPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    testTag: String? = null
) {
    Button(
        onClick = { if (!isLoading && enabled) onClick() },
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PureWhite,
            contentColor = ObsidianBlack,
            disabledContainerColor = ElevatedCharcoal,
            disabledContentColor = DimGray
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        modifier = modifier
            .height(52.dp)
            .let { if (testTag != null) it.testTag(testTag) else it }
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = ObsidianBlack,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PeriodFilterRow(
    selectedPeriod: DateFilterPeriod,
    onSelectPeriod: (DateFilterPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DateFilterPeriod.entries.forEach { period ->
            val selected = period == selectedPeriod
            val bg by animateColorAsState(
                targetValue = if (selected) PureWhite else SurfaceCharcoal,
                label = "period_bg"
            )
            val fg by animateColorAsState(
                targetValue = if (selected) ObsidianBlack else MutedSilver,
                label = "period_fg"
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = bg,
                border = BorderStroke(1.dp, if (selected) PureWhite else GlassBorderWhite15),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onSelectPeriod(period) }
                    .testTag("period_chip_${period.name}")
            ) {
                Text(
                    text = period.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = fg,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
fun StatusBanner(
    statusMessage: String?,
    errorMessage: String?,
    onDismiss: () -> Unit
) {
    val msg = errorMessage ?: statusMessage ?: return
    val isError = errorMessage != null
    val borderColor = if (isError) ExpenseRed else IncomeGreen
    val bgColor = if (isError) ExpenseRed.copy(alpha = 0.16f) else IncomeGreen.copy(alpha = 0.16f)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag(if (isError) "error_banner" else "status_banner")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isError) Icons.Default.Warning else Icons.Default.Check,
                    contentDescription = null,
                    tint = borderColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = PureWhite
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Tutup pesan",
                    tint = MutedSilver,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    confirmText: String = "Hapus",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElevatedCharcoal,
        titleContentColor = PureWhite,
        textContentColor = MutedSilver,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = PureWhite
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSilver
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ExpenseRed,
                    contentColor = PureWhite
                ),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text(confirmText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_delete_button")
            ) {
                Text("Batal", color = PureWhite)
            }
        }
    )
}

/**
 * Native Compose Canvas Bar Chart comparing Income vs Expense across months or for current period.
 */
@Composable
fun IncomeExpenseComparisonChart(
    comparisons: List<MonthlyComparisonPoint>,
    currencyCode: String = "IDR",
    modifier: Modifier = Modifier
) {
    val maxVal = comparisons.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1L) ?: 1L

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(IncomeGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pemasukan", style = MaterialTheme.typography.bodySmall, color = MutedSilver)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(ExpenseRed)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pengeluaran", style = MaterialTheme.typography.bodySmall, color = MutedSilver)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val count = comparisons.size.coerceAtLeast(1)
            val groupWidth = size.width / count
            val barWidth = (groupWidth * 0.28f).coerceAtLeast(6f)
            val chartHeight = size.height - 8f

            // Baseline grid lines
            for (i in 0..3) {
                val y = chartHeight * (i / 3f)
                drawLine(
                    color = GlassBorderWhite15,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }

            comparisons.forEachIndexed { idx, pt ->
                val centerX = idx * groupWidth + groupWidth / 2f
                val incRatio = (pt.income.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                val expRatio = (pt.expense.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)

                val incHeight = (incRatio * (chartHeight - 12f)).coerceAtLeast(4f)
                val expHeight = (expRatio * (chartHeight - 12f)).coerceAtLeast(4f)

                // Income bar
                drawRoundRect(
                    color = IncomeGreen,
                    topLeft = Offset(centerX - barWidth - 3f, chartHeight - incHeight),
                    size = Size(barWidth, incHeight),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                // Expense bar
                drawRoundRect(
                    color = ExpenseRed,
                    topLeft = Offset(centerX + 3f, chartHeight - expHeight),
                    size = Size(barWidth, expHeight),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            comparisons.forEach { pt ->
                Text(
                    text = pt.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSilver
                )
            }
        }
    }
}

/**
 * Native Compose Canvas Line Chart for Balance Trend.
 */
@Composable
fun BalanceTrendChart(
    points: List<DailyBalancePoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return
    val minBal = points.minOf { it.cumulativeBalance }
    val maxBal = points.maxOf { it.cumulativeBalance }
    val range = (maxBal - minBal).coerceAtLeast(1L)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val w = size.width
            val h = size.height - 12f
            val stepX = if (points.size > 1) w / (points.size - 1) else w

            val path = Path()
            val fillPath = Path()

            points.forEachIndexed { index, pt ->
                val x = if (points.size > 1) index * stepX else w / 2f
                val normalized = (pt.cumulativeBalance - minBal).toFloat() / range.toFloat()
                val y = h - (normalized * (h - 20f)) - 6f

                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, h)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }

                if (index == points.lastIndex) {
                    fillPath.lineTo(x, h)
                    fillPath.close()
                }
            }

            if (points.size > 1) {
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(PureWhite.copy(alpha = 0.22f), Color.Transparent)
                    )
                )
                drawPath(
                    path = path,
                    color = PureWhite,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            points.forEachIndexed { index, pt ->
                val x = if (points.size > 1) index * stepX else w / 2f
                val normalized = (pt.cumulativeBalance - minBal).toFloat() / range.toFloat()
                val y = h - (normalized * (h - 20f)) - 6f
                drawCircle(
                    color = ObsidianBlack,
                    radius = 5.dp.toPx(),
                    center = Offset(x, y)
                )
                drawCircle(
                    color = PureWhite,
                    radius = 3.5.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = points.first().dateLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MutedSilver
            )
            Text(
                text = FinanceCalculator.formatRupiah(points.last().cumulativeBalance),
                style = MaterialTheme.typography.labelSmall,
                color = PureWhite
            )
            if (points.size > 1) {
                Text(
                    text = points.last().dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSilver
                )
            }
        }
    }
}

/**
 * Category Breakdown horizontal progress bars + proportion list.
 */
@Composable
fun CategoryBreakdownList(
    items: List<CategoryBreakdownItem>,
    currencyCode: String = "IDR",
    isExpense: Boolean = true
) {
    val barColor = if (isExpense) ExpenseRed else IncomeGreen
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { item ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${item.categoryName} (${item.transactionCount})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PureWhite,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${FinanceCalculator.formatRupiah(item.totalAmount, currencyCode)} • ${String.format("%.1f", item.percentage)}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFamily,
                        color = MutedSilver
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(GlassSurfaceWhite12)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = (item.percentage / 100f).coerceIn(0.03f, 1f))
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(barColor)
                    )
                }
            }
        }
    }
}

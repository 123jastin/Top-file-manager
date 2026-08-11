package com.example.ui.components

import android.content.Context
import android.graphics.Typeface
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

@Composable
fun NativeAdGridItem(
    nativeAd: NativeAd?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.surface.toArgb()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary.toArgb()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.9f)
            .testTag("native_ad_item_grid"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        if (nativeAd == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ad",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Sponsored",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Loading ad...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    createNativeAdGridView(
                        ctx = ctx,
                        surfaceColor = surfaceColor,
                        onSurfaceColor = onSurfaceColor,
                        onSurfaceVariantColor = onSurfaceVariantColor,
                        primaryColor = primaryColor,
                        onPrimaryColor = onPrimaryColor
                    )
                },
                update = { adView ->
                    bindNativeAdToGridView(adView, nativeAd)
                }
            )
        }
    }
}

private fun createNativeAdGridView(
    ctx: Context,
    surfaceColor: Int,
    onSurfaceColor: Int,
    onSurfaceVariantColor: Int,
    primaryColor: Int,
    onPrimaryColor: Int
): NativeAdView {
    val adView = NativeAdView(ctx)
    adView.layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
    )

    val rootLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
        setPadding(dpToPx(ctx, 8), dpToPx(ctx, 8), dpToPx(ctx, 8), dpToPx(ctx, 8))
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
    }

    // Top Row: "Ad" Badge
    val topRow = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.START
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    val adBadge = TextView(ctx).apply {
        text = "Ad"
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
        setTypeface(null, Typeface.BOLD)
        setTextColor(android.graphics.Color.parseColor("#10B981"))
        setPadding(dpToPx(ctx, 6), dpToPx(ctx, 2), dpToPx(ctx, 6), dpToPx(ctx, 2))
        val badgeBackground = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dpToPx(ctx, 4).toFloat()
            setColor(android.graphics.Color.parseColor("#1E293B"))
            setStroke(dpToPx(ctx, 1), android.graphics.Color.parseColor("#10B981"))
        }
        background = badgeBackground
    }
    topRow.addView(adBadge)
    rootLayout.addView(topRow)

    // Center Icon
    val iconView = ImageView(ctx).apply {
        layoutParams = LinearLayout.LayoutParams(dpToPx(ctx, 48), dpToPx(ctx, 48)).apply {
            setMargins(0, dpToPx(ctx, 4), 0, dpToPx(ctx, 6))
        }
        scaleType = ImageView.ScaleType.CENTER_CROP
        clipToOutline = true
        val drawable = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dpToPx(ctx, 12).toFloat()
            setColor(primaryColor)
        }
        background = drawable
    }
    adView.iconView = iconView
    rootLayout.addView(iconView)

    // Headline TextView
    val headlineView = TextView(ctx).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTypeface(null, Typeface.BOLD)
        setTextColor(onSurfaceColor)
        gravity = Gravity.CENTER
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }
    adView.headlineView = headlineView
    rootLayout.addView(headlineView)

    // Body TextView
    val bodyView = TextView(ctx).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
        setTextColor(onSurfaceVariantColor)
        gravity = Gravity.CENTER
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, dpToPx(ctx, 2), 0, dpToPx(ctx, 6))
        }
    }
    adView.bodyView = bodyView
    rootLayout.addView(bodyView)

    // CTA Button
    val ctaView = Button(ctx).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        setTypeface(null, Typeface.BOLD)
        setTextColor(onPrimaryColor)
        val buttonDrawable = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dpToPx(ctx, 8).toFloat()
            setColor(primaryColor)
        }
        background = buttonDrawable
        setPadding(dpToPx(ctx, 8), dpToPx(ctx, 2), dpToPx(ctx, 8), dpToPx(ctx, 2))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            dpToPx(ctx, 32)
        )
    }
    adView.callToActionView = ctaView
    rootLayout.addView(ctaView)

    adView.addView(rootLayout)
    return adView
}

private fun bindNativeAdToGridView(adView: NativeAdView, nativeAd: NativeAd) {
    (adView.headlineView as? TextView)?.text = nativeAd.headline

    val body = nativeAd.body ?: nativeAd.advertiser
    if (body.isNullOrEmpty()) {
        adView.bodyView?.visibility = View.GONE
    } else {
        adView.bodyView?.visibility = View.VISIBLE
        (adView.bodyView as? TextView)?.text = body
    }

    val icon = nativeAd.icon
    if (icon != null && icon.drawable != null) {
        (adView.iconView as? ImageView)?.setImageDrawable(icon.drawable)
        adView.iconView?.visibility = View.VISIBLE
    } else {
        adView.iconView?.visibility = View.GONE
    }

    if (nativeAd.callToAction != null) {
        (adView.callToActionView as? Button)?.text = nativeAd.callToAction
        adView.callToActionView?.visibility = View.VISIBLE
    } else {
        adView.callToActionView?.visibility = View.GONE
    }

    adView.setNativeAd(nativeAd)
}

private fun dpToPx(ctx: Context, dp: Int): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        ctx.resources.displayMetrics
    ).toInt()
}

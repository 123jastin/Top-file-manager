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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

@Composable
fun NativeAdListItem(
    nativeAd: NativeAd?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.surface.toArgb()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary.toArgb()

    if (nativeAd == null) {
        // Render skeleton card matching file item design while ad is loading
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("native_ad_loading"),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ad",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sponsored Content",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Loading advertisement...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
        return
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("native_ad_item_list"),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            factory = { ctx ->
                createNativeAdListView(
                    ctx = ctx,
                    surfaceColor = surfaceColor,
                    onSurfaceColor = onSurfaceColor,
                    onSurfaceVariantColor = onSurfaceVariantColor,
                    primaryColor = primaryColor,
                    onPrimaryColor = onPrimaryColor
                )
            },
            update = { adView ->
                bindNativeAdToListView(adView, nativeAd)
            }
        )
    }
}

private fun createNativeAdListView(
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
        FrameLayout.LayoutParams.WRAP_CONTENT
    )

    val rootLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
    }

    // 1. Icon View Container
    val iconView = ImageView(ctx).apply {
        layoutParams = LinearLayout.LayoutParams(dpToPx(ctx, 48), dpToPx(ctx, 48)).apply {
            setMargins(0, 0, dpToPx(ctx, 12), 0)
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

    // 2. Middle Content Column
    val contentLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
    }

    // Header Row: Ad Badge + Headline
    val headerLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    // "Ad" Badge
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
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, dpToPx(ctx, 6), 0)
        }
    }
    headerLayout.addView(adBadge)

    // Headline TextView
    val headlineView = TextView(ctx).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        setTypeface(null, Typeface.BOLD)
        setTextColor(onSurfaceColor)
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }
    adView.headlineView = headlineView
    headerLayout.addView(headlineView)

    contentLayout.addView(headerLayout)

    // Body TextView
    val bodyView = TextView(ctx).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTextColor(onSurfaceVariantColor)
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, dpToPx(ctx, 2), 0, 0)
        }
    }
    adView.bodyView = bodyView
    contentLayout.addView(bodyView)

    rootLayout.addView(contentLayout)

    // 3. CTA Button
    val ctaView = Button(ctx).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTypeface(null, Typeface.BOLD)
        setTextColor(onPrimaryColor)
        val buttonDrawable = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dpToPx(ctx, 8).toFloat()
            setColor(primaryColor)
        }
        background = buttonDrawable
        setPadding(dpToPx(ctx, 12), dpToPx(ctx, 6), dpToPx(ctx, 12), dpToPx(ctx, 6))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            dpToPx(ctx, 36)
        ).apply {
            setMargins(dpToPx(ctx, 8), 0, 0, 0)
        }
    }
    adView.callToActionView = ctaView
    rootLayout.addView(ctaView)

    adView.addView(rootLayout)
    return adView
}

private fun bindNativeAdToListView(adView: NativeAdView, nativeAd: NativeAd) {
    (adView.headlineView as? TextView)?.text = nativeAd.headline

    val body = nativeAd.body ?: nativeAd.advertiser
    if (body.isNullOrBlank()) {
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

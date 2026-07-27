package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.TakeawayItem
import com.example.ui.metadata.PresentationPolicy
import com.example.ui.metadata.ListStyle
import com.example.ui.metadata.LayoutType
import com.example.getTakeawayIcon
import com.example.parseMarkdownToAnnotatedString

@Composable
fun TakeawayCard(
    takeaway: TakeawayItem,
    index: Int,
    policy: PresentationPolicy,
    activeColor: Color,
    showIcon: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(activeColor.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                if (policy.listStyle == ListStyle.NUMBERED) {
                    Text(
                        text = String.format("%02d", index + 1),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = activeColor
                    )
                } else {
                    Text(
                        text = "•",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = activeColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = parseMarkdownToAnnotatedString(takeaway.title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    letterSpacing = 0.sp
                )
                
                val rawRisk = (takeaway.visualMetadata["risk_level"] ?: takeaway.visualMetadata["severity"])?.lowercase()
                if (policy.layoutType == LayoutType.RISK_LIST && rawRisk in setOf("low", "medium", "high")) {
                    val (badgeText, badgeColor, badgeBg) = when (rawRisk) {
                        "low" -> Triple("Niedrig", Color(0xFF2E7D32), Color(0xFFE8F5E9))
                        "medium" -> Triple("Mittel", Color(0xFFE65100), Color(0xFFFFF3E0))
                        "high" -> Triple("Hoch", Color(0xFFC62828), Color(0xFFFFEBEE))
                        else -> Triple("", Color.Transparent, Color.Transparent)
                    }
                    if (badgeText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(badgeBg)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Risiko: $badgeText",
                                color = badgeColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = takeaway.details.replace("**", "").replace("__", "").replace("*", "").trim(),
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )
            }

            if (showIcon && policy.showTakeawayIcons) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = getTakeawayIcon(takeaway.title, takeaway.details, takeaway.visualMetadata),
                    contentDescription = null,
                    tint = activeColor.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

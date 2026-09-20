package com.lojia.pos.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.pos.R
import com.lojia.pos.data.POSProduct
import com.lojia.pos.ui.theme.*
import com.lojia.pos.util.MoneyFormat

/**
 * Clean product row matching the exact Loyverse POS design.
 */
@Composable
fun LoyverseProductRow(
    product: POSProduct,
    currency: String,
    quantityInCart: Int,
    onItemClick: () -> Unit
) {
    Surface(
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .testTag("pos_item_${product.id}")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(LoyverseItemGrey)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = product.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = LoyverseTextDark
                        )
                        if (quantityInCart > 0) {
                            Text(
                                text = stringResource(R.string.in_ticket, quantityInCart),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = LoyverseTopGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                Text(
                    text = MoneyFormat.format(product.price, currency),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = LoyverseTextDark
                )
            }

            HorizontalDivider(
                color = LoyverseDividerGrey,
                thickness = 1.dp
            )
        }
    }
}

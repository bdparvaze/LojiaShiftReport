package com.lojia.shiftreport.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.lojia.shiftreport.R
import com.lojia.shiftreport.ui.theme.Dimens

/**
 * Compact circular profile avatar with an overlapping bottom-end edit/add badge.
 * The outer container remains unclipped so the badge sits cleanly on the perimeter.
 */
@Composable
fun ProfileAvatar(
    bitmap: ImageBitmap?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasPhoto = bitmap != null
    Box(
        modifier = modifier
            .size(Dimens.ProfileAvatarSize)
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = Dimens.AvatarBorderWidth,
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = stringResource(R.string.profile_picture),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = stringResource(R.string.profile_picture),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Dimens.IconXl)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(Dimens.ProfileAvatarBadgeSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .border(
                    width = Dimens.ProfileAvatarBadgeRing,
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (hasPhoto) Icons.Default.Edit else Icons.Default.Add,
                contentDescription = stringResource(
                    if (hasPhoto) R.string.profile_edit_picture_cd else R.string.profile_add_picture_cd
                ),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(Dimens.ProfileAvatarBadgeIcon)
            )
        }
    }
}

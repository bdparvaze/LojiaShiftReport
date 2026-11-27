package com.lojia.shiftreport.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.lojia.shiftreport.R
import com.lojia.shiftreport.auth.AdminAuthDialog
import com.lojia.shiftreport.data.SavedAddress
import com.lojia.shiftreport.report.ReportViewModel
import com.lojia.shiftreport.ui.theme.BackgroundLight
import com.lojia.shiftreport.ui.theme.Dimens
import com.lojia.shiftreport.ui.theme.ErrorRedLight
import com.lojia.shiftreport.ui.theme.LojiaGradientHeader
import com.lojia.shiftreport.ui.theme.OnSurfaceLight
import com.lojia.shiftreport.ui.theme.OnSurfaceVariantLight
import com.lojia.shiftreport.ui.theme.OutlineVariantLight
import com.lojia.shiftreport.ui.theme.PositiveBannerBorder
import com.lojia.shiftreport.ui.theme.PrimaryIndigoDark
import com.lojia.shiftreport.ui.theme.PrimaryIndigoLight
import com.lojia.shiftreport.ui.theme.PureWhite
import com.lojia.shiftreport.ui.theme.SuccessContainer
import com.lojia.shiftreport.ui.theme.SuccessTextDark
import com.lojia.shiftreport.ui.theme.SurfaceLight
import com.lojia.shiftreport.ui.theme.SurfaceVariantLight
import com.lojia.shiftreport.ui.theme.TextHintColor
import com.lojia.shiftreport.ui.theme.cardRise
import com.lojia.shiftreport.ui.theme.depthCard
import com.lojia.shiftreport.ui.theme.depthRaisedButton
import com.lojia.shiftreport.ui.theme.depthSemanticCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedAddressesScreen(
    viewModel: ReportViewModel,
    onBack: () -> Unit,
    onAddNew: () -> Unit,
    onEdit: (String) -> Unit
) {
    BackHandler { onBack() }

    val addresses by viewModel.savedAddresses.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var pendingAuthSubtitle by remember { mutableStateOf<String?>(null) }
    var pendingAuthAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            LojiaGradientHeader(
                shape = RoundedCornerShape(
                    bottomStart = Dimens.RadiusXl,
                    bottomEnd = Dimens.RadiusXl
                ),
                contentPadding = PaddingValues(Dimens.SpacingNone)
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.saved_addresses_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("btn_saved_addresses_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = PureWhite
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = PureWhite,
                        navigationIconContentColor = PureWhite
                    ),
                    modifier = Modifier.statusBarsPadding()
                )
            }
        },
        bottomBar = {
            Surface(
                color = SurfaceLight,
                border = BorderStroke(Dimens.RowDividerWidth, OutlineVariantLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingMd)
                ) {
                    Button(
                        onClick = onAddNew,
                        shape = RoundedCornerShape(Dimens.RadiusPill),
                        elevation = null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = PureWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Dimens.MinTouchTarget)
                            .depthRaisedButton(enabled = true, cornerRadius = Dimens.RadiusPill)
                            .testTag("btn_add_new_address")
                    ) {
                        Text(
                            text = stringResource(R.string.add_new_address),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundLight)
        ) {
            // Encrypted Security Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.SpacingLg, vertical = Dimens.SpacingSm)
                    .depthSemanticCard(
                        backgroundColor = SuccessContainer,
                        borderColor = PositiveBannerBorder
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.SpacingMd, vertical = Dimens.SpacingSm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = SuccessTextDark,
                        modifier = Modifier.size(Dimens.IconSm)
                    )
                    Text(
                        text = stringResource(R.string.all_info_encrypted),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = SuccessTextDark
                    )
                }
            }

            if (addresses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.SpacingHuge),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(Dimens.EmptyStateIconBgSize)
                                .clip(CircleShape)
                                .background(SurfaceVariantLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = TextHintColor,
                                modifier = Modifier.size(Dimens.EmptyStateIconSize)
                            )
                        }
                        Spacer(modifier = Modifier.height(Dimens.SpacingLg))
                        Text(
                            text = stringResource(R.string.no_addresses_yet),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Medium,
                            color = OnSurfaceLight
                        )
                        Spacer(modifier = Modifier.height(Dimens.SpacingXs))
                        Text(
                            text = stringResource(R.string.add_first_address_desc),
                            style = MaterialTheme.typography.titleMedium,
                            color = OnSurfaceVariantLight,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(Dimens.SpacingLg),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
                ) {
                    itemsIndexed(addresses, key = { _, it -> it.id }) { index, address ->
                        AddressCard(
                            address = address,
                            riseIndex = index,
                            setAsDefault = { id ->
                                pendingAuthSubtitle = null
                                pendingAuthAction = {
                                    coroutineScope.launch { viewModel.setDefaultAddress(id) }
                                }
                            },
                            onDelete = { target ->
                                pendingAuthSubtitle = null
                                pendingAuthAction = {
                                    coroutineScope.launch { viewModel.deleteSavedAddress(target) }
                                }
                            },
                            onEdit = onEdit
                        )
                    }
                }
            }
        }
    }

    if (pendingAuthAction != null) {
        AdminAuthDialog(
            title = stringResource(R.string.admin_authorization_required),
            subtitle = pendingAuthSubtitle ?: stringResource(R.string.saved_addresses_title),
            onDismissRequest = {
                pendingAuthSubtitle = null
                pendingAuthAction = null
            },
            onAuthSuccess = {
                val action = pendingAuthAction
                pendingAuthSubtitle = null
                pendingAuthAction = null
                action?.invoke()
            }
        )
    }
}

@Composable
private fun AddressCard(
    address: SavedAddress,
    riseIndex: Int = 0,
    setAsDefault: (String) -> Unit,
    onDelete: (SavedAddress) -> Unit,
    onEdit: (String) -> Unit
) {
    val fullName = "${address.firstName} ${address.lastName}".trim().ifBlank {
        address.addressLabel.ifBlank { stringResource(R.string.address) }
    }
    val displayPhone = if (address.phone.startsWith("+") || address.dialCode.isBlank()) {
        address.phone
    } else if (address.phone.isNotBlank()) {
        "${address.dialCode} ${address.phone}"
    } else {
        ""
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .cardRise(riseIndex)
            .depthCard(accent = address.isDefault)
            .testTag("card_saved_address_${address.id}")
    ) {
        Column(modifier = Modifier.padding(Dimens.CardPadding)) {
            Text(
                text = fullName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceLight
            )
            Spacer(modifier = Modifier.height(Dimens.SpacingXs))
            Text(
                text = displayPhone,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = PrimaryIndigoLight
            )
            Spacer(modifier = Modifier.height(Dimens.SpacingSm))
            Text(
                text = address.addressLine,
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurfaceLight,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (address.arabicAddressLine.isNotBlank()) {
                Spacer(modifier = Modifier.height(Dimens.SpacingXs))
                Text(
                    text = address.arabicAddressLine,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurfaceVariantLight,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(Dimens.SpacingMd))
            HorizontalDivider(thickness = Dimens.RowDividerWidth, color = OutlineVariantLight)
            Spacer(modifier = Modifier.height(Dimens.SpacingSm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Default badge OR "Set as default" link
                if (address.isDefault) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingXs)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = stringResource(R.string.cd_default),
                            tint = PrimaryIndigoDark,
                            modifier = Modifier.size(Dimens.IconSm)
                        )
                        Text(
                            text = stringResource(R.string.cd_default),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = PrimaryIndigoDark
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.set_as_permanent_address_btn),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryIndigoLight,
                        modifier = Modifier
                            .defaultMinSize(minHeight = Dimens.MinTouchTarget)
                            .clickable { setAsDefault(address.id) }
                            .wrapContentHeight(Alignment.CenterVertically)
                            .padding(horizontal = Dimens.SpacingXs)
                            .testTag("btn_set_default_${address.id}")
                    )
                }

                // Right: Delete + Edit
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingXs)) {
                    IconButton(
                        onClick = { onDelete(address) },
                        modifier = Modifier
                            .size(Dimens.MinTouchTarget)
                            .testTag("btn_delete_address_${address.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = stringResource(R.string.cd_delete),
                            tint = ErrorRedLight,
                            modifier = Modifier.size(Dimens.IconAction)
                        )
                    }
                    IconButton(
                        onClick = { onEdit(address.id) },
                        modifier = Modifier
                            .size(Dimens.MinTouchTarget)
                            .testTag("btn_edit_address_${address.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.cd_edit),
                            tint = PrimaryIndigoLight,
                            modifier = Modifier.size(Dimens.IconAction)
                        )
                    }
                }
            }
        }
    }
}

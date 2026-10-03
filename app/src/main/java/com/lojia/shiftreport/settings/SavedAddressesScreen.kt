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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lojia.shiftreport.data.SavedAddress
import com.lojia.shiftreport.report.ReportViewModel
import com.lojia.shiftreport.ui.theme.BackgroundLight
import com.lojia.shiftreport.ui.theme.ErrorRedLight
import com.lojia.shiftreport.ui.theme.OnSurfaceLight
import com.lojia.shiftreport.ui.theme.OnSurfaceVariantLight
import com.lojia.shiftreport.ui.theme.OutlineLight
import com.lojia.shiftreport.ui.theme.OutlineVariantLight
import com.lojia.shiftreport.ui.theme.PrimaryIndigoDark
import com.lojia.shiftreport.ui.theme.PrimaryIndigoLight
import com.lojia.shiftreport.ui.theme.PureBlack
import com.lojia.shiftreport.ui.theme.PureWhite
import com.lojia.shiftreport.ui.theme.SuccessContainer
import com.lojia.shiftreport.ui.theme.SuccessGreen
import com.lojia.shiftreport.ui.theme.SurfaceLight
import com.lojia.shiftreport.ui.theme.SurfaceVariantLight
import com.lojia.shiftreport.ui.theme.TextHintColor

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

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Saved Addresses",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_saved_addresses_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PureWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryIndigoLight,
                    titleContentColor = PureWhite,
                    navigationIconContentColor = PureWhite
                )
            )
        },
        bottomBar = {
            Surface(
                color = SurfaceLight,
                border = BorderStroke(0.5.dp, OutlineVariantLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = onAddNew,
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PureBlack,
                            contentColor = PureWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_add_new_address")
                    ) {
                        Text(
                            text = "Add New Address",
                            fontSize = 15.sp,
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
            Surface(
                color = SuccessContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "All information is encrypted",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SuccessGreen
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
                            .padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(SurfaceVariantLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = TextHintColor,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No addresses yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = OnSurfaceLight
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add your first address to get started",
                            fontSize = 13.sp,
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
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(addresses, key = { it.id }) { address ->
                        AddressCard(
                            address = address,
                            setAsDefault = { id ->
                                coroutineScope.launch { viewModel.setDefaultAddress(id) }
                            },
                            onDelete = { target ->
                                coroutineScope.launch { viewModel.deleteSavedAddress(target) }
                            },
                            onEdit = onEdit
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddressCard(
    address: SavedAddress,
    setAsDefault: (String) -> Unit,
    onDelete: (SavedAddress) -> Unit,
    onEdit: (String) -> Unit
) {
    val fullName = "${address.firstName} ${address.lastName}".trim().ifBlank {
        address.addressLabel.ifBlank { "Store Address" }
    }
    val displayPhone = if (address.phone.startsWith("+") || address.dialCode.isBlank()) {
        address.phone
    } else if (address.phone.isNotBlank()) {
        "${address.dialCode} ${address.phone}"
    } else {
        ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_saved_address_${address.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = BorderStroke(1.dp, OutlineLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = fullName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceLight
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = displayPhone,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = PrimaryIndigoLight
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = address.addressLine,
                fontSize = 14.sp,
                color = OnSurfaceLight,
                lineHeight = 20.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (address.arabicAddressLine.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = address.arabicAddressLine,
                    fontSize = 13.sp,
                    color = OnSurfaceVariantLight,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = OutlineVariantLight)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Default badge OR "Set as default" link
                if (address.isDefault) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Default",
                            tint = PrimaryIndigoDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Default",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = PrimaryIndigoDark
                        )
                    }
                } else {
                    Text(
                        text = "Set as default",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryIndigoLight,
                        modifier = Modifier
                            .clickable { setAsDefault(address.id) }
                            .padding(vertical = 4.dp)
                            .testTag("btn_set_default_${address.id}")
                    )
                }

                // Right: Delete + Edit
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { onDelete(address) },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("btn_delete_address_${address.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete",
                            tint = ErrorRedLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { onEdit(address.id) },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("btn_edit_address_${address.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit",
                            tint = PrimaryIndigoLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

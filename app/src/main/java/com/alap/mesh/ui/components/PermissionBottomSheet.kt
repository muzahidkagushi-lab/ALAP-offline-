package com.alap.mesh.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alap.mesh.mesh.PermissionHelper
import com.alap.mesh.ui.theme.AlapMint
import com.alap.mesh.ui.theme.Emerald50
import com.alap.mesh.ui.theme.Emerald600
import com.alap.mesh.ui.theme.HindSiliguriFontFamily
import com.alap.mesh.ui.theme.Sky50
import com.alap.mesh.ui.theme.Sky600
import com.alap.mesh.ui.theme.Slate200
import com.alap.mesh.ui.theme.Slate50
import com.alap.mesh.ui.theme.Slate500
import com.alap.mesh.ui.theme.Slate700
import com.alap.mesh.ui.theme.Slate800
import com.alap.mesh.ui.theme.Slate900
import com.alap.mesh.ui.theme.Teal600

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionBottomSheet(
    onDismiss: () -> Unit,
    onCompleted: (Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val anyGranted = results.values.any { it }
        onCompleted(anyGranted)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Slate200)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column {
                Text(
                    text = "হার্ডওয়্যার অনুমতি সেটআপ",
                    color = Slate900,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = HindSiliguriFontFamily
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "অফলাইন মেশ চ্যাট, রাডার ও দ্রুত ফাইল শেয়ারিংয়ের জন্য অনুমতি দিন",
                    color = Slate500,
                    fontSize = 12.sp,
                    fontFamily = HindSiliguriFontFamily
                )
            }

            PermissionItemRow(
                icon = Icons.Rounded.Bluetooth,
                iconTint = Sky600,
                iconBg = Sky50,
                title = "ব্লুটুথ (Bluetooth)",
                subtitle = "ডিভাইস খোঁজা ও লো-পাওয়ার মেশ সংযোগ"
            )
            PermissionItemRow(
                icon = Icons.Rounded.Wifi,
                iconTint = Emerald600,
                iconBg = Emerald50,
                title = "ওয়াই-ফাই ও নেয়ারবাই (Nearby)",
                subtitle = "দ্রুত ফাইল আদান-প্রদান ও সংযোগ"
            )
            PermissionItemRow(
                icon = Icons.Rounded.LocationOn,
                iconTint = Teal600,
                iconBg = Color(0xFFF0FDFA),
                title = "লোকেশন ও নেয়ারবাই স্ক্যান",
                subtitle = "মেশ নেটওয়ার্ক ডিসকভারির জন্য প্রয়োজন"
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("permission_cancel_button")
                ) {
                    Text(
                        text = "বাতিল",
                        color = Slate700,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFontFamily
                    )
                }

                BrandGradientButton(
                    text = "অনুমতি দিন ও অ্যাপে যান",
                    onClick = {
                        permissionLauncher.launch(PermissionHelper.requiredRuntimePermissions())
                    },
                    cornerRadius = 16.dp,
                    modifier = Modifier
                        .weight(2f)
                        .testTag("permission_grant_button")
                )
            }
        }
    }
}

@Composable
private fun PermissionItemRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Slate50)
            .border(1.dp, Slate200, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    color = Slate800,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = HindSiliguriFontFamily
                )
                Text(
                    text = subtitle,
                    color = Slate500,
                    fontSize = 10.5.sp,
                    fontFamily = HindSiliguriFontFamily
                )
            }
        }
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = AlapMint,
            modifier = Modifier.size(18.dp)
        )
    }
}

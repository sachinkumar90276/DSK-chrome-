package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.BrowserSettings
import com.example.data.PermissionDecision
import com.example.data.SitePermissionEntity
import com.example.data.SitePermissionType

@Composable
fun PermissionsScreen(
    settings: BrowserSettings,
    sitePermissions: List<SitePermissionEntity>,
    onBack: () -> Unit,
    onUpdateDefaultPermission: (SitePermissionType, PermissionDecision) -> Unit,
    onUpdateSitePermission: (String, SitePermissionType, PermissionDecision) -> Unit,
    onRemoveSite: (String) -> Unit
) {
    BackHandler(onBack = onBack)

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .navigationBarsPadding()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("permissions_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Website Permissions", style = MaterialTheme.typography.titleLarge)
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "DEFAULT POLICIES",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            DefaultPermissionRow(
                                type = SitePermissionType.CAMERA,
                                current = settings.defaultCameraDecision,
                                onSelect = { onUpdateDefaultPermission(SitePermissionType.CAMERA, it) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            DefaultPermissionRow(
                                type = SitePermissionType.MICROPHONE,
                                current = settings.defaultMicDecision,
                                onSelect = { onUpdateDefaultPermission(SitePermissionType.MICROPHONE, it) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            DefaultPermissionRow(
                                type = SitePermissionType.LOCATION,
                                current = settings.defaultLocationDecision,
                                onSelect = { onUpdateDefaultPermission(SitePermissionType.LOCATION, it) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            DefaultPermissionRow(
                                type = SitePermissionType.NOTIFICATIONS,
                                current = settings.defaultNotificationsDecision,
                                onSelect = { onUpdateDefaultPermission(SitePermissionType.NOTIFICATIONS, it) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            DefaultPermissionRow(
                                type = SitePermissionType.DOWNLOADS,
                                current = if (settings.askBeforeDownloading) PermissionDecision.ASK else PermissionDecision.ALLOW,
                                onSelect = { onUpdateDefaultPermission(SitePermissionType.DOWNLOADS, it) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            DefaultPermissionRow(
                                type = SitePermissionType.POPUPS,
                                current = if (settings.blockPopups) PermissionDecision.BLOCK else PermissionDecision.ALLOW,
                                onSelect = { onUpdateDefaultPermission(SitePermissionType.POPUPS, it) }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "WEBSITES WITH CUSTOM ACCESS (${sitePermissions.size})",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (sitePermissions.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No sites have custom permission overrides yet. When a website asks for Camera, Microphone, or Location and you choose Remember, it appears here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(sitePermissions, key = { it.host }) { site ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = site.host,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    IconButton(onClick = { onRemoveSite(site.host) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Reset site permissions",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                SitePermissionType.entries.forEach { type ->
                                    val decision = site.decisionFor(type)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val next = when (decision) {
                                                    PermissionDecision.ASK -> PermissionDecision.ALLOW
                                                    PermissionDecision.ALLOW -> PermissionDecision.BLOCK
                                                    PermissionDecision.BLOCK -> PermissionDecision.ASK
                                                }
                                                onUpdateSitePermission(site.host, type, next)
                                            }
                                            .padding(vertical = 6.dp)
                                    ) {
                                        Text(type.label, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = decision.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = when (decision) {
                                                PermissionDecision.ALLOW -> MaterialTheme.colorScheme.primary
                                                PermissionDecision.BLOCK -> MaterialTheme.colorScheme.error
                                                PermissionDecision.ASK -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultPermissionRow(
    type: SitePermissionType,
    current: PermissionDecision,
    onSelect: (PermissionDecision) -> Unit
) {
    val icon = when (type) {
        SitePermissionType.CAMERA -> Icons.Default.CameraAlt
        SitePermissionType.MICROPHONE -> Icons.Default.Mic
        SitePermissionType.LOCATION -> Icons.Default.LocationOn
        SitePermissionType.NOTIFICATIONS -> Icons.Default.Notifications
        SitePermissionType.DOWNLOADS -> Icons.Default.Download
        SitePermissionType.POPUPS -> Icons.Default.OpenInNew
    }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(10.dp))
            Text(type.label, style = MaterialTheme.typography.titleSmall)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PermissionDecision.entries.forEach { decision ->
                FilterChip(
                    selected = current == decision,
                    onClick = { onSelect(decision) },
                    label = { Text(decision.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}

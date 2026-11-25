package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.browser.BrowserTab
import com.example.browser.ContextLinkAction
import com.example.browser.FindInPageState
import com.example.browser.PendingDownloadPrompt
import com.example.browser.PendingSafeBrowsingWarning
import com.example.browser.PendingSitePermissionPrompt
import com.example.browser.PendingSslWarning
import com.example.data.BookmarkEntity
import com.example.download.DownloadEngine
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SecureGreen
import com.example.ui.theme.WarningAmber

@Composable
fun FindInPageBar(
    state: FindInPageState,
    onQueryChange: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    BasicTextField(
                        value = state.query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("find_in_page_input"),
                        decorationBox = { inner ->
                            if (state.query.isEmpty()) {
                                Text(
                                    "Find in page...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            inner()
                        }
                    )
                    if (state.query.isNotEmpty()) {
                        Text(
                            text = "${state.currentMatchIndex}/${state.totalMatches}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            IconButton(onClick = onPrevious, modifier = Modifier.testTag("find_prev_button")) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous match")
            }
            IconButton(onClick = onNext, modifier = Modifier.testTag("find_next_button")) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next match")
            }
            IconButton(onClick = onClose, modifier = Modifier.testTag("find_close_button")) {
                Icon(Icons.Default.Close, contentDescription = "Close find in page")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityConnectionSheet(
    tab: BrowserTab,
    onDismiss: () -> Unit,
    onOpenPermissions: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when {
                        tab.isHomepage -> Icons.Default.Shield
                        tab.isSecure -> Icons.Default.Lock
                        else -> Icons.Default.WarningAmber
                    },
                    contentDescription = null,
                    tint = when {
                        tab.isHomepage -> MaterialTheme.colorScheme.primary
                        tab.isSecure -> SecureGreen
                        else -> WarningAmber
                    },
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = when {
                            tab.isHomepage -> "DSK CHROME Local Page"
                            tab.isSecure -> "Connection is Secure (HTTPS)"
                            else -> "Connection is Not Secure (HTTP)"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (tab.securityHost.isNotBlank()) {
                        Text(
                            text = tab.securityHost,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = when {
                    tab.isHomepage -> "You are viewing a built-in DSK CHROME screen."
                    tab.isSecure -> "Information sent to and from this website is encrypted with TLS."
                    else -> "This website does not use HTTPS encryption. Avoid entering passwords or payment information."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (tab.isSecure && tab.certificateIssuer.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Certificate Issued By: ${tab.certificateIssuer}", style = MaterialTheme.typography.bodySmall)
                        if (tab.certificateValidTo.isNotBlank()) {
                            Text("Valid Until: ${tab.certificateValidTo}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = {
                    onDismiss()
                    onOpenPermissions()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Manage Website Permissions")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun SitePermissionPromptDialog(
    prompt: PendingSitePermissionPrompt,
    onResolve: (allow: Boolean, remember: Boolean) -> Unit
) {
    var rememberChoice by remember { mutableStateOf(true) }
    val permNames = prompt.types.joinToString(" & ") { it.label }
    AlertDialog(
        onDismissRequest = { onResolve(false, false) },
        title = { Text("${prompt.host} wants permission") },
        text = {
            Column {
                Text("Allow this website to access your $permNames?")
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { rememberChoice = !rememberChoice }
                ) {
                    Checkbox(checked = rememberChoice, onCheckedChange = { rememberChoice = it })
                    Text("Remember my decision for this site", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onResolve(true, rememberChoice) }) { Text("Allow") }
        },
        dismissButton = {
            TextButton(onClick = { onResolve(false, rememberChoice) }) { Text("Block") }
        }
    )
}

@Composable
fun DownloadConfirmDialog(
    prompt: PendingDownloadPrompt,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Download File?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(prompt.fileName, style = MaterialTheme.typography.titleSmall)
                if (prompt.contentLength > 0L) {
                    Text(
                        text = "Size: ${DownloadEngine.formatBytes(prompt.contentLength)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = prompt.url,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Download") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SslWarningDialog(
    warning: PendingSslWarning,
    onResolve: (proceed: Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onResolve(false) },
        icon = { Icon(Icons.Default.WarningAmber, contentDescription = null, tint = DangerRed) },
        title = { Text("Your Connection Is Not Private") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(warning.errorDescription, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = warning.url,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onResolve(false) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Back to Safety")
            }
        },
        dismissButton = {
            TextButton(onClick = { onResolve(true) }) {
                Text("Proceed (Unsafe)", color = DangerRed)
            }
        }
    )
}

@Composable
fun SafeBrowsingWarningDialog(
    warning: PendingSafeBrowsingWarning,
    onResolve: (proceed: Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onResolve(false) },
        icon = { Icon(Icons.Default.Shield, contentDescription = null, tint = DangerRed) },
        title = { Text("Suspicious Website Warning") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(warning.reason, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = warning.url,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        confirmButton = {
            Button(onClick = { onResolve(false) }) { Text("Back to Safety") }
        },
        dismissButton = {
            TextButton(onClick = { onResolve(true) }) {
                Text("Visit Anyway", color = DangerRed)
            }
        }
    )
}

@Composable
fun ContextLinkActionDialog(
    action: ContextLinkAction,
    onOpenNewTab: (String) -> Unit,
    onDownload: (String) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (action.isImage) "Image Options" else "Link Options") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = action.url,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                ContextActionItem(Icons.Default.OpenInNew, "Open in New Tab") {
                    onOpenNewTab(action.url)
                    onDismiss()
                }
                ContextActionItem(
                    Icons.Default.Download,
                    if (action.isImage) "Download Image" else "Download Link"
                ) {
                    onDownload(action.url)
                    onDismiss()
                }
                ContextActionItem(Icons.Default.ContentCopy, "Copy Address") {
                    onCopy(action.url)
                    onDismiss()
                }
                ContextActionItem(Icons.Default.Share, "Share Address") {
                    onShare(action.url)
                    onDismiss()
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ContextActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun BookmarkEditDialog(
    initial: BookmarkEntity,
    onSave: (id: Long, title: String, url: String, folder: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember(initial) { mutableStateOf(initial.title) }
    var url by remember(initial) { mutableStateOf(initial.url) }
    var folder by remember(initial) { mutableStateOf(initial.folder.ifBlank { "Bookmarks" }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "Add Bookmark" else "Edit Bookmark") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = folder,
                    onValueChange = { folder = it },
                    label = { Text("Folder") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank() && url.isNotBlank(),
                onClick = { onSave(initial.id, title, url, folder) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddShortcutDialog(
    onAdd: (title: String, url: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Homepage Shortcut") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (e.g., Wikipedia)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("shortcut_title_input")
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Website URL") },
                    placeholder = { Text("https://wikipedia.org") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("shortcut_url_input")
                )
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank() && url.isNotBlank(),
                onClick = { onAdd(title, url) },
                modifier = Modifier.testTag("shortcut_save_button")
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AboutDskChromeScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    var showPolicyType by remember { mutableStateOf<String?>(null) }

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
                IconButton(onClick = onBack, modifier = Modifier.testTag("about_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("About DSK CHROME", style = MaterialTheme.typography.titleLarge)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_dsk_brand_emblem),
                    contentDescription = "DSK CHROME Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "DSK CHROME",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Version 1.0.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Developed by DSK OFFICIAL",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPolicyType = "Privacy Policy" }
                                .padding(16.dp)
                        ) {
                            Text("Privacy Policy", style = MaterialTheme.typography.bodyLarge)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPolicyType = "Terms & Conditions" }
                                .padding(16.dp)
                        ) {
                            Text("Terms & Conditions", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }

    if (showPolicyType != null) {
        AlertDialog(
            onDismissRequest = { showPolicyType = null },
            title = { Text("DSK CHROME ${showPolicyType}") },
            text = {
                Text(
                    text = if (showPolicyType == "Privacy Policy") {
                        "DSK CHROME by DSK OFFICIAL stores bookmarks, history, tabs, downloads, and site permissions locally on your device. Private Incognito tabs do not record browsing history and clear session data when closed. When you use DSK AI Search or Translate Page, only the query or webpage excerpt you request is sent securely via Firebase AI Logic."
                    } else {
                        "DSK CHROME is provided by DSK OFFICIAL for lawful web browsing. Downloaded media and webpages remain subject to the copyrights and terms of their respective publishers. Always verify HTTPS security indicators before submitting sensitive information."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showPolicyType = null }) { Text("Close") }
            }
        )
    }
}

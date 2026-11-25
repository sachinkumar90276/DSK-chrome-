package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.browser.BrowserOverlay
import com.example.browser.BrowserTab
import com.example.browser.BrowserViewModel
import com.example.browser.SearchSuggestionItem
import com.example.browser.UrlUtils
import com.example.data.SearchEngine
import com.example.ui.theme.SecureGreen
import com.example.ui.theme.WarningAmber

@Composable
fun BrowserAddressBar(
    tab: BrowserTab,
    searchEngine: SearchEngine,
    isBookmarked: Boolean,
    suggestions: List<SearchSuggestionItem>,
    applyTopStatusInset: Boolean,
    onQueryChange: (String) -> Unit,
    onSubmitUrl: (String) -> Unit,
    onClearSuggestions: () -> Unit,
    onReloadOrStop: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenSecurityInfo: () -> Unit,
    onVoiceSearch: () -> Unit,
    onCopyUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var isEditing by remember { mutableStateOf(false) }
    var omniboxText by remember(tab.id, tab.url, tab.isHomepage) {
        mutableStateOf(if (tab.isHomepage) "" else tab.url)
    }

    LaunchedEffect(tab.url, isEditing) {
        if (!isEditing) {
            omniboxText = if (tab.isHomepage) "" else tab.url
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (applyTopStatusInset) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                // Address / Search Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            width = 1.dp,
                            color = if (isEditing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clickable {
                            isEditing = true
                            focusRequester.requestFocus()
                        }
                        .padding(horizontal = 10.dp)
                ) {
                    // Security / Mode Indicator
                    IconButton(
                        onClick = onOpenSecurityInfo,
                        modifier = Modifier
                            .size(36.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("address_security_button")
                    ) {
                        when {
                            tab.isIncognito -> Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = "Private Incognito tab",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            tab.isHomepage -> Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            tab.isSecure -> Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Secure HTTPS connection",
                                tint = SecureGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            else -> Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = "Not secure HTTP connection",
                                tint = WarningAmber,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (!isEditing && !tab.isHomepage) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        isEditing = true
                                        omniboxText = tab.url
                                        focusRequester.requestFocus()
                                    }
                            ) {
                                Text(
                                    text = tab.title.ifBlank { UrlUtils.extractHost(tab.url) },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = UrlUtils.extractHost(tab.url).ifBlank { tab.url },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            BasicTextField(
                                value = omniboxText,
                                onValueChange = {
                                    omniboxText = it
                                    onQueryChange(it)
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        isEditing = false
                                        focusManager.clearFocus()
                                        onSubmitUrl(omniboxText)
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { state ->
                                        isEditing = state.isFocused
                                        if (!state.isFocused) onClearSuggestions()
                                    }
                                    .testTag("omnibox_input"),
                                decorationBox = { innerTextField ->
                                    if (omniboxText.isEmpty()) {
                                        Text(
                                            text = "Search ${searchEngine.displayName} or enter URL",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }

                    if (isEditing && omniboxText.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                omniboxText = ""
                                onQueryChange("")
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("omnibox_clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear address bar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else if (!tab.isHomepage) {
                        IconButton(
                            onClick = onToggleBookmark,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("omnibox_bookmark_button")
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onVoiceSearch,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("omnibox_voice_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice search",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Reload / Stop button
                IconButton(
                    onClick = onReloadOrStop,
                    enabled = !tab.isHomepage,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("omnibox_reload_button")
                ) {
                    Icon(
                        imageVector = if (tab.isLoading) Icons.Default.Close else Icons.Default.Refresh,
                        contentDescription = if (tab.isLoading) "Stop loading" else "Reload webpage",
                        tint = if (tab.isHomepage) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }

            // Copy / Paste bar while editing omnibox
            AnimatedVisibility(visible = isEditing) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    if (tab.url.isNotBlank()) {
                        QuickPillAction(
                            icon = Icons.Default.ContentCopy,
                            label = "Copy URL",
                            testTag = "omnibox_copy_button",
                            onClick = { onCopyUrl(tab.url) }
                        )
                    }
                    QuickPillAction(
                        icon = Icons.Default.ContentPaste,
                        label = "Paste & Go",
                        testTag = "omnibox_paste_button",
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipText = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                            if (clipText.isNotBlank()) {
                                omniboxText = clipText
                                isEditing = false
                                focusManager.clearFocus()
                                onSubmitUrl(clipText)
                            }
                        }
                    )
                }
            }

            // Live search & history suggestions
            AnimatedVisibility(visible = isEditing && suggestions.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    suggestions.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isEditing = false
                                    focusManager.clearFocus()
                                    onSubmitUrl(item.url ?: item.text)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isHistory) Icons.Default.History else Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!item.url.isNullOrBlank()) {
                                    Text(
                                        text = item.url,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    omniboxText = item.url ?: item.text
                                    onQueryChange(omniboxText)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NorthWest,
                                    contentDescription = "Fill into address bar",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (tab.isLoading) {
                LinearProgressIndicator(
                    progress = { (tab.progress / 100f).coerceIn(0.05f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            } else {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun QuickPillAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChromeUnifiedBar(
    viewModel: BrowserViewModel,
    tab: BrowserTab,
    searchEngine: SearchEngine,
    suggestions: List<SearchSuggestionItem>,
    tabCount: Int,
    isAtBottom: Boolean,
    onQueryChange: (String) -> Unit,
    onSubmitUrl: (String) -> Unit,
    onClearSuggestions: () -> Unit,
    onOpenSecurityInfo: () -> Unit,
    onVoiceSearch: () -> Unit,
    onCopyUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val closedTabs by viewModel.closedTabs.collectAsStateWithLifecycle()
    var isEditing by remember { mutableStateOf(false) }
    var showTabActionsMenu by remember { mutableStateOf(false) }
    var omniboxText by remember(tab.id, tab.url, tab.isHomepage) {
        mutableStateOf(if (tab.isHomepage) "" else tab.url)
    }

    LaunchedEffect(tab.url, isEditing) {
        if (!isEditing) {
            omniboxText = if (tab.isHomepage) "" else tab.url
        }
    }

    val editingExtras: @Composable () -> Unit = {
        AnimatedVisibility(visible = isEditing) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                if (tab.url.isNotBlank()) {
                    QuickPillAction(
                        icon = Icons.Default.ContentCopy,
                        label = "Copy URL",
                        testTag = "omnibox_copy_button",
                        onClick = { onCopyUrl(tab.url) }
                    )
                }
                QuickPillAction(
                    icon = Icons.Default.ContentPaste,
                    label = "Paste & Go",
                    testTag = "omnibox_paste_button",
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipText = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                        if (clipText.isNotBlank()) {
                            omniboxText = clipText
                            isEditing = false
                            focusManager.clearFocus()
                            onSubmitUrl(clipText)
                        }
                    }
                )
            }
        }

        AnimatedVisibility(visible = isEditing && suggestions.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                suggestions.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isEditing = false
                                focusManager.clearFocus()
                                onSubmitUrl(item.url ?: item.text)
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isHistory) Icons.Default.History else Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!item.url.isNullOrBlank()) {
                                Text(
                                    text = item.url,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                omniboxText = item.url ?: item.text
                                onQueryChange(omniboxText)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NorthWest,
                                contentDescription = "Fill into address bar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    val progressOrDivider: @Composable () -> Unit = {
        if (tab.isLoading) {
            LinearProgressIndicator(
                progress = { (tab.progress / 100f).coerceIn(0.05f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        } else {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = if (isAtBottom) 8.dp else 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (!isAtBottom) {
                        Modifier.windowInsetsPadding(WindowInsets.statusBars)
                    } else {
                        Modifier
                    }
                )
        ) {
            if (isAtBottom) {
                progressOrDivider()
                editingExtras()
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isAtBottom) Modifier.navigationBarsPadding() else Modifier)
                    .padding(horizontal = 6.dp, vertical = 7.dp)
            ) {
                IconButton(
                    onClick = viewModel::goHome,
                    modifier = Modifier
                        .size(42.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("nav_home_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = if (tab.isHomepage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                // Chrome-style Rounded Address / Search Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            width = 1.dp,
                            color = if (isEditing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .clickable {
                            isEditing = true
                            focusRequester.requestFocus()
                        }
                        .padding(horizontal = 8.dp)
                ) {
                    IconButton(
                        onClick = onOpenSecurityInfo,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("address_security_button")
                    ) {
                        when {
                            tab.isIncognito -> Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = "Private Incognito tab",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(17.dp)
                            )
                            tab.isHomepage -> Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                            tab.isSecure -> Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Secure HTTPS connection",
                                tint = SecureGreen,
                                modifier = Modifier.size(17.dp)
                            )
                            else -> Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = "Not secure HTTP connection",
                                tint = WarningAmber,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (!isEditing && !tab.isHomepage) {
                            Text(
                                text = tab.url.removePrefix("https://").removePrefix("http://").ifBlank { tab.title },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        isEditing = true
                                        omniboxText = tab.url
                                        focusRequester.requestFocus()
                                    }
                            )
                        } else {
                            BasicTextField(
                                value = omniboxText,
                                onValueChange = {
                                    omniboxText = it
                                    onQueryChange(it)
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        isEditing = false
                                        focusManager.clearFocus()
                                        onSubmitUrl(omniboxText)
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { state ->
                                        isEditing = state.isFocused
                                        if (!state.isFocused) onClearSuggestions()
                                    }
                                    .testTag("omnibox_input"),
                                decorationBox = { innerTextField ->
                                    if (omniboxText.isEmpty()) {
                                        Text(
                                            text = "Search ${searchEngine.displayName} or type URL",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }

                    if (isEditing && omniboxText.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                omniboxText = ""
                                onQueryChange("")
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("omnibox_clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear address bar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    } else if (tab.isHomepage) {
                        IconButton(
                            onClick = onVoiceSearch,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("omnibox_voice_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice search",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(2.dp))

                IconButton(
                    onClick = { viewModel.setOverlay(BrowserOverlay.AiSearch) },
                    modifier = Modifier
                        .size(40.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("chrome_bar_ai_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "DSK AI Search",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Tab Counter Badge (tap: Tab Overview, long-press: Quick Tab Actions)
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onClick = { viewModel.setOverlay(BrowserOverlay.Tabs) },
                                onLongClick = { showTabActionsMenu = true },
                                onClickLabel = "Open tabs overview",
                                onLongClickLabel = "Tab actions"
                            )
                            .minimumInteractiveComponentSize()
                            .testTag("nav_tabs_button")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(22.dp)
                                .border(
                                    width = 1.8.dp,
                                    color = if (tab.isIncognito) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    shape = RoundedCornerShape(6.dp)
                                )
                        ) {
                            Text(
                                text = if (tabCount > 99) "99" else tabCount.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (tab.isIncognito) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showTabActionsMenu,
                        onDismissRequest = { showTabActionsMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("New Tab") },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                            onClick = {
                                showTabActionsMenu = false
                                viewModel.createNewTab(isIncognito = false)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("New Private Tab") },
                            leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) },
                            onClick = {
                                showTabActionsMenu = false
                                viewModel.createNewTab(isIncognito = true)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate Tab") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                showTabActionsMenu = false
                                viewModel.duplicateTab(tab.id)
                            }
                        )
                        if (closedTabs.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Reopen Closed Tab") },
                                leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null) },
                                onClick = {
                                    showTabActionsMenu = false
                                    viewModel.restoreClosedTab()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Close Current Tab") },
                            leadingIcon = { Icon(Icons.Default.Close, contentDescription = null) },
                            onClick = {
                                showTabActionsMenu = false
                                viewModel.closeTab(tab.id)
                            }
                        )
                    }
                }

                // 3-Dot Menu
                IconButton(
                    onClick = { viewModel.setOverlay(BrowserOverlay.Menu) },
                    modifier = Modifier
                        .size(40.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("nav_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (!isAtBottom) {
                editingExtras()
                progressOrDivider()
            }
        }
    }
}

@Composable
fun BrowserBottomNavBar(
    viewModel: BrowserViewModel,
    isAtTop: Boolean = false,
    applyTopStatusInset: Boolean = false,
    modifier: Modifier = Modifier
) {
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val closedTabs by viewModel.closedTabs.collectAsStateWithLifecycle()
    val currentModeTabCount = remember(tabs, activeTab.isIncognito) {
        tabs.count { it.isIncognito == activeTab.isIncognito }
    }

    BrowserBottomNavBar(
        isHomepage = activeTab.isHomepage,
        isLoading = activeTab.isLoading,
        canGoBack = activeTab.canGoBack || !activeTab.isHomepage,
        canGoForward = activeTab.canGoForward,
        canReload = !activeTab.isHomepage && activeTab.url.isNotBlank(),
        tabCount = currentModeTabCount,
        isIncognito = activeTab.isIncognito,
        canRestoreClosedTab = closedTabs.isNotEmpty(),
        isAtTop = isAtTop,
        applyTopStatusInset = applyTopStatusInset,
        onBack = { viewModel.goBack() },
        onForward = viewModel::goForward,
        onReloadOrStop = viewModel::reloadOrStop,
        onHome = viewModel::goHome,
        onNewTab = { viewModel.createNewTab(isIncognito = activeTab.isIncognito) },
        onNewIncognitoTab = { viewModel.createNewTab(isIncognito = true) },
        onDuplicateCurrentTab = { viewModel.duplicateTab(activeTab.id) },
        onCloseCurrentTab = { viewModel.closeTab(activeTab.id) },
        onRestoreClosedTab = viewModel::restoreClosedTab,
        onOpenTabs = { viewModel.setOverlay(BrowserOverlay.Tabs) },
        onOpenMenu = { viewModel.setOverlay(BrowserOverlay.Menu) },
        modifier = modifier
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowserBottomNavBar(
    isHomepage: Boolean,
    isLoading: Boolean,
    canGoBack: Boolean,
    canGoForward: Boolean,
    canReload: Boolean,
    tabCount: Int,
    isIncognito: Boolean,
    canRestoreClosedTab: Boolean,
    isAtTop: Boolean = false,
    applyTopStatusInset: Boolean = false,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReloadOrStop: () -> Unit,
    onHome: () -> Unit,
    onNewTab: () -> Unit,
    onNewIncognitoTab: () -> Unit,
    onDuplicateCurrentTab: () -> Unit,
    onCloseCurrentTab: () -> Unit,
    onRestoreClosedTab: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showTabActionsMenu by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isAtTop) 4.dp else 6.dp,
        shadowElevation = if (isAtTop) 2.dp else 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isAtTop && applyTopStatusInset) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier)
        ) {
            if (!isAtTop) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            }
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (!isAtTop) Modifier.navigationBarsPadding() else Modifier)
                    .height(if (isAtTop) 48.dp else 58.dp)
                    .padding(horizontal = 4.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    enabled = canGoBack,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("nav_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (canGoBack) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        }
                    )
                }

                IconButton(
                    onClick = onForward,
                    enabled = canGoForward,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("nav_forward_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Forward",
                        tint = if (canGoForward) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        }
                    )
                }

                IconButton(
                    onClick = onReloadOrStop,
                    enabled = canReload,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("nav_reload_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Stop loading",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload webpage",
                                tint = if (canReload) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                }
                            )
                        }
                    }
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onHome)
                        .testTag("nav_home_button")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isHomepage) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = if (isHomepage) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onNewTab,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("nav_new_tab_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Tab",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .combinedClickable(
                                onClick = onOpenTabs,
                                onLongClick = { showTabActionsMenu = true },
                                onClickLabel = "Open tabs overview",
                                onLongClickLabel = "Tab actions"
                            )
                            .minimumInteractiveComponentSize()
                            .testTag("nav_tabs_button")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(23.dp)
                                .border(
                                    width = 1.8.dp,
                                    color = if (isIncognito) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                )
                        ) {
                            Text(
                                text = if (tabCount > 99) "99" else tabCount.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isIncognito) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showTabActionsMenu,
                        onDismissRequest = { showTabActionsMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("New Tab") },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                            onClick = {
                                showTabActionsMenu = false
                                onNewTab()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("New Private Tab") },
                            leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) },
                            onClick = {
                                showTabActionsMenu = false
                                onNewIncognitoTab()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate Tab") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                showTabActionsMenu = false
                                onDuplicateCurrentTab()
                            }
                        )
                        if (canRestoreClosedTab) {
                            DropdownMenuItem(
                                text = { Text("Reopen Closed Tab") },
                                leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null) },
                                onClick = {
                                    showTabActionsMenu = false
                                    onRestoreClosedTab()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Close Current Tab") },
                            leadingIcon = { Icon(Icons.Default.Close, contentDescription = null) },
                            onClick = {
                                showTabActionsMenu = false
                                onCloseCurrentTab()
                            }
                        )
                    }
                }

                IconButton(
                    onClick = onOpenMenu,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("nav_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if (isAtTop) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun BrowserTabStrip(
    tabs: List<BrowserTab>,
    activeTabId: String,
    applyTopStatusInset: Boolean,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onNewTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (applyTopStatusInset) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                LazyRow(
                    contentPadding = PaddingValues(start = 10.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    items(tabs, key = { it.id }) { tab ->
                        val isActive = tab.id == activeTabId
                        val containerColor = if (isActive) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                        }
                        val borderColor = if (isActive) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        }
                        val contentColor = if (isActive) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .widthIn(min = 120.dp, max = 180.dp)
                                .height(34.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(containerColor)
                                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                .clickable { onSelectTab(tab.id) }
                                .padding(start = 10.dp, end = 4.dp)
                                .testTag("tab_strip_item_${tab.id}")
                        ) {
                            Icon(
                                imageVector = when {
                                    tab.isIncognito -> Icons.Default.VisibilityOff
                                    tab.isHomepage -> Icons.Default.Home
                                    else -> Icons.Default.Public
                                },
                                contentDescription = null,
                                tint = if (isActive) MaterialTheme.colorScheme.primary else contentColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.title.ifBlank {
                                    if (tab.isHomepage) "New Tab" else UrlUtils.extractHost(tab.url).ifBlank { "Tab" }
                                },
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium
                                ),
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onCloseTab(tab.id) },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("tab_strip_close_${tab.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close ${tab.title}",
                                    tint = contentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onNewTab,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("tab_strip_new_tab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New tab",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
}


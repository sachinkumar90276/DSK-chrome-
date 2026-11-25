package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.webkit.WebChromeClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.configureAppCheck
import com.example.browser.BrowserViewModel
import com.example.ui.screens.BrowserScreen
import com.example.ui.theme.DskChromeTheme
import java.io.File
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: BrowserViewModel by viewModels()

    private var pendingRuntimePermissionCallback: ((Boolean) -> Unit)? = null
    private var pendingCameraCaptureUri: Uri? = null

    private val runtimePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        pendingRuntimePermissionCallback?.invoke(allGranted)
        pendingRuntimePermissionCallback = null
    }

    private val voiceSearchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.submitOmniboxInput(spoken)
            }
        }
    }

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onFileChooserResult(uris.toTypedArray())
        } else {
            viewModel.onFileChooserResult(null)
        }
    }

    private val cameraCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraCaptureUri
        pendingCameraCaptureUri = null
        if (success && uri != null) {
            viewModel.onFileChooserResult(arrayOf(uri))
        } else {
            viewModel.onFileChooserResult(null)
        }
    }

    private val wallpaperPickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.setCustomWallpaperUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        viewModel.onRequestRuntimeOsPermissions = { perms, callback ->
            val missing = perms.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (missing.isEmpty()) {
                callback(true)
            } else {
                pendingRuntimePermissionCallback = callback
                runtimePermissionsLauncher.launch(missing.toTypedArray())
            }
        }

        viewModel.onLaunchFileChooserUi = { params ->
            launchWebFileChooser(params)
        }

        if (intent?.action == Intent.ACTION_VIEW) {
            viewModel.handleIncomingIntentUrl(intent?.dataString)
        }

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()

            DskChromeTheme(
                themeMode = settings.themeMode,
                isIncognito = activeTab.isIncognito
            ) {
                BrowserScreen(
                    viewModel = viewModel,
                    onVoiceSearch = ::launchVoiceSearch,
                    onUploadWallpaper = {
                        wallpaperPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onAddToHomeScreen = ::pinWebsiteToHomeScreen
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_VIEW) {
            viewModel.handleIncomingIntentUrl(intent.dataString)
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.webViewPool.pauseAll()
    }

    override fun onResume() {
        super.onResume()
        viewModel.webViewPool.resumeTab(viewModel.activeTab.value.id)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            viewModel.onTrimMemory()
        }
    }

    private fun launchVoiceSearch() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search on DSK CHROME")
        }
        if (intent.resolveActivity(packageManager) != null) {
            voiceSearchLauncher.launch(intent)
        } else {
            viewModel.showMessage("Voice search service is not available on this device")
        }
    }

    private fun launchWebFileChooser(params: WebChromeClient.FileChooserParams?) {
        val isCapture = params?.isCaptureEnabled == true
        val acceptsImage = params?.acceptTypes?.any { it.contains("image", ignoreCase = true) } == true
        if (isCapture && acceptsImage) {
            val hasCamera = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            if (hasCamera) {
                startCameraCapture()
            } else {
                pendingRuntimePermissionCallback = { granted ->
                    if (granted) startCameraCapture() else viewModel.onFileChooserResult(null)
                }
                runtimePermissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA))
            }
            return
        }

        val mimeTypes = params?.acceptTypes
            ?.filter { it.isNotBlank() }
            ?.toTypedArray()
            ?.takeIf { it.isNotEmpty() }
            ?: arrayOf("*/*")
        fileChooserLauncher.launch(mimeTypes)
    }

    private fun startCameraCapture() {
        val captureFile = File(cacheDir, "upload_capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", captureFile)
        pendingCameraCaptureUri = uri
        cameraCaptureLauncher.launch(uri)
    }

    private fun pinWebsiteToHomeScreen(title: String, url: String) {
        if (url.isBlank()) {
            viewModel.showMessage("Open a website first to pin it")
            return
        }
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(this)) {
            val launchIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url), this, MainActivity::class.java)
            val shortcut = ShortcutInfoCompat.Builder(this, "dsk_pin_${url.hashCode()}")
                .setShortLabel(title.ifBlank { "Web Shortcut" }.take(24))
                .setLongLabel(title.ifBlank { url })
                .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher))
                .setIntent(launchIntent)
                .build()
            ShortcutManagerCompat.requestPinShortcut(this, shortcut, null)
            viewModel.showMessage("Shortcut added to Home screen")
        } else {
            viewModel.showMessage("Home screen pinning is not supported by your launcher")
        }
    }
}

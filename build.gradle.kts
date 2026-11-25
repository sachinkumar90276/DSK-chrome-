// Top-level build file where you can add configuration options common to all sub-projects/modules.
import java.util.Base64

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
}

val rootDirFile = layout.projectDirectory.asFile

val ensureDebugKeystore by tasks.registering {
  val keystoreFile = File(rootDirFile, "debug.keystore")
  val base64File = File(rootDirFile, "debug.keystore.base64")
  val userAndroidDebugKeystore = File(System.getProperty("user.home"), ".android/debug.keystore")

  onlyIf { !keystoreFile.exists() || keystoreFile.length() == 0L }

  doLast {
    if (keystoreFile.exists() && keystoreFile.length() > 0L) return@doLast

    if (base64File.exists() && base64File.length() > 0L) {
      val decoded = Base64.getMimeDecoder().decode(base64File.readText(Charsets.UTF_8))
      keystoreFile.writeBytes(decoded)
    } else if (userAndroidDebugKeystore.exists() && userAndroidDebugKeystore.length() > 0L) {
      userAndroidDebugKeystore.copyTo(keystoreFile, overwrite = true)
    } else {
      val process = ProcessBuilder(
        "keytool",
        "-genkeypair",
        "-noprompt",
        "-keystore", keystoreFile.absolutePath,
        "-storepass", "android",
        "-alias", "androiddebugkey",
        "-keypass", "android",
        "-keyalg", "RSA",
        "-keysize", "2048",
        "-validity", "10000",
        "-dname", "CN=Android Debug,O=Android,C=US"
      ).redirectErrorStream(true).start()
      val output = process.inputStream.bufferedReader().readText()
      val exitCode = process.waitFor()
      if (exitCode != 0) {
        throw GradleException("Failed to generate standard debug keystore (exit $exitCode): $output")
      }
    }
  }
}

subprojects {
  tasks.matching { it.name == "validateSigningDebug" }.configureEach {
    dependsOn(ensureDebugKeystore)
  }
}

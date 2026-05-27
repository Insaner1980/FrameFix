import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.detekt)
  alias(libs.plugins.owasp.dependency.check)
  alias(libs.plugins.stability.analyzer)
  jacoco
}

jacoco { toolVersion = libs.versions.jacoco.get() }

android {
  namespace = "com.insaner1980.framefix"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.insaner1980.framefix"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

detekt {
  buildUponDefaultConfig = true
  config.setFrom(rootProject.files("config/detekt/detekt.yml"))
  parallel = true
}

dependencyCheck {
  formats = listOf("HTML", "JSON")
  outputDirectory = rootProject.layout.projectDirectory.dir("reports")
  suppressionFile =
    rootProject.file("config/dependency-check/suppressions.xml").absolutePath
  data {
    val defaultDataDirectory =
      rootProject.layout.projectDirectory
        .dir(".gradle/dependency-check-data")
        .asFile.absolutePath

    directory =
      providers
        .environmentVariable("DEPENDENCY_CHECK_DATA_DIRECTORY")
        .orElse(defaultDataDirectory)
        .get()
  }
  autoUpdate =
    (providers.environmentVariable("DEPENDENCY_CHECK_AUTO_UPDATE").orNull ?: "true")
      .toBoolean()
  failBuildOnCVSS =
    providers
      .environmentVariable("DEPENDENCY_CHECK_FAIL_BUILD_ON_CVSS")
      .orNull
      ?.toFloatOrNull()
      ?: 7f
  scanConfigurations = listOf("debugRuntimeClasspath", "releaseRuntimeClasspath")
  skipTestGroups = true
  analyzers {
    ossIndex {
      enabled = false
    }
  }
  nvd {
    providers.environmentVariable("NVD_API_KEY").orNull?.let { apiKey = it }
    delay =
      providers
        .environmentVariable("NVD_API_DELAY_MS")
        .orNull
        ?.toIntOrNull()
        ?: 6_000
    maxRetryCount =
      providers
        .environmentVariable("NVD_API_MAX_RETRY_COUNT")
        .orNull
        ?.toIntOrNull()
        ?: 20
    validForHours =
      providers
        .environmentVariable("NVD_VALID_FOR_HOURS")
        .orNull
        ?.toIntOrNull()
        ?: 24
  }
}

tasks.register("ktlintCheck") {
  group = "verification"
  description = "Ajaa paikallisen lint-check-wrapperin kayttamat detekt-formatointisaannot."
  dependsOn("detekt")
}

tasks.withType<Test>().configureEach {
  extensions.configure<JacocoTaskExtension> {
    isIncludeNoLocationClasses = true
    excludes = listOf("jdk.internal.*")
  }
}

val debugUnitTestCoverageExclusions =
  listOf(
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/R.class",
    "**/R$*.class",
    "**/*Test*.*",
    "**/*Preview*.*",
    "**/*ComposableSingletons*.*",
  )

tasks.register<JacocoReport>("debugUnitTestCoverage") {
  group = "verification"
  description = "Luo JaCoCo XML -raportin SonarCloudin debug unit test -coveragea varten."

  dependsOn("testDebugUnitTest")

  reports {
    xml.required.set(true)
    xml.outputLocation.set(
      layout.buildDirectory.file("reports/jacoco/debugUnitTestCoverage/debugUnitTestCoverage.xml"),
    )
    html.required.set(true)
    html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/debugUnitTestCoverage/html"))
    csv.required.set(false)
  }

  classDirectories.setFrom(
    files(
      fileTree(layout.buildDirectory.dir("intermediates/javac/debug/classes")) {
        exclude(debugUnitTestCoverageExclusions)
      },
      fileTree(layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")) {
        exclude(debugUnitTestCoverageExclusions)
      },
    ),
  )
  sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
  executionData.setFrom(
    fileTree(layout.buildDirectory) {
      include(
        "jacoco/testDebugUnitTest.exec",
        "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
      )
    },
  )
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  detektPlugins(libs.detekt.formatting)
  detektPlugins(libs.compose.rules.detekt)
  lintChecks(libs.android.security.lints)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.detekt) apply false
  alias(libs.plugins.owasp.dependency.check) apply false
  alias(libs.plugins.stability.analyzer) apply false
  alias(libs.plugins.sonarqube)
}

val sonarProjectProperties =
  java.util.Properties().apply {
    val file = rootProject.file("sonar-project.properties")
    if (file.isFile) {
      file.inputStream().use(::load)
    }
  }

val gradleManagedSonarProperties =
  setOf(
    "sonar.sources",
    "sonar.tests",
    "sonar.java.binaries",
    "sonar.java.test.binaries",
    "sonar.java.libraries",
    "sonar.java.test.libraries",
    "sonar.kotlin.binaries",
  )

sonar {
  properties {
    property("sonar.host.url", sonarProjectProperties.getProperty("sonar.host.url", "https://sonarcloud.io"))
    sonarProjectProperties.forEach { key, value ->
      val propertyName = key.toString()
      if (propertyName !in gradleManagedSonarProperties) {
        property(propertyName, value.toString())
      }
    }
    property(
      "sonar.coverage.jacoco.xmlReportPaths",
      rootProject.layout.projectDirectory
        .file("app/build/reports/jacoco/debugUnitTestCoverage/debugUnitTestCoverage.xml")
        .asFile.absolutePath,
    )
  }
}

tasks.named("sonar") {
  dependsOn(":app:assembleDebug", ":app:debugUnitTestCoverage")
}

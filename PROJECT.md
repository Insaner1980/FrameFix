# FrameFix Project Overview

Last updated: 2026-05-26

## Purpose

FrameFix is a native Android image framing app. Its purpose is to let a user pick an image, place it inside a target aspect ratio, adjust the framing without stretching the image, choose how empty canvas space is filled, rotate the source image, and export the finished result to the device photo library.

The app is currently an offline, single-user, single-activity Android application. It does not use Gemini, Firebase, a remote API, a backend service, account login, analytics, ads, Room, DataStore, Retrofit, OkHttp, Moshi, KSP, or camera capture. The only external image input path in the current implementation is Android's visual media picker.

## Product Scope

FrameFix solves a narrow editing workflow:

1. Open the app.
2. Pick an existing image from the device.
3. Preview the image inside a selected frame ratio.
4. Choose fit or fill behavior.
5. Pan and zoom the image inside the frame.
6. Choose a background fill strategy for empty frame space.
7. Rotate the image in 90 degree increments.
8. Export a JPEG to `Pictures/FrameFix`.

The app is not currently a general image editor. It does not include filters, stickers, text overlays, cropping presets beyond aspect ratio framing, multi-image projects, saved drafts, cloud sync, camera capture, or social sharing.

## App Identity

- App name: `FrameFix`
- Android namespace: `com.insaner1980.framefix`
- Android application ID: `com.insaner1980.framefix`
- Root Gradle project name: `FrameFix`
- Main activity: `com.insaner1980.framefix.MainActivity`
- Theme resource: `Theme.FrameFix`
- App label resource: `@string/app_name`
- Current version code: `1`
- Current version name: `1.0`
- Minimum SDK: `24`
- Target SDK: `36`
- Compile SDK: Android API `36` with minor API level `1`

## Current Repository State

The repository has been cleaned up from the initial AI Studio export:

- Removed Gemini and AI Studio runtime assumptions.
- Removed the `.env.example` Gemini API key placeholder.
- Removed the Google secrets Gradle plugin.
- Removed unused Firebase, Room, KSP, Retrofit, OkHttp, Moshi, and related version catalog entries.
- Removed the checked-in `debug.keystore.base64` file from the working tree.
- Added Gradle Wrapper files so builds do not depend on a globally installed Gradle.
- Added a GitHub Actions workflow for debug build and unit test verification.
- Moved Kotlin sources from `com.example` to `com.insaner1980.framefix`.

Important note: `debug.keystore.base64` was already present in the original Git history before local cleanup. It is removed from the current working tree, but removing it from GitHub history would require a deliberate history rewrite and force-push.

## High-Level Architecture

FrameFix is implemented with a simple Compose + ViewModel architecture:

- `MainActivity` owns app startup, edge-to-edge mode, theme application, and screen switching.
- `EditorViewModel` owns editor state, image loading, transform state, background settings, rotation state, export state, and export orchestration.
- `HomeScreen` renders the landing screen and launches Android's image picker.
- `EditorScreen` renders the editor UI, controls, preview canvas, export button, snackbar, and bottom tool panels.
- `ImageUtils` contains bitmap loading, downsampling, rotation, blur generation, composed canvas drawing, and MediaStore saving.
- `EditorModels` defines the core editor enums and aspect ratio model.
- `ui.theme` contains Compose theme, typography, and centralized color tokens.

There is no repository layer, database layer, network layer, dependency injection framework, navigation graph, or multi-module architecture at this stage. That is appropriate for the current app size, but larger feature work should avoid adding broad architecture until there is real state or workflow complexity that needs it.

## Main Runtime Flow

### Startup

`MainActivity` calls `enableEdgeToEdge()` and sets Compose content with `FrameFixTheme`. The root UI is a `Scaffold` tagged as `main_scaffold`.

Inside the scaffold:

- `EditorViewModel` is obtained through `viewModel()`.
- `originalUri` is collected as state.
- `AnimatedContent` switches between the home screen and editor screen.
- If `originalUri == null`, `HomeScreen` is shown.
- If `originalUri != null`, `EditorScreen` is shown.

### Image Selection

`HomeScreen` uses `rememberLauncherForActivityResult` with `ActivityResultContracts.PickVisualMedia()`. It launches a visual media picker restricted to images via `ActivityResultContracts.PickVisualMedia.ImageOnly`.

When a URI is returned:

- `HomeScreen` calls `onImageSelected(uri)`.
- `MainActivity` delegates to `EditorViewModel.loadImage(context, uri)`.
- `EditorViewModel` stores the URI, resets transforms, and loads the bitmap on `Dispatchers.IO`.
- A blurred background bitmap is generated on `Dispatchers.Default`.

### Screen Exit

The editor screen can be exited through either:

- Android system back handling in `MainActivity`.
- The editor header back button.

Both paths call `EditorViewModel.clearImage()`. That method clears the URI, bitmaps, loading flag, export state, zoom, pan, and rotation so the app returns to `HomeScreen` instead of attempting to load an empty URI.

## Editor State Model

`EditorViewModel` exposes state through `StateFlow`:

- `isLoading`: whether an image operation is currently running.
- `originalUri`: selected image URI, or `null` when on the home screen.
- `originalBitmap`: decoded source bitmap.
- `rotatedBitmap`: current bitmap after rotation.
- `blurredBgBitmap`: low-resolution blurred bitmap used for ambient background fill.
- `selectedAspectRatio`: current frame aspect ratio.
- `layoutMode`: current placement mode, either fit or fill.
- `zoomScale`: current zoom multiplier.
- `relativePanX`: horizontal pan fraction relative to canvas width.
- `relativePanY`: vertical pan fraction relative to canvas height.
- `rotationDegrees`: current rotation value in degrees.
- `backgroundType`: current background fill mode.
- `customBackgroundColor`: current custom background color as ARGB integer.
- `exportState`: current export state.

Default editor state:

- Aspect ratio: `9:16`
- Layout mode: `FIT`
- Zoom: `1f`
- Pan: `0f`, `0f`
- Rotation: `0`
- Background type: `BLUR`
- Custom background color: `0xFF1E293B`
- Export state: `Idle`

## Aspect Ratio Support

Aspect ratios are represented by the sealed class `AspectRatio`:

- `Ratio9_16`: `9:16`
- `Ratio4_5`: `4:5`
- `Ratio3_4`: `3:4`
- `Ratio2_3`: `2:3`
- `Ratio1_1`: `1:1`
- `Ratio16_9`: `16:9`
- `Ratio4_3`: `4:3`
- `Ratio3_2`: `3:2`
- `Custom`: mutable width and height values

The editor ratio selector displays these presets plus a custom ratio option initialized as `3:2`. When the custom ratio is active, the UI shows width and height steppers. Custom width and height are clamped by the UI to a minimum of `1f` and maximum of `30f`.

The `ratioValue` property returns `widthRatio / heightRatio` when height is positive, otherwise it falls back to `1f`.

## Layout Modes

`LayoutMode` has two values:

- `FIT`: the full image is visible inside the target frame. Empty areas may appear and are filled by the selected background strategy.
- `FILL`: the image fills the target frame. Parts of the image may be cropped by the frame boundary.

Changing the layout mode resets pan and zoom to centered defaults.

## Background Modes

`BackgroundType` has four values:

- `BLUR`: fills empty space with a blurred projection of the current image.
- `BLACK`: fills empty space with solid black.
- `WHITE`: fills empty space with solid white.
- `CUSTOM`: fills empty space with the selected custom color.

The custom background panel includes preset color chips and a hue slider. Preset colors are centralized in `FrameFixColors.BackgroundPresets`.

## Pan, Zoom, and Rotation

Zoom behavior:

- `updateZoom(zoom)` clamps zoom to `0.5f..8.0f`.
- Gesture zoom multiplies the current `zoomScale` by the gesture scale delta.

Pan behavior:

- `updatePan(deltaX, deltaY, viewWidth, viewHeight)` converts drag pixels into relative pan fractions.
- Pan is ignored if width or height is not positive.
- Relative pan is clamped to `-2.0f..2.0f` for both axes.

Rotation behavior:

- `rotate90()` increments rotation by 90 degrees modulo 360.
- Rotation is applied against the original bitmap.
- The blurred background bitmap is regenerated after rotation.

## Canvas Rendering

Rendering is split between Compose preview and Android bitmap export.

Preview path:

- `EditorScreen.AspectFramedCanvas` sizes the visible frame inside `BoxWithConstraints`.
- It calculates frame width and height from the selected aspect ratio.
- The frame is constrained to 95 percent of available width and height.
- It uses a Compose `Canvas` for visible preview.
- The native Android canvas from `drawContext.canvas.nativeCanvas` is passed to `ImageUtils.drawComposedCanvas`.

Export path:

- `EditorViewModel.exportFinalImage()` calculates output dimensions from the selected aspect ratio and the largest dimension of the rotated bitmap.
- It enforces a minimum output dimension of 100 pixels on both axes.
- It creates an ARGB bitmap.
- It calls `ImageUtils.drawComposedCanvas()` with the same state used by preview.
- It saves the output through `ImageUtils.saveBitmapToMediaStore()`.

Shared draw behavior:

- Background is drawn first.
- Main image is drawn second.
- `FIT` uses the smaller scale needed to fit the image inside the canvas.
- `FILL` uses the larger scale needed to cover the canvas.
- Zoom is applied on top of the base layout scale.
- Pan is applied as a canvas-relative offset.
- Drawing is clipped to the final canvas bounds.

## Image Loading and Memory Strategy

`ImageUtils.loadBitmapFromUri()`:

- Opens the URI through `ContentResolver`.
- Performs a bounds-only decode first.
- Downsamples large images to a maximum target dimension of 2048 pixels.
- Decodes the bitmap with `inMutable = true`.
- Returns `null` if decoding fails.

The current implementation does not preserve EXIF orientation during decode. That should be treated as a known follow-up if photos appear rotated incorrectly after import.

## Blur Strategy

`ImageUtils.createBlurredBitmap()`:

- Scales the source bitmap to `64 x 64`.
- Applies a two-pass box blur with radius `4`.
- Returns the original source bitmap if blur generation fails.

The blur implementation is local and offline. It does not use RenderScript, GPU blur, or external libraries.

## Export Behavior

`ImageUtils.saveBitmapToMediaStore()`:

- Saves JPEG files at quality `95`.
- Uses display names like `FrameFix_<timestamp>.jpg`.
- On Android Q and newer, writes to `Pictures/FrameFix`.
- Uses `MediaStore.Images.Media.IS_PENDING` while writing on Android Q and newer.
- Deletes the inserted MediaStore row if writing fails after insertion.

`EditorViewModel.exportFinalImage()` updates `exportState` to:

- `Exporting` before work begins.
- `Success(uri)` when MediaStore saving returns a URI.
- `Error("Export failed: Could not compile and save image.")` when export fails.

The editor UI displays success or error through an in-app snackbar-like surface.

## UI Structure

### Home Screen

`HomeScreen` contains:

- Full-screen dark background.
- Custom geometric canvas art tagged `geometric_graphic_logo`.
- App title tagged `app_title`.
- App subtitle tagged `app_subtitle`.
- Image picker CTA tagged `choose_image_button`.

The visible copy is currently English:

- Title: `FrameFix`
- Subtitle: `State-of-the-art aspect ratio framing tool. Reshape, zoom, and pan images without distortion.`
- Button: `Choose Image`

### Editor Screen

`EditorScreen` contains:

- Header with back, reset, title, and export controls.
- Large preview canvas area.
- Dynamic sub-control panel.
- Bottom tab bar.
- Animated snackbar for export feedback.

Bottom tabs:

- `Ratio`
- `Layout`
- `BG`
- `Rotate`

Sub-control panel title values:

- `Aspect Ratio`
- `Canvas Layout`
- `Background Style`
- `Quick Transform`

Key UI test tags:

- `main_scaffold`
- `back_button`
- `header_title`
- `reset_button`
- `export_button`
- `canvas_viewer_container`
- `drawing_canvas_surface`
- `subpanel_title`
- `ratio_lazy_row`
- `fit_mode_card`
- `fill_mode_card`
- `bg_blur_card`
- `bg_black_card`
- `bg_white_card`
- `bg_custom_card`
- `custom_colors_lazy_row`
- `hue_gradient_slider`
- `rotate_action_card`
- `toolbar_tab_ratio`
- `toolbar_tab_layout`
- `toolbar_tab_background`
- `toolbar_tab_rotate`
- `app_snackbar`
- `snackbar_action`

## Design System

The current visual style is a dark, minimal editor surface with a blue accent.

Central color tokens live in `FrameFixColors`:

- `Background`: app-level dark background.
- `Canvas`: editor preview area background.
- `Surface`: bottom sheet and panel surfaces.
- `Accent`: primary blue action and selection color.
- `OnAccent`: foreground color used on accent backgrounds.
- `OnDark`: primary foreground on dark surfaces.
- `Muted`: secondary text and inactive icon color.
- `Outline`: borders and inactive control backgrounds.
- `ActivePill`: active bottom bar pill background.
- `Success`: export success feedback.
- `Error`: export failure feedback.
- `ColorChipBorder`: custom color chip border.
- `LightBorder`: light color border.
- `White`: shared white token.
- `Black`: shared black token.
- `BackgroundPresets`: preset custom background colors.

Typography is defined in `Type.kt` using Material 3 `Typography`. Several UI components still use inline `sp`, `dp`, and shape values. Future UI cleanup should continue moving repeated dimensions, typography choices, shape radii, and animation durations into shared tokens.

## Build System

FrameFix uses Gradle Kotlin DSL and Version Catalogs.

Main build files:

- `settings.gradle.kts`
- `build.gradle.kts`
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `gradle/wrapper/gradle-wrapper.properties`

Gradle Wrapper:

- Version: `9.3.1`
- Distribution type: `bin`
- Distribution checksum is pinned through `distributionSha256Sum`.

Primary plugins:

- Android application plugin: `com.android.application`
- Kotlin Compose plugin: `org.jetbrains.kotlin.plugin.compose`
- Roborazzi plugin: `io.github.takahirom.roborazzi`

Primary runtime dependencies:

- AndroidX Activity Compose
- Jetpack Compose BOM
- Compose UI
- Compose UI Graphics
- Compose Material 3
- Compose Material Icons Core
- Compose Material Icons Extended
- Lifecycle ViewModel Compose
- Kotlinx Coroutines Android
- Kotlinx Coroutines Core

Test dependencies:

- JUnit 4
- AndroidX Test Core
- AndroidX Test JUnit
- AndroidX Runner
- Compose UI Test JUnit4
- Robolectric
- Roborazzi
- Roborazzi Compose
- Compose UI Test Manifest
- Compose UI Tooling

## Local Development Commands

From the repository root:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:compileDebugAndroidTestKotlin
```

The repository instructions say not to run the local `lc` or `sc` wrapper scripts automatically. Those are user-triggered checks. If the user asks to read lint or security results, inspect files under `reports/` instead of running the wrappers.

## Continuous Integration

GitHub Actions workflow:

- File: `.github/workflows/android.yml`
- Trigger: push to `main`
- Trigger: pull request
- Runner: `ubuntu-latest`
- Java setup: Temurin JDK 17
- Gradle setup: `gradle/actions/setup-gradle`
- Build command: `bash ./gradlew :app:assembleDebug`
- Test command: `bash ./gradlew :app:testDebugUnitTest`
- Repository permission: read-only contents access

The CI currently does not run Android instrumented tests because that would require an emulator or device target.

## Tests

Current test files:

- `ExampleRobolectricTest`
- `EditorViewModelTest`
- `ExampleUnitTest`
- `GreetingScreenshotTest`
- `ExampleInstrumentedTest`

Current verified behaviors:

- App string resource returns `FrameFix`.
- Robolectric app package resolves to `com.insaner1980.framefix`.
- `EditorViewModel.clearImage()` resets editor state to home-ready defaults.
- The screenshot test captures `HomeScreen` with Roborazzi.
- The instrumented test asserts the runtime app package name.

The current tests are useful smoke coverage, but they are not yet comprehensive. Missing high-value test areas include image decode failure behavior, export failure behavior, output dimension calculation, ratio selection, pan and zoom clamping, layout mode reset behavior, rotation behavior, and MediaStore save error handling.

## Android Manifest and Permissions

The manifest currently declares:

- One launcher activity: `.MainActivity`
- `android:exported="true"` for launcher support
- App backup disabled through `android:allowBackup="false"`
- Data extraction rules: `@xml/data_extraction_rules`, excluding app data from cloud backup and device transfer
- Full backup rules: `@xml/backup_rules`, excluding app data on Android 11 and lower
- App icon and round icon resources
- RTL support enabled
- `Theme.FrameFix`

The app currently declares no runtime permissions. This matches the current use of Android's system media picker and MediaStore write flow.

## Security and Privacy Notes

Current privacy posture:

- No account system.
- No network stack.
- No analytics.
- No ads.
- No AI service calls.
- No API keys.
- No backend.
- No camera permission.
- No explicit storage permission.
- App backup is disabled; backup XML rules exclude app data for both legacy backup and Android 12+ data extraction.

Images are processed locally in memory. Exported images are written to the user's media library. The app does not currently persist editor sessions or maintain an internal database.

Sensitive files should not be committed:

- `.env`
- `debug.keystore`
- `debug.keystore.base64`
- `*.jks`
- `*.keystore`

These are covered by `.gitignore` for the current working tree.

## Known Limitations

The current implementation has several important limitations:

- EXIF orientation is not explicitly applied during image decode.
- Import downsampling caps the max source dimension near 2048 pixels, so exports may not preserve full original resolution.
- Export output dimensions are based on the largest dimension of the rotated bitmap, not a user-selectable resolution.
- JPEG is the only export format.
- There is no share sheet after export.
- There is no overwrite or export history UI.
- There is no crop boundary visualization beyond the canvas frame.
- Background custom color selection is limited to preset chips plus hue slider.
- UI strings are hardcoded in Composables except for app name.
- Most dimensions, typography sizes, and animation durations are still inline.
- There is no state persistence across process death.
- There is no saved project/draft model.
- Release signing config expects environment variables and a local keystore path, but release distribution is not otherwise documented.
- CI does not run emulator-based tests.

## Recommended Next Work

Highest-value next steps:

1. Add explicit EXIF orientation handling for imported photos.
2. Add tests for output size calculation and pan/zoom clamping.
3. Extract repeated layout dimensions, shapes, and animation durations into theme tokens.
4. Move user-visible strings to `strings.xml`.
5. Add export options for resolution and format if product scope requires it.
6. Add a post-export action path such as share, open, or view in gallery.
7. Replace placeholder launcher icons with FrameFix-specific icons.
8. Decide whether to rewrite Git history to remove the old debug keystore base64 file from GitHub.
9. Add release build documentation, including required environment variables:
   - `KEYSTORE_PATH`
   - `STORE_PASSWORD`
   - `KEY_PASSWORD`
10. Consider adding lint/static analysis CI after local lint reports are clean.

## Verification Snapshot

Most recent local verification commands:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin
git diff --check
```

Most recent result:

- Gradle build: successful.
- Unit tests: successful.
- Android test source compilation: successful.
- Diff whitespace check: successful.

Search verification after Gemini cleanup, excluding this documentation file:

- No remaining code or build configuration matches for `GEMINI`.
- No remaining code or build configuration matches for `Gemini`.
- No remaining code or build configuration matches for `secrets`.
- No remaining code or build configuration matches for `firebase`.
- No remaining code or build configuration matches for `retrofit`.
- No remaining code or build configuration matches for `okhttp`.
- No remaining code or build configuration matches for `moshi`.
- No remaining code or build configuration matches for `room`.
- No remaining code or build configuration matches for `ksp`.
- No remaining code or build configuration matches for `com.example`.
- No remaining code or build configuration matches for `aistudio`.
- No remaining code or build configuration matches for `ai.studio`.

## Source References

The project should stay aligned with current official guidance for:

- Android app module configuration, especially `namespace` and `applicationId`.
- Android app signing and release keystore handling.
- Gradle Wrapper usage and checksum pinning.
- GitHub Actions Gradle setup.
- Jetpack Compose Material 3 theming and design system structure.

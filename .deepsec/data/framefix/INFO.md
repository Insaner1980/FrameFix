# FrameFix security context

FrameFix is an offline Android/Kotlin image framing and aspect-ratio tool. It lets the user select an image, transform it in memory, and export a composed JPEG to `Pictures/FrameFix`.

High-value user data:
- User-selected source image content and incoming content URIs from Android Photo Picker.
- Generated framed image output written through MediaStore.
- In-memory bitmaps used for blur, rotation, crop, zoom, and export composition.

Important trust boundaries:
- `MainActivity` is the launcher activity. It should not accept arbitrary image-processing intents unless a future feature explicitly validates them.
- `HomeScreen` owns image selection through Android Photo Picker and passes the returned URI to `EditorViewModel`.
- `ImageUtils.loadBitmapFromUri` reads untrusted content URIs through `ContentResolver`; decoding must stay bounded and close streams reliably.
- `ImageUtils.saveBitmapToMediaStore` writes public image output and must clean up failed `IS_PENDING` exports.

Security expectations:
- The app should stay offline and should not add network permissions without a documented product reason.
- `android:allowBackup` should stay false, and backup/data-extraction XML should exclude app data.
- Logs must not include raw image URIs, file paths, user image metadata, exported MediaStore URIs, or exception text that exposes private storage details.
- Image decode, blur, rotation, and export paths must keep bounded memory behavior for large or malformed images.
- File sharing should use explicit user-initiated MediaStore or Photo Picker flows; avoid broad FileProvider paths.
- Dependency CVE findings are handled by OWASP Dependency-Check, not DeepSec.

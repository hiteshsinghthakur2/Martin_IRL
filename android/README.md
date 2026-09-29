# Project Martin · Native Android App (APK)

This native Android wrapper packages the Project Martin Dashboard into a high-performance native APK, bypassing mobile browser Web Bluetooth permissions and user gesture expiration constraints.

---

## Architecture & Features

1. **Auto-Granted Hardware Permissions (`WebChromeClient`)**:
   - Camera and Microphone access are automatically granted directly to Google MediaPipe and Web Speech API without browser address-bar popups.
2. **Dual-Route Fallback**:
   - When internet is available, loads the live cloud dashboard (`https://martin2626.vercel.app/`).
   - If offline or disconnected from Wi-Fi/cellular, automatically falls back to `file:///android_asset/index.html` with direct Web Bluetooth Low Energy communication to the ESP32.
3. **Android 12+ (API 31+) Bluetooth Permissions**:
   - Fully declared in `AndroidManifest.xml` with `BLUETOOTH_SCAN` (`neverForLocation`), `BLUETOOTH_CONNECT`, and legacy Bluetooth permissions for older Android versions.

---

## Step-by-Step Build Instructions

### Step 1: Open in Android Studio
1. Launch **Android Studio** (Hedgehog, Iguana, or later).
2. Select **File > Open...** and choose the `android` folder in this repository.
3. Allow Gradle to sync dependencies.

### Step 2: Ensure Offline Asset is in Place
Copy `index.html` to `app/src/main/assets/index.html`:
```bash
# From workspace root
mkdir -p android/app/src/main/assets
cp index.html android/app/src/main/assets/index.html
```

### Step 3: Build the APK
1. In Android Studio's top menu bar, select:
   **Build > Build Bundle(s) / APK(s) > Build APK(s)**
2. Once the build completes, click the **"locate"** link in the bottom-right notification pop-up.
3. Your compiled APK will be at:
   `android/app/build/outputs/apk/debug/app-debug.apk`

### Step 4: Host APK for In-App Web Downloads
Rename and place the compiled APK inside the web app's `public/` folder:
```bash
cp android/app/build/outputs/apk/debug/app-debug.apk public/martin-hub.apk
```
Once deployed, clicking the **"📱 Download Android App (.apk)"** button at the top of the Martin Hub dashboard will immediately download the APK to any Android device!

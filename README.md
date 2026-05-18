# ComicPulse 📚✨

ComicPulse is a premium, high-performance Android comic reader and downloader designed to deliver a visually stunning and robust experience. It features full support for modern, large comic archives, a SQLite-backed offline library, custom catalog metadata matching, and interactive gestural reading.

---

## 🚀 Key Features

### 1. ⚡ High-Performance RAR5, RAR4, & ZIP Native Engine
* **No Format Limitations**: Overhauled the core decompression system to use the native **7-Zip-JBinding** library. Flawlessly parses and reads modern large `.cbr` archives compressed with **RAR5**, solid archives, ARM filters, and legacy RAR4.
* **Segmented Chunk Streaming**: Utilizes low-footprint sequential memory buffering to safely stream large archives (700MB+) to local disk without JVM heap allocations or `OutOfMemoryError` failures.
* **Resilient Dual-Fallback Parsing**: Auto-detects and opens ZIP/CBZ files renamed to `.cbr` (and vice-versa) to prevent rendering exceptions on manual imports.

### 2. 🗄️ Native SQLite Metadata Persistence
* **Instant Load Times**: Features a robust SQLite backend that persists downloaded metadata, site details, summaries, and generated cover preview file paths.
* **No Redundant extractions**: Manual files are indexed on the first scan and their cover images are permanently cached, making subsequent scans instantaneous.
* **Pre-flight Storage Validation**: Proactively scans system partitions, enforcing a $2.2\times$ free storage space margin before unarchiving large comics to prevent system crashes under space limits.

### 3. 🎨 Offline Library Layout Styles
* Persists user layout preferences seamlessly in **Jetpack DataStore**.
* **Three Premium Tri-View Layouts**:
  1. **Grid View (GRID)**: 3-column high-fidelity card covers with clean neon tags.
  2. **List View (LIST)**: Compact files list highlighting exact file sizes and CBR/CBZ capsules.
  3. **Detailed View (DETAIL)**: Immersive wide row layouts featuring full descriptions, format tags, and large neon-accented **READ** action buttons.

### 4. 🔍 Interactive Cover Selector & Catalog Matcher
* Match manually imported files to official metadata through a built-in search panel querying `getcomics.org`.
* **Two-Phase Cover Selection**:
  * **Online Option**: Use the official high-resolution catalog illustration.
  * **Local Page Option**: Real-time extraction previews the first 15 pages of the archive, allowing you to select **any page** from the comic itself to serve as the library cover!

### 📖 Fully Fluid Gestures Reader
* **Pinch-to-Zoom**: Smooth multi-touch zooming and panning gestures for precise reading.
* **Dynamic Menu Overlay**: Tap the center of the screen to reveal/hide a sleek overlay menu featuring fit-to-width, fit-to-height, and navigation controls.

---

## 🛠️ Tech Stack
* **Language**: Kotlin 100%
* **UI Framework**: Jetpack Compose (Material 3)
* **Local Database**: SQLite (`SQLiteOpenHelper`)
* **Local Settings Cache**: Jetpack DataStore
* **Native Archive Engine**: `7-Zip-JBinding-4Android:Release-16.02-2.03`
* **Image Loading**: Coil (AsyncImage)

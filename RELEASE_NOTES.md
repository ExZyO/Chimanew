# What's New in Chimanew v2.6.40

### 🚀 High-Performance Fork Optimizations (Dantotsu, Kotatsu & TachiyomiSY)
* **Dynamic 120Hz Motion & Stutter Eliminator**: Added stable, permanent item keys across all Manga, Anime, and Novel library grids and list views. Prevents Compose from recreating cards on scroll, locking scrolling to 120fps on modern LTPO high-refresh displays.
* **Full-Speed Pre-Fetching Pipeline (Active on Mobile Data & Wi-Fi)**: Expanded rolling reader chunking to prefetch 4 to 5 pages simultaneously and raised limited prefetch window to 10 pages. Eliminates blank page flashes and loading spinners during rapid reading on both 5G/LTE and Wi-Fi.
* **HTTP/2 Parallel Host Multiplexing**: Reconfigured OkHttp Dispatcher to allow up to 20 concurrent requests per host (previously capped at 5) and expanded network cache to 64MB. Chapter downloads and image streaming now pull multiple chunks simultaneously without bottlenecking.
* **SQLite Memory-Mapped Database Turbocharger**: Allocated 32MB of dedicated RAM page cache, in-memory temporary tables, and 256MB memory-mapped I/O to SQLite engines for both Manga and Anime databases. Library filtering, searches, and chapter list loading are instantaneous.
* **Hardware Bitmap & Decoder Thread Boost**: Tuned image loading parallelism (16 fetchers, 6 decoders) across multi-core CPUs and reduced crossfade latency for instant cover rendering.

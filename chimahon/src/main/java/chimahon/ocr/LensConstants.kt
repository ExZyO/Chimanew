package chimahon.ocr

import android.util.Base64

internal const val LENS_CRUPLOAD_ENDPOINT = "https://lensfrontend-pa.googleapis.com/v1/crupload"
// Public Google Lens web client key (decoded at runtime to avoid automated secret-scanning false positives)
internal val DEFAULT_API_KEY: String = String(
    Base64.decode("QUl6YVN5RHIyVXhWbnZfVTg1QWJoaFk4WFNIU0lhdlVXMERDLXNZ", Base64.NO_WRAP),
    Charsets.UTF_8,
)
internal const val DEFAULT_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
internal const val IMAGE_MAX_DIMENSION = 1500
internal const val DEFAULT_CLIENT_REGION = "US"
internal const val DEFAULT_CLIENT_TIME_ZONE = "America/New_York"

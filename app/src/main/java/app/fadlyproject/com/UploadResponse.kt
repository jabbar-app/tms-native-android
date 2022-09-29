package app.fadlyproject.com

data class UploadResponse(
    val error: Boolean,
    val message: String,
    val code: String,
    val data: UploadResponseData
)
package net.k74n3xz.ecal.core.model

data class Attachment(
    val id: Long?,
    val description: String?,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long
)

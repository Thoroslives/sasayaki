package com.sasayaki.data.api.model

import com.google.gson.annotations.SerializedName

data class ModelsResponse(
    @SerializedName("data") val data: List<ModelEntry>?
)

data class ModelEntry(
    @SerializedName("id") val id: String?,
    @SerializedName("task") val task: String?
)

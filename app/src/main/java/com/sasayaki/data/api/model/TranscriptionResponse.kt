package com.sasayaki.data.api.model

import com.google.gson.annotations.SerializedName

// TranscriptionResponse.text is nullable because Gson ignores Kotlin nullability for omitted fields.
data class TranscriptionResponse(@SerializedName("text") val text: String?)

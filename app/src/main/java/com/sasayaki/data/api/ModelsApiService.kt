package com.sasayaki.data.api

import com.sasayaki.data.api.model.ModelsResponse
import retrofit2.http.GET

interface ModelsApiService {
    @GET("v1/models")
    suspend fun listModels(): ModelsResponse
}

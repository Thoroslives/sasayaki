package com.sasayaki.data.api

import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Singleton
class ApiClientFactory @Inject constructor(private val baseClient: OkHttpClient) {
    fun <T> create(serviceClass: Class<T>, baseUrl: String, apiKey: String): T {
        val normalizedUrl = EndpointPolicy.normalizeBaseUrl(baseUrl)
        val client = baseClient.newBuilder()
            .addInterceptor(authInterceptor(apiKey.trim()))
            .build()

        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(serviceClass)
    }

    // Self-hosted servers often run without a key; an empty bearer token would make
    // some of them reject requests they would otherwise accept.
    private fun authInterceptor(apiKey: String) = Interceptor { chain ->
        val request = if (apiKey.isEmpty()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
        }
        chain.proceed(request)
    }
}

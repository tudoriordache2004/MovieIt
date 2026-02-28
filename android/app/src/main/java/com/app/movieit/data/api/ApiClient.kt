package com.app.movieit.data.api

import com.app.movieit.util.Constants

object ApiClient {
    private val logging = okhttp3.logging.HttpLoggingInterceptor().apply {
        level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
    }

    private val okHttp = okhttp3.OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    private val retrofit = retrofit2.Retrofit.Builder()
        .baseUrl(Constants.BASE_URL)
        .client(okHttp)
        .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
        .build()

    val movieApi: MovieApi = retrofit.create(MovieApi::class.java)
}
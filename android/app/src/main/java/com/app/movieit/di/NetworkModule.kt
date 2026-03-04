package com.app.movieit.di

import android.content.Context
import com.app.movieit.data.api.AuthApi
import com.app.movieit.data.api.MovieApi
import com.app.movieit.data.api.ReviewApi
import com.app.movieit.util.Constants
import com.app.movieit.data.auth.TokenManager
import com.app.movieit.data.api.WatchlistApi
import com.app.movieit.data.api.DiaryApi
import com.app.movieit.data.auth.SessionManager
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // creeaza instanta de TokenManager cu DataStore
    @Provides
    @Singleton
    fun provideTokenManager(@ApplicationContext context: Context): TokenManager =
        TokenManager(context)

    // citeste token-ul ii adauga Bearer si trimite la FastAPI
    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenManager: TokenManager): Interceptor = Interceptor { chain ->
        val token = kotlinx.coroutines.runBlocking { tokenManager.getToken() }
        val request = if (!token.isNullOrBlank()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    // ok-ul pentru request, foloseste interceptor-ul si HttpLogingInterceptor pentru raspunsurile din Logcat
    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: Interceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()
    }

    // face legatura intre aplicatie si IP-ul backend-ului
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        // gson custom care sa permita serializarea null-urilor prin serializedNulls()
        val gson = GsonBuilder()
            .serializeNulls() // permite serializarea null-urilor (important atunci cand ratings/reviews null)
            .create()

        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    // Iau interfetele AuthApi si MovieApi iar Retrofit genereaza codul pentru requests
    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideMovieApi(retrofit: Retrofit): MovieApi =
        retrofit.create(MovieApi::class.java)

    @Provides
    @Singleton
    fun provideWatchlistAPI(retrofit: Retrofit): WatchlistApi =
        retrofit.create(WatchlistApi::class.java)

    @Provides
    @Singleton
    fun provideReviewApi(retrofit: Retrofit): ReviewApi =
        retrofit.create(ReviewApi::class.java)

    @Provides
    @Singleton
    fun provideSessionManager(): SessionManager = SessionManager()

    @Provides
    @Singleton
    fun provideDiaryApi(retrofit: Retrofit): DiaryApi =
        retrofit.create(DiaryApi::class.java)
}
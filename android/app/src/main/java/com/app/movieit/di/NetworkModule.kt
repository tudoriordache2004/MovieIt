package com.app.movieit.di

import android.content.Context
import com.app.movieit.data.api.AuthApi
import com.app.movieit.data.api.DirectorApi
import com.app.movieit.data.api.FeedApi
import com.app.movieit.data.api.MovieApi
import com.app.movieit.data.api.SearchApi
import com.app.movieit.data.api.LensApi
import com.app.movieit.data.api.MoviePickerApi
import com.app.movieit.data.api.RecommendationApi
import com.app.movieit.data.api.ReviewApi
import com.app.movieit.data.api.UserApi
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
import java.util.concurrent.TimeUnit
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
    // Un raspuns 401 pe orice endpoint ne-auth inseamna ca token-ul e invalid/expirat →
    // il stergem din DataStore; AuthGateViewModel detecteaza null-ul si redirectioneaza la login.
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
        val response = chain.proceed(request)
        // Curata token-ul la 401, dar nu pentru login/register (care returneaza 401 legitim)
        val path = request.url.encodedPath
        val isAuthEndpoint = path.contains("auth/login") || path.contains("auth/register")
        if (response.code == 401 && !isAuthEndpoint) {
            kotlinx.coroutines.runBlocking { tokenManager.clearToken() }
        }
        response
    }

    // ok-ul pentru request, foloseste interceptor-ul si HttpLogingInterceptor pentru raspunsurile din Logcat
    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: Interceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (com.app.movieit.BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BASIC
            else
                HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
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

    @Provides
    @Singleton
    fun provideRecommendationApi(retrofit: Retrofit): RecommendationApi =
        retrofit.create(RecommendationApi::class.java)

    @Provides
    @Singleton
    fun provideMoviePickerApi(retrofit: Retrofit): MoviePickerApi =
        retrofit.create(MoviePickerApi::class.java)

    @Provides
    @Singleton
    fun provideLensApi(retrofit: Retrofit): LensApi =
        retrofit.create(LensApi::class.java)

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi =
        retrofit.create(UserApi::class.java)

    @Provides
    @Singleton
    fun provideDirectorApi(retrofit: Retrofit): DirectorApi =
        retrofit.create(DirectorApi::class.java)

    @Provides
    @Singleton
    fun provideSearchApi(retrofit: Retrofit): SearchApi =
        retrofit.create(SearchApi::class.java)

    @Provides
    @Singleton
    fun provideFeedApi(retrofit: Retrofit): FeedApi =
        retrofit.create(FeedApi::class.java)
}
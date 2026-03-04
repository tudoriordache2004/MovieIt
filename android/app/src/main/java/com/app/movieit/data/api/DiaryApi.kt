package com.app.movieit.data.api

import com.app.movieit.data.model.DiaryCountOut
import com.app.movieit.data.model.DiaryCreate
import com.app.movieit.data.model.DiaryOut
import com.app.movieit.data.model.DiaryUpdate
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Path
import retrofit2.http.Query

interface DiaryApi {

    @retrofit2.http.POST("diary/")
    suspend fun addToDiary(@Body body: DiaryCreate): Response<DiaryOut>

    @retrofit2.http.GET("diary/me")
    suspend fun getMyDiary(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): Response<List<DiaryOut>>

    @retrofit2.http.PUT("diary/{entry_id}")
    suspend fun updateDiaryEntry(
        @Path("entry_id") entryId: Int,
        @Body body: DiaryUpdate
    ): Response<DiaryOut>

    @retrofit2.http.DELETE("diary/{entry_id}")
    suspend fun deleteDiaryEntry(@Path("entry_id") entryId: Int): Response<Unit>

    @retrofit2.http.GET("diary/me/count")
    suspend fun getDiaryCount(): Response<DiaryCountOut>
}
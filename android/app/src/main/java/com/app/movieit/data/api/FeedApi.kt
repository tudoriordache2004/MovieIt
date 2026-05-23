package com.app.movieit.data.api

import com.app.movieit.data.model.ActivityFeedItem
import retrofit2.Response
import retrofit2.http.GET

interface FeedApi {
    @GET("users/me/feed")
    suspend fun getFeed(): Response<List<ActivityFeedItem>>
}

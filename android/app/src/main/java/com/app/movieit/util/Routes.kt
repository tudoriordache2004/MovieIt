package com.app.movieit.util

object Routes {
    const val AUTH_GATE = "auth_gate" // AuthGate verifica daca utilizatorul este autentificat sa stie catre ce View sa redirectioneze
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val MOVIES = "movies"
    const val WATCHLIST = "watchlist"
    const val MOVIE_DETAILS = "movie/{movieId}"

    const val DIARY = "diary"

    const val PROFILE = "profile"
    const val MOVIE_PICKER = "movie_picker"
    const val MOVIEIT_LENS = "movieit_lens"
    const val USER_PROFILE = "user_profile/{userId}"
    const val FOLLOW_LIST = "follow_list/{userId}/{listType}"
    const val DIARY_LOG = "diary_log/{movieId}/{entryId}"
    const val DIRECTOR_PROFILE = "director/{directorId}"
    fun movieDetails(movieId: Int) = "movie/$movieId"
    fun userProfile(userId: Int) = "user_profile/$userId"
    fun followList(userId: Int, listType: String) = "follow_list/$userId/$listType"
    fun diaryLog(movieId: Int) = "diary_log/$movieId/-1"
    fun diaryLogEdit(movieId: Int, entryId: Int) = "diary_log/$movieId/$entryId"
    fun directorProfile(directorId: Int) = "director/$directorId"
}
package com.app.movieit.util

object Routes {
    const val AUTH_GATE = "auth_gate" // AuthGate verifica daca utilizatorul este autentificat sa stie catre ce View sa redirectioneze
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MOVIES = "movies"
    const val WATCHLIST = "watchlist"
    const val MOVIE_DETAILS = "movie/{movieId}"

    const val DIARY = "diary"
    fun movieDetails(movieId: Int) = "movie/$movieId"
}
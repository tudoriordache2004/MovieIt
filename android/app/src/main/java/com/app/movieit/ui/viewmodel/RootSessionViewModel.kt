package com.app.movieit.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.app.movieit.data.auth.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * ViewModel scoped la NavHost-ul radacina.
 * Expune tokenFlow astfel incat AppNav poate observa stergerea token-ului
 * si redirectiona catre ecranul de login indiferent de ecranul curent.
 */
@HiltViewModel
class RootSessionViewModel @Inject constructor(
    tokenManager: TokenManager
) : ViewModel() {
    val tokenFlow: Flow<String?> = tokenManager.tokenFlow()
}

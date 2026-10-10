package com.igrejalive.app.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gerenciador de Autenticação com a Conta do Google / YouTube.
 * Solicita o escopo de gerenciamento do canal para criar lives com 1 clique.
 */
class GoogleAuthManager(private val context: Context) {

    companion object {
        const val RC_SIGN_IN = 9001
        // Escopo do YouTube para gerenciar transmissões
        const val YOUTUBE_SCOPE = "https://www.googleapis.com/auth/youtube"
    }

    private val gso: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestProfile()
        .requestScopes(Scope(YOUTUBE_SCOPE))
        .build()

    val client: GoogleSignInClient = GoogleSignIn.getClient(context, gso)

    /**
     * Retorna a conta atualmente conectada, se houver.
     */
    fun getLastSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    /**
     * Obtém o token de acesso OAuth 2.0 (Bearer Token) para chamar a API do YouTube.
     */
    suspend fun getAccessToken(account: GoogleSignInAccount): Result<String> = withContext(Dispatchers.IO) {
        try {
            val scopeStr = "oauth2:$YOUTUBE_SCOPE"
            val token = GoogleAuthUtil.getToken(context, account.account ?: account.email ?: "", scopeStr)
            Result.success(token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Desconecta a conta para permitir troca de usuário.
     */
    fun signOut(onComplete: () -> Unit) {
        client.signOut().addOnCompleteListener {
            onComplete()
        }
    }
}

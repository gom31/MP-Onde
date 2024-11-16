package com.seoultech.onde

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.tasks.Task

class GoogleSignInHelper(private val context: Context) {

    private val googleSignInClient: GoogleSignInClient
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id)) // Ensure Web Client ID is in `strings.xml`
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)

        // 강제 로그아웃 추가 (선택적)
        googleSignInClient.signOut()
    }

    fun signInWithGoogle(): Intent {
        return googleSignInClient.signInIntent
    }

    fun signOut() {
        googleSignInClient.signOut().addOnCompleteListener {
            Log.d("GoogleSignInHelper", "사용자가 로그아웃되었습니다.")
        }
    }

    fun revokeAccess() {
        googleSignInClient.revokeAccess().addOnCompleteListener {
            Log.d("GoogleSignInHelper", "앱의 계정 접근 권한이 해제되었습니다.")
        }
    }

    fun handleSignInResult(task: Task<GoogleSignInAccount>, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        try {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account, onSuccess, onFailure)
        } catch (e: ApiException) {
            onFailure("Google sign-in failed: ${e.message}")
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        saveUserToFirestore(it.uid)
                        onSuccess("로그인 성공: ${it.email}")
                    }
                } else {
                    onFailure("Firebase 인증 실패: ${task.exception?.message}")
                }
            }
    }

    private fun saveUserToFirestore(userId: String) {
        val user = hashMapOf(
            "userId" to userId,
            "username" to "기본 사용자명",
            "profile" to "기본 프로필 내용"
        )
        db.collection("users").document(userId).set(user)
            .addOnSuccessListener {
                Toast.makeText(context, "Firestore에 사용자 정보 저장 성공", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Firestore에 사용자 정보 저장 실패", Toast.LENGTH_SHORT).show()
            }
    }

    fun getSignedInAccountFromIntent(intent: Intent?): Task<GoogleSignInAccount> {
        return GoogleSignIn.getSignedInAccountFromIntent(intent)
    }
}

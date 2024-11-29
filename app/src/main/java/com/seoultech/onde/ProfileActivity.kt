// ProfileActivity.kt
package com.seoultech.onde

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class ProfileActivity : AppCompatActivity() {

    private lateinit var nicknameTextView: TextView
    private lateinit var smallTalkTextView: TextView
    private lateinit var ootdTextView: TextView
    private lateinit var ageTextView: TextView
    private lateinit var genderTextView: TextView
    private lateinit var interestTextView: TextView
    private lateinit var profilePhoto : ImageView

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        nicknameTextView = findViewById(R.id.nicknameTextView)
        smallTalkTextView = findViewById(R.id.smallTalkTextView)
        ootdTextView = findViewById(R.id.ootdTextView)
        ageTextView = findViewById(R.id.ageTextView)
        genderTextView = findViewById(R.id.genderTextView)
        interestTextView = findViewById(R.id.interestTextView)
        profilePhoto = findViewById(R.id.profilePhoto)

        val userIdHash = intent.getStringExtra("userIdHash")

        if (userIdHash != null) {
            fetchUserProfile(userIdHash)
        } else {
            Log.e("ProfileActivity", "userIdHash가 전달되지 않았습니다.")
            finish()
        }
    }

    private fun fetchUserProfile(userIdHash: String) {
        db.collection("users")
            .whereEqualTo("userIdHash", userIdHash)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.first()
                    val nickname = document.getString("nickname") ?: "Unknown"
                    val smallTalk = document.getString("smallTalk") ?: ""
                    val age = document.getString("age") ?: ""
                    val gender = document.getString("gender") ?: ""
                    val interest = document.getString("interest") ?: ""
                    val ootd = document.getString("ootd") ?: ""
                    val photoUrl = document.getString("photoUrl")
                    if (photoUrl != null) {
                        Picasso.get()
                            .load(photoUrl)
                            .into(profilePhoto)
                    }

                    nicknameTextView.text = nickname
                    smallTalkTextView.text = smallTalk
                    ootdTextView.text = ootd
                    ageTextView.text = age
                    genderTextView.text = gender
                    interestTextView.text = interest

                } else {
                    Log.e("ProfileActivity", "사용자 정보를 찾을 수 없습니다: $userIdHash")
                    finish()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ProfileActivity", "사용자 정보 조회 실패: ${exception.message}")
                finish()
            }
    }
}
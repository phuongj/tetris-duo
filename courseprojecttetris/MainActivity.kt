package com.cs646.program.courseprojecttetris

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

private const val GOOGLE_SIGN_IN = 1
const val SCORE_ONE = "score 1"
const val SCORE_TWO = "score 2"
const val SCORE_THREE = "score 3"
const val SCORE_FOUR= "score 4"
const val SCORE_FIVE = "score 5"


class MainActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInOptions: GoogleSignInOptions
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var playButton: Button
    private lateinit var scoreButton: Button
    private lateinit var instructionsButton: Button
    private lateinit var settingsButton: Button
    private lateinit var accountButton: Button
    private var score1 =  0
    private var score2 =  0
    private var score3 =  0
    private var score4 =  0
    private var score5 =  0
    private var clickableAccountButton = true
    private val database = Firebase.database
    private val firebaseReference : DatabaseReference = database.getReference("users")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        // Google sign in and authentication through firebase
        configureGoogleSignIn()
        firebaseAuth = FirebaseAuth.getInstance()

        playButton = findViewById(R.id.play_button)
        scoreButton = findViewById(R.id.score_button)
        instructionsButton = findViewById(R.id.instructions_button)
        accountButton = findViewById(R.id.account_button)

        // On click listeners

        playButton.setOnClickListener {
            if (clickableAccountButton) {
                val intent = Intent(this, PlayGameActivity::class.java)
                startActivity(intent)
            }
        }
        scoreButton.setOnClickListener {
            if (clickableAccountButton) {
                val intent = Intent(this, ScoreActivity::class.java).apply {
                    if(FirebaseAuth.getInstance().currentUser != null) {
                        putExtra(SCORE_ONE, score1)
                        putExtra(SCORE_TWO, score2)
                        putExtra(SCORE_THREE, score3)
                        putExtra(SCORE_FOUR, score4)
                        putExtra(SCORE_FIVE, score5)
                    }
                }
                startActivity(intent)
            }
        }
        instructionsButton.setOnClickListener {
            if (clickableAccountButton) {
                val intent = Intent(this, InstructionsActivity::class.java)
                startActivity(intent)
            }
        }
        accountButton.setOnClickListener {
            if (clickableAccountButton) {
                if(FirebaseAuth.getInstance().currentUser == null) {
                    clickableAccountButton = false
                    signIn()
                } else {
                    clickableAccountButton = false
                    signOut()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val user = FirebaseAuth.getInstance().currentUser

        // Checks if user is already signed in
        if (user != null) {
            accountButton.text = this.resources.getString(R.string.account_sign_out_button_label)
            updateAccountScores()
        }
    }

    // Google sign in and firebase authentication helper methods

    private fun configureGoogleSignIn() {
        googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)
    }

    private fun signIn() {
        val signInIntent: Intent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN)
    }

    private fun signOut() {
        FirebaseAuth.getInstance().signOut()
        googleSignInClient.signOut().addOnCompleteListener(OnCompleteListener {
            clickableAccountButton = true
            accountButton.text = this.resources.getString(R.string.account_sign_in_button_label)
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_SIGN_IN) {
            if (resultCode != Activity.RESULT_CANCELED) {

            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.result

                if (account != null) {
                    firebaseAuthWithGoogle(account)
                }
            } else {
                clickableAccountButton = true
            }
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful) {
                clickableAccountButton = true
                accountButton.text = this.resources.getString(R.string.account_sign_out_button_label)

                updateAccountScores()
            }
        }
    }

    private fun updateAccountScores() {
        val user = FirebaseAuth.getInstance().currentUser

        firebaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val children = dataSnapshot.children

                // Checks if user is already registered
                children.forEach {
                    if (it.key == user?.uid) {
                        score1 = it.child("0").value.toString().toInt()
                        score2 = it.child("1").value.toString().toInt()
                        score3 = it.child("2").value.toString().toInt()
                        score4 = it.child("3").value.toString().toInt()
                        score5 = it.child("4").value.toString().toInt()
                        return
                    }
                }
                // Sets a new user's scores to 0
                user?.let {
                    val map : HashMap<String, Int> = hashMapOf(
                        "0" to 0,
                        "1" to 0,
                        "2" to 0,
                        "3" to 0,
                        "4" to 0
                    )
                    firebaseReference.child(user.uid).setValue(map)
                }
            }

            override fun onCancelled(error : DatabaseError) {

            }
        })
    }
}
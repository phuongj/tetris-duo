package com.cs646.program.courseprojecttetris

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.iterator
import com.google.firebase.auth.FirebaseAuth

class ScoreActivity : AppCompatActivity() {

    private lateinit var scoresLayout: LinearLayout
    private lateinit var scoresText: TextView
    private lateinit var mainMenuButton: Button
    private var scores = IntArray(5)
    private var scoreOne = 0
    private var scoreTwo = 0
    private var scoreThree = 0
    private var scoreFour = 0
    private var scoreFive = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        this.supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_score)

        scoresText = findViewById(R.id.scores_text)
        scoresLayout = findViewById(R.id.scores_layout)

        if(FirebaseAuth.getInstance().currentUser != null){
            scoreOne = intent.getIntExtra(SCORE_ONE, -1)
            scoreTwo = intent.getIntExtra(SCORE_TWO, -1)
            scoreThree = intent.getIntExtra(SCORE_THREE, -1)
            scoreFour = intent.getIntExtra(SCORE_FOUR, -1)
            scoreFive = intent.getIntExtra(SCORE_FIVE, -1)
        } else {
            for (i in scoresLayout) {
                if (i.id != R.id.scores_text) {
                    scoresLayout.removeView(i)
                }
            }
        }

        scores[0] = scoreOne
        scores[1] = scoreTwo
        scores[2] = scoreThree
        scores[3] = scoreFour
        scores[4] = scoreFive

        if(FirebaseAuth.getInstance().currentUser != null) {
            scoresText.text = this.resources.getString(R.string.score_text_label)
            for (i in scores.indices) {
                val textView = TextView(this)
                textView.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.LEFT
                    width = 275
                    if (i == 4) {
                        setMargins(300, 0, 0, 0)
                    }
                    else {
                        setMargins(300, 0, 0, 0)
                    }
                }
                textView.text = ((i + 1).toString() + ") " + scores[i].toString())
                textView.textSize = 20f
                textView.setTextColor(this.resources.getColor(R.color.text))
                scoresLayout.addView(textView)
            }
        }

        val mainMenuButton = Button(this)

        mainMenuButton.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            width = 300
            setMargins(0, 50, 0, 0)
        }
        mainMenuButton.text = resources.getString(R.string.main_menu_button_label)
        scoresLayout.addView(mainMenuButton)

        mainMenuButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}

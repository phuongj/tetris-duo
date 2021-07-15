package com.cs646.program.courseprojecttetris

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnPreDraw
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class PlayGameActivity : AppCompatActivity() {

    private lateinit var tetrisBoard: TetrisBoardView
    private lateinit var scoreDisplay: TextView
    private lateinit var pauseGameButton: Button
    private lateinit var mainMenuButton: Button
    private lateinit var tetrisBoardLayout: FrameLayout
    private lateinit var mediaPlayer: MediaPlayer
    private var gamePaused = false
    private var lost = false
    private val viewModel: PlayGameViewModel by viewModels()
    private val database = Firebase.database
    private val firebaseReference = database.getReference("course-project-tetris")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.supportActionBar?.hide()
        setContentView(R.layout.activity_play_game)

        tetrisBoard = findViewById(R.id.tetris_board)
        scoreDisplay = findViewById(R.id.score_display)
        pauseGameButton = findViewById(R.id.pause_game_button)
        mainMenuButton = findViewById(R.id.main_menu_button)
        tetrisBoardLayout = findViewById(R.id.tetris_board_layout)

        /*viewModel.currentScore.observe(this, Observer {
            scoreDisplay.text = it.toString()
        })*/

        pauseGameButton.setOnClickListener {
            if (!gamePaused) {
                Thread {
                    tetrisBoard.pauseGame()
                    gamePaused = true
                    pauseGameButton.text = this.resources.getString(R.string.unpause_game_button_label)
                }.start()
            } else {
                tetrisBoard.unpauseGame()
                gamePaused = false
                pauseGameButton.text = this.resources.getString(R.string.pause_game_button_label)
            }
        }

        mainMenuButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Live data observers
        tetrisBoard.score.observe(this, Observer {
            scoreDisplay.text = it.toString()
        })

        tetrisBoard.lost.observe(this, Observer {
            if (it) {
                this.lost = true
                val playAgainButton = Button(this)
                val mainMenuButton = Button(this)

                //firebaseReference.setValue("Hellop")

                // Creates play again button
                playAgainButton.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER
                    width = 275
                    setMargins(35, 450, 0, 0)
                }
                playAgainButton.text = resources.getString(R.string.play_again_button_label)
                tetrisBoardLayout.addView(playAgainButton)

                // Creates home button
                mainMenuButton.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER
                    width = 275
                    setMargins(350, 450, 0, 0)
                }
                mainMenuButton.text = resources.getString(R.string.main_menu_button_label)
                tetrisBoardLayout.addView(mainMenuButton)

                // On click listeners for dynamic button
                playAgainButton.setOnClickListener {
                    val intent = Intent(this, PlayGameActivity::class.java)
                    startActivity(intent)
                }
                mainMenuButton.setOnClickListener {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
            }
        })


        // Updates width to be a percentage of the height
        tetrisBoard.doOnPreDraw {
            tetrisBoard.layoutParams.width = (tetrisBoard.height / 2.2).toInt()
            tetrisBoard.requestLayout()
        }
        tetrisBoard.startGamePhase()
    }

    override fun onResume() {
        super.onResume()

        if (!this.lost) {
            tetrisBoard.unpauseGame()
        }
        mediaPlayer = MediaPlayer.create(applicationContext, R.raw.ost)
        mediaPlayer.isLooping = true
        mediaPlayer.start()
    }

    override fun onPause() {
        super.onPause()

        if (!this.lost) {
            tetrisBoard.pauseGame()
        }
        mediaPlayer.release()
    }

    override fun onBackPressed() {
        return
    }
}

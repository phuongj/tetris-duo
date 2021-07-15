package com.cs646.program.courseprojecttetris

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.scheduleAtFixedRate
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

class TetrisBoardView : View, View.OnTouchListener {

    // Grid values
    private val squareCountX = 10
    private val squareCountY = 22
    private var spacing: Float = 0f

    // Grids to map out board's coordinates, block availability, and block color
    private val coordinatesGrid = Array(squareCountX) {Array(squareCountY) {FloatArray(4) {0f} } }
    private val availabilityGrid = Array(squareCountX) {Array(squareCountY) {true} }
    private val colorGrid = Array(squareCountX) {Array(squareCountY) {Paint()} }

    // Current block variables
    private var currentBlock = ArrayList<IntArray>()
    private var currentBlockName = ""
    private var currentColor: Paint = Paint()

    // Vertical movement variables
    private lateinit var detector: GestureDetectorCompat
    private var dropSpeed: Long = 1500
    private var dropSpeedSpeedup: Long = 75
    private var clearLineSpeed: Long = 500
    private var roundDelay: Long = 0
    private var firstMove = true

    // Horizontal movement variables
    private var currentX:Float = 0f
    private var moveOnDownX: Float = -1f
    private var moveOnDownY: Float = -1f

    // Rotate variables
    private var rotateOnDownX: Float = -1f
    private var rotateOnDownY: Float = -1f
    private var rotateOnDownTime: Long = -1L

    // Subphases
    @Volatile private var moveBlockSubphase = false
    private var longPressSubphase = false

    // Game setup variables
    private var dropCount = 0
    private var direction = ""
    private var directionY = 0
    private var pausedAt = ""
    private var lineClearPoints = 1000
    var score = MutableLiveData<Int>()
    var lost = MutableLiveData<Boolean>()

    // Paints
    private val paintGridLine = Paint()
    private val arrowPaint = Paint()
    private val spawningRowPaint = Paint()
    private val paintOBlock = Paint()
    private val paintIBlock = Paint()
    private val paintTBlock = Paint()
    private val paintLBlock = Paint()
    private val paintJBlock = Paint()
    private val paintSBlock = Paint()
    private val paintZBlock = Paint()

    // Timers
    private lateinit var dropTimer: Timer
    private lateinit var moveTimer: Timer
    private lateinit var clearLineTimer: Timer
    private var dropTimerIsRunning = false
    private var clearLineTimerIsRunning = false

    // User score variables
    private val database = Firebase.database
    private val firebaseReference : DatabaseReference = database.getReference("users")

    constructor(context: Context): super(context)
    constructor(context: Context, attributes: AttributeSet): super(context,attributes){
        this.setOnTouchListener(this)

        // Initialize live data
        score.postValue(0)
        lost.postValue(false)

        detector = GestureDetectorCompat(context, MyGestureListener())

        this.doOnPreDraw {
            spacing = this.height.toFloat() / squareCountY.toFloat()
            mapGrid(coordinatesGrid)
        }
    }

    init  {
        paintGridLine.color = this.resources.getColor((R.color.grid))
        paintGridLine.strokeWidth = 3f
        paintOBlock.color = this.resources.getColor(R.color.oBlock)
        paintIBlock.color = this.resources.getColor(R.color.iBlock)
        paintTBlock.color = this.resources.getColor(R.color.tBlock)
        paintLBlock.color = this.resources.getColor(R.color.lBlock)
        paintJBlock.color = this.resources.getColor(R.color.jBlock)
        paintSBlock.color = this.resources.getColor(R.color.sBlock)
        paintZBlock.color = this.resources.getColor(R.color.zBlock)
        arrowPaint.color = Color.RED
        arrowPaint.strokeWidth = 10.0f
        spawningRowPaint.color = this.resources.getColor(R.color.boarderBackground)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        fun drawBlock(coordinates: IntArray, paint: Paint) {
            // Squares referenced by indices converted to pixel coordinates for drawing
            canvas?.drawRect(
                coordinatesGrid[coordinates[0]][coordinates[1]][0],
                coordinatesGrid[coordinates[0]][coordinates[1]][1],
                coordinatesGrid[coordinates[0]][coordinates[1]][2],
                coordinatesGrid[coordinates[0]][coordinates[1]][3],
                paint)
        }
        // Draws horizontal grid lines
        for (i in 0..squareCountY) {
            canvas?.drawLine(
                0f, i * spacing,
                width.toFloat(), i * spacing,
                paintGridLine)
        }
        // Draws vertical grid line
        for (i in 0..squareCountX) {
            canvas?.drawLine(
                i * spacing, 0f,
                i * spacing, height.toFloat(),
                paintGridLine
            )
        }

        // Draws spawning rectangle
        canvas?.drawRect(
            0f, (squareCountY / 2 - 1) * spacing,
            width.toFloat(), (squareCountY / 2 + 1) * spacing,
            spawningRowPaint)

        // Draws saved grid
        for (i in 0 until squareCountX) {
            for (j in 0 until squareCountY) {
                if (!availabilityGrid[i][j])
                {
                    drawBlock(intArrayOf(i, j), colorGrid[i][j])
                }
            }
        }
        // Draws current block
        for (i in currentBlock.indices) {
            drawBlock(currentBlock[i], currentColor)
        }

        // Draws arrows
        if (direction == "up") {
            val pathLeft = Path()
            //path.fillType = Path.FillType.EVEN_ODD
            pathLeft.moveTo(40f, (height / 2 + 20).toFloat())
            pathLeft.lineTo(70f, (height / 2 - 20).toFloat())
            pathLeft.lineTo(100f, (height / 2 + 20).toFloat())
            pathLeft.lineTo(40f, (height / 2 + 20).toFloat())
            pathLeft.close()

            canvas?.drawPath(pathLeft, arrowPaint)

            val pathRight = Path()
            //path.fillType = Path.FillType.EVEN_ODD
            pathRight .moveTo(width - 40f, (height / 2 + 20).toFloat())
            pathRight .lineTo(width - 70f, (height / 2 - 20).toFloat())
            pathRight .lineTo(width - 100f, (height / 2 + 20).toFloat())
            pathRight .lineTo(width - 40f, (height / 2 + 20).toFloat())
            pathRight .close()

            canvas?.drawPath(pathRight, arrowPaint)
        } else if (direction == "down") {
            val pathLeft = Path()
            //path.fillType = Path.FillType.EVEN_ODD
            pathLeft.moveTo(40f, (height / 2 - 20).toFloat())
            pathLeft.lineTo(70f, (height / 2 + 20).toFloat())
            pathLeft.lineTo(100f, (height / 2 - 20).toFloat())
            pathLeft.lineTo(40f, (height / 2 - 20).toFloat())
            pathLeft.close()

            canvas?.drawPath(pathLeft, arrowPaint)

            val pathRight = Path()
            //path.fillType = Path.FillType.EVEN_ODD
            pathRight .moveTo(width - 40f, (height / 2 - 20).toFloat())
            pathRight .lineTo(width - 70f, (height / 2 + 20).toFloat())
            pathRight .lineTo(width - 100f, (height / 2 - 20).toFloat())
            pathRight .lineTo(width - 40f, (height / 2 - 20).toFloat())
            pathRight .close()

            canvas?.drawPath(pathRight, arrowPaint)
        }
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {

        detector.onTouchEvent(event)

        // Stops block speedup on touch lift up after long press
        if (longPressSubphase) {
            if (event?.action == MotionEvent.ACTION_UP) {
                dropBlockPhase(dropSpeed, dropSpeed)
                longPressSubphase = false
            }
        }

        // Moves blocks left and right
        if (moveBlockSubphase) {
            if (event?.action == MotionEvent.ACTION_DOWN) {
                moveOnDownX = event.x
                moveOnDownY = event.y
            }
            if (event?.action == MotionEvent.ACTION_MOVE) {
                if (sqrt((moveOnDownX - event.x).pow(2) + (moveOnDownY - event.y).pow(2)) > 20) {
                    if (event.x > 0f && event.x < width.toFloat()) {

                        if (firstMove) {
                            moveTimer = if (this::moveTimer.isInitialized) {
                                moveTimer.cancel()
                                moveTimer.purge()
                                Timer()
                            }else {
                                Timer()
                            }

                            moveTimer.scheduleAtFixedRate(70, 70) {
                                moveHorizontal()
                            }
                            firstMove = false
                        }
                        currentX = event.x
                    }
                }
            }
        }
        if (!firstMove) {
            if (event?.action == MotionEvent.ACTION_UP) {
                if (this::moveTimer.isInitialized) {
                    moveTimer.cancel()
                    moveTimer.purge()
                }
                firstMove = true
            }
        }

        // Rotates blocks clockwise
        if (moveBlockSubphase) {
            if (event?.action == MotionEvent.ACTION_DOWN) {
                rotateOnDownX = event.x
                rotateOnDownY = event.y
                rotateOnDownTime = System.currentTimeMillis()
            }
            if (event?.action == MotionEvent.ACTION_UP) {
                if (sqrt((rotateOnDownX - event.x).pow(2) + (rotateOnDownY - event.y).pow(2)) < 20 &&
                        System.currentTimeMillis() - rotateOnDownTime < 200) {
                    val eventX = floor(event.x / spacing).toInt()
                    val eventY = floor(event.y / spacing).toInt()
                    for (i in currentBlock.indices) {
                        if (abs(eventX - currentBlock[i][0]) <= 1 && abs(eventY - currentBlock[i][1]) <= 1) {
                            rotateBlock()
                            break
                        }
                    }
                }
            }
        }
        return true
    }

    // Phases
    fun startGamePhase() {
        postDelayed( { chooseBlockPhase() }, 100)
    }

    private fun chooseBlockPhase() {
        // Center blocked used for reference during block creation
        val centerSquareX = 4
        val centerSquareY = 11

        // Squares to be drawn referenced by 2D array indices
        when((0..6).random()) {
            // O block
            0-> {
                currentBlock.add(intArrayOf(centerSquareX, centerSquareY))
                currentBlock.add(intArrayOf(centerSquareX , centerSquareY - 1))
                currentBlock.add(intArrayOf(centerSquareX + 1, centerSquareY))
                currentBlock.add(intArrayOf(centerSquareX + 1, centerSquareY - 1))
                currentBlockName = "O-Block"
                currentColor = paintOBlock
            }

            // I block
            1-> {
                currentBlock.add(intArrayOf(centerSquareX, centerSquareY))
                currentBlock.add(intArrayOf(centerSquareX - 1, centerSquareY))
                currentBlock.add(intArrayOf(centerSquareX + 1, centerSquareY))
                currentBlock.add(intArrayOf(centerSquareX + 2, centerSquareY))
                currentBlockName = "I-Block"
                currentColor = paintIBlock
            }

            // T block
            2-> {
                currentBlock.add(intArrayOf(centerSquareX, centerSquareY))
                currentBlock.add(intArrayOf(centerSquareX, centerSquareY - 1))
                currentBlock.add(intArrayOf(centerSquareX - 1, centerSquareY))
                currentBlock.add(intArrayOf(centerSquareX + 1, centerSquareY))
                currentBlockName = "T-Block"
                currentColor = paintTBlock
            }

            // L block
            3-> {
                currentBlock.add(intArrayOf(centerSquareX, centerSquareY))
                currentBlock.add(intArrayOf(centerSquareX - 1, centerSquareY))
                currentBlock.add(intArrayOf(centerSquareX + 1, centerSquareY))
                currentBlock.add(intArrayOf(centerSquareX + 1, centerSquareY - 1))
                currentBlockName = "L-Block"
                currentColor = paintLBlock
            }

            // J block
            4-> {
                currentBlock.add(intArrayOf(centerSquareX, centerSquareY))
                currentBlock.add(intArrayOf(centerSquareX - 1, centerSquareY))
                currentBlock.add(intArrayOf(centerSquareX - 1, centerSquareY - 1))
                currentBlock.add(intArrayOf(centerSquareX + 1, centerSquareY))
                currentBlockName = "J-Block"
                currentColor = paintJBlock
            }

            // S block
            5-> {
                currentBlock.add(intArrayOf(centerSquareX, centerSquareY))
                currentBlock.add(intArrayOf(centerSquareX - 1, centerSquareY))
                currentBlock.add(intArrayOf(centerSquareX, centerSquareY - 1))
                currentBlock.add(intArrayOf(centerSquareX + 1, centerSquareY - 1))
                currentBlockName = "S-Block"
                currentColor = paintSBlock
            }

            // Z block
            6-> {
                currentBlock.add(intArrayOf(centerSquareX, centerSquareY))
                currentBlock.add(intArrayOf(centerSquareX - 1, centerSquareY - 1))
                currentBlock.add(intArrayOf(centerSquareX, centerSquareY - 1))
                currentBlock.add(intArrayOf(centerSquareX + 1, centerSquareY))
                currentBlockName = "Z-Block"
                currentColor = paintZBlock
            }
        }
        invalidate()

        // Determines direction of movement each round
        determineDirection()

        dropBlockPhase(dropSpeed, dropSpeed)
    }

    private fun dropBlockPhase(delay: Long, period: Long) {
        // Removes previously running drop timer
        dropTimer = if (this::dropTimer.isInitialized) {
            dropTimer.cancel()
            dropTimer.purge()
            Timer()
        }else {
            Timer()
        }

        // Starts drop timer animation
        dropTimer.scheduleAtFixedRate(delay, period) {
            moveVertical()
        }
        dropTimerIsRunning = true

        // Allows for horizontal movement of blocks before dropping from center
        moveBlockSubphase = true
    }

    private fun checkLossPhase() {
        for (i in currentBlock.indices) {
            if (currentBlock[i][1] in (squareCountY / 2 - 1) until (squareCountY / 2 + 1)) {
                // Saves board as bitmap and initializes end round screen
                lost.postValue(true)
                updateUserScore()
                return
            }
        }
        currentBlock.clear()
        checkLineClearPhase()
    }

    private fun checkLineClearPhase() {
        clearLineTimer = if (this::clearLineTimer.isInitialized) {
            clearLineTimer.cancel()
            clearLineTimer.purge()
            Timer()
        }else {
            Timer()
        }

        clearLineTimer.scheduleAtFixedRate(clearLineSpeed, clearLineSpeed) {
            clearLine()
        }
        clearLineTimerIsRunning = true
    }

    // Pause functions
    fun pauseGame() {
        while (!dropTimerIsRunning && !clearLineTimerIsRunning) {
            Thread.sleep(100)
        }

        moveBlockSubphase = false
        longPressSubphase = false

        if (dropTimerIsRunning) {
            pausedAt = "dropTimer"
            if (this::dropTimer.isInitialized) {
                dropTimer.cancel()
                dropTimer.purge()
            }
            if (this::clearLineTimer.isInitialized) {
                clearLineTimer.cancel()
                clearLineTimer.purge()
            }
            if (this::moveTimer.isInitialized) {
                moveTimer.cancel()
                moveTimer.purge()
            }
        }
        else if (clearLineTimerIsRunning) {
            pausedAt = "clearLineTimer"
            if (this::clearLineTimer.isInitialized) {
                clearLineTimer.cancel()
                clearLineTimer.purge()
            }
            if (this::dropTimer.isInitialized) {
                dropTimer.cancel()
                dropTimer.purge()
            }
            if (this::moveTimer.isInitialized) {
                moveTimer.cancel()
                moveTimer.purge()
            }
        }
    }

    fun unpauseGame() {
        if (pausedAt == "dropTimer") {
            dropBlockPhase(dropSpeed, dropSpeed)
        }
        else if (pausedAt == "clearLineTimer") {
            checkLineClearPhase()
        }

    }

    // Helper functions
    private fun mapGrid(emptyGrid: Array<Array<FloatArray>>) {
        for (x in emptyGrid.indices) {
            for (y in emptyGrid[0].indices){
                emptyGrid[x][y] = floatArrayOf(
                    x * spacing + 2,
                    + y * spacing + 2,
                    + (x + 1) * spacing - 2,
                    + (y + 1) * spacing - 2)
            }
        }
    }

    private fun saveCurrentGrid() {
        // Saves current block into the grid
        for (i in currentBlock.indices) {
            availabilityGrid[currentBlock[i][0]][currentBlock[i][1]] = false
            colorGrid[currentBlock[i][0]][currentBlock[i][1]] = currentColor
        }
        invalidate()
    }

    private fun moveHorizontal() {

        val moveCenter = floor(currentX / spacing).toInt()
        var blocked = false
        val currentBlockTemp = ArrayList<IntArray>()

        if (moveBlockSubphase) {
            if (moveCenter != currentBlock[0][0]) {
                for (i in currentBlock.indices) {
                    if (moveCenter < currentBlock[0][0]) {
                        val setX = currentBlock[i][0] - 1
                        val setY = currentBlock[i][1]

                        if (setX >= 0 && availabilityGrid[setX][setY]) {
                            currentBlockTemp.add(intArrayOf(setX, setY))
                        } else {
                            blocked = true
                            break
                        }
                    } else if (moveCenter > currentBlock[0][0]) {
                        val setX = currentBlock[i][0] + 1
                        val setY = currentBlock[i][1]

                        if (setX < squareCountX && availabilityGrid[setX][setY]) {
                            currentBlockTemp.add(intArrayOf(setX, setY))
                        } else {
                            blocked = true
                            break
                        }
                    }
                }
                if (!blocked) {
                    currentBlock = currentBlockTemp
                    invalidate()
                }
            }
        }
    }

    private fun moveVertical() {
        val currentBlockTemp = ArrayList<IntArray>()

        // Checks for blocks in the way and the bottom of the board
        for (i in currentBlock.indices) {
            val setX = currentBlock[i][0]
            val setY = currentBlock[i][1] + directionY

            if (setY in 0 until squareCountY && availabilityGrid[setX][setY]) {
                currentBlockTemp.add(intArrayOf(setX, setY))
            } else {
                // Resets for next block
                if (this::dropTimer.isInitialized) {
                    dropTimer.cancel()
                    dropTimer.purge()
                }
                dropTimerIsRunning = false
                prepareForCheck()
                checkLossPhase()
                return
            }
        }
        currentBlock = currentBlockTemp
        invalidate()
    }

    private fun rotateBlock() {
        val currentBlockTemp = ArrayList<IntArray>()
        var blocked = false

        if (currentBlockName != "O-Block") {
            for (i in 0 until currentBlock.size) {
                val setX = currentBlock[0][0] + (currentBlock[i][1] - currentBlock[0][1]) * -1
                val setY = currentBlock[0][1] + (currentBlock[i][0] - currentBlock[0][0])

                // Prevents rotation into another block or out of the board
                if (setX in 0 until squareCountX &&
                    setY in 0 until squareCountY &&
                    availabilityGrid[setX][setY]) {
                    currentBlockTemp.add(intArrayOf(setX, setY))
                } else {
                    blocked = true
                }
            }
            if (!blocked) {
                currentBlock = currentBlockTemp
                invalidate()
            }
        }
    }

    private fun dropInstant() {
        var blocked = false

        // Remove drop down timer
        if (this::dropTimer.isInitialized) {
            dropTimer.cancel()
            dropTimer.purge()
        }
        dropTimerIsRunning = false

        do {
            val currentBlockTemp = ArrayList<IntArray>()

            // Checks for blocks below
            for (i in currentBlock.indices) {
                val setX = currentBlock[i][0]
                val setY = currentBlock[i][1] + directionY

                if (setY in 0 until squareCountY && availabilityGrid[setX][setY]) {
                    currentBlockTemp.add(intArrayOf(setX, setY))
                }
                else {
                    blocked = true
                    break
                }
            }
            if (!blocked) {
                currentBlock = currentBlockTemp
            }
        } while (!blocked)
        // Resets for next block
        prepareForCheck()
        checkLossPhase()
        return
    }

    private fun determineDirection() {
        if (direction == "" || direction == "up") {
            direction = "down"
            directionY = 1
        } else if (direction == "down") {
            direction = "up"
            directionY = -1
        }
    }

    private fun clearLine() {
        var tetrisFoundFlag: Boolean

        if (direction == "down") {
            for (j in squareCountY - 1 downTo 0) {
                tetrisFoundFlag = true
                // Finds rows needing to be cleared
                for (i in 0 until squareCountX) {
                    if (availabilityGrid[i][j]) {
                        tetrisFoundFlag = false
                        break
                    }
                }
                if (tetrisFoundFlag) {
                    score.postValue(score.value?.plus(lineClearPoints))
                    for (l in j downTo (squareCountY / 2) + 2) {
                        for (k in 0 until squareCountX) {
                            availabilityGrid[k][l] = availabilityGrid[k][l - 1]
                            colorGrid[k][l] = colorGrid[k][l - 1]
                        }
                    }
                    for (k in 0 until squareCountX) {
                        availabilityGrid[k][(squareCountY / 2) + 1] = true
                        colorGrid[k][(squareCountY / 2) + 1] = Paint()
                    }
                    invalidate()
                    return
                }
            }
            if (this::clearLineTimer.isInitialized) {
                clearLineTimer.cancel()
                clearLineTimer.purge()
            }
            clearLineTimerIsRunning = false
            postDelayed( { chooseBlockPhase() }, roundDelay)
        }

        else if (direction == "up") {
            for (j in 0 until squareCountY) {
                tetrisFoundFlag = true
                // Finds rows needing to be cleared
                for (i in 0 until squareCountX) {
                    if (availabilityGrid[i][j]) {
                        tetrisFoundFlag = false
                        break
                    }
                }
                if (tetrisFoundFlag) {
                    score.postValue(score.value?.plus(lineClearPoints))
                    for (l in j..(squareCountY / 2) - 3) {
                        for (i in 0 until squareCountX) {
                            availabilityGrid[i][l] = availabilityGrid[i][l + 1]
                            colorGrid[i][l] = colorGrid[i][l + 1]
                        }
                    }
                    for (k in 0 until squareCountX) {
                        availabilityGrid[k][(squareCountY / 2) - 2] = true
                        colorGrid[k][(squareCountY / 2) - 2] = Paint()
                    }
                    invalidate()
                    return
                }
            }
            if (this::clearLineTimer.isInitialized) {
                clearLineTimer.cancel()
                clearLineTimer.purge()
            }
            clearLineTimerIsRunning = false
            postDelayed( { chooseBlockPhase() }, roundDelay)
        }
    }

    private fun prepareForCheck() {
        moveBlockSubphase = false
        longPressSubphase = false
        saveCurrentGrid()
    }

    inner class MyGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)

            val eventX = floor(e.x / spacing).toInt()
            val eventY = floor(e.y / spacing).toInt()

            if (moveBlockSubphase) {
                for (i in currentBlock.indices) {
                    if (abs(eventX - currentBlock[i][0]) <= 1 && abs(eventY - currentBlock[i][1]) <= 1) {
                        return
                    }
                }
                longPressSubphase = true
                dropBlockPhase(0, dropSpeedSpeedup)
            }
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (moveBlockSubphase) {
                val eventX = floor(e.x / spacing).toInt()
                val eventY = floor(e.y / spacing).toInt()

                for (i in currentBlock.indices) {
                    if (abs(eventX - currentBlock[i][0]) <= 1 && abs(eventY - currentBlock[i][1]) <= 1) {
                        return false
                    }
                }
                dropInstant()
            }
            return super.onDoubleTap(e)
        }
    }

    // Update user's score
    private fun updateUserScore() {


        if(FirebaseAuth.getInstance().currentUser != null){
            val user = FirebaseAuth.getInstance().currentUser
            var updatedOnce = false

            firebaseReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (!updatedOnce) {
                        updatedOnce = true
                        val children = dataSnapshot.children
                        var score1 = 0
                        var score2 = 0
                        var score3 = 0
                        var score4 = 0
                        var score5 = 0
                        var scoresList = arrayListOf<Int>()

                        // Checks if user is already registered
                        children.forEach {
                            if (it.key == user?.uid) {
                                score1 = it.child("0").value.toString().toInt()
                                score2 = it.child("1").value.toString().toInt()
                                score3 = it.child("2").value.toString().toInt()
                                score4 = it.child("3").value.toString().toInt()
                                score5 = it.child("4").value.toString().toInt()
                            }
                        }
                        scoresList.add(score1)
                        scoresList.add(score2)
                        scoresList.add(score3)
                        scoresList.add(score4)
                        scoresList.add(score5)
                        scoresList.add(score.value as Int)

                        val scoresListSorted: List<Int> = scoresList.sortedWith(reverseOrder())
                        Log.i("hi", scoresList[0].toString())

                        user?.let {
                            val map: HashMap<String, Int> = hashMapOf(
                                "0" to scoresListSorted[0],
                                "1" to scoresListSorted[1],
                                "2" to scoresListSorted[2],
                                "3" to scoresListSorted[3],
                                "4" to scoresListSorted[4]
                            )
                            firebaseReference.child(user.uid).setValue(map)
                        }
                    }
                }

                override fun onCancelled(error : DatabaseError) {

                }
            })
        }
    }
}
package com.cs646.program.courseprojecttetris

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PlayGameViewModel : ViewModel() {

    val currentScore: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }
}
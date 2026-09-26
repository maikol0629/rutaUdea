package com.udea.rutaudea.ui.screen.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udea.rutaudea.data.repository.QuestionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashViewModel(
    private val repository: QuestionRepository
) : ViewModel() {

    suspend fun loadQuestions() {
        var count = 0
        repeat(30) {
            count = repository.countQuestions()
            if (count > 0) return
            delay(100)
        }
        require(count > 0) { "Database seeding timed out" }
    }
}
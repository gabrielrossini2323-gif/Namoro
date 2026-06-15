package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class UiEvent {
    data class MatchCreated(val matchedProfile: UserProfile) : UiEvent()
}

class CupidViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: CupidRepository

    // Base flows from repository
    val allProfiles: Flow<List<UserProfile>>
    val currentUserProfile: Flow<UserProfile?>
    val allMatches: Flow<List<MatchConversation>>

    // Live decks to swipe
    private val _swipedUserIds = MutableStateFlow<List<Long>>(emptyList())
    val swipedUserIds: StateFlow<List<Long>> = _swipedUserIds.asStateFlow()

    // Filtered cards deck
    private val _unswipedProfiles = MutableStateFlow<List<UserProfile>>(emptyList())
    val unswipedProfiles: StateFlow<List<UserProfile>> = _unswipedProfiles.asStateFlow()

    // Active screen navigation inside the tabs model
    private val _activeMatch = MutableStateFlow<MatchConversation?>(null)
    val activeMatch: StateFlow<MatchConversation?> = _activeMatch.asStateFlow()

    // Active chat messages
    private val _activeMatchMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val activeMatchMessages: StateFlow<List<ChatMessage>> = _activeMatchMessages.asStateFlow()

    // Match Celebration overlay details
    private val _celebrationProfile = MutableStateFlow<UserProfile?>(null)
    val celebrationProfile: StateFlow<UserProfile?> = _celebrationProfile.asStateFlow()

    init {
        val database = CupidDatabase.getDatabase(application)
        val dao = database.cupidDao()
        repository = CupidRepository(dao)

        allProfiles = repository.allProfiles
        currentUserProfile = repository.currentUserProfile
        allMatches = repository.allMatches

        // Kickoff database seed & initial flows sync
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
            refreshSwipedList()
        }

        // Combine all profiles and swiped IDs to build the Tinder Swipe Deck
        viewModelScope.launch {
            combine(allProfiles, swipedUserIds) { profiles, swipedIds ->
                profiles.filter { !it.isCurrentUser && !swipedIds.contains(it.id) }
            }.collect { filtered ->
                _unswipedProfiles.value = filtered
            }
        }
    }

    private suspend fun refreshSwipedList() {
        val user = repository.getCurrentUser()
        if (user != null) {
            val swiped = repository.getSwipedUserIds(user.id)
            _swipedUserIds.value = swiped
        }
    }

    fun handleSwipeLeft(targetProfile: UserProfile) {
        viewModelScope.launch {
            val user = repository.getCurrentUser() ?: return@launch
            repository.swipe(fromUserId = user.id, toUserId = targetProfile.id, isLike = false)
            refreshSwipedList()
        }
    }

    fun handleSwipeRight(targetProfile: UserProfile) {
        viewModelScope.launch {
            val user = repository.getCurrentUser() ?: return@launch
            val isMatch = repository.swipe(fromUserId = user.id, toUserId = targetProfile.id, isLike = true)
            refreshSwipedList()
            if (isMatch) {
                // Trigger match celebration popup overlay!
                _celebrationProfile.value = targetProfile
            }
        }
    }

    fun dismissCelebration() {
        _celebrationProfile.value = null
    }

    fun selectMatch(match: MatchConversation) {
        _activeMatch.value = match
        viewModelScope.launch {
            repository.getMessagesForMatch(match.id).collect { messages ->
                _activeMatchMessages.value = messages
            }
        }
    }

    fun closeChat() {
        _activeMatch.value = null
        _activeMatchMessages.value = emptyList()
    }

    fun sendChatMessage(matchId: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val user = repository.getCurrentUser() ?: return@launch
            // Add your message
            repository.sendMessage(matchId = matchId, senderId = user.id, content = content)
            
            // Auto simulated dynamic reply!
            delay(1500)
            val match = _activeMatch.value
            if (match != null && match.id == matchId) {
                val partnerId = if (match.user1Id == user.id) match.user2Id else match.user1Id
                val replies = listOf(
                    "Que demais! Me conta mais sobre isso! 😊",
                    "Adorei ouvir isso! O que você costuma fazer nos finais de semana?",
                    "Hahaha que engraçado! Com certeza concordo.",
                    "Interessante... O que acha de tomarmos um café qualquer dia desses? ☕",
                    "Que bacana! 😊 Estou curtindo bastante conversar com você.",
                    "Nossa, sim! Super me identifico com isso."
                )
                repository.sendMessage(matchId = matchId, senderId = partnerId, content = replies.random())
            }
        }
    }

    fun saveMyProfile(name: String, age: Int, bio: String, occupation: String, interestTags: String, avatarIndex: Int) {
        viewModelScope.launch {
            val profile = UserProfile(
                id = 999, // Static ID for current user
                name = name,
                age = age,
                bio = bio,
                occupation = occupation,
                interestTags = interestTags,
                avatarIndex = avatarIndex,
                isCurrentUser = true,
                distanceKm = 0
            )
            repository.updateProfile(profile)
        }
    }

    fun resetDemo() {
        viewModelScope.launch {
            // Drop tables or delete swipes and matches in-memory style safely
            val database = CupidDatabase.getDatabase(getApplication())
            database.clearAllTables()
            _activeMatch.value = null
            _activeMatchMessages.value = emptyList()
            _swipedUserIds.value = emptyList()
            repository.seedDatabaseIfEmpty()
            refreshSwipedList()
        }
    }
}

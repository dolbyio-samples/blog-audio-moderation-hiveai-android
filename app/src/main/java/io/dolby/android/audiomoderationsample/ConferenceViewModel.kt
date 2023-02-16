package io.dolby.android.audiomoderationsample

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voxeet.sdk.events.sdk.ConferenceStatusUpdatedEvent
import com.voxeet.sdk.logger.VoxeetLogger
import com.voxeet.sdk.models.Conference
import com.voxeet.sdk.services.conference.information.ConferenceStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.dolby.android.audiomoderationsample.coroutines.launch
import io.dolby.android.audiomoderationsample.features.audiorecord.ObserveLocalAudioSamplesUseCase
import io.dolby.android.audiomoderationsample.features.conference.CloseSessionUseCase
import io.dolby.android.audiomoderationsample.features.conference.JoinConferenceUseCase
import io.dolby.android.audiomoderationsample.features.conference.LeaveConferenceUseCase
import io.dolby.android.audiomoderationsample.features.conference.OpenSessionUseCase
import io.dolby.android.audiomoderationsample.features.moderation.AudioModerationUseCase
import io.dolby.android.audiomoderationsample.features.moderation.model.Clasification
import io.dolby.android.audiomoderationsample.features.moderation.model.Status
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject
import kotlin.random.Random
import kotlin.random.nextInt

data class ConferenceUiState(
    val isSessionOpened: Boolean = false,
    val isSessionOpening: Boolean = false,
    val isConferenceJoining: Boolean = false,
    val isConferenceJoined: Boolean = false,
    val conferenceName: String = "",
    val username: String = "",
    val userExternalId: String = "",
    val moderationInProgress: Boolean = false,
    val moderationStatus: Status? = null,
    val moderationLanguage: String? = null,
    val moderationTranscribedText: String? = "",
    val moderationClassifications: List<Clasification>? = null
)

sealed class ConferenceUiAction {
    data class ShowErrorSnackbar(val message: String) : ConferenceUiAction()
}

interface StateHandler<UiState> {
    val states: StateFlow<UiState>
    fun setState(state: UiState)
    fun updateState(block: UiState.() -> UiState)
}

interface ActionsHandler<UiAction> {
    val actions: Flow<UiAction>
    suspend fun emitAction(action: UiAction)
}

private class StateHandlerImpl<UiState>(defaultState: UiState) : StateHandler<UiState> {
    private val _state = MutableStateFlow(defaultState)
    override val states: StateFlow<UiState> = _state.asStateFlow()

    override fun setState(state: UiState) {
        _state.value = state
    }

    override fun updateState(block: UiState.() -> UiState) {
        _state.value = block(_state.value)
    }
}

private class ActionsHandlerImpl<UiAction> : ActionsHandler<UiAction> {
    private val _actions = Channel<UiAction>(capacity = Channel.BUFFERED)
    override val actions: Flow<UiAction> = _actions.receiveAsFlow()

    override suspend fun emitAction(action: UiAction) {
        _actions.send(action)
    }
}

@Suppress("UnnecessaryAbstractClass")
abstract class ActionsViewModel<UiAction> :
    ViewModel(),
    ActionsHandler<UiAction> by ActionsHandlerImpl()

@Suppress("UnnecessaryAbstractClass")
abstract class StateActionsViewModel<UiState, UiAction>(defaultState: UiState) :
    ActionsViewModel<UiAction>(),
    StateHandler<UiState> by StateHandlerImpl(defaultState)


fun <UiAction> ActionsViewModel<UiAction>.action(action: UiAction) = viewModelScope.launch {
    emitAction(action)
}

fun ViewModel.launch(
    onError: (Throwable) -> Unit = { throw IllegalStateException("Error block not implemented", it) },
    onSuccess: suspend CoroutineScope.() -> Unit
) = viewModelScope.launch(onError, onSuccess)


@HiltViewModel
class ConferenceViewModel @Inject constructor(
    private val joinConference: JoinConferenceUseCase,
    private val leaveConference: LeaveConferenceUseCase,
    private val closeSession: CloseSessionUseCase,
    private val openSession: OpenSessionUseCase,
    private val observeLocalAudioSamples: ObserveLocalAudioSamplesUseCase,
    private val audioModeration: AudioModerationUseCase,
    @ApplicationContext context: Context
) : StateActionsViewModel<ConferenceUiState, ConferenceUiAction>(ConferenceUiState()){

    private val logger: VoxeetLogger = VoxeetLogger(this::class.java.simpleName)

    @SuppressLint("StaticFieldLeak")
    private var contextApp : Context

    init {
        contextApp = context
        EventBus.getDefault().register(this)

        viewModelScope.launch {
            val userName = createDefaultUserName()
            val userExternalId = getUserExternalIdFromName(userName)
            updateState {
                copy(
                    username = userName,
                    userExternalId = userExternalId,
                    conferenceName = conferenceName.ifEmpty { DEFAULT_CONFERENCE_NAME }
                )
            }
        }
    }


    private var lastConference: Conference? = null

    fun usernameChanged(name: String) = updateState {
        copy(username = name, userExternalId = getUserExternalIdFromName(name))
    }

    fun conferenceNameChanged(name: String) = updateState { copy(conferenceName = name) }

    private fun getUserExternalIdFromName(name: String) = name.filter { it.isLetterOrDigit() }.lowercase()
    private fun createDefaultUserName() = "John Doe ${Random.nextInt(0..MAX_USER_ID_NUMBER)}"

    fun joinConference() = launch(onError = {
        logger.e("Could not join conference", it)
        action(ConferenceUiAction.ShowErrorSnackbar(contextApp.getString(R.string.common_error)))
        updateState { copy(isConferenceJoining = false, isConferenceJoined = false,
            isSessionOpening = false, isSessionOpened = false) }
    }) {

        updateState { copy(isSessionOpening = true) }

        openSession.run(states.value.username, states.value.userExternalId,
            onSuccess = {
                updateState { copy(isSessionOpening = false, isSessionOpened = true,
                    isConferenceJoining = true, isConferenceJoined = false) }
                joinConference.run(states.value.conferenceName,
                    onSuccess = { conference ->
                        lastConference = conference
                        updateState { copy(isConferenceJoining = false, isConferenceJoined = true) }
                        logger.i("Conference joined " + conference.alias)
                        action(ConferenceUiAction.ShowErrorSnackbar("Conference joined " + conference.alias))
                    },
                    onError = {
                        logger.e("Could not join conference", it)
                        action(ConferenceUiAction.ShowErrorSnackbar(contextApp.getString(R.string.common_error)))
                        updateState { copy(isConferenceJoining = false, isConferenceJoined = false) }
                    }
                )
            },
            onError = {
                logger.e("Could not open session", it)
                action(ConferenceUiAction.ShowErrorSnackbar(contextApp.getString(R.string.common_error)))
                updateState { copy(isSessionOpening = false, isSessionOpened = false) }
            }
        )
    }

    fun leaveConference() = launch(onError = {
        logger.e("Could not leave conference", it)
        action(ConferenceUiAction.ShowErrorSnackbar(contextApp.getString(R.string.common_error)))
    }) {
        leaveConference.run(onSuccess = {
            logger.i("Conference left " + it)
            updateState { copy(isConferenceJoining = false, isConferenceJoined = false ) }
            closeSession.run(onSuccess = {
                logger.i("Session closed " + it)
                updateState { copy(isSessionOpened = false) }
            }, onError = {
                logger.e("Could not close conference", it)
                action(ConferenceUiAction.ShowErrorSnackbar(contextApp.getString(R.string.common_error)))
                updateState { copy(isSessionOpened = false) }
            })
        }, onError = {
            logger.e("Could not leave conference", it)
            action(ConferenceUiAction.ShowErrorSnackbar(contextApp.getString(R.string.common_error)))
        })
    }

    fun moderateAudio() = launch{

        updateState { copy(moderationInProgress = true) }
        observeLocalAudioSamples.getWaveFile(contextApp, onSuccess = {
            audioModeration.run( waveFile = it,
                onSuccess = { result ->
                    logger.i("Audio Moderation finished")
                    val status = result.status?.firstOrNull()?.status
                    val language = result.status?.firstOrNull()?.response?.language
                    val transcript = result.status?.firstOrNull()?.response?.output?.firstOrNull()?.transcript
                    val classifications =  result.status?.firstOrNull()?.response?.output?.firstOrNull()?.classifications
                    updateState { copy(
                        moderationInProgress = false,
                        moderationStatus = status,
                        moderationLanguage = language,
                        moderationTranscribedText = transcript,
                        moderationClassifications = classifications)
                    }
                },
                onError = {
                    logger.e("Audio Moderation exception", it)
                    action(ConferenceUiAction.ShowErrorSnackbar(contextApp.getString(R.string.common_error)))
                    updateState {
                        copy(moderationInProgress = false)
                    }
                })
        }, onError = {
            logger.e("Cannot create wave file for moderation", it)
            action(ConferenceUiAction.ShowErrorSnackbar(contextApp.getString(R.string.common_error)))
        })

    }


    private fun startObserveLocalAudioSamples() = launch {
        observeLocalAudioSamples.run()
    }

    private fun stopObserveLocalAudioSamples() = launch {
        observeLocalAudioSamples.stop()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    internal fun onConferenceStatusUpdated(event: ConferenceStatusUpdatedEvent) = MainScope().launch {
        when (event.state) {
            ConferenceStatus.JOINED -> {
                startObserveLocalAudioSamples()
            }
            ConferenceStatus.LEFT -> {
                stopObserveLocalAudioSamples()
            }
            else -> {
            }
        }
    }

    companion object {
        private const val MAX_USER_ID_NUMBER = 100
        private const val DEFAULT_CONFERENCE_NAME = "AudioModeration"
    }
}
package io.dolby.android.audiomoderationsample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.insets.navigationBarsWithImePadding
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.voxeet.sdk.logger.VoxeetLogger
import com.voxeet.sdk.utils.RuntimePermissions
import dagger.hilt.android.AndroidEntryPoint
import io.dolby.android.audiomoderationsample.Splash.initializeSplashScreen
import io.dolby.android.audiomoderationsample.coroutines.launch
import io.dolby.android.audiomoderationsample.ui.theme.AudioModerationSampleTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val logger: VoxeetLogger = VoxeetLogger(this::class.java.simpleName)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeSplashScreen()


        setContent {
            AudioModerationSampleTheme {
                Content()
            }
        }
    }
}
@Preview
@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
fun Content() {

    val snackState = remember { SnackbarHostState() }

    // A surface container using the 'background' color from the theme
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        val viewModel: ConferenceViewModel = hiltViewModel()
        val isLightTheme = MaterialTheme.colors.isLight
        val backgroundGradientColors = listOf(
            colorResource(R.color.primary),
            colorResource(R.color.primary_light)
        )

        val surfaceColor = MaterialTheme.colors.surface
        val systemUiController = rememberSystemUiController()
        SideEffect {
            systemUiController.setStatusBarColor(Color.Transparent, darkIcons = false)
            systemUiController.setNavigationBarColor(surfaceColor, darkIcons = isLightTheme)
        }

        val lifecycleOwner = LocalLifecycleOwner.current
        val coroutineScope = rememberCoroutineScope()

        DisposableEffect(Unit) {
            val job = coroutineScope.launch {
                lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.actions.collect { action ->
                        when (action) {
                            is ConferenceUiAction.ShowErrorSnackbar -> snackState.showSnackbar(action.message)
                        }
                    }
                }
            }
            onDispose { job.cancel() }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsWithImePadding()
                .background(brush = Brush.horizontalGradient(colors = backgroundGradientColors))
                .statusBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.ic_dolbyio_white_logo),
                contentDescription = "dolby.io logo",
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f, true)
                    .padding(horizontal = 64.dp)
            )
            Form(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                    .background(MaterialTheme.colors.surface)
                    .padding(32.dp)
                    .weight(3f, true)
            ) {
                val uiState by viewModel.states.collectAsStateWithLifecycle()
                ConferenceContent( uiState = uiState, viewModel = viewModel, snackState = snackState)
            }
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) { SnackbarHost(hostState = snackState, Modifier.align(Alignment.BottomCenter)) }
}

@Composable
private fun Form(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) = Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Top
) {
    content()
}
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ColumnScope.ConferenceContent(
    uiState: ConferenceUiState,
    viewModel: ConferenceViewModel,
    snackState: SnackbarHostState
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value } || permissions.isEmpty()) {
            viewModel.joinConference()
        } else {
            val message = context.getString(R.string.common_missing_permissions, permissions.keys.joinToString(" "))
            coroutineScope.launch {
                snackState.showSnackbar(message)
            }
            Log.e("Conference", "PERMISSION DENIED: " + message)
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    Spacer(modifier = Modifier.height(16.dp))
    Text(stringResource(
        id = R.string.welcome),
        style = MaterialTheme.typography.h5,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(32.dp))
    OutlinedTextField(
        value = uiState.username,
        enabled = !uiState.isConferenceJoined,
        onValueChange = viewModel::usernameChanged,
        label = { Text(text = stringResource(id = R.string.hint_username)) },
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })

    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = uiState.conferenceName,
        enabled = !uiState.isConferenceJoined,
        onValueChange = viewModel::conferenceNameChanged,
        label =  { Text(stringResource(id = R.string.hint_conference_name)) },
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            keyboardController?.hide()
            focusManager.clearFocus()
        })
    )
    Spacer(modifier = Modifier.height(16.dp))
    ProgressButton(
        modifier = Modifier.width(TextFieldDefaults.MinWidth),
        action = {
            if (!uiState.isConferenceJoined) {
                val perm = listOfNotNull(
                    Manifest.permission.RECORD_AUDIO,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_CONNECT else null
                ).filter {
                    RuntimePermissions.check(context, it) != PackageManager.PERMISSION_GRANTED
                }
                if (perm.isNotEmpty()) {
                    launcher.launch(perm.toTypedArray())
                } else {
                    viewModel.joinConference()
                }
            } else {
                viewModel.leaveConference()
            }
        },
        isLoading = uiState.isConferenceJoining
    ) {
        if (uiState.isConferenceJoined) {
            Text(text = stringResource(id = R.string.leave_button))
        } else {
            Text(text = stringResource(id = R.string.join_button))
        }
    }
    Button(
        modifier = Modifier.width(TextFieldDefaults.MinWidth),
        enabled = uiState.isConferenceJoined && !uiState.moderationInProgress,
        onClick = {
            viewModel.moderateAudio()
        }
    ) {
        if (uiState.moderationInProgress)
            CircularProgressIndicator(modifier = Modifier.size(25.dp))
        else
            Text(text = stringResource(id = R.string.moderation_button))
    }

    ModerationResults(uiState = uiState)
}


@Composable
private fun ModerationResults(
    uiState: ConferenceUiState
) {

    if(uiState.moderationStatus != null) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            stringResource(id = R.string.moderation_result),
            style = MaterialTheme.typography.h5
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                stringResource(id = R.string.moderation_result_code),
                textAlign = TextAlign.Start
            )
            Text(
                text = uiState.moderationStatus.code ?: stringResource(id = R.string.unknown_code),
                textAlign = TextAlign.End
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(id = R.string.moderation_result_message),
                textAlign = TextAlign.Start
            )
            Text(
                text = uiState.moderationStatus.message.toString(),
                textAlign = TextAlign.End
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(id = R.string.moderation_result_language),
                textAlign = TextAlign.Start
            )
            Text(
                text = uiState.moderationLanguage.toString(),
                textAlign = TextAlign.End
            )
        }

        val text = if(uiState.moderationTranscribedText.isNullOrEmpty())
            stringResource(id = R.string.transcribed_text_no_text)
        else
            uiState.moderationTranscribedText

        TextField(
            value = text,
            onValueChange = { },
            label = { Text(stringResource(id = R.string.transcribed_text)) },
            maxLines = 2,
            modifier = Modifier.padding(20.dp)
        )
    }
    Classifications(uiState = uiState)
}
@Composable
private fun Classifications(
    uiState: ConferenceUiState
) {
    uiState.moderationClassifications?.forEach {

        TextField(
            value = it.text ?: stringResource(id = R.string.transcribed_result),
            onValueChange = { },
            readOnly = true,
            label = { Text(stringResource(id = R.string.transcribed_result)) },
            maxLines = 2,
            //textStyle = TextStyle(color = Color.Blue, fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(20.dp)
        )

        it.classes?.forEach {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = it.klass ?: stringResource(id = R.string.unknown_class),
                    textAlign = TextAlign.End
                )
                Text(
                    text = it.score.toString(),
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

@Composable
private fun ProgressButton(
    modifier: Modifier = Modifier,
    action: () -> Unit,
    isLoading: Boolean,
    content: @Composable () -> Unit
) {
    Button(
        modifier = modifier,
        enabled = !isLoading,
        onClick = action
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(25.dp))
        } else {
            content()
        }
    }
}

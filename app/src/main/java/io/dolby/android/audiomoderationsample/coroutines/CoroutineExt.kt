package io.dolby.android.audiomoderationsample.coroutines

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun CoroutineScope.launch(
    onError: (Throwable) -> Unit = { throw IllegalStateException("Error block not implemented", it) },
    onSuccess: suspend CoroutineScope.() -> Unit
) = launch(CoroutineExceptionHandler { _, exception -> onError(exception) }) { onSuccess() }

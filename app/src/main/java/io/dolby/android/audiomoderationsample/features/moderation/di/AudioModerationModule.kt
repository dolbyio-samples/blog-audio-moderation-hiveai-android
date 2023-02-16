package io.dolby.android.audiomoderationsample.features.moderation.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.dolby.android.audiomoderationsample.features.moderation.AudioModeration
import io.dolby.android.audiomoderationsample.features.moderation.AudioModerationApi
import io.dolby.android.audiomoderationsample.features.moderation.AudioModerationImpl
import io.dolby.comms.sdk.testapp.networking.ApiProvider
import io.dolby.comms.sdk.testapp.networking.provide
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface AudioModerationModule {

    @Binds
    fun bindAuthorizationRepository(impl: AudioModerationImpl): AudioModeration

    companion object {
        @Provides
        @Singleton
        fun providesTokenApi(
            apiProvider: ApiProvider
        ): AudioModerationApi = apiProvider.provide("https://api.thehive.ai")
    }
}

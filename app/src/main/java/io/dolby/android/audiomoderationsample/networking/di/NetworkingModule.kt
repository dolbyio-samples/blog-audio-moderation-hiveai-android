package io.dolby.comms.sdk.testapp.networking.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.dolby.comms.sdk.testapp.networking.ApiProvider
import io.dolby.comms.sdk.testapp.networking.RetrofitApiProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface NetworkingModule {

    @Binds
    @Singleton
    fun bindApiProvider(impl: RetrofitApiProvider): ApiProvider
}

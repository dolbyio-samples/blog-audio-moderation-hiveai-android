package io.dolby.comms.sdk.testapp.networking

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.zacsweers.moshix.sealed.reflect.MoshiSealedJsonAdapterFactory
import io.dolby.android.audiomoderationsample.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BODY
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.Date
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject

internal class RetrofitApiProvider(private val debug: Boolean) : ApiProvider {

    companion object {
        private const val DEFAULT_TIMEOUT = 60L
    }

    private val moshi by lazy {
        Moshi.Builder()
            .add(MoshiSealedJsonAdapterFactory())
            .add(KotlinJsonAdapterFactory())
            .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
            .build()
    }

    private val httpClient by lazy {
        OkHttpClient().newBuilder()
            .readTimeout(DEFAULT_TIMEOUT, SECONDS)
            .connectTimeout(DEFAULT_TIMEOUT, SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .apply { if (debug) addInterceptor(HttpLoggingInterceptor().setLevel(BODY)) }
            .build()
    }

    @Inject
    constructor() : this(BuildConfig.DEBUG)

    override fun <T> provide(url: String, type: Class<T>): T {
        return Retrofit.Builder()
            .client(httpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .baseUrl(url)
            .build()
            .create(type)
    }
}

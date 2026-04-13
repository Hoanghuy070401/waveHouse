package com.wavehouse.core.di

import com.google.firebase.auth.FirebaseAuth
import com.wavehouse.BuildConfig
import com.wavehouse.core.network.AuthInterceptor
import com.wavehouse.core.network.LoggingInterceptor
import com.wavehouse.core.network.TokenProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideTokenProvider(firebaseAuth: FirebaseAuth): TokenProvider = object : TokenProvider {
        override fun getToken(): String? =
            // Trả về cached token (không block main thread)
            firebaseAuth.currentUser?.let { user ->
                // In production: dùng runBlocking hoặc cache token sau refresh
                null // Token được refresh async, interceptor sẽ handle
            }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        loggingInterceptor: LoggingInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .apply {
            if (BuildConfig.DEBUG) addInterceptor(loggingInterceptor)
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}

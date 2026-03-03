package com.driverai.b2c.di

import com.driverai.b2c.BuildConfig
import com.driverai.b2c.data.network.ApiService
import com.driverai.b2c.data.network.AuthInterceptor
import com.driverai.b2c.data.network.ScanApiService
import com.driverai.b2c.data.network.SubscriptionApiService
import com.driverai.b2c.data.network.VehicleApiService
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            )
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        // LOWER_CASE_WITH_UNDERSCORES: camelCase ↔ snake_case automatically
        // vehicleId → vehicle_id, overallRisk → overall_risk, etc.
        val gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL + "/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)

    @Provides @Singleton
    fun provideVehicleApiService(retrofit: Retrofit): VehicleApiService =
        retrofit.create(VehicleApiService::class.java)

    @Provides @Singleton
    fun provideScanApiService(retrofit: Retrofit): ScanApiService =
        retrofit.create(ScanApiService::class.java)

    @Provides @Singleton
    fun provideSubscriptionApiService(retrofit: Retrofit): SubscriptionApiService =
        retrofit.create(SubscriptionApiService::class.java)
}

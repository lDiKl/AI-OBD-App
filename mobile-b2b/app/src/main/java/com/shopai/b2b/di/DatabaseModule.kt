package com.shopai.b2b.di

import android.content.Context
import androidx.room.Room
import com.shopai.b2b.data.local.AppDatabase
import com.shopai.b2b.data.local.DiagnosticCaseDao
import com.shopai.b2b.data.local.ShopProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "shopai_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideDiagnosticCaseDao(db: AppDatabase): DiagnosticCaseDao = db.diagnosticCaseDao()

    @Provides
    fun provideShopProfileDao(db: AppDatabase): ShopProfileDao = db.shopProfileDao()
}

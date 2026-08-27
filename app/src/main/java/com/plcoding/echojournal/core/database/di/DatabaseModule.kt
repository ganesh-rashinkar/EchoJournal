package com.plcoding.echojournal.core.database.di

import androidx.room.Room
import com.plcoding.echojournal.core.database.EchoDatabase
import com.plcoding.echojournal.core.database.echo.EchoDao
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val databaseModule = module {
    single<EchoDatabase> {
        Room.databaseBuilder(
            androidApplication(),
            EchoDatabase::class.java,
            "echos.db"
        ).build()
    }
    single<EchoDao> {
        get<EchoDatabase>().echoDao
    }
}
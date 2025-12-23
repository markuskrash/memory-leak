package com.memoryleak.server.di

import com.memoryleak.server.service.AuthService
import org.koin.dsl.module

val appModule = module {
    single { AuthService() }
}
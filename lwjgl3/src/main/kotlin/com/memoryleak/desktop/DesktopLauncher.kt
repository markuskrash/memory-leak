package com.memoryleak.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.memoryleak.core.MemoryLeakGame

fun main() {
    println("--- LAUNCHING CLIENT ---")
    val config = Lwjgl3ApplicationConfiguration()
    config.setForegroundFPS(60)
    config.setTitle("Memory Leak")
    config.setWindowedMode(800, 600)
    Lwjgl3Application(MemoryLeakGame(), config)
}

package com.fuzzyfilesearch.settings

interface SettingsComponent {
    fun initialize  (): Unit
    fun modified    (): Boolean
    fun store       (): Unit
}
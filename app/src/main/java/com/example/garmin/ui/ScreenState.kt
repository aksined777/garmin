package com.example.garmin.ui

sealed class ScreenState<out T : Any?> {
    object Loading : ScreenState<Nothing>()
    object None : ScreenState<Nothing>()
    data class Content<out T : Any>(val data: T) : ScreenState<T>()
    data class Error(val errorMessage: String) : ScreenState<String>()
}
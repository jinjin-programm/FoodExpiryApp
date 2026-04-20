package com.example.foodexpiryapp.util

import android.util.Log

object AppLog {

    private const val MAX_TAG_LENGTH = 23

    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(safeTag(tag), message)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(safeTag(tag), message, throwable)
        } else {
            Log.e(safeTag(tag), message)
        }
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(safeTag(tag), message, throwable)
        } else {
            Log.w(safeTag(tag), message)
        }
    }

    fun i(tag: String, message: String) {
        Log.i(safeTag(tag), message)
    }

    private fun safeTag(tag: String): String {
        return if (tag.length > MAX_TAG_LENGTH) tag.substring(0, MAX_TAG_LENGTH) else tag
    }

    private object BuildConfig {
        val DEBUG = com.example.foodexpiryapp.BuildConfig.DEBUG
    }
}

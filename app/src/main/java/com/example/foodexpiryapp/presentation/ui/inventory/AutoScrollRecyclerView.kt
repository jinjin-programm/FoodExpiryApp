package com.example.foodexpiryapp.presentation.ui.inventory

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AutoScrollRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private val scrollHandler = Handler(Looper.getMainLooper())
    private val resumeHandler = Handler(Looper.getMainLooper())

    private var isAutoScrolling = false
    private var isUserTouching = false

    private var scrollAccumulator = 0f

    private val autoScrollRunnable = object : Runnable {
        override fun run() {
            if (isAutoScrolling && !isUserTouching) {
                val adapter = adapter
                if (adapter == null || adapter.itemCount == 0) {
                    stopAutoScroll()
                    return
                }
                scrollAccumulator += 1f
                val pixels = scrollAccumulator.toInt()
                if (pixels > 0) {
                    scrollBy(pixels, 0)
                    scrollAccumulator -= pixels
                }
            }
            scrollHandler.postDelayed(this, 16)
        }
    }

    private val resumeRunnable = Runnable {
        if (!isUserTouching) {
            startAutoScroll()
        }
    }

    fun startAutoScroll() {
        if (!isAutoScrolling) {
            isAutoScrolling = true
            scrollHandler.post(autoScrollRunnable)
        }
    }

    fun stopAutoScroll() {
        isAutoScrolling = false
        scrollHandler.removeCallbacks(autoScrollRunnable)
    }

    fun cancelResumeTimer() {
        resumeHandler.removeCallbacks(resumeRunnable)
    }

    private fun resetResumeTimer() {
        resumeHandler.removeCallbacks(resumeRunnable)
        resumeHandler.postDelayed(resumeRunnable, 3000)
    }

    fun scrollToMiddlePosition() {
        val adapter = adapter ?: return
        val count = adapter.itemCount
        if (count == 0) return
        val layoutManager = layoutManager as? LinearLayoutManager ?: return
        val middle = count / 2
        layoutManager.scrollToPositionWithOffset(middle, 0)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                isUserTouching = true
                stopAutoScroll()
                resetResumeTimer()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isUserTouching = false
                resetResumeTimer()
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAutoScroll()
        cancelResumeTimer()
    }
}

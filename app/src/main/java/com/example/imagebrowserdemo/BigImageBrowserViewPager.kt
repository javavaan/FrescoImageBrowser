package com.example.imagebrowserdemo

import android.content.Context
import android.graphics.drawable.Animatable
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent

/**
 * 自定义ViewPager
 * 解决图片放大后滑动图片滑动到边缘后手势传递到ViewPager会引起ViewPager跳动的问题
 */
class BigImageBrowserViewPager : ViewPager {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    // 强制viewpager拦截触摸事件的标志 true表示强制拦截 false表示默认处理
    var onInterceptTouchEventFlag = false
    /**
     * 得到触摸手指的id
     */
    fun getActivePointerId(): Int {
        try {
            val activePointerIdField = this.javaClass.superclass.getDeclaredField("mActivePointerId")
            activePointerIdField.isAccessible = true

            return activePointerIdField.getInt(this)
        } catch (error: Exception) {
            return -1
        }
    }

    /**
     * 重要： 正常切换viewpager的item时，mLastMotionX这个值和手指按下的rawx值相等
     */
    fun setLastMotionX(x: Float) {
        try {
            val lastMotionXField = this.javaClass.superclass.getDeclaredField("mLastMotionX")
            val initialMotionXField = this.javaClass.superclass.getDeclaredField("mInitialMotionX")

            lastMotionXField.isAccessible = true
            lastMotionXField.set(this, x)

            initialMotionXField.isAccessible = true
            initialMotionXField.set(this, x)
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }

    fun getLastMotionX():Float{
        val lastMotionXField = this.javaClass.superclass.getDeclaredField("mLastMotionX")
        val initialMotionXField = this.javaClass.superclass.getDeclaredField("mInitialMotionX")

        lastMotionXField.isAccessible = true
        return lastMotionXField.get(this) as Float
    }

    fun setLastMotionY(y: Float) {
        try {
            val lastMotionYField = this.javaClass.superclass.getDeclaredField("mLastMotionY")
            val initialMotionYField = this.javaClass.superclass.getDeclaredField("mInitialMotionY")

            lastMotionYField.isAccessible = true
            lastMotionYField.set(this, y)

            initialMotionYField.isAccessible = true
            initialMotionYField.set(this, y)
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }



    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (null != ev && ev.pointerCount > 1) {
            return false
        }

        if (onInterceptTouchEventFlag) {
            return true
        }

        return try {
            super.onInterceptTouchEvent(ev)
        } catch (error: IllegalArgumentException) {
            false
        }
    }

}
package com.github.vatbub.scoreboard

import android.view.View
import android.view.ViewTreeObserver

object ViewUtil {
    fun runJustBeforeBeingDrawn(view: View, functionToExecute: ((View) -> Unit)) {
        val preDrawListener = object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                view.viewTreeObserver.removeOnPreDrawListener(this)
                functionToExecute.invoke(view)
                return true
            }
        }
        view.viewTreeObserver.addOnPreDrawListener(preDrawListener)
    }
}
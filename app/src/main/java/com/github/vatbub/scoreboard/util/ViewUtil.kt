/*
 * Copyright (c) 2019 Frederik Kammel <vatbub123@googlemail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.vatbub.scoreboard.util

import android.app.Activity
import android.support.annotation.IdRes
import android.view.View
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnGlobalLayoutListener


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

    fun <T : View> runOnGlobalLayoutChange(activity: Activity, @IdRes viewId: Int, removeListenerPolicy: RemoveListenerPolicy, functionToExecute: ((T) -> Unit)) {
        val viewTreeObserver = activity.window.decorView.viewTreeObserver
        viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val view = activity.findViewById<T>(viewId)
                if (view != null) {
                    functionToExecute(view)
                    if (removeListenerPolicy == ViewUtil.RemoveListenerPolicy.OnlyIfViewExists)
                        viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
                if (removeListenerPolicy == ViewUtil.RemoveListenerPolicy.Always)
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    enum class RemoveListenerPolicy {
        OnlyIfViewExists, Always, Never
    }
}
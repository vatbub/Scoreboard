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

import kotlin.reflect.KProperty

class ResettableLazyProperty<out T>(private val initializer: (() -> T)) {
    private var _value: T? = null
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (thisRef == null)
            return internalGetValue()
        else {
            synchronized(thisRef) {
                return internalGetValue()
            }
        }
    }

    val value: T
        get() = internalGetValue()

    private fun internalGetValue(): T {
        if (_value == null) {
            _value = initializer.invoke()
        }
        return _value!!
    }

    fun resetValue() {
        _value = null
    }
}
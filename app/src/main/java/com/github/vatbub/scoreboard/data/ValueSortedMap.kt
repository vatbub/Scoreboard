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

package com.github.vatbub.scoreboard.data

import java.util.*

class ValueSortedMap<K, V> private constructor(initialMap: MutableMap<K, V>?, comparator: Comparator<in V>?, invertOrder: Boolean) : MutableMap<K, V> {
    private val map: MutableMap<K, V>
    private val lookupMap = HashMap<K, V>(3)

    constructor(invertOrder: Boolean) : this(null, null, invertOrder)

    constructor(comparator: Comparator<in V>) : this(null, comparator, false)

    constructor(m: Map<out K, V>) : this(TreeMap(m), null, false)

    constructor(m: SortedMap<out K, out V>) : this(TreeMap<K, V>(m), null, false)

    init {
        val finalComparator = if (comparator != null)
            KeyByValueComparator(comparator)
        else
            KeyByValueComparator(invertOrder)

        this.map = initialMap ?: TreeMap(finalComparator)
    }

    override val size = map.size

    override fun isEmpty() = map.isEmpty()

    override fun containsKey(key: K) = map.containsKey(key)

    override fun containsValue(value: V) = map.containsValue(value)

    override operator fun get(key: K) = map[key]

    override fun put(key: K, value: V): V? {
        if (lookupMap.containsKey(key))
            map.remove(key)
        lookupMap[key] = value
        return map.put(key, value)
    }

    override fun remove(key: K): V? {
        lookupMap.remove(key)
        return map.remove(key)
    }

    override fun putAll(from: Map<out K, V>) {
        lookupMap.putAll(from)
        for (k in from.keys)
            map[k] = from[k]!!
    }

    override fun clear() {
        lookupMap.clear()
        map.clear()
    }

    override val keys = map.keys

    override val entries = map.entries

    override val values = map.values

    override fun equals(other: Any?) = map == other

    override fun hashCode() = map.hashCode()

    override fun toString() = map.toString()


    inner class KeyByValueComparator constructor(private val valueComparator: Comparator<in V>) : Comparator<K> {

        constructor(invertOrder: Boolean = false) : this(Comparator { o1: V, o2: V ->
            @Suppress("UNCHECKED_CAST")
            o1 as Comparable<V>
            if (invertOrder)
                -o1.compareTo(o2)
            else
                o1.compareTo(o2)
        })

        override fun compare(a: K, b: K): Int {
            val va = lookupMap[a]
            val vb = lookupMap[b]

            var valueResult = 0
            if (va != null && vb != null) {
                valueResult = valueComparator.compare(va, vb)
            }
            if (valueResult != 0)
                return -valueResult
            if (a is Comparable<*>) {
                @Suppress("UNCHECKED_CAST")
                a as Comparable<K>

                return -a.compareTo(b)
            }
            return -1
        }
    }
}
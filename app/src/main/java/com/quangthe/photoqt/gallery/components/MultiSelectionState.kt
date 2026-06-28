/*
 *   Copyright 2020–2026 Leon Latsch
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.quangthe.photoqt.gallery.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun rememberMultiSelectionState(items: List<String>): MultiSelectionState {
    val state = remember { MultiSelectionState(items) }
    state.updateItems(items)
    return state
}

class MultiSelectionState(
    allItems: List<String>,
) {

    var allItems by mutableStateOf(allItems)
        private set

    fun updateItems(items: List<String>) {
        allItems = items
    }

    var isActive = mutableStateOf(false)
    var selectedItems = mutableStateOf(emptyList<String>())
    val showMore = mutableStateOf(false)
    var rectangleSelectEnabled = mutableStateOf(false)

    fun selectAll() {
        isActive.value = true
        selectedItems.value = allItems

    }
    fun cancelSelection() {
        isActive.value = false
        selectedItems.value = emptyList()
        rectangleSelectEnabled.value = false
    }
    fun selectItem(uuid: String) {
        isActive.value = true
        selectedItems.value += uuid
    }
    fun deselectItem(uuid: String) {
        if (selectedItems.value.size == 1) {
            isActive.value = false
        }

        selectedItems.value -= uuid
    }

    fun replaceSelection(uuids: Set<String>) {
        if (uuids.isEmpty()) {
            cancelSelection()
            return
        }
        isActive.value = true
        selectedItems.value = uuids.toList()
    }

    fun showMore() {
        showMore.value = true
    }

    fun dismissMore() {
        showMore.value = false
    }
}

/**
 * Designed and developed by Aidan Follestad (@afollestad)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.afollestad.materialdialogs.files

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.utils.MDUtil.resolveColor

class PathDividerDecoration(context: Context) : RecyclerView.ItemDecoration() {
    private val divider = ">"
    private val textPaint = Paint().apply {
        color = resolveColor(context, attr = android.R.attr.textColorSecondary)
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        for (i in 0 until parent.childCount - 1) { // 最后一个节点不加分隔符
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams
            val x = child.right + params.rightMargin
            val y = child.top + (child.height / 2f) + 10 // 垂直居中
            c.drawText(divider, x.toFloat(), y, textPaint)
        }
    }
}

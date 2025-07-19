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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PathAdapter(
  val pathComponents: MutableList<String>,
  private val onNodeClick: (PathAdapter, Int) -> Unit,
  val scollCallback: (Int) -> Unit
) : RecyclerView.Adapter<PathAdapter.ViewHolder>() {
  init {
    setHasStableIds(true)
  }

  class ViewHolder(
    itemView: View
  ) : RecyclerView.ViewHolder(itemView) {
    val tvNode: TextView = itemView.findViewById(R.id.tv_path_node)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): ViewHolder {
    val view =
      LayoutInflater
        .from(parent.context)
        .inflate(R.layout.md_file_chooser_tree_item, parent, false)
    return ViewHolder(view)
  }

  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int
  ) {
    println("执行onBindViewHolder")
    val isLastNode = position == pathComponents.size - 1
    holder.tvNode.text = pathComponents[position]
    // 样式处理
//        holder.tvNode.setTextColor(
//            if (isLastNode) Color.GRAY else Color.BLUE
//        )
    if (!isLastNode) {
      val attrs = intArrayOf(android.R.attr.textColorSecondary)
      val typedArray = holder.itemView.context.obtainStyledAttributes(attrs)
      val textColor = typedArray.getColorStateList(0)
      holder.tvNode.setTextColor(textColor)
      typedArray.recycle()
    } else {
      val attrs = intArrayOf(android.R.attr.textColorPrimary)
      val typedArray = holder.itemView.context.obtainStyledAttributes(attrs)
      val textColor = typedArray.getColorStateList(0)
      holder.tvNode.setTextColor(textColor)
      typedArray.recycle()
    }
    // 点击事件
    holder.tvNode.setOnClickListener {
      if (!isLastNode) {
        onNodeClick(this, position)
      }
    }
  }

  override fun getItemCount() = pathComponents.size
}

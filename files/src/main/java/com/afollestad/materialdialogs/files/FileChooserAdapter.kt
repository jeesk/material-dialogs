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

import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton.POSITIVE
import com.afollestad.materialdialogs.actions.hasActionButtons
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.files.util.betterParent
import com.afollestad.materialdialogs.files.util.friendlyName
import com.afollestad.materialdialogs.files.util.hasParent
import com.afollestad.materialdialogs.files.util.jumpOverEmulated
import com.afollestad.materialdialogs.files.util.queryEmulatedCard
import com.afollestad.materialdialogs.files.util.queryStorage
import com.afollestad.materialdialogs.files.util.setVisible
import com.afollestad.materialdialogs.list.getItemSelector
import com.afollestad.materialdialogs.utils.MDUtil.isColorDark
import com.afollestad.materialdialogs.utils.MDUtil.maybeSetTextColor
import com.afollestad.materialdialogs.utils.MDUtil.resolveColor
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class FileChooserViewHolder(
  itemView: View,
  private val adapter: FileChooserAdapter
) : RecyclerView.ViewHolder(itemView), OnClickListener {

  init {
    itemView.setOnClickListener(this)
  }

  val iconView: ImageView = itemView.findViewById(R.id.icon)
  val nameView: TextView = itemView.findViewById(R.id.name)

  override fun onClick(view: View) = adapter.itemClicked(adapterPosition)
}

/** @author Aidan Follestad (afollestad */
internal class FileChooserAdapter(
  private val dialog: MaterialDialog,
  initialFolder: File,
  private val waitForPositiveButton: Boolean,
  private val emptyView: TextView,
  private val onlyFolders: Boolean,
  private val filter: FileFilter,
  private val allowFolderCreation: Boolean,
  @StringRes private val folderCreationLabel: Int?,
  private val callback: FileCallback
) : RecyclerView.Adapter<FileChooserViewHolder>() {

  var selectedFile: File? = null

  private var currentFolder = initialFolder
  private var listingJob: Job? = null
  private var contents: List<File>? = null

  private val isLightTheme =
    resolveColor(dialog.windowContext, attr = android.R.attr.textColorPrimary).isColorDark()

  init {
    dialog.onDismiss { listingJob?.cancel() }
    switchDirectory(initialFolder)
  }

  fun itemClicked(index: Int) {
    val parent = currentFolder.betterParent(dialog.context, allowFolderCreation, filter)
/*
    if (parent != null && index == goUpIndex()) {
      // go up
      switchDirectory(parent)
      return
    }
*/
      if (currentFolder.canWrite() && allowFolderCreation && index == newFolderIndex()) {
      // New folder
      dialog.showNewFolderCreator(
        parent = currentFolder,
        folderCreationLabel = folderCreationLabel
      ) {
        // Refresh view
        switchDirectory(currentFolder)
      }
      return
    }

    val actualIndex = actualIndex(index)
    val selected = contents!![actualIndex].jumpOverEmulated(dialog.context)

    if (selected.isDirectory) {
      switchDirectory(selected)
    } else {
      val previousSelectedIndex = getSelectedIndex()
      this.selectedFile = selected
      val actualWaitForPositive = waitForPositiveButton && dialog.hasActionButtons()

      if (actualWaitForPositive) {
        dialog.setActionButtonEnabled(POSITIVE, true)
        notifyItemChanged(index)
        notifyItemChanged(previousSelectedIndex)
      } else {
        callback?.invoke(dialog, selected)
        dialog.dismiss()
      }
    }
  }

  fun switchDirectory(directory: File) {
    val rv = dialog.findViewById<RecyclerView>(R.id.directory_tree)
    if (rv != null && rv.adapter != null) {
      val adpter = rv.adapter as PathAdapter
      val filePaths = directory.canonicalPath.split("/").filter { obj -> obj != "" }
      adpter.pathComponents.clear()
      adpter.pathComponents.addAll(filePaths)
      adpter.notifyDataSetChanged()
      adpter.scollCallback(adpter.pathComponents.size - 1)
    }
    listingJob?.cancel()
    listingJob = GlobalScope.launch(Main) {
      if (onlyFolders) {
        selectedFile = directory
        dialog.setActionButtonEnabled(POSITIVE, true)
      }
      currentFolder = directory
      dialog.title(text = directory.friendlyName(dialog.context))
      val result = withContext(IO) {
        val rawContents = directory.listFiles() ?: emptyArray()
        if (onlyFolders) {
          rawContents
            .filter { it.isDirectory && filter?.invoke(it) ?: true }
            .sortedBy { it.name.toLowerCase(Locale.getDefault()) }
        } else {
          rawContents
            .filter { filter?.invoke(it) ?: true }
            .sortedWith(compareBy({ !it.isDirectory }, {
              it.nameWithoutExtension.toLowerCase(Locale.getDefault())
            }))
        }
      }
      if (result.size != 0) {
        contents = result.apply {
          emptyView.setVisible(isEmpty())
        }
      } else {
          val paths = directory.canonicalPath.split("/").filter { obj -> obj != "" }
          if (paths.size == 1) {
            val queryNotCurrentUserSpace = queryStorage(dialog.context)
            if (queryNotCurrentUserSpace.size > 0) {
              val initList = mutableListOf<File>()
              val baseDir = Environment.getExternalStorageDirectory()
              initList.add(baseDir.parentFile)
              for (s in queryNotCurrentUserSpace) {
                initList.add(File(s))
              }
              contents = initList.apply {
                emptyView.setVisible(isEmpty())
              }
            } else {
              contents = emptyList()
              emptyView.setVisible(true)
            }
          } else if (paths.size == 2) {
              val baseDir = Environment.getExternalStorageDirectory()
              val initList = mutableListOf<File>()
              initList.add(File(baseDir.canonicalPath))
              val queryNotCurrentUserSpace = queryEmulatedCard(dialog.context)
              if (queryNotCurrentUserSpace.size > 0) {
                for (s in queryNotCurrentUserSpace) {
                  initList.add(File(s))
                }
              }
              contents = initList.apply {
                emptyView.setVisible(isEmpty())
              }
            } else {
              contents = result.apply {
                emptyView.setVisible(isEmpty())
              }
            }
      }
      notifyDataSetChanged()
    }
  }

  override fun getItemCount(): Int {
    var count = contents?.size ?: 0
/*
    if (currentFolder.hasParent(dialog.context, allowFolderCreation, filter)) {
      count += 1
    }
*/
    if (allowFolderCreation && currentFolder.canWrite()) {
      count += 1
    }
    return count
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): FileChooserViewHolder {
    val view = LayoutInflater.from(parent.context)
      .inflate(R.layout.md_file_chooser_item, parent, false)
    view.background = dialog.getItemSelector()

    val viewHolder = FileChooserViewHolder(view, this)
    viewHolder.nameView.maybeSetTextColor(dialog.windowContext, R.attr.md_color_content)
    return viewHolder
  }

  override fun onBindViewHolder(
    holder: FileChooserViewHolder,
    position: Int
  ) {
/*
    val currentParent = currentFolder.betterParent(dialog.context, allowFolderCreation, filter)
    if (currentParent != null && position == goUpIndex()) {
      // Go up
      holder.iconView.setImageResource(
        if (isLightTheme) R.drawable.icon_return_dark
        else R.drawable.icon_return_light
      )
      holder.nameView.text = currentParent.name
      holder.itemView.isActivated = false
      holder.itemView.visibility = View.GONE
      return
    }
*/

    if (allowFolderCreation && currentFolder.canWrite() && position == newFolderIndex()) {
      // New folder
      holder.iconView.setImageResource(
        if (isLightTheme) R.drawable.icon_new_folder_dark
        else R.drawable.icon_new_folder_light
      )
      holder.nameView.text = dialog.windowContext.getString(
        folderCreationLabel ?: R.string.files_new_folder
      )
      holder.itemView.isActivated = false
      return
    }
    if (contents == null) {
      return
    }
    val actualIndex = actualIndex(position)
    try {
      val item = contents!![actualIndex]
      holder.iconView.setImageResource(item.iconRes())
      holder.nameView.text = item.name
      holder.itemView.isActivated = selectedFile?.absolutePath == item.absolutePath ?: false
    } catch (e: Exception) {
      println(e)
    }
  }

  private fun goUpIndex() =
    if (currentFolder.hasParent(dialog.context, allowFolderCreation, filter)) 0 else -1

  private fun newFolderIndex() =
    if (currentFolder.hasParent(dialog.context, allowFolderCreation, filter)) 0 else -1

  private fun actualIndex(position: Int): Int {
    var actualIndex = position
    if (currentFolder.canWrite() && allowFolderCreation) {
      actualIndex -= 1
    }
    return actualIndex
  }

  private fun File.iconRes(): Int {
    return if (isLightTheme) {
      if (this.isDirectory) R.drawable.icon_folder_dark
      else R.drawable.icon_file_dark
    } else {
      if (this.isDirectory) R.drawable.icon_folder_light
      else R.drawable.icon_file_light
    }
  }

  private fun getSelectedIndex(): Int {
    if (selectedFile == null) return -1
    else if (contents?.isEmpty() == true) return -1
    val index = contents?.indexOfFirst { it.absolutePath == selectedFile?.absolutePath } ?: -1
    return if (index > -1 && currentFolder.hasParent(
        dialog.context,
        allowFolderCreation,
        filter
      )
    ) index + 1 else index
  }
}

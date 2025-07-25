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
@file:Suppress("unused")

package com.afollestad.materialdialogs.files

import android.annotation.SuppressLint
import android.content.Context
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.CheckResult
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton.POSITIVE
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.files.util.getExternalFilesDir
import com.afollestad.materialdialogs.internal.list.DialogRecyclerView
import com.afollestad.materialdialogs.utils.MDUtil.maybeSetTextColor
import java.io.File

/** Gets the selected folder for the current folder chooser dialog. */
@CheckResult
fun MaterialDialog.selectedFolder(): File? {
  val list: DialogRecyclerView = getCustomView().findViewById(R.id.list)
  return (list.adapter as? FileChooserAdapter)?.selectedFile
}

/**
 * Shows a dialog that lets the user select a local folder.
 *
 * @param initialDirectory The directory that is listed initially, defaults to external storage.
 * @param filter A filter to apply when listing folders, defaults to only show non-hidden folders.
 * @param waitForPositiveButton When true, the callback isn't invoked until the user selects a
 *    folder and taps on the positive action button. Defaults to true if the dialog has buttons.
 * @param emptyTextRes A string resource displayed on the empty view shown when a directory is
 *    empty. Defaults to "This folder's empty!".
 * @param selection A callback invoked when a folder is selected.
 */
@SuppressLint("CheckResult")
fun MaterialDialog.folderChooser(
  context: Context,
  initialDirectory: File? = context.getExternalFilesDir(),
  filter: FileFilter = null,
  waitForPositiveButton: Boolean = true,
  emptyTextRes: Int = R.string.files_default_empty_text,
  allowFolderCreation: Boolean = false,
  @StringRes folderCreationLabel: Int? = null,
  selection: FileCallback = null
): MaterialDialog {
  var actualFilter: FileFilter = filter

  if (allowFolderCreation) {
    if (filter == null) {
      actualFilter = { !it.isHidden && it.canWrite() }
    }
  } else {
    if (filter == null) {
      actualFilter = { !it.isHidden && it.canRead() }
    }
  }

  check(initialDirectory != null) {
    "The initial directory is null."
  }

  customView(R.layout.md_file_chooser_base, noVerticalPadding = true)
  setActionButtonEnabled(POSITIVE, false)

  val customView = getCustomView()
  val list: DialogRecyclerView = customView.findViewById(R.id.list)
  val directoryTree: RecyclerView = customView.findViewById(R.id.directory_tree)
  val emptyText: TextView = customView.findViewById(R.id.empty_text)
  emptyText.setText(emptyTextRes)
  emptyText.maybeSetTextColor(windowContext, R.attr.md_color_content)

  list.attach(this)
  list.layoutManager = LinearLayoutManager(windowContext)
  directoryTree.layoutManager =
    LinearLayoutManager(windowContext, LinearLayoutManager.HORIZONTAL, false)
  // 添加分隔符
  directoryTree.addItemDecoration(PathDividerDecoration(windowContext))

  val adapter = FileChooserAdapter(
    dialog = this,
    initialFolder = initialDirectory,
    waitForPositiveButton = waitForPositiveButton,
    emptyView = emptyText,
    onlyFolders = true,
    filter = actualFilter,
    allowFolderCreation = allowFolderCreation,
    folderCreationLabel = folderCreationLabel,
    callback = selection
  )
  list.adapter = adapter
  val pathComponents = mutableListOf<String>()
  pathComponents.addAll(initialDirectory.canonicalPath.split("/").filter { obj -> obj != "" })
  fun updatePathDisplay() {
    val ad = PathAdapter(pathComponents, {
      ad, clickedPosition ->
      val subList =  try{
        ad.pathComponents.subList(0, clickedPosition + 1)
      }catch (e: Exception){
        Toast.makeText(context,"选择文件夹异常，请向开发者反馈问题。", Toast.LENGTH_LONG).show()
        emptyList<String>()
      }
      val filePath = subList.joinToString("/")
      adapter.switchDirectory(File(filePath))
      directoryTree.post {
        directoryTree.smoothScrollToPosition(clickedPosition)
      }
    }, {
      directoryTree.post {
        directoryTree.smoothScrollToPosition(it)
      }
    })
    directoryTree.adapter = ad
    ad.scollCallback(pathComponents.size - 1)
  }
  updatePathDisplay()

  if (waitForPositiveButton && selection != null) {
    positiveButton {
      val selectedFile = adapter.selectedFile
      if (selectedFile != null) {
        selection.invoke(this, selectedFile)
      }
    }
  }

  return this
}

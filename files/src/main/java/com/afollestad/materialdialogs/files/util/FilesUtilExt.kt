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
@file:Suppress("SpellCheckingInspection")

package com.afollestad.materialdialogs.files.util

import android.Manifest.permission
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.FileFilter
import java.io.File

internal fun File.hasParent(
  context: Context,
  writeable: Boolean,
  filter: FileFilter
) = betterParent(context, writeable, filter) != null

internal fun File.isExternalStorage(context: Context) =
  absolutePath == context.getExternalFilesDir()?.absolutePath

internal fun File.isRoot() = absolutePath == "/"

val fileUri: Uri by lazy { MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL) }

fun queryAlbumV25ForBothUser(
  ctx: Context,
  baseDir: String,
  needMediaTypeFilter: Boolean = true
): MutableMap<Long, String> {
  val maps = mutableMapOf<Long, String>()
  // 构建查询条件
  val selection =
    StringBuilder()
      .apply {
        if (needMediaTypeFilter) {
          //                append("${MediaStore.Files.FileColumns.MEDIA_TYPE} in (1,3)")
          if (baseDir != "") {
            append("${MediaStore.Files.FileColumns.DATA} not like '${baseDir + "/%"}' ")
          }
        }
      }
      .toString()
  val cursor =
    ctx.contentResolver.query(
      fileUri,
      arrayOf(MediaStore.Files.FileColumns.BUCKET_ID, MediaStore.Files.FileColumns.DATA),
      selection,
      null,
      null)
  cursor?.use {
    while (cursor.moveToNext()) {
      val albumIdIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.BUCKET_ID)
      val albumId = cursor.getLong(albumIdIndex)
      val existAlbumPath = maps.get(albumId)
      if (existAlbumPath != null) {
        continue
      }
      val dataIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
      val albumPath = cursor.getString(dataIndex)
      val path = albumPath.substring(0, albumPath.lastIndexOf("/"))
      maps[albumId] = path
    }
  }
  return maps
}

fun queryEmulatedCard(ctx: Context): List<String> {
  val baseDir = Environment.getExternalStorageDirectory()
  val rootDir = baseDir.parentFile.canonicalPath
  val maps = mutableMapOf<Long, String>()
  if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
    maps.putAll(queryAlbumV25ForBothUser(ctx, baseDir!!.canonicalPath))
    //        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
  } else {
    val bundle =
      Bundle().apply {
        putString(
          ContentResolver.QUERY_ARG_SQL_SELECTION,
          "(${MediaStore.Files.FileColumns.DATA} NOT LIKE '${baseDir!!.canonicalPath + "/%"}' ) ")
        if (Build.VERSION.SDK_INT >= 30) {
          putString(
            "android:query-arg-sql-group-by",
            "${MediaStore.Files.FileColumns.BUCKET_ID} ")
        }
        putString(
          ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
          "${MediaStore.Files.FileColumns.BUCKET_ID} ASC")
      }

    val cursor =
      ctx.contentResolver.query(
        fileUri,
        arrayOf(MediaStore.Files.FileColumns.BUCKET_ID, MediaStore.Files.FileColumns.DATA),
        bundle,
        null)
    cursor?.use {
      while (cursor.moveToNext()) {
        val albumIdIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.BUCKET_ID)
        val albumId = cursor.getLong(albumIdIndex)
        val existAlbumPath = maps.get(albumId)
        if (existAlbumPath != null) {
          continue
        }
        val dataIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
        val albumPath = cursor.getString(dataIndex)
        val path = albumPath.substring(0, albumPath.lastIndexOf("/"))
        maps[albumId] = path
      }
    }
  }
  val paths = mutableSetOf<String>()
  maps.entries.forEach {
    val path = it.value
    val toOkioPath = File(path).canonicalPath.split("/").filter { obj -> obj != "" }
    if (toOkioPath.size >= 3) {
      if (path.startsWith(rootDir)) {
        paths.add(toOkioPath.subList(0, 3).joinToString("/"))
      }
    }
  }
  return paths.toMutableList()
}

fun queryStorage(ctx: Context): List<String> {
  val baseDir = Environment.getExternalStorageDirectory()
  val rootDir = baseDir.parentFile.canonicalPath
  val maps = mutableMapOf<Long, String>()
  if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
    maps.putAll(queryAlbumV25ForBothUser(ctx, rootDir))
    //        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
  } else {
    val bundle =
      Bundle().apply {
        putString(
          ContentResolver.QUERY_ARG_SQL_SELECTION,
          "(${MediaStore.Files.FileColumns.DATA} NOT LIKE '${rootDir + "/%"}' ) ")
        if (Build.VERSION.SDK_INT >= 30) {
          putString(
            "android:query-arg-sql-group-by",
            "${MediaStore.Files.FileColumns.BUCKET_ID} ")
        }
        putString(
          ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
          "${MediaStore.Files.FileColumns.BUCKET_ID} ASC")
      }

    val cursor =
      ctx.contentResolver.query(
        fileUri,
        arrayOf(MediaStore.Files.FileColumns.BUCKET_ID, MediaStore.Files.FileColumns.DATA),
        bundle,
        null)
    cursor?.use {
      while (cursor.moveToNext()) {
        val albumIdIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.BUCKET_ID)
        val albumId = cursor.getLong(albumIdIndex)
        val existAlbumPath = maps.get(albumId)
        if (existAlbumPath != null) {
          continue
        }
        val dataIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
        val albumPath = cursor.getString(dataIndex)
        val path = albumPath.substring(0, albumPath.lastIndexOf("/"))
        maps[albumId] = path
      }
    }
  }
  val paths = mutableSetOf<String>()
  maps.entries.forEach {
    val path = it.value
    val toOkioPath = File(path).canonicalPath
      .split("/").filter { obj -> obj != "" }
    if (toOkioPath.size == 2) {
      paths.add(path)
    }
  }
  return paths.toMutableList()
}

internal fun File.betterParent(
  context: Context,
  writeable: Boolean,
  filter: FileFilter
): File? {
  val parentToUse = (if (isExternalStorage(context)) {
    // Emulated external storage's parent is empty so jump over it
    context.getExternalFilesDir()?.parentFile?.parentFile
  } else {
    parentFile
  }) ?: return null

  if ((writeable && !parentToUse.canWrite()) || !parentToUse.canRead()) {
    // We can't access this folder
    // 这里需要判断路径才可以处理
//    if(hasNotCurrentUserSpace(context)){
      return parentToUse
//    }
//    return null
  }

  val folderContent =
    parentToUse.listFiles()?.filter { filter?.invoke(it) ?: true } ?: emptyList()
  if (folderContent.isEmpty()) {
    // There is nothing in this folder most likely because we can't access files inside of it.
    // We don't want to get stuck here.
    return null
  }

  return parentToUse
}

internal fun File.jumpOverEmulated(context: Context): File {
  val externalFileDir = context.getExternalFilesDir()
  externalFileDir?.parentFile?.let { externalParentFile ->
    if (absolutePath == externalParentFile.absolutePath) {
      return externalFileDir
    }
  }
  return this
}

internal fun File.friendlyName(context: Context) = when {
  isExternalStorage(context) -> "External Storage"
  isRoot() -> "Root"
  else -> name
}

internal fun Context.hasPermission(permission: String): Boolean {
  return ContextCompat.checkSelfPermission(this, permission) ==
      PackageManager.PERMISSION_GRANTED
}

internal fun MaterialDialog.hasReadStoragePermission(): Boolean {
  return windowContext.hasPermission(permission.READ_EXTERNAL_STORAGE)
}

internal fun MaterialDialog.hasWriteStoragePermission(): Boolean {
  return windowContext.hasPermission(permission.WRITE_EXTERNAL_STORAGE)
}

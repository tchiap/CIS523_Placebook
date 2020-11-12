
package com.raywenderlich.placebook.util

import android.content.Context
import java.io.File

object FileUtils {

  // page 431
  fun deleteFile(context: Context, filename: String) {
    val dir = context.filesDir
    val file = File(dir, filename)
    file.delete()

  }
}

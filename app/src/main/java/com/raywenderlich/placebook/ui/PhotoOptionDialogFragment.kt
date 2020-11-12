
package com.raywenderlich.placebook.ui

import android.provider.MediaStore
import androidx.appcompat.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle

import androidx.fragment.app.DialogFragment

class PhotoOptionDialogFragment : DialogFragment() {

  interface PhotoOptionDialogListener {
    fun onCaptureClick()
    fun onPickClick()
  }

  private lateinit var listener: PhotoOptionDialogListener


  // page 394
  override fun onCreateDialog(savedInstanceState: Bundle?):
      Dialog {


    // 4
    listener = activity as PhotoOptionDialogListener

    // 5
    var captureSelectIdx = -1
    var pickSelectIdx = -1


    // 6
    val options = ArrayList<String>()

    val context = activity as Context

    if (canCapture(context)) {
      options.add("Camera")
      captureSelectIdx = 0
    }

    if (canPick(context)) {
      options.add("Gallery")
      pickSelectIdx = if (captureSelectIdx == 0) 1 else 0
    }

    // 10 - page 395
    return AlertDialog.Builder(context)
        .setTitle("Photo Option")
        .setItems(options.toTypedArray<CharSequence>()) {
          _, which ->
          if (which == captureSelectIdx) {

            listener.onCaptureClick()
          } else if (which == pickSelectIdx) {

            listener.onPickClick()
          }
        }
        .setNegativeButton("Cancel", null)
        .create()
  }
  companion object {

    // 13
    fun canPick(context: Context) : Boolean {
      val pickIntent = Intent(Intent.ACTION_PICK,
          MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
      return (pickIntent.resolveActivity(
          context.packageManager) != null)
    }

    // 14
    fun canCapture(context: Context) : Boolean {
      val captureIntent = Intent(
          MediaStore.ACTION_IMAGE_CAPTURE)
      return (captureIntent.resolveActivity(
          context.packageManager) != null)
    }

    // page 395 - 15
    fun newInstance(context: Context):
        PhotoOptionDialogFragment? {

      if (canPick(context) || canCapture(context)) {
        val frag = PhotoOptionDialogFragment()
        return frag
      } else {

        return null
      }
    }
  }
}

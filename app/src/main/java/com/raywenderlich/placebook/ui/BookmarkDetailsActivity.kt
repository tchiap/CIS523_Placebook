

package com.raywenderlich.placebook.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.raywenderlich.placebook.R
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.raywenderlich.placebook.util.ImageUtils
import com.raywenderlich.placebook.viewmodel.BookmarkDetailsViewModel
import kotlinx.android.synthetic.main.activity_bookmark_details.*
import java.io.File
import java.net.URLEncoder

class BookmarkDetailsActivity : AppCompatActivity(),
    PhotoOptionDialogFragment.PhotoOptionDialogListener {

  private val bookmarkDetailsViewModel by viewModels<BookmarkDetailsViewModel>()

  private var bookmarkDetailsView: BookmarkDetailsViewModel.BookmarkDetailsView? = null

  private var photoFile: File? = null

  override fun onCaptureClick() {

    photoFile = null

    try {

      photoFile = ImageUtils.createUniqueImageFile(this)

    } catch (ex: java.io.IOException) {

      return

    }

    photoFile?.let { photoFile ->

      val photoUri = FileProvider.getUriForFile(this, "com.raywenderlich.placebook.fileprovider", photoFile)

      val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

      captureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoUri)

      val intentActivities = packageManager.queryIntentActivities(
          captureIntent, PackageManager.MATCH_DEFAULT_ONLY)

      intentActivities.map { it.activityInfo.packageName }
          .forEach { grantUriPermission(it, photoUri,
              Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
          }

      startActivityForResult(captureIntent, REQUEST_CAPTURE_IMAGE)

    }

  }



  override fun onOptionsItemSelected(item: MenuItem): Boolean {

    when (item.itemId) {

      R.id.action_save -> {
        saveChanges()
        return true
      }

      R.id.action_delete -> {
        deleteBookmark()
        return true
      }
      else -> return super.onOptionsItemSelected(item)

    }
  }



  override fun onCreate(savedInstanceState: android.os.Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_bookmark_details)
    setupToolbar()
    getIntentData()
    setupFab()
  }

  override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
    val inflater = menuInflater
    inflater.inflate(R.menu.menu_bookmark_details, menu)
    return true
  }


  override fun onPickClick() {

    val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
    startActivityForResult(pickIntent, REQUEST_GALLERY_IMAGE)

  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    // page 406

    // 1
    if (resultCode == android.app.Activity.RESULT_OK) {

      // 2
      when (requestCode) {

        REQUEST_CAPTURE_IMAGE -> {

          val photoFile = photoFile ?: return

          val uri = FileProvider.getUriForFile(this, "com.raywenderlich.placebook.fileprovider", photoFile)

          revokeUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

          // 6
          val image = getImageWithPath(photoFile.absolutePath)
          image?.let { updateImage(it) }
        }

        REQUEST_GALLERY_IMAGE -> if (data != null && data.data != null) {
          val imageUri = data.data as Uri
          val image = getImageWithAuthority(imageUri)
          image?.let { updateImage(it) }
        }
      }
    }
  }

  // page 422
  private fun populateCategoryList() {

    // 1
    val bookmarkView = bookmarkDetailsView ?: return

    val resourceId =
        bookmarkDetailsViewModel.getCategoryResourceId(
            bookmarkView.category)

    // 3
    resourceId?.let { imageViewCategory.setImageResource(it) }

    val categories = bookmarkDetailsViewModel.getCategories()

    val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)

    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

    // 6
    spinnerCategory.adapter = adapter

    val placeCategory = bookmarkView.category
    spinnerCategory.setSelection(adapter.getPosition(placeCategory))

    spinnerCategory.post {
      spinnerCategory.onItemSelectedListener = object :
          AdapterView.OnItemSelectedListener {

        override fun onItemSelected(parent: AdapterView<*>, view: View,
                                    position: Int, id: Long) {



          val category = parent.getItemAtPosition(position) as String

          val resourceId = bookmarkDetailsViewModel.getCategoryResourceId(category)

          resourceId?.let {
            imageViewCategory.setImageResource(it) }

        }


        override fun onNothingSelected(parent: AdapterView<*>) {

        }
      }
    }
  }


  // page 409
  private fun getImageWithAuthority(uri: Uri): Bitmap? {
    return ImageUtils.decodeUriStreamToSize(uri, resources.getDimensionPixelSize(
            R.dimen.default_image_width), resources.getDimensionPixelSize(
            R.dimen.default_image_height), this)
  }

  private fun updateImage(image: Bitmap) {
    val bookmarkView = bookmarkDetailsView ?: return
    imageViewPlace.setImageBitmap(image)
    bookmarkView.setImage(this, image)
  }

  // page 398
  private fun replaceImage() {
    val newFragment = PhotoOptionDialogFragment.newInstance(this)
    newFragment?.show(supportFragmentManager, "photoOptionDialog")
  }

  private fun getImageWithPath(filePath: String): Bitmap? {
    return ImageUtils.decodeFileToSize(filePath, resources.getDimensionPixelSize(
            R.dimen.default_image_width), resources.getDimensionPixelSize(
            R.dimen.default_image_height))
  }



  private fun saveChanges() {

    val name = editTextName.text.toString()
    if (name.isEmpty()) {
      return
    }

    // page 376
    bookmarkDetailsView?.let { bookmarkView ->
      bookmarkView.name = editTextName.text.toString()
      bookmarkView.notes = editTextNotes.text.toString()
      bookmarkView.address = editTextAddress.text.toString()
      bookmarkView.phone = editTextPhone.text.toString()
      bookmarkView.category = spinnerCategory.selectedItem as String
      bookmarkDetailsViewModel.updateBookmark(bookmarkView)
    }

    finish()
  }

  private fun deleteBookmark()
  {
    val bookmarkView = bookmarkDetailsView ?: return
    AlertDialog.Builder(this)
        .setMessage("Delete?")
        .setPositiveButton("Ok") { _, _ ->
          bookmarkDetailsViewModel.deleteBookmark(bookmarkView)
          finish() }
        .setNegativeButton("Cancel", null)
        .create().show()
  }



  private fun getIntentData() {

    // page 371
    // 1
    val bookmarkId = intent.getLongExtra(
        MapsActivity.Companion.EXTRA_BOOKMARK_ID, 0)

    bookmarkDetailsViewModel.getBookmark(bookmarkId)?.observe(
        this, Observer<BookmarkDetailsViewModel.BookmarkDetailsView> {

      // 3
      it?.let {
        bookmarkDetailsView = it
        // Populate fields from bookmark
        populateFields()
        populateImageView()
        populateCategoryList()
      }
    })
  }



  private fun populateFields() {
    bookmarkDetailsView?.let { bookmarkView ->
      editTextName.setText(bookmarkView.name)
      editTextPhone.setText(bookmarkView.phone)
      editTextNotes.setText(bookmarkView.notes)
      editTextAddress.setText(bookmarkView.address)
    }
  }

  // page 472
  private fun setupToolbar() {
    setSupportActionBar(toolbar)
  }



  private fun populateImageView() {
    bookmarkDetailsView?.let { bookmarkView ->

      val placeImage = bookmarkView.getImage(this)
      placeImage?.let {
        imageViewPlace.setImageBitmap(placeImage)
      }

    }

    imageViewPlace.setOnClickListener {
      replaceImage()
    }
  }

  private fun setupFab() {
    fab.setOnClickListener { sharePlace() }
  }

  private fun sharePlace() {

    // page 434

    // 1
    val bookmarkView = bookmarkDetailsView ?: return

    // 2
    var mapUrl = ""

    if (bookmarkView.placeId == null) {

      val location = URLEncoder.encode("${bookmarkView.latitude},"
          + "${bookmarkView.longitude}", "utf-8")
      mapUrl = "https://www.google.com/maps/dir/?api=1" +
          "&destination=$location"
    } else {

      val name = URLEncoder.encode(bookmarkView.name, "utf-8")
      mapUrl = "https://www.google.com/maps/dir/?api=1" +
          "&destination=$name&destination_place_id=" +
          "${bookmarkView.placeId}"

    }

    // 5
    val sendIntent = Intent()
    sendIntent.action = Intent.ACTION_SEND

    // 6
    sendIntent.putExtra(Intent.EXTRA_TEXT, "Check out ${bookmarkView.name} at:\n$mapUrl")
    sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Sharing ${bookmarkView.name}")

    // 7
    sendIntent.type = "text/plain"

    // 8
    startActivity(sendIntent)
  }

  companion object {
    private const val REQUEST_CAPTURE_IMAGE = 1
    private const val REQUEST_GALLERY_IMAGE = 2
  }
}

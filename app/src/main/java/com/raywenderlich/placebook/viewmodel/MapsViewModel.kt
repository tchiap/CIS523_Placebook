
package com.raywenderlich.placebook.viewmodel

import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.google.android.gms.maps.model.LatLng
import android.app.Application
import android.content.Context
import android.graphics.Bitmap

import com.google.android.libraries.places.api.model.Place
import com.raywenderlich.placebook.model.Bookmark
import com.raywenderlich.placebook.repository.BookmarkRepo
import com.raywenderlich.placebook.util.ImageUtils

class MapsViewModel(application: Application) : AndroidViewModel(application) {

  private val TAG = "MapsViewModel"

  // 2 - page 330
  private var bookmarkRepo: BookmarkRepo = BookmarkRepo(getApplication())



  private var bookmarks: LiveData<List<BookmarkView>>? = null

  // page 429
  fun addBookmark(latLng: LatLng) : Long? {
    val bookmark = bookmarkRepo.createBookmark()
    bookmark.name = "Untitled"
    bookmark.longitude = latLng.longitude
    bookmark.latitude = latLng.latitude

    bookmark.category = "Other"
    return bookmarkRepo.addBookmark(bookmark)
  }

  fun addBookmarkFromPlace(place: Place, image: Bitmap?) {

    val bookmark = bookmarkRepo.createBookmark()
    bookmark.placeId = place.id
    bookmark.name = place.name.toString()
    bookmark.longitude = place.latLng?.longitude ?: 0.0
    bookmark.latitude = place.latLng?.latitude ?: 0.0
    bookmark.phone = place.phoneNumber.toString()
    bookmark.address = place.address.toString()
    bookmark.category = getPlaceCategory(place)

    val newId = bookmarkRepo.addBookmark(bookmark)
    image?.let { bookmark.setImage(it, getApplication()) }

    Log.i(TAG, "New bookmark $newId added to the database.")
  }

  fun getBookmarkViews() :
      LiveData<List<BookmarkView>>? {
    if (bookmarks == null) {
      mapBookmarksToBookmarkView()
    }
    return bookmarks
  }

  private fun mapBookmarksToBookmarkView() {
    bookmarks = Transformations.map(bookmarkRepo.allBookmarks) { repoBookmarks ->
      repoBookmarks.map { bookmark ->
        bookmarkToBookmarkView(bookmark)
      }
    }
  }

  private fun getPlaceCategory(place: Place): String {

    // 1 - page 416
    var category = "Other"
    val placeTypes = place.types

    placeTypes?.let { placeTypes ->
      if (placeTypes.size > 0) {

        // 3
        val placeType = placeTypes[0]
        category = bookmarkRepo.placeTypeToCategory(placeType)
      }
    }

    return category
  }



  private fun bookmarkToBookmarkView(bookmark: Bookmark):
      MapsViewModel.BookmarkView {
    return MapsViewModel.BookmarkView(
        bookmark.id,
        LatLng(bookmark.latitude, bookmark.longitude),
        bookmark.name,
        bookmark.phone,
        bookmarkRepo.getCategoryResourceId(bookmark.category))
  }

  data class BookmarkView(val id: Long? = null,
                          val location: LatLng = LatLng(0.0, 0.0),
                          val name: String = "",
                          val phone: String = "",
                          val categoryResourceId: Int? = null) {

    fun getImage(context: Context): Bitmap? {
      id?.let {
        return ImageUtils.loadBitmapFromFile(context,
            Bookmark.generateImageFilename(it))
      }
      return null
    }
  }
}

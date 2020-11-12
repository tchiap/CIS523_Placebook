

package com.raywenderlich.placebook.adapter

import android.app.Activity
import android.view.View
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.raywenderlich.placebook.R
import android.widget.ImageView
import android.widget.TextView

import com.raywenderlich.placebook.ui.MapsActivity
import com.raywenderlich.placebook.viewmodel.MapsViewModel

class BookmarkInfoWindowAdapter(val context: Activity) : GoogleMap.InfoWindowAdapter {

    private val contents: View

    init {
        contents = context.layoutInflater.inflate(
                R.layout.content_bookmark_info, null)
    }



    override fun getInfoContents(marker: Marker): View? {
        val titleView = contents.findViewById<TextView>(R.id.title)
        titleView.text = marker.title ?: ""

        val phoneView = contents.findViewById<TextView>(R.id.phone)
        phoneView.text = marker.snippet ?: ""


        // page 313
        val imageView = contents.findViewById<ImageView>(R.id.photo)


        when (marker.tag) {

            is MapsActivity.PlaceInfo -> {
                imageView.setImageBitmap(

                        //imageView.setImageBitmap(marker.tag as Bitmap?)

                        (marker.tag as MapsActivity.PlaceInfo).image)
            }

            is MapsViewModel.BookmarkView -> {
                var bookMarkview = marker.tag as
                        MapsViewModel.BookmarkView

                imageView.setImageBitmap(bookMarkview.getImage(context))
            }

        }
        return contents
    }

    override fun getInfoWindow(marker: Marker): View? {
        // we aren't replacing the entire info window, so we can return null.  We need this required function, though.
        return null
    }
}

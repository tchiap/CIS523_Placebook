
package com.raywenderlich.placebook.adapter

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.raywenderlich.placebook.R
import com.raywenderlich.placebook.ui.MapsActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

import com.raywenderlich.placebook.viewmodel.MapsViewModel.BookmarkView
import kotlinx.android.synthetic.main.bookmark_item.view.*

class BookmarkListAdapter(

    private var bookmarkData: List<BookmarkView>?,
    private val mapsActivity: MapsActivity) :
    RecyclerView.Adapter<BookmarkListAdapter.ViewHolder>() {

  class ViewHolder(v: View, private val mapsActivity: MapsActivity) : RecyclerView.ViewHolder(v) {
    val nameTextView: TextView = v.bookmarkNameTextView
    val categoryImageView: ImageView = v.bookmarkIcon

    init {

      v.setOnClickListener {
        val bookmarkView = itemView.tag as BookmarkView
        mapsActivity.moveToBookmark(bookmarkView)
      }

    }

  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkListAdapter.ViewHolder {

    val vh = ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                    R.layout.bookmark_item, parent, false), mapsActivity)
    return vh

  }



  fun setBookmarkData(bookmarks: List<BookmarkView>) {
    this.bookmarkData = bookmarks
    notifyDataSetChanged()
  }



  override fun getItemCount(): Int {
    return bookmarkData?.size ?: 0
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {

    val bookmarkData = bookmarkData ?: return
    val bookmarkViewData = bookmarkData[position]

    holder.itemView.tag = bookmarkViewData
    holder.nameTextView.text = bookmarkViewData.name

    bookmarkViewData.categoryResourceId?.let {
      holder.categoryImageView.setImageResource(it)
    }
  }


}

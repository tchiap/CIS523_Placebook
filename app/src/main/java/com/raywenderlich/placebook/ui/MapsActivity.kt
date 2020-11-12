

package com.raywenderlich.placebook.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.WindowManager
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.lifecycle.Observer

import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.common.GooglePlayServicesRepairableException

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode

import com.google.android.libraries.places.api.model.Place

import com.raywenderlich.placebook.R
import com.raywenderlich.placebook.adapter.BookmarkInfoWindowAdapter
import com.raywenderlich.placebook.adapter.BookmarkListAdapter
import com.raywenderlich.placebook.viewmodel.MapsViewModel
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.android.synthetic.main.drawer_view_maps.*
import kotlinx.android.synthetic.main.main_view_maps.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

  private lateinit var map: GoogleMap
  private lateinit var placesClient: PlacesClient

  private lateinit var fusedLocationClient: FusedLocationProviderClient

  private val mapsViewModel by viewModels<MapsViewModel>()
  private var markers = HashMap<Long, Marker>()

  private lateinit var bookmarkListAdapter: BookmarkListAdapter





  override fun onCreate(savedInstanceState: Bundle?) {

    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_maps)

    val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
    mapFragment.getMapAsync(this)

    setupLocationClient()
    setupToolbar()
    setupPlacesClient()
    setupNavigationDrawer()
  }

  override fun onMapReady(googleMap: GoogleMap) {

    map = googleMap

    setupMapListeners()
    createBookmarkObserver()
    getCurrentLocation()

  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)


    // page 426
    // 1
    when (requestCode) {
      AUTOCOMPLETE_REQUEST_CODE ->


        // 2
        if (resultCode == Activity.RESULT_OK && data != null) {

          val place = Autocomplete.getPlaceFromIntent(data)


          // 4
          val location = Location("")
          location.latitude = place.latLng?.latitude ?: 0.0
          location.longitude = place.latLng?.longitude ?: 0.0
          updateMapToLocation(location)
          showProgress()

          // 5
          displayPoiGetPhotoStep(place)
        }
    }
  }

  private fun setupPlacesClient() {
    Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
    placesClient = Places.createClient(this);
  }


  private fun requestLocationPermissions() {

    // page 274
    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION)
  }


  private fun setupMapListeners() {

    // page 311
    map.setInfoWindowAdapter(BookmarkInfoWindowAdapter(this))

    map.setOnPoiClickListener {
      displayPoi(it)
    }

    // page 334
    map.setOnInfoWindowClickListener {
      handleInfoWindowClick(it)
    }

    fab.setOnClickListener {
      searchAtCurrentLocation()
    }

    // page 429
    map.setOnMapLongClickListener { latLng ->
      newBookmark(latLng)
    }
  }
  
  private fun displayPoi(pointOfInterest: PointOfInterest) {
    showProgress()
    displayPoiGetPlaceStep(pointOfInterest)
  }

  private fun displayPoiGetPlaceStep(pointOfInterest: PointOfInterest) {
    val placeId = pointOfInterest.placeId

    // page 417
    val placeFields = listOf(Place.Field.ID,
        Place.Field.NAME,
        Place.Field.PHONE_NUMBER,
        Place.Field.PHOTO_METADATAS,
        Place.Field.ADDRESS,
        Place.Field.LAT_LNG,
        Place.Field.TYPES)

    val request = FetchPlaceRequest
        .builder(placeId, placeFields)
        .build()

    /*
                    Toast.makeText(this,
                            "${place.name}, " +
                                    "${place.phoneNumber}",
                            Toast.LENGTH_LONG).show()

                     */

    placesClient.fetchPlace(request)
        .addOnSuccessListener { response ->

      val place = response.place
      displayPoiGetPhotoStep(place)
    }.addOnFailureListener { exception ->

      if (exception is ApiException) {
        val statusCode = exception.statusCode
        Log.e(TAG, "Place not found: " + exception.message + ", " + "statusCode: " + statusCode)
        hideProgress()
      }
    }
  }  

  private fun displayPoiGetPhotoStep(place: Place) {

    val photoMetadata = place.getPhotoMetadatas()?.get(0)
    if (photoMetadata == null) {
      displayPoiDisplayStep(place, null)
      return
    }

    // page 298

    // 3
    val photoRequest = FetchPhotoRequest.builder(photoMetadata).setMaxWidth(resources.getDimensionPixelSize(R.dimen.default_image_width)).setMaxHeight(resources.getDimensionPixelSize(R.dimen.default_image_height)).build()

    placesClient.fetchPhoto(photoRequest).addOnSuccessListener { fetchPhotoResponse ->
      val bitmap = fetchPhotoResponse.bitmap

      displayPoiDisplayStep(place, bitmap)

    }.addOnFailureListener { exception ->


      // 6
      if (exception is ApiException) {
        val statusCode = exception.statusCode
        Log.e(TAG, "Place not found: " + exception.message + ", statusCode: " + statusCode)

      }
      hideProgress()
    }
  }

  private fun setupToolbar() {
    setSupportActionBar(toolbar)
    val toggle = ActionBarDrawerToggle(
            this,  drawerLayout, toolbar,
            R.string.open_drawer, R.string.close_drawer)
    toggle.syncState()
  }

  private fun displayPoiDisplayStep(place: Place, photo: Bitmap?) {
    hideProgress()
    val marker = map.addMarker(MarkerOptions()
        .position(place.latLng as LatLng)
        .title(place.name)
        .snippet(place.phoneNumber)
    )

    // page 333
    marker?.tag = PlaceInfo(place, photo)
    //marker?.tag = photo
    marker?.showInfoWindow()

  }
  
  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {

    if (requestCode == REQUEST_LOCATION) {
      if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        getCurrentLocation()
      } else {
        Log.e(TAG, "Location permission denied")
      }
    }

  }

  private fun setupLocationClient() {
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
  }

  private fun startBookmarkDetails(bookmarkId: Long) {

    val intent = Intent(this, BookmarkDetailsActivity::class.java)
    intent.putExtra(EXTRA_BOOKMARK_ID, bookmarkId)
    startActivity(intent)

  }


  private fun handleInfoWindowClick(marker: Marker) {

    when (marker.tag) {

      is MapsActivity.PlaceInfo -> {
        val placeInfo = (marker.tag as PlaceInfo)
        if (placeInfo.place != null) {
          GlobalScope.launch {
            mapsViewModel.addBookmarkFromPlace(placeInfo.place,
                placeInfo.image)
          }

        }
        marker.remove()
      }

      is MapsViewModel.BookmarkView -> {

        val bookmarkMarkerView = (marker.tag as MapsViewModel.BookmarkView)
        marker.hideInfoWindow()
        bookmarkMarkerView.id?.let {
          startBookmarkDetails(it)

        }
      }
    }
  }

 private fun createBookmarkObserver() {
    mapsViewModel.getBookmarkViews()?.observe(
        this, Observer<List<MapsViewModel.BookmarkView>> {

          // 2
          map.clear()

          markers.clear()

          // 3
          it?.let {
            displayAllBookmarks(it)
            bookmarkListAdapter.setBookmarkData(it)
          }
        })
  }

  private fun displayAllBookmarks(
      bookmarks: List<MapsViewModel.BookmarkView>) {
    for (bookmark in bookmarks) {

      addPlaceMarker(bookmark)

    }
  }

  private fun addPlaceMarker(bookmark: MapsViewModel.BookmarkView): Marker? {

    val marker = map.addMarker(MarkerOptions()
        .position(bookmark.location)
        .title(bookmark.name)
        .snippet(bookmark.phone)
        .icon(bookmark.categoryResourceId?.let {
          BitmapDescriptorFactory.fromResource(it)
        })
        .alpha(0.8f))

    marker.tag = bookmark
    bookmark.id?.let { markers.put(it, marker) }

    return marker
  }

  private fun getCurrentLocation() {

    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      requestLocationPermissions()
    } else {

      /*
            if (locationRequest == null) {
                locationRequest = LocationRequest.create()
                locationRequest?.let { locationRequest ->
                    // 1
                    locationRequest.priority =
                            LocationRequest.PRIORITY_HIGH_ACCURACY
                    // 2
                    locationRequest.interval = 5000
                    // 3
                    locationRequest.fastestInterval = 1000
                    // 4
                    val locationCallback = object : LocationCallback() {
                        override fun onLocationResult(locationResult:
                                                      LocationResult?) {
                            getCurrentLocation()
                        }
                    }
                    // 5
                    fusedLocationClient.requestLocationUpdates(locationRequest,
                            locationCallback, null)
                }
            }

 */


      // page 289
      map.isMyLocationEnabled = true

      // 3
      fusedLocationClient.lastLocation.addOnCompleteListener {
        val location = it.result
        if (location != null) {

          val latLng = LatLng(location.latitude, location.longitude)



          // Page 288
          //map.clear()

          //map.addMarker(MarkerOptions().position(latLng)
          //    .title("You are here!"))
          // 6

          val update = CameraUpdateFactory.newLatLngZoom(latLng, 16.0f)
          map.moveCamera(update)

        } else {

          Log.e(TAG, "No location found")

        }
      }
    }
  }



  private fun setupNavigationDrawer() {
    val layoutManager = LinearLayoutManager(this)
    bookmarkRecyclerView.layoutManager = layoutManager

    bookmarkListAdapter = BookmarkListAdapter(null, this)
    bookmarkRecyclerView.adapter = bookmarkListAdapter
  }

  private fun updateMapToLocation(location: Location) {

    val latLng = LatLng(location.latitude, location.longitude)
    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16.0f))

  }


  // page 392
  fun moveToBookmark(bookmark: MapsViewModel.BookmarkView) {

    // 1
    drawerLayout.closeDrawer(drawerView)

    // 2
    val marker = markers[bookmark.id]

    marker?.showInfoWindow()

    val location = Location("")

    location.latitude =  bookmark.location.latitude
    location.longitude = bookmark.location.longitude

    updateMapToLocation(location)
  }


  // PlaceAutocomplete search

  private fun searchAtCurrentLocation() {


    // 1
    val placeFields = listOf(
        Place.Field.ID,
        Place.Field.NAME,
        Place.Field.PHONE_NUMBER,
        Place.Field.PHOTO_METADATAS,
        Place.Field.LAT_LNG,
        Place.Field.ADDRESS,
        Place.Field.TYPES)


    // 2
    val bounds = RectangularBounds.newInstance(map.projection.visibleRegion.latLngBounds)


    // page 425
    try {

      // 3
      val intent = Autocomplete.IntentBuilder(
          AutocompleteActivityMode.OVERLAY, placeFields)
          .setLocationBias(bounds)
          .build(this)
      startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)

    } catch (e: GooglePlayServicesRepairableException) {

    } catch (e: GooglePlayServicesNotAvailableException) {

    }
  }


  // page 440
  private fun disableUserInteraction() {
    window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
  }


  private fun showProgress() {
    progressBar.visibility = ProgressBar.VISIBLE
    disableUserInteraction()
  }

  private fun hideProgress() {

    // page 440
    progressBar.visibility = ProgressBar.GONE
    enableUserInteraction()
  }


  // page 440
  private fun enableUserInteraction() {
    window.clearFlags(
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
  }


  private fun newBookmark(latLng: LatLng) {
    GlobalScope.launch {

      val bookmarkId = mapsViewModel.addBookmark(latLng)
      bookmarkId?.let {
        startBookmarkDetails(it)
      }
    }
  }





  companion object {

    // page 366
    const val EXTRA_BOOKMARK_ID = "com.raywenderlich.placebook.EXTRA_BOOKMARK_ID"

    private const val REQUEST_LOCATION = 1
    private const val TAG = "MapsActivity"
    private const val AUTOCOMPLETE_REQUEST_CODE = 2
  }

  class PlaceInfo(val place: Place? = null, val image: Bitmap? = null)
}

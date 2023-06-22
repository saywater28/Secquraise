package com.example.secquraise

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationRequest
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat.requestLocationUpdates
import androidx.databinding.DataBindingUtil
import com.example.secquraise.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import java.text.DateFormat
import java.util.Calendar
import java.util.Date


class MainActivity : AppCompatActivity() {

    private val REQUEST_IMAGE_CAPTURE = 1
    private var imageBitmap: Bitmap?=null
    private val PERMISSION_CODE = 1000;
    var image_uri: Uri? = null
    private val IMAGE_CAPTURE_CODE  = 1001

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var coordinatesTextView: TextView

    private fun openCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
        image_uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        //camera intent
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri)
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE)

    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val calendar: Date = Calendar.getInstance().time
        val currentDate: String = DateFormat.getDateInstance(DateFormat.FULL).format(calendar.time)
        val currentTime: String = DateFormat.getTimeInstance().format(calendar)

        val textViewDate: TextView = findViewById(R.id.date)
        val textViewTime: TextView = findViewById(R.id.time)
        val image_view: ImageView = findViewById(R.id.image_view)
        val connectivity: TextView = findViewById(R.id.connectivity)
        val charging : TextView = findViewById(R.id.charging)
        val location : TextView = findViewById(R.id.location)

        var fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult?.lastLocation?.let { location ->
                    val latitude = location.latitude
                    val longitude = location.longitude
                    coordinatesTextView.text = "Latitude: $latitude, Longitude: $longitude"
                }
            }


        }

        if (isLocationPermissionGranted()) {
            requestLocationUpdates()
        } else {
            requestLocationPermission()
        }

        var batteryLevelReceiver = BatteryLevelReceiver()
        registerBatteryLevelReceiver()



        fun onDestroy() {
            super.onDestroy()
            unregisterBatteryLevelReceiver()
        }

        val isConnected = isInternetConnected()
        val connectivityState = if (isConnected) "ON" else "OFF"
        showConnectivityState(connectivityState)
        if (isConnected) {
            // Device is connected to the internet
            

        } else {
            // Device is not connected to the internet
            // Perform actions for offline mode
        }

        textViewDate.text = currentDate
        textViewTime.text = currentTime

//        button click
        val button: Button = findViewById(R.id.button)
        button.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(android.Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED
                ) {
                    //permission not given
                    val permission = arrayOf(
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )

                } else {
                    //permission already granted
                    openCamera()

                }
            } else {
                //system os is < marshmallow
                openCamera()
            }
        }


        val CAMERA_PERMISSION_CODE = 1

        fun requestCameraPermission() {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_CODE
                )
            }
        }

        fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray) {
            when(requestCode){
                PERMISSION_CODE -> {
                    if(grantResults.size > 0 && grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED){
                        //permission popup was granted
                        openCamera()
                    }
                    else{
                        //permission popup was denied
                        Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            if(resultCode == Activity.RESULT_OK){
                image_view.setImageURI(image_uri)
            }
        }


    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestLocationUpdates() {

        val locationRequest = LocationRequest.CREATOR

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        )

//        var fusedLocationClient = null
//        fusedLocationClient.lastLocation
//            .addOnSuccessListener { location: Location? ->
//                location?.let {
//                    val latitude = location.latitude
//                    val longitude = location.longitude
//                    location.latitude = "Latitude: $latitude"
//                    location.longitude = "Longitude: $longitude"
//                }
//            }

    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    private fun isLocationPermissionGranted(): Boolean {

        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun registerBatteryLevelReceiver() {


        val batteryLevelReceiver = object : BatteryLevelReceiver() {
            fun onBatteryLevelChanged(level: Int) {
                // Update the battery level text view
                charging.text = "Battery Level: $level%"
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryLevelReceiver, filter)    }

    fun unregisterBatteryLevelReceiver() {
        try {
            unregisterReceiver(BatteryLevelReceiver())
        } catch (e: IllegalArgumentException) {
            // Receiver was not registered or has already been unregistered
        }
    }


    private fun showConnectivityState(state: String) {
        "Connectivity State: $state"

    }

    private fun isInternetConnected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork ?: return false
            val actNetwork =
                connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false

            return when {
                actNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                actNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnected
        }
    }

    private open inner class BatteryLevelReceiver : BroadcastReceiver() {
        lateinit var charging : TextView
        override fun onReceive(context: Context, intent: Intent) {
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPercentage = (level.toFloat() / scale.toFloat() * 100).toInt()

            // Update the battery level TextView
            charging.text = "Battery Level: $batteryPercentage%"
        }
    }

}

private fun FusedLocationProviderClient.requestLocationUpdates(locationRequest: Parcelable.Creator<LocationRequest>, locationCallback: LocationCallback, nothing: Nothing?) {

}

package com.example.proximityalert

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.proximityalert.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var destinationMarker: Marker? = null
    private var destinationLocation: LatLng? = null
    
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                enableMyLocation()
            }
            else -> {
                Toast.makeText(this, "위치 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private val notificationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "알림 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        // Setup map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        
        // Setup buttons
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        binding.btnStartTracking.setOnClickListener {
            startTracking()
        }
        
        binding.btnStopTracking.setOnClickListener {
            stopTracking()
        }
        
        binding.btnClearDestination.setOnClickListener {
            clearDestination()
        }
        
        // Request permissions
        checkAndRequestPermissions()
    }
    
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // Enable location if permission granted
        enableMyLocation()
        
        // Set map click listener for destination selection
        googleMap.setOnMapClickListener { latLng ->
            setDestination(latLng)
        }
        
        // Move camera to Seoul as default
        val seoul = LatLng(37.5665, 126.9780)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(seoul, 12f))
    }
    
    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
            
            // Get current location and move camera
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                }
            }
        }
    }
    
    private fun setDestination(latLng: LatLng) {
        // Remove previous marker
        destinationMarker?.remove()
        
        // Add new marker
        destinationMarker = googleMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("목적지")
                .snippet("이 위치로 설정되었습니다")
        )
        
        destinationLocation = latLng
        
        // Show marker info
        destinationMarker?.showInfoWindow()
        
        // Update UI
        binding.tvDestinationInfo.text = "목적지: ${latLng.latitude}, ${latLng.longitude}"
        binding.btnStartTracking.isEnabled = true
        
        Toast.makeText(this, "목적지가 설정되었습니다", Toast.LENGTH_SHORT).show()
    }
    
    private fun clearDestination() {
        destinationMarker?.remove()
        destinationMarker = null
        destinationLocation = null
        binding.tvDestinationInfo.text = "지도를 클릭하여 목적지를 설정하세요"
        binding.btnStartTracking.isEnabled = false
        Toast.makeText(this, "목적지가 해제되었습니다", Toast.LENGTH_SHORT).show()
    }
    
    private fun startTracking() {
        destinationLocation?.let { destination ->
            // Check if we need background location permission (Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // Show explanation dialog
                    AlertDialog.Builder(this)
                        .setTitle("백그라운드 위치 권한 필요")
                        .setMessage("앱이 백그라운드에서 실행 중일 때도 위치 추적을 계속하려면 '항상 허용' 권한이 필요합니다.")
                        .setPositiveButton("권한 설정") { _, _ ->
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                                BACKGROUND_LOCATION_REQUEST_CODE
                            )
                        }
                        .setNegativeButton("취소", null)
                        .show()
                    return
                }
            }
            
            // Start location tracking service
            val serviceIntent = Intent(this, LocationTrackingService::class.java).apply {
                putExtra("destination_lat", destination.latitude)
                putExtra("destination_lng", destination.longitude)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            
            binding.btnStartTracking.isEnabled = false
            binding.btnStopTracking.isEnabled = true
            Toast.makeText(this, "추적을 시작합니다", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun stopTracking() {
        val serviceIntent = Intent(this, LocationTrackingService::class.java)
        stopService(serviceIntent)
        
        binding.btnStartTracking.isEnabled = true
        binding.btnStopTracking.isEnabled = false
        Toast.makeText(this, "추적을 중지합니다", Toast.LENGTH_SHORT).show()
    }
    
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            locationPermissionRequest.launch(permissionsToRequest.toTypedArray())
        }
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == BACKGROUND_LOCATION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start tracking
                startTracking()
            } else {
                Toast.makeText(
                    this,
                    "백그라운드 위치 권한이 거부되었습니다. 앱이 백그라운드에서 작동하지 않을 수 있습니다.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    companion object {
        private const val BACKGROUND_LOCATION_REQUEST_CODE = 1001
    }
}

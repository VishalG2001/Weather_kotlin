package com.example.weathery

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.weathery.databinding.ActivityMainBinding
import com.squareup.picasso.Picasso
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var weatherRVArrayList: ArrayList<WeatherRVModal>
    private lateinit var weatherRVAdapter: WeatherRVAdapter
    private val PERMISSION_CODE = 1
    private var cityName: String = "Mumbai"
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        setContentView(view)

        weatherRVArrayList = ArrayList()
        weatherRVAdapter = WeatherRVAdapter(this, weatherRVArrayList)
        binding.idrvweather.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.idrvweather.adapter = weatherRVAdapter

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), PERMISSION_CODE)
        } else {
            getLocationAndWeatherInfo()
        }

        binding.idsearch.setOnClickListener {
            val city = binding.editcity.text.toString()
            if (city.isEmpty()) {
                Toast.makeText(this, "Please enter city Name", Toast.LENGTH_SHORT).show()
            } else {
                binding.idcityname.text = city
                getWeatherInfo(city)
                hideKeyboard()
            }
        }

        // Load default weather information for Mumbai
        getWeatherInfo(cityName)
    }

    @SuppressLint("MissingPermission")
    private fun getLocationAndWeatherInfo() {
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                cityName = getCityName(location.longitude, location.latitude)
                getWeatherInfo(cityName)
                locationManager.removeUpdates(this)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

            override fun onProviderEnabled(provider: String) {}

            override fun onProviderDisabled(provider: String) {}
        }

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, locationListener)
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)

        val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (location != null) {
            cityName = getCityName(location.longitude, location.latitude)
            getWeatherInfo(cityName)
        } else {
            Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationAndWeatherInfo()
            } else {
                Toast.makeText(this, "Please provide the permissions", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun getWeatherInfo(cityName: String) {
        val url = "http://api.weatherapi.com/v1/forecast.json?key=b270fa5bc7e24887992110256241203&q=$cityName&days=1&aqi=no&alerts=no"
        binding.idcityname.text = cityName
        val requestQueue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null, { response: JSONObject ->
            binding.loadiingvg.visibility = View.GONE
            binding.RLhome.visibility = View.VISIBLE
            weatherRVArrayList.clear()

            try {
                val temperature = response.getJSONObject("current").getString("temp_c")
                binding.temprature.text = "$temperature°C"
                val isday = response.getJSONObject("current").getInt("is_day")
                val condition = response.getJSONObject("current").getJSONObject("condition").getString("text")
                val conditionIcon = response.getJSONObject("current").getJSONObject("condition").getString("icon")
                Picasso.get().load("http:$conditionIcon").into(binding.idivcon)
                binding.idtvcondition.text = condition
                binding.bgimage.setImageResource(if (isday == 1) R.drawable.dayimage else R.drawable.nightimage)

                val forecastObj = response.getJSONObject("forecast")
                val forecastO = forecastObj.getJSONArray("forecastday").getJSONObject(0)
                val hourArray = forecastO.getJSONArray("hour")

                for (i in 0 until hourArray.length()) {
                    val hourObj = hourArray.getJSONObject(i)
                    val time = hourObj.getString("time")
                    val temper = hourObj.getString("temp_c")
                    val img = hourObj.getJSONObject("condition").getString("icon")
                    val wind = hourObj.getString("wind_kph")
                    weatherRVArrayList.add(WeatherRVModal(time, temper, img, wind))
                }
                weatherRVAdapter.notifyDataSetChanged()
            } catch (e: JSONException) {
                e.printStackTrace()
                Toast.makeText(this, "Error parsing weather data", Toast.LENGTH_SHORT).show()
            }
        }, {
            Toast.makeText(this, "Please enter valid city name..", Toast.LENGTH_SHORT).show()
        })
        requestQueue.add(jsonObjectRequest)
    }

    private fun getCityName(longitude: Double, latitude: Double): String {
        var cityName = "Not found"
        val gcd = Geocoder(baseContext, Locale.getDefault())
        try {
            val addresses = gcd.getFromLocation(latitude, longitude, 10)
            if (addresses != null) {
                for (adr in addresses) {
                    val city = adr.locality
                    if (!city.isNullOrEmpty()) {
                        cityName = city
                        break
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return cityName
    }
    fun AppCompatActivity.hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}

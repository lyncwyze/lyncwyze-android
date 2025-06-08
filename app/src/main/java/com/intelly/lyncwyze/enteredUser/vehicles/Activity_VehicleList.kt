package com.intelly.lyncwyze.enteredUser.vehicles

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.intelly.lyncwyze.Assest.Loader
import com.intelly.lyncwyze.Assest.modals.Vehicle
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import com.intelly.lyncwyze.Assest.utilities.HttpUtilities
import com.intelly.lyncwyze.Assest.utilities.VEHICLE_ID
import com.intelly.lyncwyze.Assest.utilities.showToast
import com.intelly.lyncwyze.R
import kotlinx.coroutines.launch
import mu.KotlinLogging

class Activity_VehicleList : AppCompatActivity() {
    private val logger = KotlinLogging.logger {}
    private val loader = Loader(this)

    private lateinit var backButton: ImageView
    private lateinit var scrollView: ScrollView
    private lateinit var theListNoContent: LinearLayout
    private lateinit var addMore: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_vehicle_list)
        setupUI()
        setUpFunctionality()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    override fun onResume() {
        super.onResume()
//        appCheck()
        fetchAllVehicles()
    }

    private fun setupUI() {
        backButton = findViewById(R.id.vlBackButton)

        scrollView = findViewById(R.id.vlScrollView)
        theListNoContent = findViewById(R.id.vlNoContent)

        addMore = findViewById(R.id.vlAddMore)
    }
    private fun setUpFunctionality() {
        backButton.setOnClickListener { this.onBackPressed() }
        addMore.setOnClickListener { startActivity(Intent(this, Activity_Vehicle_Add::class.java)) }
        fetchAllVehicles()
    }

    private fun fetchAllVehicles() {
        lifecycleScope.launch {
            try {
                loader.showLoader(this@Activity_VehicleList)
                val response = NetworkManager.apiService.getVehicles()
                if (response.isSuccessful) {
                    loader.hideLoader(this@Activity_VehicleList)
                    val vehicles = response.body()
                    if (vehicles != null)
                        displayVehicles(vehicles.data)
                    else
                        showToast(this@Activity_VehicleList, getString(R.string.fetching_error))
                } else {
                    loader.hideLoader(this@Activity_VehicleList)
                    showToast(this@Activity_VehicleList, "${getString(R.string.error_txt)}: ${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
                }
            } catch (e: Exception) {
                loader.hideLoader(this@Activity_VehicleList)
                showToast(this@Activity_VehicleList, "${getString(R.string.exception_collen)} ${e.message}")
            }
        }
    }
    private fun displayVehicles(vehicles: List<Vehicle>) {
        val vehicleLL = findViewById<LinearLayout>(R.id.llVehicleList)
        vehicleLL.removeAllViews()

        if (vehicles.isEmpty()) {
            scrollView.visibility = View.INVISIBLE
            theListNoContent.visibility = View.VISIBLE
            showToast(this@Activity_VehicleList, getString(R.string.add_vehicle_to_list_them))
        } else {
            for (eachVehicle in vehicles) {
                val vehicleItemView = layoutInflater.inflate(R.layout.layout_vehicle_item, vehicleLL, false)

//                val imageViewChild = vehicleItemView.findViewById<ImageView>(R.id.vehicleImage)
//                if (eachVehicle.image != null) Glide.with(this).load(eachVehicle.image).into(imageViewChild)
//                else imageViewChild.setImageResource(R.drawable.user)

                vehicleItemView.findViewById<TextView>(R.id.vehicleLicensePlateNo).text = eachVehicle.licensePlate
                vehicleItemView.findViewById<TextView>(R.id.vehicleColor).text = eachVehicle.bodyColor
                vehicleItemView.findViewById<TextView>(R.id.vehicleCompany).text = eachVehicle.model
                vehicleItemView.findViewById<ImageView>(R.id.editVehicle).setOnClickListener {
                    startActivity(Intent(this, Activity_VehicleViewEditUpdate::class.java).apply { putExtra(VEHICLE_ID, eachVehicle.id) })
                }
                vehicleLL.addView(vehicleItemView)
            }
            scrollView.visibility = View.VISIBLE
            theListNoContent.visibility = View.INVISIBLE
        }
    }
}
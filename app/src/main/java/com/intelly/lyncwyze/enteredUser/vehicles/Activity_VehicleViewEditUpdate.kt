package com.intelly.lyncwyze.enteredUser.vehicles

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.intelly.lyncwyze.Assest.Loader
import com.intelly.lyncwyze.Assest.modals.Vehicle
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import com.intelly.lyncwyze.Assest.utilities.HttpUtilities
import com.intelly.lyncwyze.Assest.utilities.Utilities
import com.intelly.lyncwyze.Assest.utilities.VEHICLE_ID
import com.intelly.lyncwyze.Assest.utilities.showToast
import com.intelly.lyncwyze.R
import kotlinx.coroutines.launch
import mu.KotlinLogging

class Activity_VehicleViewEditUpdate : AppCompatActivity() {
    private val logger = KotlinLogging.logger {}
    private val loader = Loader(this)

    private var vehicleID: String = ""

    private lateinit var backButton: ImageView

    private lateinit var vehEditMaker: EditText
    private lateinit var vehEditModel: EditText
    private lateinit var vehEditColor: EditText
    private lateinit var vehEditLicPlate: EditText
    private lateinit var vehEditVehicleType: RadioGroup
    private lateinit var vehEditAlias: EditText
    private lateinit var vehEditIsPrimaryVehicle: CheckBox

    private lateinit var vehEditUpdate: Button
    private lateinit var vehEditDelete: Button
    private var vehicle: Vehicle = Vehicle.default()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_vehicle_view_edit_update)
        activityCheck()
        setupUI()
        setUpFunctionality()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun activityCheck() {
        val id = intent.getStringExtra(VEHICLE_ID)
        if (id.isNullOrBlank()) {
            showToast(this, getString(R.string.error_getting_vehicle_id))
            this@Activity_VehicleViewEditUpdate.finish()
        } else {
            vehicleID = id
            fetchVehicle()
        }
    }
    private fun setupUI() {
        backButton = findViewById(R.id.vehicleEditBtn)

        vehEditMaker = findViewById(R.id.vehEditMaker)
        vehEditModel = findViewById(R.id.vehEditModel)
        vehEditVehicleType = findViewById(R.id.vehEditVehicleType)
        vehEditColor = findViewById(R.id.vehEditColor)
        vehEditLicPlate = findViewById(R.id.vehEditLicPlate)
        vehEditAlias = findViewById(R.id.vehEditAlias)
        vehEditIsPrimaryVehicle = findViewById(R.id.vehEditIsPrimaryVehicle)

        vehEditUpdate = findViewById(R.id.vehEditUpdate)
        vehEditDelete = findViewById(R.id.vehEditDelete)
    }
    private fun setUpFunctionality() {
        backButton.setOnClickListener { this.onBackPressed() }
        backButton.setOnClickListener { this.onBackPressed() }
        vehEditUpdate.setOnClickListener { updateVehicle() }
        vehEditDelete.setOnClickListener { deleteVehicle() }
    }

    private fun fetchVehicle() {
        lifecycleScope.launch {
            try {
                loader.showLoader(this@Activity_VehicleViewEditUpdate)
                val response = NetworkManager.apiService.getVehicleById(vehicleID)
                if (response.isSuccessful) {
                    loader.hideLoader(this@Activity_VehicleViewEditUpdate)

                    response.body()?.let {
                        logger.info { it }
                        vehicle = it
                        displayVehicle()
                    } ?: showToast(this@Activity_VehicleViewEditUpdate, "Fetching error!")
                } else {
                    loader.hideLoader(this@Activity_VehicleViewEditUpdate)
                    showToast(this@Activity_VehicleViewEditUpdate, "${getString(R.string.error_txt)}: ${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
                }
            } catch (e: Exception) {
                loader.hideLoader(this@Activity_VehicleViewEditUpdate)
                logger.error { e.message }
                showToast(this@Activity_VehicleViewEditUpdate, "${getString(R.string.exception_collen)} ${e.message}")
            }
        }
    }
    private fun displayVehicle() {
        vehEditMaker.setText(vehicle.make)
        vehEditModel.setText(vehicle.model)
        vehEditColor.setText(vehicle.bodyColor)
        when (vehicle.bodyStyle) {
            "SUV" -> vehEditVehicleType.check(R.id.vehEditRadioSUV)
            "SEDAN" -> vehEditVehicleType.check(R.id.vehEditRadioSedan)
            "TRUCK" -> vehEditVehicleType.check(R.id.vehEditRadioTruck)
            else -> { vehEditVehicleType.clearCheck() }
        }
        vehEditLicPlate.setText(vehicle.licensePlate)
        vehEditAlias.setText(vehicle.alias)
        vehEditIsPrimaryVehicle.isChecked = vehicle.primary
    }
    private fun updateVehicle() {
        lifecycleScope.launch {
            try {
                vehicle.make = vehEditMaker.text.toString()
                vehicle.model = vehEditModel.text.toString()
                vehicle.bodyColor = vehEditColor.text.toString()
                vehicle.bodyStyle = findViewById<RadioButton>(vehEditVehicleType.checkedRadioButtonId).tag.toString()
                vehicle.licensePlate = vehEditLicPlate.text.toString()
                vehicle.alias = vehEditAlias.text.toString()
                vehicle.primary = vehEditIsPrimaryVehicle.isChecked

                loader.showLoader(this@Activity_VehicleViewEditUpdate)
                val response = NetworkManager.apiService.updateVehicle(body = vehicle)
                if (response.isSuccessful) {
                    loader.hideLoader(this@Activity_VehicleViewEditUpdate)
                    val dataResp = response.body()
                    if (dataResp != null) {
                        showToast(this@Activity_VehicleViewEditUpdate, getString(R.string.update_successful))
                        this@Activity_VehicleViewEditUpdate.finish()
                    } else
                        showToast(this@Activity_VehicleViewEditUpdate, "Fetching error!")
                } else {
                    loader.hideLoader(this@Activity_VehicleViewEditUpdate)
                    showToast(this@Activity_VehicleViewEditUpdate, "${getString(R.string.error_txt)}: ${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
                }
            } catch (e: Exception) {
                loader.hideLoader(this@Activity_VehicleViewEditUpdate)
                logger.error { e.message }
                showToast(this@Activity_VehicleViewEditUpdate, "${getString(R.string.exception_collen)} ${e.message}")
            }
        }
    }
    private fun deleteVehicle() {
        lifecycleScope.launch {
            try {
                loader.showLoader(this@Activity_VehicleViewEditUpdate)
                val response = NetworkManager.apiService.deleteVehicle(vehicleID)
                if (response.isSuccessful) {
                    loader.hideLoader(this@Activity_VehicleViewEditUpdate)
                    val deleted = response.body()
                    if (deleted != null && deleted) {
                        showToast(this@Activity_VehicleViewEditUpdate, getString(R.string.delete_successful))
                        this@Activity_VehicleViewEditUpdate.finish()
                    } else
                        showToast(this@Activity_VehicleViewEditUpdate, "Fetching error!")
                } else {
                    loader.hideLoader(this@Activity_VehicleViewEditUpdate)
                    showToast(this@Activity_VehicleViewEditUpdate, "${getString(R.string.error_txt)}: ${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
                }
            } catch (e: Exception) {
                loader.hideLoader(this@Activity_VehicleViewEditUpdate)
                logger.error { e.message }
                showToast(this@Activity_VehicleViewEditUpdate, "${getString(R.string.exception_collen)} ${e.message}")
            }
        }
    }
}
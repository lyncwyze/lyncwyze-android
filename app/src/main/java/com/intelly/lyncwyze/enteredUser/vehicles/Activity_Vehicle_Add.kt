package com.intelly.lyncwyze.enteredUser.vehicles

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.intelly.lyncwyze.Assest.Loader
import com.intelly.lyncwyze.Assest.modals.DataCountResp
import com.intelly.lyncwyze.Assest.modals.Vehicle
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import com.intelly.lyncwyze.Assest.utilities.HttpUtilities
import com.intelly.lyncwyze.Assest.utilities.SharedPreferencesManager
import com.intelly.lyncwyze.Assest.utilities.UserRequiredDataCount
import com.intelly.lyncwyze.Assest.utilities.Utilities
import com.intelly.lyncwyze.Assest.utilities.showToast
import com.intelly.lyncwyze.R
import com.intelly.lyncwyze.enteredUser.emergencyContacts.Activity_EmergencyContact_Add
import kotlinx.coroutines.launch
import mu.KotlinLogging


class Activity_Vehicle_Add : AppCompatActivity() {
    private val logger = KotlinLogging.logger {}
    private val loader = Loader(this)

    private val addVehicle: Vehicle = Vehicle.default()

    private lateinit var backButton: ImageView
    private lateinit var logoutButton: ImageButton
    private lateinit var inputMaker: EditText
    private lateinit var inputVehicleModel: EditText
    private lateinit var vaRadioGroupVehicleType: RadioGroup
    private lateinit var inputVehicleColor: EditText
    private lateinit var inputVehicleLicensePlate: EditText
    private lateinit var inputVehicleAlias: EditText
    private lateinit var checkboxPrimaryVehicle: CheckBox
    private lateinit var vaSaveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_vehicle_add)
        setupUI()
        setUpFunctionality()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupUI() {
        backButton = findViewById(R.id.vaBackButton)
        logoutButton = findViewById(R.id.logoutButton)
        logoutButton.visibility = View.GONE

        inputMaker = findViewById(R.id.inputMaker)
        inputVehicleModel = findViewById(R.id.inputVehicleModel)
        vaRadioGroupVehicleType = findViewById(R.id.vaRadioGroupVehicleType)
        inputVehicleColor = findViewById(R.id.inputVehicleColor)
        inputVehicleLicensePlate = findViewById(R.id.inputVehicleLicensePlate)
        inputVehicleAlias = findViewById(R.id.inputVehicleAlias)
        checkboxPrimaryVehicle = findViewById(R.id.checkboxPrimaryVehicle)
        vaSaveButton = findViewById(R.id.vaSaveButton)
        if (SharedPreferencesManager.getObject<DataCountResp>(UserRequiredDataCount)!!.vehicle < 1) {
            vaSaveButton.text = "Next: Add Emergency Contact (7/8)"
            logoutButton.visibility = View.VISIBLE
        }
    }
    private fun setUpFunctionality() {
        backButton.setOnClickListener { this.onBackPressed() }
        logoutButton.setOnClickListener {
            Utilities.letLogout(this)
        }
        vaSaveButton.setOnClickListener { saveVehicle() }
    }

    private fun saveVehicle() {
        if (validateVehicleData()) {
            lifecycleScope.launch {
                try {
                    loader.showLoader(this@Activity_Vehicle_Add)
                    val response = NetworkManager.apiService.addVehicle(body = addVehicle)
                    if (response.isSuccessful) {
                        loader.hideLoader(this@Activity_Vehicle_Add)
                        response.body()?.let {
                            showToast(this@Activity_Vehicle_Add, getString(R.string.saved_successfully))

                            val dataCount = SharedPreferencesManager.getObject<DataCountResp>(UserRequiredDataCount)!!
                            dataCount.vehicle += 1;
                            SharedPreferencesManager.saveObject(UserRequiredDataCount, dataCount)
                            if (dataCount.emergencyContact < 1)
                                startActivity(Intent(this@Activity_Vehicle_Add, Activity_EmergencyContact_Add::class.java))
                            else this@Activity_Vehicle_Add.finish()
                        } ?: showToast(this@Activity_Vehicle_Add, getString(R.string.fetching_error))
                    } else {
                        loader.hideLoader(this@Activity_Vehicle_Add)
                        showToast(this@Activity_Vehicle_Add, "${getString(R.string.error_txt)}: ${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
                    }
                } catch (e: Exception) {
                    loader.hideLoader(this@Activity_Vehicle_Add)
                    showToast(this@Activity_Vehicle_Add, "${getString(R.string.exception_collen)} ${e.message}")
                }
            }
        }
    }
    private fun validateVehicleData(): Boolean {
        if (inputMaker.text.isNullOrEmpty()) {
            inputMaker.error = getString(R.string.please_enter_the_vehicle_make)
            inputMaker.requestFocus()
            return false
        } else
            addVehicle.make = inputMaker.text.toString().trim()

        if (inputVehicleModel.text.isNullOrEmpty()) {
            inputVehicleModel.error = getString(R.string.please_select_the_vehicle_model)
            inputVehicleModel.requestFocus()
            return false
        } else
            addVehicle.model = inputVehicleModel.text.toString().trim()

        val selectedVehicleTypeId = vaRadioGroupVehicleType.checkedRadioButtonId
        if (selectedVehicleTypeId == -1) {
            showToast(this, getString(R.string.please_select_a_vehicle_type))
            return false
        } else
            addVehicle.bodyStyle = findViewById<RadioButton>(selectedVehicleTypeId).tag.toString()

        if (inputVehicleColor.text.isNullOrEmpty()) {
            inputVehicleColor.error = getString(R.string.please_enter_the_vehicle_color)
            inputVehicleColor.requestFocus()
            return false
        } else
            addVehicle.bodyColor = inputVehicleColor.text.toString().trim()

        if (inputVehicleLicensePlate.text.isNullOrEmpty()) {
            inputVehicleLicensePlate.error = getString(R.string.please_enter_the_license_plate)
            inputVehicleLicensePlate.requestFocus()
            return false
        } else
            addVehicle.licensePlate = inputVehicleLicensePlate.text.toString().trim()

        addVehicle.alias = inputVehicleAlias.text.toString().trim()

        addVehicle.seatingCapacity = 5
        addVehicle.primary = checkboxPrimaryVehicle.isChecked
        return true
    }

}
package com.intelly.lyncwyze.enteredUser.emergencyContacts

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
import com.intelly.lyncwyze.Assest.modals.EmergencyContact
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import com.intelly.lyncwyze.Assest.utilities.CONTACT_ID
import com.intelly.lyncwyze.Assest.utilities.HttpUtilities
import com.intelly.lyncwyze.Assest.utilities.showToast
import com.intelly.lyncwyze.R
import kotlinx.coroutines.launch
import mu.KotlinLogging

class  Activity_EmergencyContact_List : AppCompatActivity() {
    private val logger = KotlinLogging.logger {}
    private var loader = Loader(this)

    private lateinit var backButton: ImageView
    private lateinit var scrollView: ScrollView
    private lateinit var theListNoContent: LinearLayout

    private lateinit var addMore: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_emergency_contact_list)
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
        fetchAllContacts()
    }


    private fun setupUI() {
        backButton = findViewById(R.id.aecBackButton)
        scrollView = findViewById(R.id.aecScrollView)
        theListNoContent = findViewById(R.id.aecNoList)
        addMore = findViewById(R.id.aecAddMoreBtn)
    }
    private fun setUpFunctionality() {
        backButton.setOnClickListener { this.onBackPressed() }
        addMore.setOnClickListener { startActivity(Intent(this, Activity_EmergencyContact_Add::class.java)) }
    }
    private fun fetchAllContacts() {
        loader.showLoader(this@Activity_EmergencyContact_List)
        lifecycleScope.launch {
            try {
                val response = NetworkManager.apiService.getEmergencyContacts(pageSize = 1000, offSet = 0, sortOrder = "ASC")
                if (response.isSuccessful) {
                    loader.hideLoader(this@Activity_EmergencyContact_List)
                    val dataList = response.body()
                    if (dataList != null)
                        displayChildrenList(dataList.data)
                    else
                        showToast(this@Activity_EmergencyContact_List, getString(R.string.fetching_error))
                } else {
                    loader.hideLoader(this@Activity_EmergencyContact_List)
                    showToast(this@Activity_EmergencyContact_List, "${getString(R.string.error_txt)}: ${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
                }
            } catch (e: Exception) {
                loader.hideLoader(this@Activity_EmergencyContact_List)
                showToast(this@Activity_EmergencyContact_List, "${getString(R.string.exception_collen)} ${e.message}")
            }
        }
    }
    private fun displayChildrenList(theList: List<EmergencyContact>) {
        val emCListLayout = findViewById<LinearLayout>(R.id.aecList)
        emCListLayout.removeAllViews()

        if (theList.isNotEmpty()) {
            for (emCont in theList) {
                val itemView = layoutInflater.inflate(R.layout.child_emergency_contact_item_layout, emCListLayout, false)

                val textViewName = itemView.findViewById<TextView>(R.id.ecFullName)
                "${emCont.firstName} ${emCont.lastName}".also { textViewName.text = it }

                val ecNumber = itemView.findViewById<TextView>(R.id.ecNumber)
                ecNumber.text = emCont.mobileNumber

                val ecEmail = itemView.findViewById<TextView>(R.id.ecEmail)
                if (!emCont.email.isNullOrBlank()) {
                    ecEmail.text = emCont.email
                    ecEmail.visibility = View.VISIBLE
                } else ecEmail.visibility = View.GONE

                val viewEmergencyContact = itemView.findViewById<ImageView>(R.id.viewEmergencyContact)
                viewEmergencyContact.setOnClickListener {
                    val intent = Intent(this, Activity_EmergencyContact_Edit::class.java)
                        .apply { putExtra(CONTACT_ID, emCont.id) }
                    startActivity(intent)
                }
                emCListLayout.addView(itemView)
            }
            scrollView.visibility = View.VISIBLE
            theListNoContent.visibility = View.INVISIBLE
        } else {
            showToast(this@Activity_EmergencyContact_List, getString(R.string.found_nothing))
            scrollView.visibility = View.INVISIBLE
            theListNoContent.visibility = View.VISIBLE
        }
    }
}
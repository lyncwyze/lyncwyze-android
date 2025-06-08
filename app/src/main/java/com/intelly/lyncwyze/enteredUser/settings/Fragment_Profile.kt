package com.intelly.lyncwyze.enteredUser.settings

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.intelly.lyncwyze.Assest.Loader
import com.intelly.lyncwyze.Assest.modals.UserProfile
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import com.intelly.lyncwyze.Assest.utilities.HttpUtilities
import com.intelly.lyncwyze.Assest.utilities.ImageUtilities
import com.intelly.lyncwyze.Assest.utilities.SharedPreferencesManager
import com.intelly.lyncwyze.Assest.utilities.formatDateTime
import com.intelly.lyncwyze.Assest.utilities.showToast
import com.intelly.lyncwyze.R
import kotlinx.coroutines.launch
import mu.KotlinLogging


class Fragment_Profile : Fragment() {
    private val logger = KotlinLogging.logger {}
    private val loader = Loader(this)

    private var profileInfo: UserProfile? = null;

    private lateinit var userImage: ImageView
    private lateinit var firstName: TextView
    private lateinit var lastName: TextView
    private lateinit var userId: TextView
    private lateinit var email: TextView
    private lateinit var createdDate: TextView
    private lateinit var modifiedData: TextView
    private lateinit var mobileNumber: TextView
    private lateinit var line1: TextView
    private lateinit var line2: TextView
    private lateinit var city: TextView
    private lateinit var state: TextView
    private lateinit var landmark: TextView
    private lateinit var pinCode: TextView
    private lateinit var edit: Button


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment__profile, container, false)

        // Setup UI
        userImage = view.findViewById(R.id.userImage)
        firstName = view.findViewById(R.id.firstName)
        lastName = view.findViewById(R.id.lastName)
        userId = view.findViewById(R.id.userId)
        email = view.findViewById(R.id.email)
        createdDate = view.findViewById(R.id.createdDate)
        modifiedData = view.findViewById(R.id.modifiedData)
        mobileNumber = view.findViewById(R.id.mobileNumber)
        line1 = view.findViewById(R.id.line1)
        line2 = view.findViewById(R.id.line2)
        city = view.findViewById(R.id.city)
        state = view.findViewById(R.id.state)
        landmark = view.findViewById(R.id.landmark)
        pinCode = view.findViewById(R.id.pinCode)
        edit = view.findViewById(R.id.edit)
        edit.setOnClickListener { openEditScreen() }
        return view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchProfileDetails()
    }

    private fun fetchProfileDetails() {
        lifecycleScope.launch {
            try {
                if (!isAdded) return@launch
                loader.showLoader()
                val response = NetworkManager.apiService.getUserById(SharedPreferencesManager.getDecodedAccessToken()!!.userId)
                if (!isAdded) return@launch
                if (response.isSuccessful) {
                    if (view != null) loader.hideLoader()
                    response.body()?.let {
                        profileInfo = it
                        displayInfo()
                    }
                } else {
                    if (view != null) loader.hideLoader()
                    if (context != null) {
                        showToast(requireContext(), "${getString(R.string.error_txt)}: ${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
                    }
                }
            } catch (e: Exception) {
                if (view != null) loader.hideLoader()
                if (context != null) {
                    showToast(requireContext(), e.message.toString())
                }
            }
        }
    }
    private fun displayInfo() {
        profileInfo?.let {
            firstName.text = "${it.firstName} ${it.lastName}"
            lastName.text = it.lastName
            lastName.visibility = View.GONE
            userId.text = it.id
            email.text = it.email
            createdDate.text = formatDateTime(it.createdDate, "dd/MM/YYYY hh:mm a")
            modifiedData.text = formatDateTime(it.modifiedDate, "dd/MM/YYYY hh:mm a")
            mobileNumber.text = it.mobileNumber
            it.image?.let { endpoint ->
                lifecycleScope.launch {
                    ImageUtilities.imageFullPath(endpoint)?.let { base64Data ->
                        try {
                            val decodedString = Base64.decode(base64Data, Base64.DEFAULT)
                            val bitmap: Bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                            Glide.with(requireContext()).load(bitmap).into(userImage)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            userImage.setImageResource(R.drawable.user) // Fallback image on error
                        }
                    } ?: run { userImage.setImageResource(R.drawable.user) }
                }
            }
            if (it.addresses.isNotEmpty()) {
                val address = it.addresses[0] // Assuming one address for simplicity
                
                // Only show and set text for non-empty address fields
                if (!address.addressLine1.isNullOrBlank()) {
                    line1.text = address.addressLine1
                    line1.visibility = View.VISIBLE
                } else {
                    line1.visibility = View.GONE
                }

                if (!address.addressLine2.isNullOrBlank()) {
                    line2.text = address.addressLine2
                    line2.visibility = View.VISIBLE
                } else {
                    line2.visibility = View.GONE
                }

                if (!address.city.isNullOrBlank()) {
                    city.text = address.city
                    city.visibility = View.VISIBLE
                } else {
                    city.visibility = View.GONE
                }

                if (!address.state.isNullOrBlank()) {
                    state.text = address.state
                    state.visibility = View.VISIBLE
                } else {
                    state.visibility = View.GONE
                }

                if (!address.landMark.isNullOrBlank()) {
                    landmark.text = address.landMark
                    landmark.visibility = View.VISIBLE
                } else {
                    landmark.visibility = View.GONE
                }

                if (address.pincode != null) {
                    pinCode.text = address.pincode.toString()
                    pinCode.visibility = View.VISIBLE
                } else {
                    pinCode.visibility = View.GONE
                }
            }
        } ?: {
            showToast(requireContext(), getString(R.string.no_data_found))
            requireActivity().onBackPressed()
        }
    }
    private fun openEditScreen() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, Fragment_ProfileEdit())
            .addToBackStack("Fragment_ProfileEdit").commit()
    }
}
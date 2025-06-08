package com.intelly.lyncwyze.enteredUser.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.intelly.lyncwyze.Assest.utilities.ARG_PARAM1
import com.intelly.lyncwyze.Assest.utilities.Utilities
import com.intelly.lyncwyze.R


class Fragment_Settings : Fragment() {
    private var userEmail: String? = null

    private lateinit var myProfile: LinearLayout
    private lateinit var faqDirect: LinearLayout
    private lateinit var logoutMe: LinearLayout

    companion object {
        @JvmStatic
        fun newInstance(param1: String) = Fragment_Settings().apply { arguments = Bundle().apply { putString(ARG_PARAM1, param1) } }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { userEmail = it.getString(ARG_PARAM1) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment__settings, container, false)
        myProfile = view.findViewById(R.id.myProfile)
        faqDirect = view.findViewById(R.id.faqDirect)
        logoutMe = view.findViewById(R.id.logoutMe)
        setUpFunctionality()
        return view
    }

    private fun setUpFunctionality() {
        myProfile.setOnClickListener { openProfileFragment() }
        faqDirect.setOnClickListener { openFaqFragment() }
        logoutMe.setOnClickListener {
            Utilities.letLogout(requireContext())
        }
    }

    private fun openProfileFragment() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, Fragment_Profile())
            .addToBackStack("FragmentProfile").commit()
    }
    private fun openFaqFragment() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, Fragment_FAQ())
            .addToBackStack("FragmentFaq").commit()
    }
}
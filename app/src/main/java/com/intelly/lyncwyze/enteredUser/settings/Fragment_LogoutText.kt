package com.intelly.lyncwyze.enteredUser.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.intelly.lyncwyze.Assest.utilities.Utilities
import com.intelly.lyncwyze.R

class Fragment_LogoutText : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_logout_text, container, false)
        view.findViewById<TextView>(R.id.theLogoutBtn).setOnClickListener { Utilities.letLogout(requireContext()) }
        return view
    }
}
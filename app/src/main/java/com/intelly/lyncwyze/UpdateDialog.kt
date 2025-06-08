package com.intelly.lyncwyze

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.Window
import android.widget.Button

class UpdateDialog(context: Context) : Dialog(context) {
    companion object {
        private var hasShownLater = false
        
        fun shouldShowDialog(): Boolean {
            return !hasShownLater
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_update)

        // Set transparent background for the dialog window
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Prevent dismissing the dialog
        setCancelable(false)
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        // Find buttons
        val btnUpdate: Button = findViewById(R.id.btnUpdate)
        val btnLater: Button = findViewById(R.id.btnLater)

        // Handle Later button click
        btnLater.setOnClickListener {
            hasShownLater = true
            dismiss()
        }

        // Redirect to Play Store on Update button click
        btnUpdate.setOnClickListener {
            val appPackageName = context.packageName
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName"))
                context.startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName"))
                context.startActivity(intent)
            }
        }
    }

    // Disable back button press
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_BACK) {
            true // Consume the event and do nothing
        } else {
            super.onKeyDown(keyCode, event)
        }
    }
}
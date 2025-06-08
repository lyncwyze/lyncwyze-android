package com.intelly.lyncwyze.Assest

import android.content.res.Configuration
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.intelly.lyncwyze.R

class Loader(private val context: Any) {
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var progressBar: ProgressBar

    fun showLoader() {
        when (context) {
            is AppCompatActivity -> showLoader(context)
            is View -> showLoader(context)
            is Fragment -> showLoader(context.requireView())
            else -> throw IllegalArgumentException("Unsupported context type: ${context.javaClass.simpleName}")
        }
    }

    fun hideLoader() {
        when (context) {
            is AppCompatActivity -> hideLoader(context)
            is View -> hideLoader(context)
            is Fragment -> hideLoader(context.requireView())
            else -> throw IllegalArgumentException("Unsupported context type: ${context.javaClass.simpleName}")
        }
    }

    private fun isDarkMode(activity: AppCompatActivity): Boolean {
        return activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    private fun isDarkMode(view: View): Boolean {
        return view.context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    private fun getThemeBackgroundColor(activity: AppCompatActivity): Int {
        val typedValue = TypedValue()
        activity.theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
        return typedValue.data
    }

    private fun getThemeBackgroundColor(view: View): Int {
        val typedValue = TypedValue()
        view.context.theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
        return typedValue.data
    }

    fun showLoader(activity: AppCompatActivity) {
        if (::loadingOverlay.isInitialized && loadingOverlay.parent != null) {
            return
        }
        
        val backgroundColor = getThemeBackgroundColor(activity)

        loadingOverlay = FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply { gravity = Gravity.CENTER }
            setBackgroundColor(backgroundColor)
        }

        val progressColor = if (isDarkMode(activity)) {
            ContextCompat.getColor(activity, android.R.color.white)
        } else {
            ContextCompat.getColor(activity, android.R.color.black)
        }

        progressBar = ProgressBar(activity).apply {
            indeterminateDrawable.setColorFilter(progressColor, android.graphics.PorterDuff.Mode.SRC_IN)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER }
            isIndeterminate = true
            visibility = View.VISIBLE
        }
        loadingOverlay.addView(progressBar)
        activity.addContentView(loadingOverlay, loadingOverlay.layoutParams)
    }

    fun hideLoader(activity: AppCompatActivity) {
        if (::loadingOverlay.isInitialized && loadingOverlay.parent != null)
            (activity.findViewById<ViewGroup>(android.R.id.content)).removeView(loadingOverlay)
    }

    fun showLoader(view: View) {
        if (::loadingOverlay.isInitialized && loadingOverlay.parent != null)
            return

        val backgroundColor = getThemeBackgroundColor(view)

        loadingOverlay = FrameLayout(view.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply { gravity = Gravity.CENTER }
            setBackgroundColor(backgroundColor)
        }

        val progressColor = if (isDarkMode(view)) {
            ContextCompat.getColor(view.context, android.R.color.white)
        } else {
            ContextCompat.getColor(view.context, android.R.color.black)
        }

        progressBar = ProgressBar(view.context).apply {
            indeterminateDrawable.setColorFilter(progressColor, android.graphics.PorterDuff.Mode.SRC_IN)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER }
            isIndeterminate = true
            visibility = View.VISIBLE
        }
        loadingOverlay.addView(progressBar)
        (view.parent as? ViewGroup)?.addView(loadingOverlay)
    }

    fun hideLoader(view: View) {
        if (::loadingOverlay.isInitialized && loadingOverlay.parent != null) {
            (view.parent as? ViewGroup)?.removeView(loadingOverlay)
        }
    }
}
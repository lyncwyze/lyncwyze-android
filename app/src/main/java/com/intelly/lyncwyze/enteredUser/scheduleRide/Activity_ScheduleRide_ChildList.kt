package com.intelly.lyncwyze.enteredUser.scheduleRide

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.intelly.lyncwyze.Assest.Loader
import com.intelly.lyncwyze.Assest.modals.SaveChild
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import com.intelly.lyncwyze.Assest.utilities.CHILD_ID
import com.intelly.lyncwyze.Assest.utilities.CHILD_NAME
import com.intelly.lyncwyze.Assest.utilities.HttpUtilities
import com.intelly.lyncwyze.Assest.utilities.ImageUtilities
import com.intelly.lyncwyze.Assest.utilities.showToast
import com.intelly.lyncwyze.R
import kotlinx.coroutines.launch
import mu.KotlinLogging

class Activity_ScheduleRide_ChildList : AppCompatActivity() {
    private val logger = KotlinLogging.logger { }
    private var loader = Loader(this)

    private lateinit var backButton: ImageView
    private lateinit var scrollView: ScrollView
    private lateinit var theListNoContent: LinearLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_schedule_ride_child_list)
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
        fetchAllChildren()
    }

    private fun setupUI() {
        backButton = findViewById(R.id.aclBackButton)
        scrollView = findViewById(R.id.aclScrollView)
        theListNoContent = findViewById(R.id.aclNoList)
    }

    private fun setUpFunctionality() {
        backButton.setOnClickListener { this.onBackPressed() }
    }

    private fun fetchAllChildren() {
        loader.showLoader(this@Activity_ScheduleRide_ChildList)
        lifecycleScope.launch {
            try {
                val response = NetworkManager.apiService.getChildren(
                    pageSize = 1000,
                    offSet = 0,
                    sortOrder = "ASC"
                )
                if (response.isSuccessful) {
                    loader.hideLoader(this@Activity_ScheduleRide_ChildList)
                    val childList = response.body()
                    if (childList != null)
                        displayChildrenList(childList.data)
                    else
                        showToast(this@Activity_ScheduleRide_ChildList, "Fetching error!")
                } else {
                    loader.hideLoader(this@Activity_ScheduleRide_ChildList)
                    showToast(
                        this@Activity_ScheduleRide_ChildList,
                        "${getString(R.string.error_txt)}: ${response.code()}, ${
                            HttpUtilities.parseError(response)?.errorInformation?.errorDescription
                        }"
                    )
                }
            } catch (e: Exception) {
                logger.error { e.message }
                loader.hideLoader(this@Activity_ScheduleRide_ChildList)
                showToast(this@Activity_ScheduleRide_ChildList, "Exception: ${e.message}")
            }
        }
    }

    private fun displayChildrenList(children: List<SaveChild>) {
        val childListLayout = findViewById<LinearLayout>(R.id.aclChildList)
        childListLayout.removeAllViews()

        if (children.isNotEmpty()) {
            for (child in children) {
                val childItemView = layoutInflater.inflate(R.layout.layout_child_item_with_edit, childListLayout, false)

                childItemView.findViewById<ImageView>(R.id.viewChild).visibility = View.GONE

                val textViewName = childItemView.findViewById<TextView>(R.id.textViewChildName)
                textViewName.text = "${child.firstName} ${child.lastName}"

                val childGender = childItemView.findViewById<TextView>(R.id.childGender)
                childGender.text = child.gender.name

                // Inside your Activity or Fragment
                val imageViewChild = childItemView.findViewById<ImageView>(R.id.imageViewChildImg)
                child.image?.let { endpoint ->
                    lifecycleScope.launch {
                        ImageUtilities.imageFullPath(endpoint)?.let { base64Data ->
                            try {
                                val decodedString = Base64.decode(base64Data, Base64.DEFAULT)
                                val bitmap: Bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                                Glide.with(this@Activity_ScheduleRide_ChildList).load(bitmap).into(imageViewChild)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                imageViewChild.setImageResource(R.drawable.user) // Fallback image on error
                            }
                        } ?: run { imageViewChild.setImageResource(R.drawable.user) }
                    }
                }
                childItemView.setOnClickListener {
                    val intent = Intent(this, Activity_ScheduleRide_Child_ActivitiesList::class.java)
                        .apply {
                            putExtra(CHILD_ID, child.id)
                            putExtra(CHILD_NAME, "${child.firstName} ${child.lastName}")
                        }
                    startActivity(intent)
                }
                childListLayout.addView(childItemView)
            }
            scrollView.visibility = View.VISIBLE
            theListNoContent.visibility = View.INVISIBLE
        }
    }
}

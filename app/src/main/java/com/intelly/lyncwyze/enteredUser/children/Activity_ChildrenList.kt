package com.intelly.lyncwyze.enteredUser.children

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.intelly.lyncwyze.Assest.Loader
import com.intelly.lyncwyze.Assest.modals.SaveChild
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import com.intelly.lyncwyze.Assest.utilities.CHILD_ID
import com.intelly.lyncwyze.Assest.utilities.HttpUtilities
import com.intelly.lyncwyze.Assest.utilities.ImageUtilities
import com.intelly.lyncwyze.Assest.utilities.showToast
import com.intelly.lyncwyze.R
import kotlinx.coroutines.launch
import mu.KotlinLogging

class Activity_ChildrenList : AppCompatActivity() {
    private val logger = KotlinLogging.logger { }
    private var loader = Loader(this)

    private lateinit var backButton: ImageView
    private lateinit var scrollView: ScrollView
    private lateinit var theListNoContent: LinearLayout
    private lateinit var addMore: Button
    private lateinit var noContentImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_children_list)
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
        addMore = findViewById(R.id.aclAddMore)
        noContentImage = findViewById(R.id.aclNoContentImage)

        // Handle dark mode for images
        updateImagesForTheme()
    }

    private fun updateImagesForTheme() {
        val isNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        
        // Set tint for back button
        val tintColor = if (isNightMode) {
            ContextCompat.getColor(this, android.R.color.white)
        } else {
            ContextCompat.getColor(this, android.R.color.black)
        }
        
        backButton.setColorFilter(tintColor)
        noContentImage.setColorFilter(tintColor)
    }

    private fun setUpFunctionality() {
        backButton.setOnClickListener { this.onBackPressed() }
        addMore.setOnClickListener { startActivity(Intent(this, Activity_Child_Add::class.java)) }
    }

    private fun fetchAllChildren() {
        loader.showLoader(this@Activity_ChildrenList)
        lifecycleScope.launch {
            try {
                val response = NetworkManager.apiService.getChildren(pageSize = 1000, offSet = 0, sortOrder = "ASC")
                if (response.isSuccessful) {
                    loader.hideLoader(this@Activity_ChildrenList)
                    val childList = response.body()
                    if (childList != null)
                        displayChildrenList(childList.data)
                    else
                        showToast(this@Activity_ChildrenList, "Fetching error!")
                } else {
                    loader.hideLoader(this@Activity_ChildrenList)
                    showToast(this@Activity_ChildrenList, "${getString(R.string.error_txt)}: ${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
                }
            } catch (e: Exception) {
                logger.error { e.message }
                loader.hideLoader(this@Activity_ChildrenList)
                showToast(this@Activity_ChildrenList, "Exception: ${e.message}")
            }
        }
    }

    private fun displayChildrenList(children: List<SaveChild>) {
        val childListLayout = findViewById<LinearLayout>(R.id.aclChildList)
        childListLayout.removeAllViews()

        if (children.isNotEmpty()) {
            scrollView.visibility = View.VISIBLE
            theListNoContent.visibility = View.INVISIBLE

            for (child in children) {
                val childItemView = layoutInflater.inflate(R.layout.layout_child_item_with_edit, childListLayout, false)

                childItemView.findViewById<TextView>(R.id.textViewChildName).text = "${child.firstName} ${child.lastName}"
                childItemView.findViewById<TextView>(R.id.childGender).text = child.gender.name
                val imageViewChild = childItemView.findViewById<ImageView>(R.id.imageViewChildImg)
                val editIcon = childItemView.findViewById<ImageView>(R.id.viewChild)

                // Apply theme-aware tint to edit icon
                val isNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                if (isNightMode) {
                    editIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.white))
                }

                child.image?.let { endpoint ->
                    lifecycleScope.launch {
                        ImageUtilities.imageFullPath(endpoint)?.let { base64Data ->
                            try {
                                val decodedString = Base64.decode(base64Data, Base64.DEFAULT)
                                val bitmap: Bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                                Glide.with(this@Activity_ChildrenList)
                                    .load(bitmap)
                                    .into(imageViewChild)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                // Set fallback image with appropriate tint for dark mode
                                imageViewChild.setImageResource(R.drawable.user)
                                if (isNightMode) {
                                    imageViewChild.setColorFilter(ContextCompat.getColor(this@Activity_ChildrenList, android.R.color.white))
                                } else {

                                }
                            }
                        } ?: run {
                            // Set fallback image with appropriate tint for dark mode
                            imageViewChild.setImageResource(R.drawable.user)
                            if (isNightMode) {
                                imageViewChild.setColorFilter(ContextCompat.getColor(this@Activity_ChildrenList, android.R.color.white))
                            }
                        }
                    }
                }

                editIcon.setOnClickListener {
                    startActivity(Intent(this, Activity_Child_Edit::class.java).apply { putExtra(CHILD_ID, child.id) })
                }
                childListLayout.addView(childItemView)
            }
        } else {
            scrollView.visibility = View.INVISIBLE
            theListNoContent.visibility = View.VISIBLE
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Update UI elements when theme changes
        updateImagesForTheme()
    }
}
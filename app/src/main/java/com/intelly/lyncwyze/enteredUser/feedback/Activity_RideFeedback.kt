package com.intelly.lyncwyze.enteredUser.feedback

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.iterator
import androidx.lifecycle.lifecycleScope
import com.intelly.lyncwyze.Assest.Loader
import com.intelly.lyncwyze.Assest.modals.FeedBackPreReq
import com.intelly.lyncwyze.Assest.modals.SurveyReport
import com.intelly.lyncwyze.Assest.modals.feedBackRequired
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import com.intelly.lyncwyze.Assest.utilities.SharedPreferencesManager
import com.intelly.lyncwyze.Assest.utilities.convertFromFormattedToIso
import com.intelly.lyncwyze.Assest.utilities.convertToFormattedDate
import com.intelly.lyncwyze.Assest.utilities.formatDate
import com.intelly.lyncwyze.Assest.utilities.formatDateTime
import com.intelly.lyncwyze.Assest.utilities.getLocalizedText
import com.intelly.lyncwyze.Assest.utilities.showToast
import com.intelly.lyncwyze.R
import com.intelly.lyncwyze.enteredUser.Dashboard
import kotlinx.coroutines.launch
import mu.KotlinLogging


class Activity_RideFeedback : AppCompatActivity() {
    private val logger = KotlinLogging.logger { }
    private var loader = Loader(this)

    private lateinit var backBtn: ImageButton

    private lateinit var averageRatingScore: TextView
    private lateinit var averageRatingBar: RatingBar

    private lateinit var feedBackCategories: LinearLayout
    private lateinit var reviewText: EditText

    private lateinit var favButton: Button
    private lateinit var submitBtn: Button
    private var rideDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ride_feedback)
        activityCheck()
        setupUI()
        setupFunctionality()
        getReviewPoints()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private lateinit var reviewPoints: SurveyReport
    private lateinit var feedBackPreReq: FeedBackPreReq

    private fun activityCheck() {
        SharedPreferencesManager.getObject<FeedBackPreReq>(feedBackRequired)?.let {
            feedBackPreReq = it
        } ?: {
            showToast(this, "Feedback pre-requisite does not match!")
            this.onBackPressed()
        }
    }


    private fun getReviewPoints() {
        lifecycleScope.launch {
            try {
                val data = SurveyReport(
                    id = null,
                    rideId = feedBackPreReq.rideId,
                    reviewerId = feedBackPreReq.fromUserId,
                    revieweeId = feedBackPreReq.fromUserId,
                    reviewerRole = feedBackPreReq.riderType,
                    ratings = mutableMapOf(),
                    favorite = false,
                    comments = ""
                )
                loader.showLoader()
                val response = NetworkManager.apiService.getReview(data)
                if (response.isSuccessful) {
                    loader.hideLoader()
                    response.body()?.let {
                        reviewPoints = it
                        setFeedbackCategories()
                        updateFav()
                        reviewText.setText(reviewPoints.comments)
                    } ?: showToast(this@Activity_RideFeedback, getString(R.string.no_data_found))
                } else {
                    loader.hideLoader()
                    showToast(this@Activity_RideFeedback, "${getString(R.string.error_txt)}: ${response.code()}")
                }
            } catch (e: Exception) {
                loader.hideLoader(this@Activity_RideFeedback)
                showToast(this@Activity_RideFeedback, "${getString(R.string.exception_collen)} ${e.message}")
            }
        }
    }

    private fun setupUI() {
        backBtn = findViewById(R.id.surveyBackBtn)
        rideDate = intent.getStringExtra("ride_date")
        if (rideDate != null) {
            rideDate = formatDate(rideDate!!)
        }
        findViewById<TextView>(R.id.averageRatingText).text = getString(R.string.how_was_your_ride_with, feedBackPreReq.forUserName, rideDate)

        averageRatingBar = findViewById(R.id.averageRatingBar)
        averageRatingScore = findViewById(R.id.averageRatingScore)
        feedBackCategories = findViewById(R.id.feedBackCategories)

        reviewText = findViewById(R.id.reviewText);

        favButton = findViewById(R.id.favButton);
        submitBtn = findViewById(R.id.submitBtn);
    }
    private fun setupFunctionality() {
        backBtn.setOnClickListener {
            val intent = Intent(this, Dashboard::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }
        favButton.setOnClickListener {
            reviewPoints.favorite = !reviewPoints.favorite
            updateFav()
        }
        submitBtn.setOnClickListener { submitForm() }
    }

    private fun updateFav() {
        if (reviewPoints.favorite) {
            favButton.text = getString(R.string.unfavorite)
            favButton.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(this@Activity_RideFeedback, R.drawable.fav_yes), null, null, null)
        } else {
            favButton.text = getString(R.string.favorite)
            favButton.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(this@Activity_RideFeedback, R.drawable.fav_not), null, null, null)
        }
    }
    private fun submitForm() {
        lifecycleScope.launch {
            try {
                val data = SurveyReport(
                    id = reviewPoints.id,
                    rideId = feedBackPreReq.rideId,
                    reviewerId = feedBackPreReq.fromUserId,
                    revieweeId = feedBackPreReq.forUserId,
                    reviewerRole = feedBackPreReq.riderType,
                    comments = reviewText.text.trim().toString(),
                    favorite = reviewPoints.favorite,
                    ratings = getLinearLayoutElements()
                )
                if (data.ratings.isEmpty()) {
                    showToast(this@Activity_RideFeedback, "Please provide ratings for all categories.")
                } else {
                    loader.showLoader(this@Activity_RideFeedback)
                    val response = NetworkManager.apiService.submitSurvey(data)
                    if (response.isSuccessful) {
                        loader.hideLoader(this@Activity_RideFeedback)
                        response.body()?.let {
                            showToast(this@Activity_RideFeedback, getString(R.string.saved_successful))
                            val intent = Intent(this@Activity_RideFeedback, Dashboard::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                        }
                    } else {
                        loader.hideLoader(this@Activity_RideFeedback)
                        showToast(this@Activity_RideFeedback, "${getString(R.string.error_txt)}: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                loader.hideLoader(this@Activity_RideFeedback)
                showToast(this@Activity_RideFeedback, "${getString(R.string.exception_collen)} ${e.message}")
            }
        }
    }
    private fun setFeedbackCategories() {
        feedBackCategories.removeAllViews()
        logger.info { reviewPoints.ratings }
        reviewPoints.ratings.let {
            for ((key, value) in it.entries) {
                val rideGiverItemView = layoutInflater.inflate(R.layout.layout_each_feedback_cat, feedBackCategories, false)

                val textCat = rideGiverItemView.findViewById<TextView>(R.id.textCat)
                val ratingBar = rideGiverItemView.findViewById<RatingBar>(R.id.rb)
                ratingBar.setOnRatingBarChangeListener { _, _, _ ->
                    getLinearLayoutElementsAndUpdateAverage()
                }

                textCat.text = getLocalizedText(this, key)
                textCat.tag = key
                ratingBar.rating = value.toFloat()

                feedBackCategories.addView(rideGiverItemView)
            }
        }
    }

    private fun getLinearLayoutElements(): MutableMap<String, Int> {
        val elementsMap = mutableMapOf<String, Int>()

        for (linearLayout in feedBackCategories) {
            val textCat = linearLayout.findViewById<TextView>(R.id.textCat)
            val rb = linearLayout.findViewById<RatingBar>(R.id.rb)

            elementsMap[textCat.tag.toString()] = rb.rating.toInt()
        }
        return elementsMap
    }
    private fun getLinearLayoutElementsAndUpdateAverage() {
        val elementsMap = mutableMapOf<String, Int>()

        for (i in 0 until feedBackCategories.childCount) {
            val linearLayout = feedBackCategories.getChildAt(i) as LinearLayout
            val textCat = linearLayout.findViewById<TextView>(R.id.textCat)
            val rb = linearLayout.findViewById<RatingBar>(R.id.rb)

            elementsMap[textCat.text.toString()] = rb.rating.toInt()
        }


        val average = if (elementsMap.isNotEmpty()) elementsMap.values.average().toFloat() else 0.0f
        val formattedAverage = String.format("%.2f", average)
        averageRatingBar.rating = formattedAverage.toFloat()
        averageRatingScore.text = "${getString(R.string.average_rating)} $formattedAverage"
    }
}
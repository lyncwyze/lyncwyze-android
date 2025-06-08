package com.intelly.lyncwyze.Assest.utilities

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.util.Patterns
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import androidx.core.text.isDigitsOnly
import com.google.firebase.messaging.FirebaseMessaging
import com.intelly.lyncwyze.Assest.Loader
import com.intelly.lyncwyze.Assest.modals.Gender
import com.intelly.lyncwyze.Assest.modals.RideType
import com.intelly.lyncwyze.Assest.modals.RiderType
import com.intelly.lyncwyze.Assest.modals.WeekDays
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import com.intelly.lyncwyze.R
import com.intelly.lyncwyze.enteringUser.signIn.Activity_LoginWithEmail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Utilities {
    private val logger = KotlinLogging.logger { }

    // Validate email format
    fun isValidEmail(email: String?): Boolean {
        if (email.isNullOrBlank())
            return false
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    // Validate password as per criteria
    fun isPasswordValid(password: String?): Boolean {
        if (password.isNullOrBlank())
            return false
        return password.length >= 6 //TODO: Add regex for password validation
    }
    // Validate phone number
    fun isPhoneNumberValid(phoneNumber: String?): Boolean {
        if (phoneNumber.isNullOrBlank())
            return false
        return phoneNumber.isDigitsOnly()
//        return phoneNumber.matches(Regex("^\\+?[0-9]{10,15}\$"))  // Keep it use format
    }

    // Validate if a string is not empty
    fun isNotEmpty(input: String): Boolean {
        return input.isNotEmpty()
    }

    // Clear EditText fields
    fun clearEditTexts(vararg editTexts: EditText) {
        for (editText in editTexts) {
            editText.text.clear()
        }
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    // Checks if the provided date string is a future date
    fun isFutureDate(dateString: String): Boolean {
        return try {
            val date = dateFormat.parse(dateString)
            date?.after(Date()) == true
        } catch (e: Exception) {
            false
        }
    }
    // Checks if the provided date string is a past date
    fun isPastDate(dateString: String): Boolean {
        return try {
            val date = dateFormat.parse(dateString)
            date?.before(Date()) == true
        } catch (e: Exception) {
            false // Return false for invalid date format
        }
    }

    // Register the device for notification
    fun getFCMToken(callback: FCMTokenCallback) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                callback.onTokenError()
                return@addOnCompleteListener
            }
            callback.onTokenReceived(task.result ?: "")
        }
    }
    fun letLogout(context: Context) {
        getFCMToken(object : FCMTokenCallback {
            override fun onTokenReceived(token: String) { logout(context, token) }
            override fun onTokenError() { logout(context, "") }
        })
    }
    private fun logout(context: Context, token: String) {
        val loader = Loader(context)
        // You might need to pass CoroutineScope as parameter or use GlobalScope carefully
        CoroutineScope(Dispatchers.Main).launch {
            try {
                loader.showLoader()
                val response = NetworkManager.apiService.invalidate(token, HashMap())
                if (response.isSuccessful && response.body() == true) {
                    loader.hideLoader()
                    SharedPreferencesManager.deleteAuthDetails()
                    showToast(context, context.getString(R.string.you_have_been_logged_out))
                    val intent = Intent(context, Activity_LoginWithEmail::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    context.startActivity(intent)
                }
                else {
                    loader.hideLoader()
                    showToast(context, context.getString(R.string.error_logging_out))
                }
            } catch (e: Exception) {
                logger.error { e.message.toString() }
                showToast(context, context.getString(R.string.error_logging_out))
                loader.hideLoader()
            }
        }
    }

    fun compressImage(context: Context, imageUri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            
            var quality = 100
            var outputStream: FileOutputStream
            var compressedFile: File
            var fileSize: Long
            
            do {
                compressedFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
                outputStream = FileOutputStream(compressedFile)
                
                originalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                outputStream.flush()
                outputStream.close()
                
                fileSize = compressedFile.length()
                quality -= 10
            } while (fileSize > 1024 * 1024 && quality > 10) // 1MB = 1024 * 1024 bytes
            
            originalBitmap.recycle()
            compressedFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

fun formatDate(inputDate: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        val date = inputFormat.parse(inputDate)
        outputFormat.format(date!!)
    } catch (e: Exception) {
        inputDate // return original if parsing fails
    }
}

fun formatDateTime(dateString: String?, format: String): String {
    if (dateString.isNullOrBlank())
        return ""
    val formatter = DateTimeFormatter.ofPattern(format)
    val instant = Instant.parse(dateString)
    val zdt = instant.atZone(ZoneId.systemDefault())
    return zdt.format(formatter)
}

fun convertToKilometers(meters: Int): String {
    return String.format("%.2f km", (meters / 1000))
}
fun convertToMiles(meters: Int): String {
    return String.format("%.2f mi", (meters / 1609.344))
}

fun getPlatform(): String { return if (Build.MODEL.contains("iPhone", true)) "iOS" else "android" }
fun getVersion(): String { return "${Build.VERSION.RELEASE} (${Build.VERSION.SDK_INT})" }

// Show a toast message
fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
interface FCMTokenCallback {
    fun onTokenReceived(token: String)
    fun onTokenError()
}
fun roundToNearestFiveMinutes(calendar: Calendar): Calendar {
    val minutes = calendar.get(Calendar.MINUTE)
    val remainder = minutes % 5
    if (remainder < 3)
        calendar.set(Calendar.MINUTE, minutes - remainder) // Round down
    else
        calendar.set(Calendar.MINUTE, minutes + (5 - remainder)) // Round up
    return calendar
}

fun getLocalizedText(context: Context, text: String): String {
    return when (text) {
        // Rider type
        RiderType.GIVER.name -> context.getString(R.string.giver)
        RiderType.TAKER.name -> context.getString(R.string.taker)

        // Ride type
        RideType.DROP.name ->  context.getString(R.string.drop)
        RideType.PICK.name -> context.getString(R.string.pick)
        RideType.DROP_PICK.name -> context.getString(R.string.drop_pick)

        // Days
        WeekDays.MONDAY.name -> context.getString(R.string.day_monday)
        WeekDays.TUESDAY.name -> context.getString(R.string.day_tuesday)
        WeekDays.WEDNESDAY.name -> context.getString(R.string.day_wednesday)
        WeekDays.THURSDAY.name -> context.getString(R.string.day_thursday)
        WeekDays.FRIDAY.name -> context.getString(R.string.day_friday)
        WeekDays.SATURDAY.name -> context.getString(R.string.day_saturday)
        WeekDays.SUNDAY.name -> context.getString(R.string.day_sunday)


        Gender.SELECT_GENDER.name -> context.getString(R.string.select_gender)
        Gender.MALE.name -> context.getString(R.string.male)
        Gender.FEMALE.name -> context.getString(R.string.female)
        Gender.OTHER.name -> context.getString(R.string.other)

        // Feedback
        "clean_vehicle" -> context.getString(R.string.clean_vehicle)
        "the test" -> context.getString(R.string.the_test)
        "friendliness" -> context.getString(R.string.friendliness)
        "mimi" -> context.getString(R.string.mimi)
        "Punctuality" -> context.getString(R.string.punctuality)
        "Friendliness" -> context.getString(R.string.friendliness)
        "Vehicle Cleanliness" -> context.getString(R.string.vehicle_cleanliness)
        "Driving" -> context.getString(R.string.driving)
        "Communication" -> context.getString(R.string.communication)

        else -> {
            println(text)
//            showToast(context, "${context.getString(R.string.localization_error)} $text")
            text
        }
    }
}

fun showBottomSheetDialog(
    context: Context,
    title: String,
    text: String,
    button1Text: String,
    button2Text: String,
    button1ClickListener: () -> Unit,
    button2ClickListener: () -> Unit,
    onDismissListener: () -> Unit
): Dialog {
    val dialog = Dialog(context)
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    val view = LayoutInflater.from(context).inflate(R.layout.layout_ride_completes_bottom_sheet, null)
    dialog.setContentView(view)

    dialog.window?.apply {
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        attributes?.windowAnimations = R.style.DialogAnimation
        setGravity(Gravity.BOTTOM)
    }

    val textView1 = view.findViewById<TextView>(R.id.bottomSheetText1)
    val textView2 = view.findViewById<TextView>(R.id.bottomSheetText2)
    val button1 = view.findViewById<Button>(R.id.bottomSheetButton1)
    val button2 = view.findViewById<Button>(R.id.bottomSheetButton2)

    textView1.text = title
    textView2.text = text
    button1.text = button1Text
    button2.text = button2Text

    button1.setOnClickListener {
        button1ClickListener.invoke()
        dialog.dismiss()
    }
    button2.setOnClickListener {
        button2ClickListener.invoke()
        dialog.dismiss()
    }
    dialog.setOnDismissListener {
        onDismissListener.invoke()
    }

    dialog.show()
    return dialog
}

fun convert24to12HourFormat(time24: String): String? {
    return try {
        val time = LocalTime.parse(time24, DateTimeFormatter.ofPattern("HH:mm"))
        time.format(DateTimeFormatter.ofPattern("hh:mm a"))
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
fun convertToFormattedDate(input: String?): String {
    if (input.isNullOrBlank()) return ""
    val inputFormat = DateTimeFormatter.ofPattern("d-M-yyyy", Locale.ENGLISH)
    val outputFormat = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH)

    val date = LocalDate.parse(input, inputFormat)
    return date.format(outputFormat)
}
fun convertFromFormattedToIso(input: String?): String {
    if (input.isNullOrBlank()) return ""
    val inputFormat = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH)
    val outputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH)

    val date = LocalDate.parse(input, inputFormat)
    return date.format(outputFormat)
}

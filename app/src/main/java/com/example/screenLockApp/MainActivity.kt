package com.example.screenLockApp
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.os.CountDownTimer
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var compName: ComponentName

    private var countdownTimer: CountDownTimer? = null  // Add this as a global property

    private fun formatTime(totalSeconds: Int): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this, MyDeviceAdminReceiver::class.java)

        val hoursPicker = findViewById<NumberPicker>(R.id.hoursPicker)
        val minutesPicker = findViewById<NumberPicker>(R.id.minutesPicker)
        val secondsPicker = findViewById<NumberPicker>(R.id.secondsPicker)
        val okBtn = findViewById<Button>(R.id.okButton)
        val resetBtn = findViewById<Button>(R.id.resetButton)
        val countdownText = findViewById<TextView>(R.id.countdownText)
        val mainLayout = findViewById<LinearLayout>(R.id.mainLayout)  // wrap your layout in a parent with this ID

        mainLayout.visibility = View.GONE  // Hide the UI until unlocked

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Timer App")
            .setSubtitle("Please authenticate to continue")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        val biometricPrompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    findViewById<LinearLayout>(R.id.mainLayout).visibility = View.VISIBLE
                    Toast.makeText(applicationContext, "Unlocked!", Toast.LENGTH_SHORT).show()
                    mainLayout.visibility = View.VISIBLE  // Show timer UI
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Error: $errString", Toast.LENGTH_SHORT).show()
                    finish() // Close app on cancel
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            })

        biometricPrompt.authenticate(promptInfo)

        hoursPicker.minValue = 0
        hoursPicker.maxValue = 23
        minutesPicker.minValue = 0
        minutesPicker.maxValue = 59
        secondsPicker.minValue = 0
        secondsPicker.maxValue = 59

        okBtn.setOnClickListener {
            val hours = hoursPicker.value
            val minutes = minutesPicker.value
            val seconds = secondsPicker.value
            val totalSeconds = hours * 3600 + minutes * 60 + seconds

            if (totalSeconds <= 0) {
                Toast.makeText(this, "Please select a valid time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!devicePolicyManager.isAdminActive(compName)) {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Permission needed to lock the screen")
                startActivity(intent)
            } else {
                countdownText.text = formatTime(totalSeconds)
                countdownTimer = object : CountDownTimer((totalSeconds * 1000).toLong(), 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        val secondsLeft = (millisUntilFinished / 1000).toInt()
                        countdownText.text = "Time remaining: ${formatTime(secondsLeft)}"
                    }

                    override fun onFinish() {
                        countdownText.text = ""
                        devicePolicyManager.lockNow()
                    }
                }.start()
                Toast.makeText(this, "Timer started", Toast.LENGTH_SHORT).show()
            }
        }

        resetBtn.setOnClickListener {
            countdownTimer?.cancel()
            countdownTimer = null
            countdownText.text = "Timer cancelled."
            Toast.makeText(this, "Timer reset", Toast.LENGTH_SHORT).show()
        }
    }
}
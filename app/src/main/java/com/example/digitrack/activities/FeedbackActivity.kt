package com.example.digitrack.activities

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.digitrack.databinding.ActivityFeedbackBinding
import com.google.firebase.firestore.FirebaseFirestore

class FeedbackActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeedbackBinding
    private var db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvRateError.visibility = View.GONE

        val usersCollection = db.collection("feedback")
        val sharedPref = applicationContext.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)

        var checkedStars = 0

        val starCheckboxes = listOf(
            binding.cbRateOne,
            binding.cbRateTwo,
            binding.cbRateThree,
            binding.cbRateFour,
            binding.cbRateFive
        )

        starCheckboxes.forEachIndexed { index, checkbox ->
            checkbox.setOnClickListener {
                checkedStars = index + 1
                starCheckboxes.forEachIndexed { innerIndex, innerCheckbox ->
                    innerCheckbox.isChecked = innerIndex <= index
                }
            }
        }

        binding.btnSave.setOnClickListener {
            val comment = binding.etCriticsugges.text.toString().trim()
            val name = sharedPref.getString("name", "")

            val userId = usersCollection.document().id

            if (checkedStars == 0) {
                binding.tvRateError.visibility = View.VISIBLE
            } else if (comment.isEmpty()) {
                binding.etCriticsugges.error = "Write your comment"
            } else {
                val userMap = hashMapOf(
                    "feedbackId" to userId,
                    "userName" to name,
                    "feedbackStarRate" to checkedStars,
                    "feedbackText" to comment,
                )

                usersCollection.document(userId).set(userMap).addOnSuccessListener {
                    showSuccessDialog()
                }
                    .addOnFailureListener { e ->
                        showErrorDialog()
                        Log.d("RegisterActivity", "Error: ${e.message}")
                    }
            }
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun showSuccessDialog() {
        if (!isFinishing && !isDestroyed) {
            val dialogView = LayoutInflater.from(this).inflate(com.example.digitrack.R.layout.dialog_success, null)

            val btnOkay = dialogView.findViewById<TextView>(com.example.digitrack.R.id.btnOkay)

            val dialogBuilder = AlertDialog.Builder(this)
                .setView(dialogView)

            val dialog = dialogBuilder.create()

            btnOkay.setOnClickListener {
                dialog.dismiss()
                finish()
            }

            dialog.show()
        }
    }

    private fun showErrorDialog() {
        if (!isFinishing && !isDestroyed) {
            val dialogView = LayoutInflater.from(this).inflate(com.example.digitrack.R.layout.dialog_error, null)

            val btnOkay = dialogView.findViewById<TextView>(com.example.digitrack.R.id.btnOkay)

            val dialogBuilder = AlertDialog.Builder(this)
                .setView(dialogView)

            val dialog = dialogBuilder.create()

            btnOkay.setOnClickListener {
                dialog.dismiss()
                finish()
            }

            dialog.show()
        }
    }
}
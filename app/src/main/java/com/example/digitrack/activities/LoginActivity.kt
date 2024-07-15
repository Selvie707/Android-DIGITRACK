package com.example.digitrack.activities

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.digitrack.databinding.ActivityLoginBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var token: String
    private lateinit var email: String
    private lateinit var password: String
    private var db = FirebaseFirestore.getInstance()
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        val sharedPref = applicationContext.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)

        val usersCollection = db.collection("teacher")

        binding.btnLoginOnboarding.setOnClickListener {
            email = binding.etEmailLogin.text.toString()
            password = binding.etPasswordLogin.text.toString()

            if (email.isBlank() || !email.contains("@") || !email.contains(".")) {
                binding.etEmailLogin.error = "Fill proper email"
            } else if (password.isBlank()) {
                binding.etPasswordLogin.error = "Fill your password"
            } else {
                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val query = usersCollection.whereEqualTo("userEmail", email)
                        query.get()
                            .addOnSuccessListener { result ->
                                if (result.isEmpty) {
                                    binding.etEmailLogin.error = "Email not found"
                                } else {
                                    for (document in result) {
                                        val id = document.getString("userId")
                                        val userIsActive = document.getBoolean("userIsActive")

                                        if (userIsActive!!) {
                                            FirebaseMessaging.getInstance().token.addOnCompleteListener(
                                                OnCompleteListener { task ->
                                                    if (!task.isSuccessful) {
                                                        Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                                                        return@OnCompleteListener
                                                    }

                                                    token = task.result

                                                    val db = FirebaseFirestore.getInstance()

                                                    println("userToken: $token")

                                                    db.collection("teacher").document(id!!).update("userToken", token)
                                                        .addOnSuccessListener {
                                                            println("Value updated sucessfully!")
                                                        }
                                                        .addOnFailureListener { e ->
                                                            println("Value update failed: $e")
                                                        }

                                                    val name = document.getString("userName")
                                                    val email = document.getString("userEmail")
                                                    val role = document.getString("userRole")
                                                    val isOwner = document.getBoolean("userIsOwner")
                                                    val isActive = document.getBoolean("userIsActive")

                                                    val editor = sharedPref.edit()
                                                    editor.putString("id", id)
                                                    editor.putString("name", name)
                                                    editor.putString("email", email)
                                                    editor.putString("role", role)
                                                    editor.putBoolean("isOwner", isOwner!!)
                                                    editor.putBoolean("isActive", isActive!!)
                                                    editor.apply()

                                                    startActivity(Intent(this, NearestScheduleActivity::class.java))
                                                    finish()
                                                }
                                            )
                                        } else {
                                            showErrorDialog("This user is no longer active")
                                        }
                                    }
                                }
                            }
                            .addOnFailureListener { exception ->
                                showErrorDialog("Error getting documents: $exception")
                            }
                    } else {
                        showErrorDialog("Authentication failed")
                    }
                }
            }
        }

        binding.tvRegisterLogin.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.tvForgetPassword.setOnClickListener {
            val email = binding.etEmailLogin.text.toString().trim()

            if (email.isNotEmpty()) {
                sendPasswordResetEmail(email)
            } else {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                showSuccessDialog("Reset email sent")
            } else {
                showErrorDialog("Error: ${task.exception?.message}")
            }
        }
    }

    private fun showSuccessDialog(description: String) {
        if (!isFinishing && !isDestroyed) {
            val dialogView = LayoutInflater.from(this).inflate(com.example.digitrack.R.layout.dialog_success, null)

            val tvDescription = dialogView.findViewById<TextView>(com.example.digitrack.R.id.tvTextDescription)
            val btnOkay = dialogView.findViewById<TextView>(com.example.digitrack.R.id.btnOkay)

            tvDescription.text = description

            val dialogBuilder = AlertDialog.Builder(this)
                .setView(dialogView)

            val dialog = dialogBuilder.create()

            btnOkay.setOnClickListener {
                dialog.dismiss()

                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)

                finish()
            }

            dialog.show()
        }
    }

    private fun showErrorDialog(description: String) {
        if (!isFinishing && !isDestroyed) {
            val dialogView = LayoutInflater.from(this).inflate(com.example.digitrack.R.layout.dialog_error, null)

            val tvDescription = dialogView.findViewById<TextView>(com.example.digitrack.R.id.tvTextDescription)
            val btnOkay = dialogView.findViewById<TextView>(com.example.digitrack.R.id.btnOkay)

            tvDescription.text = description

            val dialogBuilder = AlertDialog.Builder(this)
                .setView(dialogView)

            val dialog = dialogBuilder.create()

            btnOkay.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }
    }
}
package com.example.digitrack.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.digitrack.databinding.ActivityRegisterBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var invitationCode: String
    private lateinit var name: String
    private lateinit var password: String
    private lateinit var repPassword: String
    private var isOwner: Boolean = false
    private var isActive: Boolean = true
    private var db = FirebaseFirestore.getInstance()
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        btnLoginOnClick()

        binding.tvLoginRegister.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun btnLoginOnClick() {
        binding.btnLoginOnboarding.setOnClickListener {
            invitationCode = binding.etInvitationCode.text.toString().trim()
            name = binding.etName.text.toString().trim()
            password = binding.etPasswordRegister.text.toString().trim()
            repPassword = binding.etRepeatPassword.text.toString().trim()

            if (invitationCode.isBlank()) {
                binding.etInvitationCode.error = "Please fill up this field"
            } else if (name.isBlank()) {
                binding.etName.error = "Please fill up this field"
            } else if (password.isBlank()) {
                binding.etPasswordRegister.error = "Please fill up this field"
            } else if (repPassword.isBlank()) {
                binding.etRepeatPassword.error = "Please fill up this field"
            } else if (password != repPassword) {
                binding.etPasswordRegister.error = "Password not match"
                binding.etRepeatPassword.error = "Password not match"
            } else {
                verifyInviteCode(invitationCode)
            }
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

    private fun verifyInviteCode(inviteCode: String) {
        println("inv: $inviteCode")
        val db = Firebase.firestore
        val docRef = db.collection("invitation").document(inviteCode)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists() && document.getBoolean("used") == false) {
                    registerUser(document, inviteCode)
                } else {
                    showErrorDialog("The invitation code is not valid")
                }
            }
            .addOnFailureListener { e ->
                showErrorDialog("Something went wrong: $e")
            }
    }

    private fun registerUser(invitation: DocumentSnapshot, inviteCode: String) {
        val email = invitation.getString("email")!!
        val role = invitation.getString("role")!!
        val usersCollection = db.collection("teacher")

        val userName = name

        usersCollection.whereEqualTo("userName", userName).get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    showErrorDialog("Username is already taken")
                } else {
                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser

                            val userId = user?.uid

                            val userMap = hashMapOf(
                                "userId" to userId,
                                "userName" to userName,
                                "userEmail" to email,
                                "userRole" to role,
                                "userIsOwner" to isOwner,
                                "userIsActive" to isActive
                            )

                            usersCollection.document(userId!!).set(userMap).addOnSuccessListener {
                                val db = FirebaseFirestore.getInstance()

                                db.collection("invitation").document(inviteCode).update("used", true)
                                    .addOnSuccessListener {
                                        println("Value updated sucessfully!")
                                    }
                                    .addOnFailureListener { e ->
                                        println("Value update failed: $e")
                                    }

                                showSuccessDialog()
                            }
                                .addOnFailureListener { e ->
                                    showErrorDialog("Registration failed")
                                    Log.d("RegisterActivity", "Error: ${e.message}")
                                }

                        } else {
                            showErrorDialog("Something went wrong")
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                showErrorDialog("Something went wrong: $e")
            }
    }
}
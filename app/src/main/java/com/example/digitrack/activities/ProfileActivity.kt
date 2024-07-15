package com.example.digitrack.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.digitrack.R
import com.example.digitrack.databinding.ActivityProfileBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import java.util.Date
import java.util.UUID

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var userName: String
    private lateinit var userEmail: String
    private lateinit var userPassword: String
    private lateinit var email: String
    private lateinit var role: String
    private lateinit var inviteCode: String
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPref = applicationContext.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        userName = sharedPref.getString("name", "").toString()
        userEmail = sharedPref.getString("email", "").toString()
        val isOwner = sharedPref.getBoolean("isOwner", false)

        if (!isOwner) {
            binding.btnMakeInvitation.visibility = View.GONE
            binding.btnDeleteUser.visibility = View.GONE
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigationView.selectedItemId = R.id.btn_profile
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.btn_session -> {
                    startActivity(Intent(applicationContext, NearestScheduleActivity::class.java))
                    overridePendingTransition(0,0)
                    finish()
                    true
                }
                R.id.btn_home -> {
                    startActivity(Intent(applicationContext, HomeActivity::class.java))
                    overridePendingTransition(0,0)
                    finish()
                    true
                }
                R.id.btn_profile -> true
                else -> false
            }
        }

        binding.btnEditProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }
        binding.btnFeedback.setOnClickListener {
            startActivity(Intent(this, FeedbackActivity::class.java))
        }
        binding.btnHelpCenter.setOnClickListener {
            startActivity(Intent(this, HelpCenterActivity::class.java))
        }
        binding.btnLogout.setOnClickListener {
            showCustomDialog()
        }
        binding.btnMakeInvitation.setOnClickListener {
            showInvitationDialog()
        }
        binding.btnDeleteUser.setOnClickListener {
            showDeleteDialog()
        }

        val name = sharedPref.getString("name", "")
        val role = sharedPref.getString("role", "")

        binding.tvNameProfile.text = name
        binding.tvRoleProfile.text = role
    }

    private fun showInvitationDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_make_invitation, null)

        val etYourPassword = dialogView.findViewById<EditText>(R.id.etYourPassword)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEmail)
        val spRole = dialogView.findViewById<Spinner>(R.id.spRole)
        val btnSend = dialogView.findViewById<TextView>(R.id.btnSend)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancel)

        val spinnerList = listOf("Role", "Admin", "Teacher")

        val arrayAdapter: ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_list_item_activated_1, spinnerList)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spRole.adapter = arrayAdapter

        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)

        val dialog = dialogBuilder.create()

        btnSend.setOnClickListener {
            inviteCode = UUID.randomUUID().toString()
            email = etEmail.text.toString().trim()
            userPassword = etYourPassword.text.toString().trim()
            role = spRole.selectedItem.toString().trim()

            val db = Firebase.firestore
            val invitation = hashMapOf(
                "email" to email,
                "inviteCode" to inviteCode,
                "role" to role,
                "used" to false,
                "expiryDate" to Timestamp(Date())
            )
            db.collection("invitation").document(inviteCode).set(invitation)
                .addOnSuccessListener {
                    val subject = "Invitation to Register Your Account to DIGITRACK"
                    val body = "Hi, new $role! Please use this invitation code to register: $inviteCode"

                    val sender = GmailSender(userEmail, userPassword)
                    println("User password: $userPassword")
                    Thread {
                        try {
                            sender.sendMail(subject, body, userEmail, email)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }.start()

                    showSuccessDialog("The invitation sucessfully sent!")
                }
                .addOnFailureListener { e ->
                    showErrorDialog("Oh no! We failed to send the invitation, this might be because $e")
                }

            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_user, null)

        val spUser = dialogView.findViewById<Spinner>(R.id.spUser)
        val btnDelete = dialogView.findViewById<TextView>(R.id.btnDelete)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancel)

        val excludedUserName = userName

        db.collection("teacher")
            .whereEqualTo("userIsActive", true)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val teacherNames = mutableListOf<String>()

                for (document in querySnapshot) {
                    val teacherName = document.getString("userName")
                    if (teacherName != null && teacherName != excludedUserName) {
                        teacherNames.add(teacherName)
                    }
                }
                val adapterTeacher = ArrayAdapter(this, android.R.layout.simple_spinner_item, teacherNames)
                adapterTeacher.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spUser.adapter = adapterTeacher
            }
            .addOnFailureListener { exception ->
                println("Failed to load levels: ${exception.message}")
            }

        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)

        val dialog = dialogBuilder.create()

        btnDelete.setOnClickListener {
            val user = spUser.selectedItem.toString().trim()

            getUserId(user) { userId ->
                if (userId != null) {
                    println("User ID: $userId")

                    db.collection("teacher").document(userId)
                        .update("userIsActive", false)
                        .addOnSuccessListener {
                            showSuccessDialog("User sucessfully deleted!")
                        }
                        .addOnFailureListener { e ->
                            showErrorDialog("Oh no! We failed to delete the user. This might be because $e")
                        }
                } else {
                    showErrorDialog("There is no such user")
                }
            }

            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showCustomDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_detele_student, null)

        val tvText = dialogView.findViewById<TextView>(R.id.tvText)
        val btnUpdate = dialogView.findViewById<TextView>(R.id.btnYes)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnNo)

        tvText.text = "Logging out?"

        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)

        val dialog = dialogBuilder.create()

        btnUpdate.setOnClickListener {
            Firebase.auth.signOut()

            startActivity(Intent(this, OnBoardingActivity::class.java))
            finish()

            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun getUserId(userName: String, callback: (String?) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("teacher")
            .whereEqualTo("userName", userName)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    callback(null)
                    return@addOnSuccessListener
                }

                for (document in querySnapshot) {
                    val userId = document.getString("userId")
                    callback(userId)
                    return@addOnSuccessListener
                }
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
                callback(null)
            }
    }

    private fun showSuccessDialog(description: String) {
        if (!isFinishing && !isDestroyed) {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_success, null)

            val tvDescription = dialogView.findViewById<TextView>(R.id.tvTextDescription)
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
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_error, null)

            val tvDescription = dialogView.findViewById<TextView>(R.id.tvTextDescription)
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
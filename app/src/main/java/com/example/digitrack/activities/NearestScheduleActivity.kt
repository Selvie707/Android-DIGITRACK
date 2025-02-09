package com.example.digitrack.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.digitrack.BuildConfig
import com.example.digitrack.R
import com.example.digitrack.Utils
import com.example.digitrack.adapters.NearestScheduleAdapter
import com.example.digitrack.adapters.SomethingNewAdapter
import com.example.digitrack.data.Students
import com.example.digitrack.databinding.ActivityNearestScheduleBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.messaging
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class NearestScheduleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNearestScheduleBinding
    private lateinit var rvSchedule: RecyclerView
    private val db = FirebaseFirestore.getInstance()

    companion object {
        const val REQUEST_CODE_NOTIFICATIONS = 1001
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
        } else {
            // TODO: Inform user that that your app will not show notifications.
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNearestScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPref = this.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val name = sharedPref.getString("name", "")
        val role = sharedPref.getString("role", "")
        val teacherName = if (role == "Teacher") name else null

        askNotificationPermission()
        runtimeEnableAutoInit()
        subscribeTopics()

        if (!Utils.isCancel) {
            db.collection("AppVersion")
                .orderBy("avId", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot != null) {
                        println("Query successful: ${querySnapshot.documents.size} documents found")
                        for (document in querySnapshot) {
                            val avId = document.getLong("avId")
                            println("Document data: ${document.data}")
                            if (avId != null) {
                                println("avID: $avId")
                                val x = BuildConfig.VERSION_CODE
                                println("Current version code: $x")
                                if (x < avId) {
                                    if (!isFinishing && !isDestroyed) {
                                        showCustomDialog()
                                    }
                                }
                            } else {
                                println("avId is null")
                            }
                        }
                    } else {
                        println("No documents found")
                    }
                }
                .addOnFailureListener { exception ->
                    println("Error getting documents: ${exception.message}")
                }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_NOTIFICATIONS
                )
            }
        }

        val user = Firebase.auth.currentUser
        if (user != null) {
            println("Login Successfully")
        } else {
            startActivity(Intent(this, OnBoardingActivity::class.java))
            finish()
        }

        rvSchedule = binding.rvNearestSchedule
        rvSchedule.layoutManager = LinearLayoutManager(this)

        loadNearestSchedule(teacherName)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigationView.selectedItemId = R.id.btn_session
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.btn_session -> true
                R.id.btn_home -> {
                    startActivity(Intent(applicationContext, HomeActivity::class.java))
                    overridePendingTransition(0,0)
                    finish()
                    true
                }
                R.id.btn_profile -> {
                    startActivity(Intent(applicationContext, ProfileActivity::class.java))
                    overridePendingTransition(0,0)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadNearestSchedule(teacherName: String?) {
        val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yy"))

        val currentHour = LocalTime.now().hour.toString().padStart(2, '0')
        val nextHour = (LocalTime.now().hour + 1).toString().padStart(2, '0')
        val currentMinute = "00"
        val currentTime = "$currentHour.$currentMinute"
        val nextTime = "$nextHour.$currentMinute"

        val timeText = "TIME $currentTime - $nextTime WIB"
        binding.tvJam.text = timeText

        db.collection("student").addSnapshotListener { querySnapshot, exception ->
            if (exception != null) {
                Toast.makeText(this, "Failed to load students: $exception", Toast.LENGTH_SHORT)
                    .show()
                return@addSnapshotListener
            }

            if (querySnapshot != null) {
                val studentsWithSchedule = mutableListOf<Pair<Students, Map<String, String>>>()

                for (document in querySnapshot) {
                    val student = document.toObject(Students::class.java)
                    if (teacherName == null || student.teacherName == teacherName) {
                        val studentSchedule = document.get("studentSchedule") as? Map<String, String>
                        if (studentSchedule != null) {
                            studentsWithSchedule.add(Pair(student, studentSchedule))
                        }
                    }
                }

                val filteredSchedules = studentsWithSchedule.flatMap { (student, schedule) ->
                    schedule.filter { (_, value) ->
                        val scheduleDate = value.split("|").getOrNull(0) ?: ""
                        val scheduleTime = value.split("|").getOrNull(1) ?: ""
                        scheduleDate == currentDate && scheduleTime.startsWith(currentHour)
                    }.map { entry -> Triple(student, entry.key, entry.value) }
                }

                val studentsWithTimes = filteredSchedules.map { (student, scheduleKey, _) ->
                    val timeString = scheduleKey.split("|").getOrNull(1) ?: "00.00"
                    val timeParts = timeString.split(".")
                    val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 0
                    val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
                    val totalMinutes = hour * 60 + minute
                    Triple(student, scheduleKey, totalMinutes)
                }

                val sortedStudents = studentsWithTimes.sortedBy { it.third }

                rvSchedule.adapter = NearestScheduleAdapter(
                    sortedStudents.map { it.first },
                    currentDate,
                    currentTime
                ) { position ->
                    Toast.makeText(
                        this,
                        "Student clicked at position $position",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showCustomDialog() {
        if (!isFinishing && !isDestroyed) {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_update_version, null)

            val rvSomethingNew = dialogView.findViewById<RecyclerView>(R.id.rvNewUpdate)
            val btnUpdate = dialogView.findViewById<TextView>(R.id.btnUpdate)
            val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancel)

            rvSomethingNew.layoutManager = LinearLayoutManager(this)

            db.collection("AppVersion")
                .orderBy("avId", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot != null) {
                        for (document in querySnapshot) {
                            val avDescription = document.getString("avDescription")
                            if (avDescription != null) {
                                val items = avDescription.split("|")
                                rvSomethingNew.adapter = SomethingNewAdapter(items)
                            } else {
                                println("avDescription is empty")
                            }
                        }
                    } else {
                        println("No documents found")
                    }
                }
                .addOnFailureListener { exception ->
                    println("Error getting documents: ${exception.message}")
                }

            val dialogBuilder = AlertDialog.Builder(this)
                .setView(dialogView)

            val dialog = dialogBuilder.create()

            btnUpdate.setOnClickListener {
                db.collection("AppVersion")
                    .orderBy("avId", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener {
                        for (document in it) {
                            val avId = document.getLong("avId")
                            val source = document.get("avSource")
                            println("avID: $avId")

                            val x = BuildConfig.VERSION_CODE

                            println(x)
                            if (x < avId!!) {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.data = Uri.parse(source.toString())
                                startActivity(intent)
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, "Failed to load students: $exception", Toast.LENGTH_SHORT).show()
                    }

                dialog.dismiss()
            }

            btnCancel.setOnClickListener {
                Utils.isCancel = true
                dialog.dismiss()
            }

            dialog.show()
        }
    }

    private fun runtimeEnableAutoInit() {
        // [START fcm_runtime_enable_auto_init]
        Firebase.messaging.isAutoInitEnabled = true
        // [END fcm_runtime_enable_auto_init]
    }

    private fun subscribeTopics() {
        // [START subscribe_topics]
        Firebase.messaging.subscribeToTopic("DIGITRACK")
            .addOnCompleteListener { task ->
                var msg = "Subscribed"
                if (!task.isSuccessful) {
                    msg = "Subscribe failed"
                }
                Log.d("FCM", msg)
            }
        // [END subscribe_topics]
    }
}
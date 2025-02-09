package com.example.digitrack.activities

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.digitrack.adapters.OuterScheduleAdapter
import com.example.digitrack.data.Students
import com.example.digitrack.databinding.ActivityScheduleBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class ScheduleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScheduleBinding
    private lateinit var rvSchedule: RecyclerView
    private var currentDate: LocalDate = LocalDate.now()
    private val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yy")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPref = this.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val name = sharedPref.getString("name", "")
        val role = sharedPref.getString("role", "")
        val teacherName = if (role == "Teacher") name else null

        rvSchedule = binding.rvSchedule
        rvSchedule.layoutManager = LinearLayoutManager(this)

        val hari = getDayOfWeekText(currentDate)
        binding.tvDate.text = currentDate.format(dateFormatter)
        binding.tvDay.text = hari

        loadSchedule(currentDate.format(dateFormatter), teacherName)

        binding.btnPrevDay.setOnClickListener {
            previousDate()
            loadSchedule(currentDate.format(dateFormatter), teacherName)
            binding.tvDate.text = currentDate.format(dateFormatter)
            binding.tvDay.text = getDayOfWeekText(currentDate)
        }

        binding.btnNextDay.setOnClickListener {
            nextDate()
            loadSchedule(currentDate.format(dateFormatter), teacherName)
            binding.tvDate.text = currentDate.format(dateFormatter)
            binding.tvDay.text = getDayOfWeekText(currentDate)
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadSchedule(date: String, teacherName: String?) {
        val db = FirebaseFirestore.getInstance()

        db.collection("student")
            .addSnapshotListener { querySnapshot, exception ->
                if (exception != null) {
                    Toast.makeText(this, "Failed to load students: $exception", Toast.LENGTH_SHORT).show()
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

                    val filteredSchedules = mutableListOf<Pair<Students, String>>()
                    for ((student, schedule) in studentsWithSchedule) {
                        for ((_, value) in schedule) {
                            val scheduleDate = value.split("|").getOrNull(0) ?: ""
                            if (scheduleDate == date) {
                                filteredSchedules.add(Pair(student, value))
                            }
                        }
                    }

                    // Sort the schedules by time (ascending)
                    val sortedSchedules = filteredSchedules.sortedBy { (_, scheduleKey) ->
                        scheduleKey.split("|")[1] // Extract the time part
                    }

                    // Group the schedules by time
                    val groupedByTime = sortedSchedules.groupBy { (_, scheduleKey) ->
                        scheduleKey.split("|")[1]
                    }

                    rvSchedule.adapter = OuterScheduleAdapter(groupedByTime, date)
                }
            }
    }

    private fun getDayOfWeekText(date: LocalDate): String {
        val dayOfWeek = date.dayOfWeek
        return dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    }

    private fun previousDate() {
        currentDate = currentDate.minusDays(1)
    }

    private fun nextDate() {
        currentDate = currentDate.plusDays(1)
    }
}
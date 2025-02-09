package com.example.digitrack.activities

import android.R
import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.digitrack.databinding.ActivityAddNewStudentBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Calendar

class AddNewStudentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddNewStudentBinding
    private lateinit var name: String
    private lateinit var level: String
    private lateinit var schDay: String
    private lateinit var schTime: String
    private lateinit var joinDate: String
    private lateinit var teacher: String
    private lateinit var age: String
    private lateinit var dayTime: String
    private lateinit var dailyReportLink: String
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddNewStudentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivLevelAlert.visibility = View.GONE
        binding.ivTeacherAlert.visibility = View.GONE
        binding.ivDayAlert.visibility = View.GONE
        binding.ivTimeAlert.visibility = View.GONE

        getLevel()
        getTeacher()
        setDaySpinner()
        setTimeSpinner()
        btnAddSetOnClick()

        binding.etJoinDate.setOnClickListener {
            showDatePickerDialog(this) { selectedDate ->
                binding.etJoinDate.text = selectedDate
            }
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setTimeSpinner() {
        val optionTimes = arrayOf("Time", "10.00 WIB", "11.00 WIB", "12.00 WIB", "13.00 WIB", "14.00 WIB", "15.00 WIB", "16.00 WIB", "17.00 WIB")
        val adapter = ArrayAdapter(this, R.layout.simple_spinner_item, optionTimes)
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.spTime.adapter = adapter
    }

    private fun setDaySpinner() {
        val optionDays = arrayOf("Day", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val adapterDay = ArrayAdapter(this, R.layout.simple_spinner_item, optionDays)
        adapterDay.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.spDay.adapter = adapterDay
    }

    private fun getLevel() {
        db.collection("levels")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val levelNames = mutableListOf<String>()

                levelNames.add("Level")

                for (document in querySnapshot) {
                    val levelName = document.getString("levelId")
                    if (levelName != null) {
                        levelNames.add(levelName)
                    }
                }
                val adapter = ArrayAdapter(this, R.layout.simple_spinner_item, levelNames)
                adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
                binding.spLevelAdd.adapter = adapter
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to load levels: ${exception.message}", Toast.LENGTH_SHORT).show()
            }

        binding.spLevelAdd.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position).toString().substring(0,3)
                if (selectedItem == "DK3") {
                    binding.etDailyReportAdd.visibility = View.GONE
                } else {
                    binding.etDailyReportAdd.visibility = View.VISIBLE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun getTeacher() {
        db.collection("teacher")
            .whereEqualTo("userRole", "Teacher")
            .whereEqualTo("userIsActive", true)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val teacherNames = mutableListOf<String>()

                teacherNames.add("Teacher")

                for (document in querySnapshot) {
                    val teacherName = document.getString("userName")
                    if (teacherName != null) {
                        teacherNames.add(teacherName)
                    }
                }
                val adapterTeacher = ArrayAdapter(this, R.layout.simple_spinner_item, teacherNames)
                adapterTeacher.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
                binding.spTeacher.adapter = adapterTeacher
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to load teachers: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun btnAddSetOnClick() {
        val usersCollection = db.collection("student")

        binding.btnAddAddStudent.setOnClickListener {
            name = binding.etNameAdd.text.toString().trim()
            level = binding.spLevelAdd.selectedItem.toString().trim()
            joinDate = binding.etJoinDate.text.toString().trim()
            schDay = binding.spDay.selectedItem.toString().trim()
            schTime = binding.spTime.selectedItem.toString().trim()
            teacher = binding.spTeacher.selectedItem.toString().trim()
            dailyReportLink = binding.etDailyReportAdd.text.toString().trim()
            dayTime = "$schDay|$schTime"
            val attendance = 0
            age = binding.etAgeAdd.text.toString().trim()

            if (dailyReportLink.isEmpty()) {
                dailyReportLink = "No daily report"
            }
            if (name.isBlank()) {
                binding.etNameAdd.error = "Please fill up this field"
            } else if (age.isBlank()) {
                binding.etAgeAdd.error = "Please fill up this field"
            } else if (joinDate.isBlank()) {
                binding.etJoinDate.error = "Please fill up this field"
            } else if (dailyReportLink.isBlank()) {
                binding.etDailyReportAdd.error = "Please fill up this field"
            } else {
                if (teacher == "Teacher" || level == "Level" || schDay == "Day" || schTime == "Time") {
                    if (level == "Level") {
                        binding.ivLevelAlert.visibility = View.VISIBLE
                    } else {
                        binding.ivLevelAlert.visibility = View.GONE
                    }
                    if (teacher == "Teacher") {
                        binding.ivTeacherAlert.visibility = View.VISIBLE
                    } else {
                        binding.ivTeacherAlert.visibility = View.GONE
                    }
                    if (schDay == "Day") {
                        binding.ivDayAlert.visibility = View.VISIBLE
                    } else {
                        binding.ivDayAlert.visibility = View.GONE
                    }
                    if (schTime == "Time") {
                        binding.ivTimeAlert.visibility = View.VISIBLE
                    } else {
                        binding.ivTimeAlert.visibility = View.GONE
                    }
                } else {
                    val studentId = usersCollection.document().id
                    val curriculum = level.substring(0, 3)
                    val studentLevelUp = levelUpDate(curriculum, joinDate)

                    schTime = schTime.split(" ").getOrNull(0).toString()
                    println(schTime)

                    getMaterialsForLevel(level) { materialsMap ->
                        val userMap = hashMapOf(
                            "studentId" to studentId,
                            "levelId" to level,
                            "teacherName" to teacher,
                            "studentName" to name,
                            "studentAttendance" to attendance,
                            "studentAttendanceMaterials" to materialsMap,
                            "studentSchedule" to getStudentSchedule(joinDate, schDay, schTime),
                            "studentDailyReportLink" to dailyReportLink,
                            "studentAge" to age,
                            "studentJoinDate" to joinDate,
                            "studentDayTime" to dayTime,
                            "studentLevelUp" to studentLevelUp
                        )

                        usersCollection.document(studentId).set(userMap).addOnSuccessListener {
                            showSuccessDialog()
                        }
                            .addOnFailureListener { e ->
                                showErrorDialog()
                                Log.d("RegisterActivity", "Error: ${e.message}")
                            }
                    }
                }
            }
        }
    }

    private fun getMaterialsForLevel(levelId: String, callback: (HashMap<String, String>) -> Unit) {
        val materialsMap = hashMapOf<String, String>()

        db.collection("materials")
            .whereEqualTo("levelId", levelId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val materialsList = mutableListOf<Pair<Long, String>>()
                for (document in querySnapshot) {
                    val materialId = document.getLong("materialId")
                    val materialName = document.getString("materialName")
                    if (materialId != null && materialName != null) {
                        materialsList.add(Pair(materialId, materialName))
                    }
                }
                materialsList.sortBy { it.first }

                materialsList.forEachIndexed { index, pair ->
                    materialsMap[(index + 1).toString()] = pair.second
                }

                callback(materialsMap)
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting materials: ", exception)
                callback(materialsMap)
            }
    }

    private fun getStudentSchedule(joinDate: String, schDay: String, schTime: String): HashMap<String, String> {
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yy")
        val startDate = LocalDate.parse(joinDate, formatter)
        val dayOfWeek = when (schDay) {
            "Sunday" -> java.time.DayOfWeek.SUNDAY
            "Monday" -> java.time.DayOfWeek.MONDAY
            "Tuesday" -> java.time.DayOfWeek.TUESDAY
            "Wednesday" -> java.time.DayOfWeek.WEDNESDAY
            "Thursday" -> java.time.DayOfWeek.THURSDAY
            "Friday" -> java.time.DayOfWeek.FRIDAY
            "Saturday" -> java.time.DayOfWeek.SATURDAY
            else -> throw IllegalArgumentException("Invalid day of the week: $schDay")
        }

        val firstSession = if (startDate.dayOfWeek == dayOfWeek) {
            startDate
        } else {
            startDate.with(TemporalAdjusters.next(dayOfWeek))
        }

        val scheduleMap = hashMapOf<String, String>()
        for (i in 0 until 16) {
            val sessionDate = firstSession.plusWeeks(i.toLong())
            val sessionDateString = sessionDate.format(formatter)
            scheduleMap[(i + 1).toString()] = "$sessionDateString|$schTime"
        }

        return scheduleMap
    }

    private fun showDatePickerDialog(context: Context, onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(context, { _, selectedYear, selectedMonth, selectedDay ->
            val formattedYear = (selectedYear % 100).toString().padStart(2, '0')

            val formattedDate = "${selectedDay.toString().padStart(2, '0')}-${(selectedMonth + 1).toString().padStart(2, '0')}-$formattedYear"
            onDateSelected(formattedDate)
        }, year, month, day)

        datePickerDialog.show()
    }

    private fun levelUpDate(curriculum: String, date: String): String {
        val dateFormatter =
            DateTimeFormatter.ofPattern("dd-MM-yy")

        val theDate = LocalDate.parse(date, dateFormatter)

        val newDate: LocalDate = if (curriculum == "DK3") {
            theDate.plusMonths(3)
        } else {
            theDate.plusMonths(11)
        }
        return newDate.format(dateFormatter)
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

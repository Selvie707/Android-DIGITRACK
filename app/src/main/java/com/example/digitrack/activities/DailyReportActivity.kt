package com.example.digitrack.activities

import android.content.Context
import android.os.Bundle
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.digitrack.adapters.DailyReportAdapter
import com.example.digitrack.data.Students
import com.example.digitrack.databinding.ActivityDailyReportBinding
import com.google.firebase.firestore.FirebaseFirestore

class DailyReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDailyReportBinding
    private lateinit var rvDailyReport: RecyclerView
    private lateinit var adapter: DailyReportAdapter
    private val dailyReportList = mutableListOf<Students>()
    private val filteredDailyReportList = mutableListOf<Students>()
    private var teacherName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailyReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPref = this.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val role = sharedPref.getString("role", "")
        val name = sharedPref.getString("name", "")
        if (role == "Teacher") {
            teacherName = name
        }

        rvDailyReport = binding.rvDailyReport
        rvDailyReport.layoutManager = LinearLayoutManager(this)

        adapter = DailyReportAdapter(filteredDailyReportList) { position ->
            println("Student clicked at position $position")
        }
        rvDailyReport.adapter = adapter

        loadDailyReport()

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.svSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filter(newText)
                return false
            }
        })
    }

    private fun loadDailyReport() {
        val db = FirebaseFirestore.getInstance()
        val collectionRef = db.collection("student")

        // Filter query based on teacherName
        val query = if (teacherName != null) {
            collectionRef.whereEqualTo("teacherName", teacherName)
        } else {
            collectionRef
        }

        query.get().addOnSuccessListener { querySnapshot ->
            dailyReportList.clear()
            for (document in querySnapshot) {
                val student = document.toObject(Students::class.java)
                if (student.studentDailyReportLink != "No daily report") {
                    dailyReportList.add(student)
                }
            }
            filter("")
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "Failed to load students: $exception", Toast.LENGTH_SHORT).show()
        }
    }

    private fun filter(text: String?) {
        val query = text?.lowercase() ?: ""
        filteredDailyReportList.clear()

        if (query.isEmpty()) {
            filteredDailyReportList.addAll(dailyReportList)
        } else {
            for (student in dailyReportList) {
                val studentName = student.studentName.lowercase()
                val studentLevel = student.levelId.lowercase()
                if (studentName.contains(query) || studentLevel.contains(query)) {
                    filteredDailyReportList.add(student)
                }
            }
        }
        adapter.notifyDataSetChanged()
    }
}
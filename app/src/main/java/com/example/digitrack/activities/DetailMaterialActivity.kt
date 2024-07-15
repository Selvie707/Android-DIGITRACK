package com.example.digitrack.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.digitrack.adapters.SomethingNewAdapter
import com.example.digitrack.databinding.ActivityDetailMaterialBinding

class DetailMaterialActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailMaterialBinding
    private lateinit var rvTools: RecyclerView
    private lateinit var rvGoals: RecyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailMaterialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        rvTools = binding.rvTools
        rvGoals = binding.rvGoals

        binding.btnBack.setOnClickListener {
            finish()
        }

        rvTools.layoutManager = LinearLayoutManager(this)
        rvGoals.layoutManager = LinearLayoutManager(this)

        val materialName = intent.getStringExtra("materialName")
        val materialLink = intent.getStringExtra("materialLink")
        val materialTools = intent.getStringExtra("materialTools")
        val materialGoals = intent.getStringExtra("materialGoals")

        binding.llGoToFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(materialLink)
            startActivity(intent)
        }

        binding.tvMaterialName.text = materialName
        val toolItems = materialTools!!.split("|")
        rvTools.adapter = SomethingNewAdapter(toolItems)
        val goalsItems = materialGoals!!.split("|")
        rvGoals.adapter = SomethingNewAdapter(goalsItems)
    }
}
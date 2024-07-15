package com.example.digitrack.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.digitrack.R
import com.example.digitrack.databinding.ActivityAgeGroupsBinding
import com.example.digitrack.databinding.ActivityDetailMaterialBinding

class AgeGroupsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAgeGroupsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgeGroupsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}
package com.example.digitrack.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.digitrack.R
import com.example.digitrack.adapters.LevelsAdapter
import com.example.digitrack.adapters.MaterialsAdapter
import com.example.digitrack.data.Levels
import com.example.digitrack.data.Materials
import com.example.digitrack.databinding.ActivityMaterialsBinding
import com.google.firebase.firestore.FirebaseFirestore

class MaterialsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMaterialsBinding
    private lateinit var rvMaterials: RecyclerView
    private lateinit var rvLevels: RecyclerView

    private val materialsList = mutableListOf<Materials>()
    private val levelsList = mutableListOf<Levels>()
    private lateinit var materialsAdapter: MaterialsAdapter
    private var selectedLevelId: String = "K1"
    private var selectedCurName: String = "DK2"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMaterialsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        rvMaterials = binding.rvMaterials
        rvLevels = binding.rvLevels

        rvMaterials.layoutManager = LinearLayoutManager(this)
        rvLevels.layoutManager = LinearLayoutManager(this)

        val purpleColor = ContextCompat.getColor(this, R.color.purple)

        binding.btnDK2.setOnClickListener {
            binding.btnDK2.setBackgroundResource(R.drawable.bg_corner_backii)
            binding.btnDK2.setTextColor(Color.WHITE)
            binding.btnDK3.setBackgroundResource(R.drawable.bg_corner_back)
            binding.btnDK3.setTextColor(purpleColor)
            binding.btnACD.setBackgroundResource(R.drawable.bg_corner_back)
            binding.btnACD.setTextColor(purpleColor)

            selectedCurName = binding.btnDK2.text.toString()
            selectedLevelId = "K1"
            loadLevels(selectedCurName)
            loadMaterials(selectedCurName+selectedLevelId)
        }

        binding.btnDK3.setOnClickListener {
            binding.btnDK3.setBackgroundResource(R.drawable.bg_corner_backii)
            binding.btnDK3.setTextColor(Color.WHITE)
            binding.btnDK2.setBackgroundResource(R.drawable.bg_corner_back)
            binding.btnDK2.setTextColor(purpleColor)
            binding.btnACD.setBackgroundResource(R.drawable.bg_corner_back)
            binding.btnACD.setTextColor(purpleColor)

            selectedCurName = binding.btnDK3.text.toString()
            selectedLevelId = "LC1L1"
            loadLevels(selectedCurName)
            loadMaterials(selectedCurName+selectedLevelId)
        }

        binding.btnACD.setOnClickListener {
            binding.btnACD.setBackgroundResource(R.drawable.bg_corner_backii)
            binding.btnACD.setTextColor(Color.WHITE)
            binding.btnDK3.setBackgroundResource(R.drawable.bg_corner_back)
            binding.btnDK3.setTextColor(purpleColor)
            binding.btnDK2.setBackgroundResource(R.drawable.bg_corner_back)
            binding.btnDK2.setTextColor(purpleColor)

            selectedCurName = binding.btnACD.text.toString()
            selectedLevelId = "J"
            loadLevels(selectedCurName)
            loadMaterials(selectedCurName+selectedLevelId)
        }

        materialsAdapter = MaterialsAdapter(materialsList) { position ->
            val intent = Intent(this, DetailMaterialActivity::class.java)
            val materialId = materialsList[position].materialId
            val materialName = materialsList[position].materialName
            val materialLink = materialsList[position].materialLink
            var materialTools = materialsList[position].materialTools
            var materialGoals = materialsList[position].materialGoals

            if (materialLink.isEmpty()) {
                materialTools = "Link data is not available"
            }

            if (materialTools.isEmpty()) {
                materialTools = "Tools data is not available"
            }

            if (materialGoals.isEmpty()) {
                materialGoals = "Goals data is not available"
            }

            intent.putExtra("materialId", materialId)
            intent.putExtra("materialName", materialName)
            intent.putExtra("materialLink", materialLink)
            intent.putExtra("materialTools", materialTools)
            intent.putExtra("materialGoals", materialGoals)
            startActivity(intent)
        }
        rvMaterials.adapter = materialsAdapter

        loadLevels(selectedCurName)

        binding.btnAdd.setOnClickListener {
            val cur = "AKD"
            val intent = Intent(this, EditProfileActivity::class.java)
            intent.putExtra("curriculum", cur)
            startActivity(intent)
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadMaterials(levelId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("materials")
            .whereEqualTo("levelId", levelId)
            .addSnapshotListener { querySnapshot, exception ->
                if (exception != null) {
                    Toast.makeText(this, "Failed to load students: $exception", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (querySnapshot != null) {
                    materialsList.clear()
                    for (document in querySnapshot) {
                        val material = document.toObject(Materials::class.java)
                        materialsList.add(material)
                    }
                    materialsList.sortBy { it.materialId }
                    materialsAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun loadLevels(curName: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("levels").whereEqualTo("curName", curName).addSnapshotListener { querySnapshot, exception ->
            if (exception != null) {
                Toast.makeText(this, "Failed to load students: $exception", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            if (querySnapshot != null) {
                levelsList.clear()
                for (document in querySnapshot) {
                    val level = document.toObject(Levels::class.java)
                    levelsList.add(level)
                }
                levelsList.sortBy { it.levelSeq }

                loadMaterials(selectedCurName+selectedLevelId)
                rvLevels.adapter = LevelsAdapter(levelsList) { position ->
                    var levelId = levelsList[position].levelId
                    levelId = levelId.substring(3)
                    loadMaterials(selectedCurName + levelId)
                }
            }
        }
    }
}
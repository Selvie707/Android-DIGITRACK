package com.example.digitrack.data

data class Materials (
    val materialId: Int = 0,
    val levelId: String = "",
    val materialName: String = "",
    val materialLink: String = "",
    val materialTools: String = "",
    val materialGoals: String = ""
) {
    constructor() : this(0, "", "", "", "", "")
}
package com.example.digitrack.data

data class Invitation(
    val email: String = "",
    val inviteCode: String = "",
    val role: String = "",
    val used: Boolean = false,
    val expiryDate: String = ""
) {
    constructor() : this(
        email = "",
        inviteCode = "",
        role = "",
        used = false,
        expiryDate = ""
    )
}
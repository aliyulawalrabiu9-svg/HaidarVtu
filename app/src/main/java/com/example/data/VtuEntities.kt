package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallet")
data class Wallet(
    @PrimaryKey val id: Int = 1,
    val balance: Double = 5000.0 // Start balance of ₦5,000.00 Naira
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val serviceType: String, // "Airtime", "Data", "Cable TV", "Electricity", "Wallet Fund"
    val recipient: String,   // Phone number, IUC Smartcard, or Meter number
    val provider: String,    // "MTN", "Airtel", "9mobile", "Glo", "DSTV", "Gotv", "AEDC", "EKEDC", etc.
    val amount: Double,
    val fee: Double = 0.0,
    val reference: String,   // e.g., "SWIFT-982173"
    val status: String,      // "SUCCESS", "PENDING", "FAILED"
    val timestamp: Long = System.currentTimeMillis(),
    val details: String = ""
)

@Entity(tableName = "beneficiaries")
data class Beneficiary(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val alias: String,      // "My Phone", "Office Meter", etc.
    val number: String,     // Target identification number
    val type: String,       // "Airtime", "Data", "Cable TV", "Electricity"
    val provider: String    // "MTN", "AEDC", "Gotv", etc.
)

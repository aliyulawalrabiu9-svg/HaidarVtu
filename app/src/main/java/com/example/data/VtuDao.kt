package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VtuDao {
    @Query("SELECT * FROM wallet WHERE id = 1 LIMIT 1")
    fun getWallet(): Flow<Wallet?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWallet(wallet: Wallet)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Query("SELECT * FROM beneficiaries ORDER BY name ASC")
    fun getAllBeneficiaries(): Flow<List<Beneficiary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeneficiary(beneficiary: Beneficiary)

    @Delete
    suspend fun deleteBeneficiary(beneficiary: Beneficiary)
}

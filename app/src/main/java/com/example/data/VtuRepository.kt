package com.example.data

import kotlinx.coroutines.flow.Flow

class VtuRepository(private val vtuDao: VtuDao) {
    val wallet: Flow<Wallet?> = vtuDao.getWallet()
    val allTransactions: Flow<List<Transaction>> = vtuDao.getAllTransactions()
    val allBeneficiaries: Flow<List<Beneficiary>> = vtuDao.getAllBeneficiaries()

    suspend fun saveWallet(wallet: Wallet) {
        vtuDao.insertOrUpdateWallet(wallet)
    }

    suspend fun saveTransaction(transaction: Transaction) {
        vtuDao.insertTransaction(transaction)
    }

    suspend fun saveBeneficiary(beneficiary: Beneficiary) {
        vtuDao.insertBeneficiary(beneficiary)
    }

    suspend fun removeBeneficiary(beneficiary: Beneficiary) {
        vtuDao.deleteBeneficiary(beneficiary)
    }
}

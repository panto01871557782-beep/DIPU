package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY detectedAt DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE provider = :provider ORDER BY detectedAt DESC")
    fun getTransactionsByProvider(provider: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE transactionId LIKE '%' || :query || '%' 
           OR senderInfo LIKE '%' || :query || '%'
           OR provider LIKE '%' || :query || '%'
        ORDER BY detectedAt DESC
    """)
    fun searchTransactions(query: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE provider = :provider AND transactionId = :trxId LIMIT 1")
    suspend fun findByProviderAndTrxId(provider: String, trxId: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE source = 'SMS'")
    suspend fun countSms(): Int

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}

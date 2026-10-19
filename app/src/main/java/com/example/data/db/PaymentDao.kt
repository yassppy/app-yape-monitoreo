package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.YapePayment
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM yape_payments ORDER BY timestamp DESC")
    fun getAllPayments(): Flow<List<YapePayment>>

    @Query("SELECT * FROM yape_payments WHERE timestamp >= :startOfDay ORDER BY timestamp DESC")
    fun getPaymentsSince(startOfDay: Long): Flow<List<YapePayment>>

    @Query("SELECT * FROM yape_payments WHERE id = :id LIMIT 1")
    suspend fun getPaymentById(id: Long): YapePayment?

    @Query("SELECT * FROM yape_payments WHERE syncStatus = 'FAILED' OR syncStatus = 'PENDING'")
    suspend fun getUnsyncedPayments(): List<YapePayment>

    @Query("SELECT COUNT(*) FROM yape_payments WHERE sender = :sender AND amount = :amount AND ABS(timestamp - :timestamp) < 180000")
    suspend fun countSimilar(sender: String, amount: Double, timestamp: Long): Int

    @Query("SELECT COUNT(*) FROM yape_payments WHERE rawBody = :rawBody AND rawBody != ''")
    suspend fun countWithRawBody(rawBody: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: YapePayment): Long

    @Update
    suspend fun updatePayment(payment: YapePayment)

    @Query("DELETE FROM yape_payments WHERE id = :id")
    suspend fun deletePaymentById(id: Long)

    @Query("DELETE FROM yape_payments WHERE id NOT IN (SELECT MIN(id) FROM yape_payments GROUP BY sender, amount, (timestamp / 30000))")
    suspend fun deleteDuplicates()

    @Query("DELETE FROM yape_payments WHERE rawTitle LIKE '%Prueba%' OR sender LIKE '%Prueba%' OR rawTitle LIKE '%Test%'")
    suspend fun deleteTestPayments()

    @Query("DELETE FROM yape_payments")
    suspend fun clearAllPayments()

    @Query("SELECT COUNT(*) FROM yape_payments")
    fun getPaymentCount(): Flow<Int>
}

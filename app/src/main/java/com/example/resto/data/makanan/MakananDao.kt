package com.example.resto.data.makanan

import  androidx.room.*

@Dao
interface MakananDao {
    @Query("SELECT * FROM makanan WHERE nama_makanan LIKE :namaMakanan")
    suspend fun searchMakanan(namaMakanan: String) : List<Makanan>

    @Insert
    suspend fun addMakanan(makanan: Makanan)

    @Update
    suspend fun updateMakanan(makanan: Makanan)

    @Delete
    suspend fun deleteMakanan(makanan: Makanan)

    @Query("SELECT * FROM makanan")
    suspend fun getAllMakanan(): List<Makanan>
}
package com.example.resto.data.makanan

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "makanan")

data class Makanan(
    @ColumnInfo(name = "nama_makanan") var nama_makanan : String = "",
    @ColumnInfo(name = "jenis_makanan") var jenis_makanan : String = "",
    @ColumnInfo(name = "harga") var harga : Double = 0.0,
    @ColumnInfo(name = "foto_makanan") var foto_makanan : String = ""
) : Serializable {
    @PrimaryKey(autoGenerate = true) var id: Int = 0
}

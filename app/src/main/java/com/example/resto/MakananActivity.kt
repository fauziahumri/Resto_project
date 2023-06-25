package com.example.resto

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.resto.data.RestoDatabase
import com.example.resto.data.makanan.Makanan
import com.example.resto.databinding.ActivityMakananBinding
import android.Manifest
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import com.example.resto.data.makanan.addMakananFragment
import kotlinx.coroutines.launch
import java.io.File

class MakananActivity : AppCompatActivity() {

    private var _binding: ActivityMakananBinding? = null
    private val binding get() = _binding!!

    private val STORAGE_PERMISSION_CODE = 102
    private val TAG = "PERMISSION_TAG"

    lateinit var makananRecyclerView: RecyclerView

    lateinit var restoDB: RestoDatabase

    lateinit var makananList: ArrayList<Makanan>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityMakananBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!checkPermission()) {
            requestPermission()
        }

        restoDB = RestoDatabase(this@MakananActivity)

        loadDataMakanan()

        binding.btnAddMakanan.setOnClickListener {
            addMakananFragment().show(supportFragmentManager, "newMakananTag")
        }

        swipeDelete()

        binding.txtSearchMakanan.addTextChangedListener {
            val keyword: String = "%${binding.txtSearchMakanan.text.toString()}%"
            if (keyword.count() > 2) {
                searchDataMakanan(keyword)
            }
            else {
                loadDataMakanan()
            }
        }
    }

    private fun checkPermission() : Boolean{
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        }
        else{
            val write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try{
                val intent =Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                val uri = Uri.fromParts("package", this.packageName, null)
                intent.data = uri
            }
            catch (e: Exception) {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
            }
        }
        else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
            STORAGE_PERMISSION_CODE)
        }
    }

    fun loadDataMakanan() {
        var layoutManager = LinearLayoutManager(this)
        makananRecyclerView = binding.makananListView
        makananRecyclerView.layoutManager = layoutManager
        makananRecyclerView.setHasFixedSize(true)

        lifecycleScope.launch {
            makananList = restoDB.getMakananDao().getAllMakanan() as ArrayList<Makanan>
            Log.e("list makanan", makananList.toString())
            makananRecyclerView.adapter = MakananAdapter(makananList)
        }
    }

    fun deleteMakanan(makanan : Makanan) {
        val builder = AlertDialog.Builder(this@MakananActivity)
        builder.setMessage("Apakah ${makanan.nama_makanan} ingin dihapus ?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id ->
                lifecycleScope.launch {
                    restoDB.getMakananDao().deleteMakanan(makanan)
                    loadDataMakanan()
                }
                val imagesDir =
                    Environment.getExternalStoragePublicDirectory("")
                //Konversi dari dir string ke dir file
                val foto_delete = File(imagesDir, makanan.foto_makanan)

                if(foto_delete.exists()) {
                    //foto ada didalam galeri
                    if(foto_delete.delete()) {
                        //foto di delete
                        val toastDelete = Toast.makeText(applicationContext,
                            "file edit foto delete", Toast.LENGTH_LONG)
                        toastDelete.show()
                    }
                }

            }
            .setNegativeButton("No") { dialog, id ->
                dialog.dismiss()
                loadDataMakanan()
            }
        val alert = builder.create()
        alert.show()
    }

    fun swipeDelete() {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0,
        ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition

                lifecycleScope.launch {
                    makananList = restoDB.getMakananDao().getAllMakanan() as ArrayList<Makanan>
                    Log.e("position swiped", makananList[position].toString())
                    Log.e("position swiped", makananList.size.toString())


                    deleteMakanan(makananList[position])
                }
            }
        }).attachToRecyclerView(makananRecyclerView)
    }

    fun searchDataMakanan(keyword: String) {
        var layoutManager = LinearLayoutManager(this)
        makananRecyclerView = binding.makananListView
        makananRecyclerView.layoutManager = layoutManager
        makananRecyclerView.setHasFixedSize(true)

        lifecycleScope.launch {
            makananList = restoDB.getMakananDao().searchMakanan(keyword) as ArrayList<Makanan>
            Log.e("List makanan", makananList.toString())
            makananRecyclerView.adapter = MakananAdapter(makananList)
        }
    }
}
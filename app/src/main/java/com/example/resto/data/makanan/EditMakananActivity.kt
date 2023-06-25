package com.example.resto.data.makanan

import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.resto.MakananActivity
import android.Manifest
import com.example.resto.data.RestoDatabase
import com.example.resto.databinding.ActivityEditMakananBinding
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.time.temporal.ValueRange
import kotlin.math.log

class EditMakananActivity : AppCompatActivity() {

    private var _binding: ActivityEditMakananBinding? = null
    private val binding get() = _binding!!

    private val REQ_CAM = 101
    private var dataGambar: Bitmap? = null
    private var old_foto_dir = ""
    private var new_foto_dir = ""

    private var id_makanan: Int = 0

    lateinit var restoDB: RestoDatabase
    private val STORAGE_PERMISSION_CODE = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityEditMakananBinding.inflate(layoutInflater)
        setContentView(binding.root)

        restoDB = RestoDatabase(this@EditMakananActivity)

        val intent = intent
        binding.TxtEditNama.setText(intent.getStringExtra("nama makanan").toString())
        binding.TxtEditJenis.setText(intent.getStringExtra("jenis makanan").toString())
        binding.TxtEditHarga.setText(intent.getStringExtra("harga makanan").toString())

        id_makanan = intent.getStringExtra("id").toString().toInt()

        old_foto_dir = intent.getStringExtra("foto_makanan").toString()
        val imgFile = File("${Environment.getExternalStorageDirectory()}/${old_foto_dir}")
        val myBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
        binding.BtnImgMakanan.setImageBitmap(myBitmap)

        if (!checkPermission()) {
            requestPermission()
        }

        binding.BtnEditMakanan.setOnClickListener {
            openCamera()
        }

        binding.BtnEditMakanan.setOnClickListener {
            editMakanan()
        }
    }

    private fun checkPermission() : Boolean{
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        }
        else {
            val write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent()
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

    fun saveMediaToStorage(bitmap: Bitmap): String {
        //Generate Nama File
        val filename = "${System.currentTimeMillis()}.jpg"
        //Output stream
        var fos: OutputStream? = null
        var image_save =""
        //For device running android >= Q
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //getting the contentResolver
            this.contentResolver?.also { resolver ->
                //Content resolver will process the contentvalues
                val contentValues = ContentValues().apply {
                    //putting file information in content values
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                //Inserting the contentValues to contentResolver and getting the Uri
                val imageUri: Uri? =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                //Opening an outputstream with the Uri that we got
                fos = imageUri?.let { resolver.openOutputStream(it) }
                //Store file dir to image_save
                image_save = "${Environment.DIRECTORY_PICTURES}/${filename}"
            }
        }
        else {
            //These for devices running on android < Q
            val permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
            }
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, filename)
            fos = FileOutputStream(image)
            image_save = "${Environment.DIRECTORY_PICTURES}/${filename}"
        }
        fos?.use {
            //Finally writting the bitmap to the output stream that we opened
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }
        return image_save
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CAM && requestCode == RESULT_OK) {
            dataGambar = data?.extras?.get("data") as Bitmap
            val image_save_uri: String = saveMediaToStorage(dataGambar!!)
            new_foto_dir = image_save_uri
            binding.BtnImgMakanan.setImageBitmap(dataGambar)
        }
    }

    fun openCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            this.packageManager?.let {
                intent?.resolveActivity(it).also {
                    startActivityForResult(intent, REQ_CAM)
                }
            }
        }
    }

    private fun editMakanan() {
        val nama_makanan = binding.TxtEditNama.text.toString()
        val jenis_makanan = binding.TxtEditJenis.text.toString()
        val harga_makanan : Double = binding.TxtEditHarga.text.toString().toDouble()
        var foto_final_dir : String = old_foto_dir

        if (old_foto_dir != new_foto_dir) {
            foto_final_dir = new_foto_dir
            val imagesDir =
                Environment.getExternalStoragePublicDirectory( "")
            //Foto dir string di konversi ke foto dir file
            val old_foto_delete = File(imagesDir, old_foto_dir)

            if(old_foto_delete.exists()) {
                //Foto lama ada
                if(old_foto_delete.delete()) {
                    //Foto lama di hapus
                    Log.e("foto final", foto_final_dir)
                }
            }
        }
        lifecycleScope.launch {
            val makanan = Makanan(nama_makanan, jenis_makanan, harga_makanan, foto_final_dir)
            makanan.id = id_makanan
            restoDB.getMakananDao().updateMakanan(makanan)
        }
        val  intentMakanan = Intent(this, MakananActivity::class.java)
        startActivity(intentMakanan)
    }
}
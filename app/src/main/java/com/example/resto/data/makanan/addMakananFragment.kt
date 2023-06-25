package com.example.resto.data.makanan

import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.resto.MakananActivity
import android.Manifest
import com.example.resto.data.RestoDatabase
import com.example.resto.databinding.FragmentAddMakananBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class addMakananFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentAddMakananBinding? = null
    private val binding get() = _binding!!

    private val REQ_CAM = 100
    private var dataGambar: Bitmap? = null
    private var saved_image_url: String = ""

    private val STORAGE_PERMISSION_CODE = 102
    private val TAG = "PERMISSION_TAG"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentAddMakananBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun addMakanan() {
        val nama_makanan = binding.TxtNama.text.toString()
        val jenis = binding.TxtJenis.text.toString()
        val harga = binding.TxtHarga.text.toString().toDouble()

        lifecycleScope.launch{
            val makanan = Makanan(nama_makanan, jenis, harga, saved_image_url)
            RestoDatabase(requireContext()).getMakananDao().addMakanan(makanan)
        }
        dismiss()
    }

    fun saveMediaToStorage(bitmap: Bitmap): String {
        //Genarate Nama File
        val filename = "${System.currentTimeMillis()}.jpg"
        //Output stream
        var fos: OutputStream? = null
        var image_save = ""

        //For devices running android >= Q
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //getting the contentResolver
            activity?.contentResolver?.also { resolver ->
                //Content resolver will process the contentvalues
                val contentValues = ContentValues().apply {
                    //putting file information in content values
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
            }
        }
        else {
            //These for devices running in android >= Q
            val permission = ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this.requireActivity(), arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE)
            }

            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, filename)
            fos = FileOutputStream(image)

            image_save = "${Environment.DIRECTORY_PICTURES}/${filename}"
        }

        fos?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        return image_save
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CAM && resultCode == AppCompatActivity.RESULT_OK) {
            dataGambar = data?.extras?.get("data") as Bitmap
            val image_save_uri: String = saveMediaToStorage(dataGambar!!)
            binding.BtnImgMakanan.setImageBitmap(dataGambar)
            saved_image_url = image_save_uri
        }
    }

    private fun openCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            this.activity?.packageManager?.let {
                intent?.resolveActivity(it).also{
                    startActivityForResult(intent, REQ_CAM)
                }
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        (activity as MakananActivity?)?.loadDataMakanan()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.BtnImgMakanan.setOnClickListener {
            openCamera()
        }

        binding.BtnAddMakanan.setOnClickListener {
            if( saved_image_url != "") {
                addMakanan()
            }
        }
    }
}
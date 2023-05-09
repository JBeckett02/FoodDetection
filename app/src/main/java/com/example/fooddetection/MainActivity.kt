package com.example.fooddetection

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.configuration.UpdateConfiguration
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.result.BitmapPhoto
import io.fotoapparat.result.PhotoResult
import io.fotoapparat.view.CameraView
import java.io.File
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var cameraView: CameraView
    private lateinit var takePhotoButton: Button
    private lateinit var fotoapparat: Fotoapparat


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        cameraView = findViewById(R.id.camera_preview)
        takePhotoButton = findViewById(R.id.take_photo_button)

        fotoapparat = Fotoapparat(
            context = this,
            view = cameraView
        )

        // Set click listener for the take photo button
        takePhotoButton.setOnClickListener {
            takePhoto()
        }
    }

    override fun onStart() {
        super.onStart()
        requestPermission()
        // Start Fotoapparat
        fotoapparat.start()
    }

    override fun onStop() {
        super.onStop()
        // Stop Fotoapparat
        fotoapparat.stop()
    }

    private fun takePhoto() {
        //Assign filename and location
        val photoName = UUID.randomUUID().toString() + ".jpg"

        val photoFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), photoName)

        // Take photo using Fotoapparat
        val photoResult = fotoapparat.takePicture()
        photoResult.saveToFile(photoFile)
    }


    val permissions = arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE)

    private fun hasNoPermissions(): Boolean{
        return ContextCompat.checkSelfPermission(this,
            Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
            Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission(){
        ActivityCompat.requestPermissions(this, permissions,0)
    }
}






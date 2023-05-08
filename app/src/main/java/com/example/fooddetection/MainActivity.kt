package com.example.fooddetection

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.Toast
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.configuration.UpdateConfiguration
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.result.BitmapPhoto
import io.fotoapparat.result.PhotoResult
import io.fotoapparat.view.CameraView
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var cameraView: CameraView
    private lateinit var takePhotoButton: Button
    private lateinit var fotoapparat: Fotoapparat

    val photoFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "foodimage.jpg")

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
        // Start Fotoapparat
        fotoapparat.start()
    }

    override fun onStop() {
        super.onStop()
        // Stop Fotoapparat
        fotoapparat.stop()
    }

    private fun takePhoto() {
        // Take photo using Fotoapparat
        fotoapparat.takePicture()
            .saveToFile(photoFile)
    }
}






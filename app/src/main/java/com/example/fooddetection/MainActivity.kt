package com.example.fooddetection

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.fotoapparat.Fotoapparat
import io.fotoapparat.view.CameraView
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.UUID


class MainActivity : AppCompatActivity() {

    private lateinit var cameraView: CameraView
    private lateinit var takePhotoButton: Button
    private lateinit var fotoapparat: Fotoapparat
    private lateinit var yoloModel: Interpreter


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

        try {
            yoloModel = Interpreter(loadModelFile(this, "best-fp16.tflite"))
        } catch (e: Exception) {
            Log.e("ObjectDetectionActivity", "Error reading model file", e)
        }
    }

    override fun onStart() {
        super.onStart()
        if (hasNoPermissions()) {
            requestPermission()
        }
        // Start Fotoapparat
        fotoapparat.start()
    }

    override fun onStop() {
        super.onStop()
        // Stop Fotoapparat
        fotoapparat.stop()
    }

    private fun takePhoto() {
        // Assign filename and location
        val photoName = UUID.randomUUID().toString() + ".jpg"
        val photoFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            photoName
        )

        // Take photo using Fotoapparat
        val photoResult = fotoapparat.takePicture()
        photoResult.saveToFile(photoFile)

        photoResult.toBitmap().whenAvailable { bitmapPhoto ->
            if (bitmapPhoto != null) {
                val shhhh = assets.open("hard-boiled-eggs1.jpg")
                val bitmapEgg = BitmapFactory.decodeStream(shhhh)
                //detectObjects(bitmapPhoto.bitmap)
                detectObjects(bitmapEgg)
            }
        }
    }

    private fun detectObjects(imageBitmap: Bitmap) {
        // Preprocess the image
        val inputTensor = preprocessImage(imageBitmap)

        inputTensor.rewind() // Reset the position of the buffer to the beginning

        val stringBuilder = StringBuilder()
        while (inputTensor.hasRemaining()) {
            val byteValue = inputTensor.get()
            stringBuilder.append(byteValue.toString()).append(", ")
        }

        Log.d("ByteBuffer Contents", stringBuilder.toString())

        setContentView(R.layout.photo_view)
        val imageView: ImageView = findViewById(R.id.image_view)

        // Run the model inference
        val outputTensor = runInference(inputTensor)

        // Process the model output
        val scores = outputTensor[0]

        // TODO: Implement code to process the detection results and display them
        val detectionResults = mutableListOf<DetectionResult>()
        for (score in scores) {
            val scoreList = score.toMutableList()

            for(i in 0..4) {
                scoreList.removeAt(i)
            }

            val highestScore = scoreList.max()
            val classIndex = scoreList.indexOf(highestScore)
            val objectness = score[4]
            Log.d("conf", objectness.toString())
            //if(objectness > 0.6){
            val confidence = score[classIndex]
            Log.d("", confidence.toString())
            val boundingBox = getBoundingBox(score)

            val detectionResult = DetectionResult(classIndex, confidence, boundingBox)
            if(confidence > 0.4f) {
                detectionResults.add(detectionResult)
            }
            //}
        }

        val newBitmap = displayDetectionResults(imageBitmap, detectionResults)

        // Example code to display the image with bounding boxes
        imageView.setImageBitmap(newBitmap)
    }

    private fun getBoundingBox(score: FloatArray): BoundingBox {
        val x = score[0]
        val y = score[1]
        val width = score[2]
        val height = score[3]
        return BoundingBox(x, y, height, width)
    }

    private fun preprocessImage(imageBitmap: Bitmap): ByteBuffer {
        val inputShape = yoloModel.getInputTensor(0).shape()
        val imageSizeX = inputShape[1]
        val imageSizeY = inputShape[2]
        val imageChannels = inputShape[3]

        val inputByteBuffer = ByteBuffer.allocateDirect(1 * imageSizeX * imageSizeY * imageChannels * 4)
        inputByteBuffer.order(ByteOrder.nativeOrder())

        var bitmapResized = resizeBitmap(imageBitmap)

        val pixels = IntArray(imageSizeX * imageSizeY)
        bitmapResized.getPixels(pixels, 0, bitmapResized.width, 0, 0, bitmapResized.width, bitmapResized.height)

        var pixel = 0
        for (i in 0 until imageSizeX) {
            for (j in 0 until imageSizeY) {
                val pixelVal = pixels[pixel++]

                val r = (pixelVal shr 16 and 0xFF).toFloat()
                val g = (pixelVal shr 8 and 0xFF).toFloat()
                val b = (pixelVal and 0xFF).toFloat()

                inputByteBuffer.putFloat(r / 255.0f)
                inputByteBuffer.putFloat(g / 255.0f)
                inputByteBuffer.putFloat(b / 255.0f)
            }
        }

        Log.println(Log.DEBUG,"acc", inputByteBuffer.toString())

        return inputByteBuffer
    }

    private fun runInference(inputTensor: ByteBuffer): Array<Array<FloatArray>> {
        val outputShape = yoloModel.getOutputTensor(0).shape()

        for(item in outputShape){
            Log.d("ansdkas", item.toString())
        }

        val height = outputShape[2]
        val width = outputShape[1]

        val outputBuffer = Array(1) { Array(width) { FloatArray(height) } }

        yoloModel.run(inputTensor, outputBuffer)

        return outputBuffer
    }

    private fun hasNoPermissions(): Boolean {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
    }

    val permissions = arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE)

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, permissions, 0)
    }

    private fun loadModelFile(context: Context, modelFileName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelFileName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun resizeBitmap(imageBitmap: Bitmap): Bitmap {
        val maxSide = 640

        val width = imageBitmap.width
        val height = imageBitmap.height

        val scaleFactor = if (width > height) {
            maxSide.toFloat() / width
        } else {
            maxSide.toFloat() / height
        }

        val resizedWidth = (width * scaleFactor).toInt()
        val resizedHeight = (height * scaleFactor).toInt()

        return Bitmap.createScaledBitmap(imageBitmap, resizedWidth, resizedHeight, false)
    }

    private fun displayDetectionResults(imageBitmap: Bitmap, detectionResults: List<DetectionResult>): Bitmap? {
        val mutableBitmap = imageBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.STROKE
        paint.color = Color.RED
        paint.strokeWidth = 2f

        for (detectionResult in detectionResults) {
            var bb = detectionResult.boundingBox

            val bbLeft = bb.x * mutableBitmap.width
            val bbTop = bb.y * mutableBitmap.height
            val bbRight = bbLeft + (bb.width * mutableBitmap.width)
            val bbBottom = bbTop + (bb.height * mutableBitmap.height)

            val rect = RectF(bbLeft, bbTop, bbRight, bbBottom)

            canvas.drawRect(rect, paint)

            // Display the class label and confidence score
            val label = getClassLabel(detectionResult.classIndex)
            Log.d("", label)
            val confidence = detectionResult.confidence
            val text = "$label: $confidence"
            canvas.drawText(text, rect.left, rect.top, paint)
        }

        return mutableBitmap
    }

    private fun getClassLabel(classIndex: Int): String {
        // Assuming there is a predefined mapping of class indices to class labels
        val classLabels = arrayOf("steak", "minced-beef", "chicken-breast", "salmon",
            "bacon","sausage","fried-egg","boiled-egg",
            "kidney-beans", "peas", "peanuts", "cashews",
            "cheddar", "mozzarella", "white-bread", "wholemeal-bread",
            "boiled-potato", "roast-potato", "fries", "pasta-penne", "pasta-spaghetti",
            "rice","carrot","broccoli","tomato","cucumber","mushroom","apple","orange","banana","euro-coin")
        return if (classIndex in classLabels.indices) {
            classLabels[classIndex]
        } else {
            "Unknown"
        }
    }

    data class DetectionResult(
        val classIndex: Int,
        val confidence: Float,
        val boundingBox: BoundingBox
    )

    data class BoundingBox(
        val x: Float,
        val y: Float,
        val height: Float,
        val width: Float
    )
}
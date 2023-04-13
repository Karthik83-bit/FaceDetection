package com.example.facedetection

import android.content.ContentResolver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.FaceDetector
import android.media.Image
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.graphics.toRectF
import com.example.facedetection.databinding.ActivityMainBinding
import com.example.facedetection.util.util
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.security.Permission
import kotlin.reflect.typeOf

class MainActivity : AppCompatActivity() {
    private lateinit var recognizer: TextRecognizer
    private lateinit var binding: ActivityMainBinding
     var imageUri: Uri?=null
     var imgBitmap:Bitmap?=null
    var mlInputImage:InputImage?=null
    var highAccuracyOpts:FaceDetectorOptions?=null
    lateinit var registerActivityForImageLoading:ActivityResultLauncher<String>
    lateinit var registerCaptureACtivityResult:ActivityResultLauncher<Uri>
    lateinit var detector:com.google.mlkit.vision.face.FaceDetector
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
    binding=ActivityMainBinding.inflate(layoutInflater)

        askPermission()
        initialization()
        registeringActivityForResult()
        onClickListeners()
        setContentView(binding.root)

    }



    private fun onClickListeners() {
        binding.capImg.setOnClickListener {
            binding.imageView.setImageResource(R.drawable.baseline_document_scanner_24)

            registerCaptureACtivityResult.launch(imageUri)
        }

        binding.getImg.setOnClickListener {
            registerActivityForImageLoading.launch("image/*")
        }


        binding.facemask.setOnClickListener {
//            binding.imageView.visibility=View.INVISIBLE
            detector.process(mlInputImage!!).addOnSuccessListener {
                Log.d("TAG", "onCreate: ${it.toString()}")
                if(it.isEmpty()||it.size==0){
                    createAlert("No Face Detected")
                }
                for(each in it){
                    Log.d("TAG", "onCreate: ${imgBitmap}")
                    val outputBitmap=imgBitmap!!.copy(Bitmap.Config.ARGB_8888,true)
                    Log.d("TAG", "onCreate: ${outputBitmap}")
                    val canvas=Canvas(outputBitmap)
                    val penRect=Paint()

                    penRect.color=resources.getColor(R.color.purple_200)

                    penRect.style=Paint.Style.FILL_AND_STROKE
                    penRect.strokeWidth=5f
                    val rect=each.boundingBox

                    Log.d("rexct", "onCreate: ${rect}")
                    canvas.drawRect(each.boundingBox,penRect)
//                    Rect(607, 389 ,636, 428)
//                    canvas.drawRect(Rect(428, 230 , 713, 606),penRect)
                    binding.imageView.setImageBitmap(outputBitmap)

                }
            }.addOnFailureListener {
                Log.d("TAG", "onCreate: ${it.toString()}")

            }
        }

        binding.adhaarNoMask.setOnClickListener {
            recognizer.process(mlInputImage!!).addOnSuccessListener {
                val result=it.textBlocks
                var tempAdhaar=""
                for(each in result){
                    Log.d("Bloc", "onClickListeners: ${each} ")

                    tempAdhaar= each.text.trim().replace(" ","")
                    Log.d("Bloc", "onClickListeners: ${tempAdhaar.replace(" ","")} ")
                    Log.d("adhaarmasking", "onClickListeners:${util.validateAadharNumber(tempAdhaar.replace(" ",""))} ")
                    if(util.validateAadharNumber(tempAdhaar)){
                        each.boundingBox
                        val outputBitmap=imgBitmap!!.copy(Bitmap.Config.ARGB_8888,true)
                        Log.d("TAG", "onCreate: ${outputBitmap}")
                        val canvas=Canvas(outputBitmap)
                        val penRect=Paint()

                        penRect.color=resources.getColor(R.color.white)

                        penRect.style=Paint.Style.FILL_AND_STROKE
                        penRect.strokeWidth=5f
                        val rect=each.boundingBox

                        Log.d("rexct", "onCreate: ${rect}")
                        canvas.drawRect(each.boundingBox!!,penRect)
//                    Rect(607, 389 ,636, 428)
//                    canvas.drawRect(Rect(428, 230 , 713, 606),penRect)
                        binding.imageView.setImageBitmap(outputBitmap)

                    }
                }


            }.addOnFailureListener {
                it.printStackTrace()
                createAlert(it.localizedMessage)
            }
        }


    }

    private fun createAlert(s: String) {
        AlertDialog.Builder(this).setTitle(s).setPositiveButton("ok",object :DialogInterface.OnClickListener{
            override fun onClick(dialog: DialogInterface?, which: Int) {
              dialog?.cancel()
                binding.imageView.setImageResource(R.drawable.baseline_document_scanner_24)
            }

        }).show()
    }

    private fun registeringActivityForResult() {
        registerActivityForImageLoading= registerForActivityResult(ActivityResultContracts.GetContent()){
            binding.imageView.setImageURI(it)
            imgBitmap=MediaStore.Images.Media.getBitmap(this.contentResolver,it)
            mlInputImage=InputImage.fromBitmap(imgBitmap!!,0)
        }
        registerCaptureACtivityResult=registerForActivityResult(ActivityResultContracts.TakePicture()){
            if(!it){
                binding.imageView.setImageURI(Uri.parse("ghhg"))
            }
                Log.d("TAG", "registeringActivityForResult:$it ")

            binding.imageView.setImageURI(imageUri)
            imgBitmap=MediaStore.Images.Media.getBitmap(this.contentResolver,imageUri)
            mlInputImage=InputImage.fromBitmap(imgBitmap!!,0)

        }
    }

    private fun initialization() {
         highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
         detector=FaceDetection.getClient(highAccuracyOpts!!)
        imageUri= createImageUri()
        recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    }

    private fun createImageUri(): Uri? {
        val image= File(applicationContext.filesDir,"camera_photo.png")
        return FileProvider.getUriForFile(applicationContext,"com.example.facedetection.fileprovider",image
        )

    }

    private fun askPermission() {
       if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
           if(checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED) {
               registerForActivityResult(
                   ActivityResultContracts.RequestPermission()
               ){

               }.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
           }

       }
    }



}
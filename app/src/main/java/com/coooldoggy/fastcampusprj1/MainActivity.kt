package com.coooldoggy.fastcampusprj1

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.vision.v1.Vision
import com.google.api.services.vision.v1.VisionRequest
import com.google.api.services.vision.v1.VisionRequestInitializer
import com.google.api.services.vision.v1.model.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.main_analyze_view.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    private val CAMERA_PERMISSION_REQUEST = 1000
    private val GALLERY_PERMISSION_REQUEST = 1001
    private val FILE_NAME = "picture.jpg"
    private var uploadChooser: UploadChooser? = null
    private val MAX_LABEL_RESULT = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupListener()
    }

    private fun setupListener() {
        upload_image.setOnClickListener {
            uploadChooser = UploadChooser().apply {
                addNotifier(object : UploadChooser.uploadChooserNotifierInterface {
                    override fun cameraOnClick() {
                        checkCameraPermission()
                    }

                    override fun galleryOnClick() {
                        checkGalleryPermission()
                    }

                })
            }
            uploadChooser!!.show(supportFragmentManager, "")
        }
    }

    private fun checkCameraPermission() {
        if (PermissionUtil().requestPermission(
                this,
                CAMERA_PERMISSION_REQUEST,
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            openCamera()
        }
    }

    private fun checkGalleryPermission() {
        if (PermissionUtil().requestPermission(
                this, GALLERY_PERMISSION_REQUEST, android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            openGallery()
        }
    }

    private fun openCamera() {
        var photoUri = FileProvider.getUriForFile(
            this,
            applicationContext.packageName + ".provider",
            createCameraFile()
        )
        startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, CAMERA_PERMISSION_REQUEST)
    }

    private fun openGallery() {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
        }
        startActivityForResult(
            Intent.createChooser(intent, "Select a photo"),
            GALLERY_PERMISSION_REQUEST
        )
    }

    private fun createCameraFile(): File {
        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File(dir, FILE_NAME)
    }

    private fun uploadImage(imageUri: Uri) {
        val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
        uploaded_image.setImageBitmap(bitmap)
        uploadChooser?.dismiss()
        requestCloudVisionApi(bitmap)
    }

    private fun prepareImageRequest(bitmap: Bitmap): Vision.Images.Annotate {
        val httpTransport = AndroidHttp.newCompatibleTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()
        val requestInitializer =
            object : VisionRequestInitializer(resources.getString(R.string.googlekey)) {
                override fun initializeVisionRequest(request: VisionRequest<*>?) {
                    super.initializeVisionRequest(request)
                    val packageName = packageName
                    request?.requestHeaders?.set(resources.getString(R.string.header), packageName)
                    val sig = PackageManagerUtil().getSignature(packageManager, packageName)
                    request?.requestHeaders?.set(resources.getString(R.string.headercerf), sig)
                }
            }

        val builder = Vision.Builder(httpTransport, jsonFactory, null)
        builder.setVisionRequestInitializer(requestInitializer)
        val vision = builder.build()
        val batchAnnotationRequest = BatchAnnotateImagesRequest()
        batchAnnotationRequest.requests = object : ArrayList<AnnotateImageRequest>() {
            init {
                val annotationImageRequest = AnnotateImageRequest()
                val base64EncodedImage = Image()
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
                val imageBytes = byteArrayOutputStream.toByteArray()
                base64EncodedImage.encodeContent(imageBytes)
                annotationImageRequest.image = base64EncodedImage
                annotationImageRequest.features = object : ArrayList<Feature>(){
                    init {
                        val labelDetection = Feature()
                        labelDetection.type = "LABEL_DETECTION"
                        labelDetection.maxResults = MAX_LABEL_RESULT
                        add(labelDetection)
                    }
                }
                add(annotationImageRequest)
            }
        }
        val annotateRequest = vision.images().annotate(batchAnnotationRequest)
        annotateRequest.disableGZipContent = true
        return annotateRequest
    }

    private fun requestCloudVisionApi(bitmap: Bitmap) {
        val visionTask = ImageRequestTask(this, ,)
    }

    private fun convertResponseToString(response: BatchAnnotateImagesResponse): String {
        val message = StringBuilder("분석 결과\n")
        val labels = response.responses[0].labelAnnotations
        labels?.let {
            it.forEach {
                message.append(String().format(Locale.US, "%.3f: %s", it.score, it.description))
                message.append("\n")
            }
            return message.toString()
        }
        return "분석실패"
    }

    inner class ImageRequestTask constructor(
        activity: MainActivity,
        val request: Vision.Images.Annotate
    ) : AsyncTask<Any, Void, String>() {
        private val weakReference: WeakReference<MainActivity>

        init {
            weakReference = WeakReference(activity)
        }

        override fun doInBackground(vararg p0: Any?): String {
            try {
                val response = request.execute()
                return convertResponseToString(response)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return "분석실패"
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                if (resultCode != Activity.RESULT_OK) return
                val photoUri = FileProvider.getUriForFile(
                    this,
                    applicationContext.packageName + ".provider",
                    createCameraFile()
                )
                uploadImage(photoUri)
            }
            GALLERY_PERMISSION_REQUEST -> {
                data?.let {
                    it.data?.let { uri -> uploadImage(uri) }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                if (PermissionUtil().permissionGranted(
                        requestCode,
                        CAMERA_PERMISSION_REQUEST,
                        grantResults
                    )
                ) {
                    openCamera()
                }
            }
            GALLERY_PERMISSION_REQUEST -> {
                if (PermissionUtil().permissionGranted(
                        requestCode,
                        GALLERY_PERMISSION_REQUEST,
                        grantResults
                    )
                ) {
                    openGallery()
                }
            }
        }
    }
}

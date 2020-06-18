package com.coooldoggy.fastcampusprj1

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.main_analyze_view.*
import java.io.File
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {

    private val CAMERA_PERMISSION_REQUEST = 1000
    private val GALLERY_PERMISSION_REQUEST = 1001
    private val FILE_NAME = "picture.jpg"
    private var uploadChooser: UploadChooser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupListener()
    }

    private fun setupListener(){
        upload_image.setOnClickListener {
            uploadChooser = UploadChooser().apply {
                addNotifier(object : UploadChooser.uploadChooserNotifierInterface{
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

    private fun checkCameraPermission(){
       if (PermissionUtil().requestPermission(
               this, CAMERA_PERMISSION_REQUEST, android.Manifest.permission.CAMERA, android.Manifest.permission.READ_EXTERNAL_STORAGE
           )){
           openCamera()
       }
    }

    private fun checkGalleryPermission(){
       if ( PermissionUtil().requestPermission(
               this, GALLERY_PERMISSION_REQUEST, android.Manifest.permission.READ_EXTERNAL_STORAGE
           )){
           openGallery()
       }
    }

    private fun openCamera(){
        var photoUri = FileProvider.getUriForFile(this, applicationContext.packageName + ".provider", createCameraFile())
        startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, CAMERA_PERMISSION_REQUEST)
    }

    private fun openGallery(){
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
        }
        startActivityForResult(Intent.createChooser(intent,"Select a photo"), GALLERY_PERMISSION_REQUEST)
    }

    private fun createCameraFile(): File{
        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File(dir, FILE_NAME)
    }

    private fun uploadImage(imageUri: Uri){
        val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
        uploaded_image.setImageBitmap(bitmap)
        uploadChooser?.dismiss()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            CAMERA_PERMISSION_REQUEST->{
                if (resultCode != Activity.RESULT_OK) return
                val photoUri = FileProvider.getUriForFile(this, applicationContext.packageName+".provider", createCameraFile())
                uploadImage(photoUri)
            }
            GALLERY_PERMISSION_REQUEST->{
                data?.let {
                    it.data?.let { uri -> uploadImage(uri) }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            CAMERA_PERMISSION_REQUEST->{
                if (PermissionUtil().permissionGranted(requestCode, CAMERA_PERMISSION_REQUEST, grantResults)){
                    openCamera()
                }
            }
            GALLERY_PERMISSION_REQUEST->{
                if(PermissionUtil().permissionGranted(requestCode, GALLERY_PERMISSION_REQUEST, grantResults)){
                    openGallery()
                }
            }
        }
    }
}

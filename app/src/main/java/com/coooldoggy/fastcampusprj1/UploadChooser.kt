package com.coooldoggy.fastcampusprj1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.upload_chooser.*

class UploadChooser: BottomSheetDialogFragment() {

    interface uploadChooserNotifierInterface{
        fun cameraOnClick()
        fun galleryOnClick()
    }

    var uploadChooserNotifier: uploadChooserNotifierInterface? = null

    fun addNotifier(listener: uploadChooserNotifierInterface){
        uploadChooserNotifier = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return  layoutInflater.inflate(R.layout.upload_chooser, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setUpListener()
    }

    private fun setUpListener(){
        upload_camera.setOnClickListener {
            uploadChooserNotifier?.cameraOnClick()
        }
        upload_gallery.setOnClickListener {
            uploadChooserNotifier?.galleryOnClick()
        }
    }
}
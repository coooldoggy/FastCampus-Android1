package com.coooldoggy.fastcampusprj1

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionUtil {

    fun requestPermission(activity: Activity, requestCode: Int, vararg permissions: String):Boolean{
        var granted = true
        val permissionNeeded = ArrayList<String>()

        permissions.forEach {
            val permissionCheck = ContextCompat.checkSelfPermission(activity, it)
            val hasPermission = permissionCheck == PackageManager.PERMISSION_GRANTED
            granted = granted and hasPermission
            if (!hasPermission){
                permissionNeeded.add(it)
            }
        }
        return if (granted){
            true
        }else{
            ActivityCompat.requestPermissions(activity, permissionNeeded.toTypedArray(), requestCode)
            false
        }
    }

    fun permissionGranted(requestCode: Int, permissionCode: Int, grantResult: IntArray): Boolean{
        return requestCode == permissionCode && grantResult.isNotEmpty() && grantResult[0] == PackageManager.PERMISSION_GRANTED
    }

}
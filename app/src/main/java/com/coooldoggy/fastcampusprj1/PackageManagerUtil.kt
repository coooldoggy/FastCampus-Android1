package com.coooldoggy.fastcampusprj1

import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Message
import com.google.common.io.BaseEncoding
import java.lang.Exception
import java.security.MessageDigest
import java.security.spec.ECField

class PackageManagerUtil {
    fun getSignature(pa: PackageManager, packageName: String): String?{
        return try {
            val packageInfo = pa.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            if (packageInfo?.signatures == null
                || packageInfo.signatures.isNotEmpty()
                || packageInfo.signatures[0] == null
            ){
                null
            }else{
                signatureDigest(packageInfo.signatures[0])
            }
        }catch (e:Exception){
            null
        }
    }

    private fun signatureDigest(sig: Signature): String?{
        val signature = sig.toByteArray()
        return try {
            val med = MessageDigest.getInstance("SHA1")
            val digest = med.digest(signature)
            BaseEncoding.base16().lowerCase().encode(digest)
        }catch (e: Exception){
            null
        }
    }
}
package com.baxramov.eloonnet

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class FilesUploader(private val activity: Activity) {
    private val isMultiple = true
    private val mime_type = "*/*"
    private var camera_data: String? = null
    private var file_data: ValueCallback<Uri>? = null
    private var file_path: ValueCallback<Array<Uri>>? = null
    private val file_req_code = 1862
    private val requestcode: Int = 1826

    fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: WebChromeClient.FileChooserParams?
    ): Boolean {

        return if (hasPermission()) {
            file_path = filePathCallback
            var takePictureIntent: Intent? = null
            var takeVideoIntent: Intent? = null
            var includeVideo = false
            var includePhoto = false

            paramCheck@ for (acceptTypes in fileChooserParams!!.acceptTypes) {
                val splitTypes = acceptTypes.split(", ?+".toRegex()).toTypedArray()
                for (acceptType in splitTypes) {
                    when (acceptType) {
                        "*/*" -> {
                            includePhoto = true
                            includeVideo = true
                            break@paramCheck
                        }
                        "image/*" -> includePhoto = true
                        "video/*" -> includeVideo = true
                    }
                }
            }
            if (fileChooserParams.acceptTypes.size === 0) {
                includePhoto = true
                includeVideo = true
            }
            if (includePhoto) {
                takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
                    var photoFile: File? = null
                    try {
                        photoFile = create_image()
                        takePictureIntent.putExtra("PhotoPath", camera_data)
                    } catch (ex: IOException) {

                    }
                    if (photoFile != null) {
                        camera_data = "file:" + photoFile.getAbsolutePath()
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile))
                    } else {
                        camera_data = null
                        takePictureIntent = null
                    }
                }
            }
            if (includeVideo) {
                takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                if (takeVideoIntent.resolveActivity(activity.getPackageManager()) != null) {
                    var videoFile: File? = null
                    try {
                        videoFile = create_video()
                    } catch (ex: IOException) {

                    }
                    if (videoFile != null) {
                        camera_data = "file:" + videoFile.getAbsolutePath()
                        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(videoFile))
                    } else {
                        camera_data = null
                        takeVideoIntent = null
                    }
                }
            }
            val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
            contentSelectionIntent.type = mime_type
            if (isMultiple) {
                contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            val intentArray: Array<Intent?>
            intentArray = if (takePictureIntent != null && takeVideoIntent != null) {
                arrayOf(takePictureIntent, takeVideoIntent)
            } else takePictureIntent?.let { arrayOf(it) }
                ?: (takeVideoIntent?.let { arrayOf(it) } ?: arrayOfNulls(0))
            val chooserIntent = Intent(Intent.ACTION_CHOOSER)
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "File chooser")
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
            activity.startActivityForResult(chooserIntent, file_req_code)
            true
        } else {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                requestcode
            )
            false
        }
    }

    @Throws(IOException::class)
    private fun create_image(): File? {
        @SuppressLint("SimpleDateFormat") val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "img_" + timeStamp + "_"
        val storageDir: File = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    @Throws(IOException::class)
    private fun create_video(): File? {
        @SuppressLint("SimpleDateFormat") val file_name =
            SimpleDateFormat("yyyy_mm_ss").format(Date())
        val new_name = "file_" + file_name + "_"
        val sd_directory: File = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(new_name, ".3gp", sd_directory)
    }

    fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (Build.VERSION.SDK_INT >= 21) {
            var results: Array<Uri?>? = null

            if (resultCode === androidx.appcompat.app.AppCompatActivity.RESULT_CANCELED) {
                file_path!!.onReceiveValue(null)
                return
            }
            if (resultCode === androidx.appcompat.app.AppCompatActivity.RESULT_OK) {
                if (null == file_path) {
                    return
                }
                var clipData: ClipData?
                var stringData: String?
                try {
                    clipData = intent!!.clipData
                    stringData = intent.dataString
                } catch (e: Exception) {
                    clipData = null
                    stringData = null
                }
                if (clipData == null && stringData == null && camera_data != null) {
                    results = arrayOf(Uri.parse(camera_data))
                } else {
                    if (clipData != null) {
                        val numSelectedFiles = clipData.itemCount
                        results = arrayOfNulls(numSelectedFiles)
                        for (i in 0 until clipData.itemCount) {
                            results[i] = clipData.getItemAt(i).uri
                        }
                    } else {
                        try {
                            val cam_photo = intent!!.extras!!["data"] as Bitmap?
                            val bytes = ByteArrayOutputStream()
                            cam_photo!!.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
                            stringData = MediaStore.Images.Media.insertImage(
                                activity.contentResolver,
                                cam_photo,
                                null,
                                null
                            )
                        } catch (ignored: Exception) {
                        }
                        results = arrayOf(Uri.parse(stringData))
                    }
                }
            }
            file_path!!.onReceiveValue(results as Array<Uri>)
            file_path = null
        } else {
            if (requestCode === file_req_code) {
                if (null == file_data) return
                val result =
                    if (intent == null || resultCode !== androidx.appcompat.app.AppCompatActivity.RESULT_OK) null else intent.data
                file_data!!.onReceiveValue(result)
                file_data = null
            }
        }
    }

}
package com.example.dropzonekotlin

import android.os.Environment
import java.io.File


class BluetoothConnectionHelper {

    companion object {

        const val MEDIA_TYPE_UNKNOWN = 0


        const val MEDIA_TYPE_IMAGE = 1


        const val MEDIA_TYPE_VIDEO = 2


        const val MEDIA_TYPE_PDF = 3


        const val MEDIA_TYPE_AUDIO = 4


        fun isExternalStorageWritable(): Boolean {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state
        }


        fun getPublicStorageDir(fileName: String): File {
            val mediaType = getFileType(fileName)
            val directoryType: String
            when (mediaType) {
                MEDIA_TYPE_IMAGE -> directoryType = Environment.DIRECTORY_PICTURES
                MEDIA_TYPE_VIDEO -> directoryType = Environment.DIRECTORY_MOVIES
                MEDIA_TYPE_AUDIO -> directoryType = Environment.DIRECTORY_MUSIC
                MEDIA_TYPE_PDF -> directoryType = Environment.DIRECTORY_DOCUMENTS
                else -> directoryType = Environment.DIRECTORY_DOWNLOADS
            }
            return File(Environment.getExternalStoragePublicDirectory(directoryType), fileName)
        }


        private fun getFileType(fileName: String): Int {
            var extension: String? = null
            val i = fileName.lastIndexOf('.')
            if (i > 0) {
                extension = fileName.substring(i + 1)
            }
            if (extension == null) {
                return MEDIA_TYPE_UNKNOWN
            }
            when (extension) {
                "png", "jpg" -> return MEDIA_TYPE_IMAGE
                "pdf" -> return MEDIA_TYPE_PDF
                "mp3" -> return MEDIA_TYPE_AUDIO
                "mp4" -> return MEDIA_TYPE_VIDEO
                else -> return MEDIA_TYPE_UNKNOWN
            }
        }
    }
}
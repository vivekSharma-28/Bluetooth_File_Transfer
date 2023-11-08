package com.example.dropzonekotlin.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.Looper
import android.widget.ProgressBar
import android.widget.TextView
import com.example.dropzonekotlin.service.BluetoothConnectionHelper.Companion.getPublicStorageDir
import com.example.dropzonekotlin.service.BluetoothConnectionHelper.Companion.isExternalStorageWritable
import java.io.*
import java.util.*
import java.nio.ByteBuffer


class BluetoothConnectionService {


    companion object {

        val uuid: UUID = UUID.fromString("8989063a-c9af-463a-b3f1-f21d9b2b827b")


        var fileURI: String = ""


        var success: Boolean = false

        var totalBytesSent=0

    }


    fun startServer() {
        BluetoothServerController().start()
    }


    fun startClient(
        device: BluetoothDevice,
        uri: String,
        progressBar: ProgressBar,
        progressPercent: TextView
    ) : Boolean {
        fileURI = uri

        val btClient = BluetoothClient(device,progressBar,progressPercent)
        btClient.start()
        try {
            btClient.join()
        } catch (e: InterruptedException) {
        }

        return success
    }


    class BluetoothClient(device: BluetoothDevice, val progressBar: ProgressBar,val progressPercent: TextView) : Thread() {

        private val socket = device.createRfcommSocketToServiceRecord(uuid)

        override fun run() {
            try {
                this.socket.connect()
            } catch (e: IOException) {
                return
            }

            val outputStream = this.socket.outputStream
            val inputStream = this.socket.inputStream

            val file = File(fileURI)
            val fileBytes: ByteArray
            try {
                fileBytes = ByteArray(file.length().toInt())
                val bis = BufferedInputStream(FileInputStream(file))
                bis.read(fileBytes, 0, fileBytes.size)
                bis.close()
            } catch (e: IOException) {
                return
            }

            val fileNameSize = ByteBuffer.allocate(4)
            fileNameSize.putInt(file.name.toByteArray().size)

            val fileSize = ByteBuffer.allocate(4)
            fileSize.putInt(fileBytes.size)

            outputStream.write(fileNameSize.array())
            outputStream.write(file.name.toByteArray())
            outputStream.write(fileSize.array())


            fileBytes.forEachIndexed { _, byte ->
                outputStream.write(byte.toInt())
                totalBytesSent++
                updateProgressBar( bytesTransferred = totalBytesSent, totalBytes = file.length().toInt())
            }

            success = true

            try {
                sleep(5000)
                outputStream.close()
                inputStream.close()
                this.socket.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        private fun updateProgressBar(bytesTransferred: Int, totalBytes: Int) {
            val progress = (bytesTransferred * 100) / totalBytes
            Handler(Looper.getMainLooper()).post {
                progressBar.progress = progress
                progressPercent.text="${progress}%/${progressBar.max}%"
            }
        }
    }


    class BluetoothServerController : Thread() {

        private var cancelled: Boolean


        private val serverSocket: BluetoothServerSocket?


        init {
            val btAdapter = BluetoothAdapter.getDefaultAdapter()

            if (btAdapter != null) {
                this.serverSocket = btAdapter.listenUsingRfcommWithServiceRecord("DropZoneKotlin", uuid)
                this.cancelled = false
            } else {
                this.serverSocket = null
                this.cancelled = true
            }

        }


        override fun run() {
            var socket: BluetoothSocket

            while(true) {
                if (this.cancelled) {
                    break
                }

                try {
                    socket = serverSocket!!.accept()
                } catch(e: IOException) {
                    break
                }

                if (!this.cancelled && socket != null) {
                    BluetoothServer(socket).start()
                }
            }
        }


        fun cancel() {
            this.cancelled = true
            this.serverSocket!!.close()
        }
    }


    class BluetoothServer(private val socket: BluetoothSocket): Thread() {

        private val inputStream = this.socket.inputStream


        private val outputStream = this.socket.outputStream


        override fun run() {
            if (isExternalStorageWritable()) {
                val totalFileNameSizeInBytes: Int
                val totalFileSizeInBytes: Int

                val fileNameSizebuffer = ByteArray(4)
                inputStream!!.read(fileNameSizebuffer, 0, 4)
                var fileSizeBuffer = ByteBuffer.wrap(fileNameSizebuffer)
                totalFileNameSizeInBytes = fileSizeBuffer.int

                val fileNamebuffer = ByteArray(1024)
                inputStream.read(fileNamebuffer, 0, totalFileNameSizeInBytes)
                val fileName = String(fileNamebuffer, 0, totalFileNameSizeInBytes)

                val fileSizebuffer = ByteArray(4) // int => 4 bytes
                inputStream.read(fileSizebuffer, 0, 4)
                fileSizeBuffer = ByteBuffer.wrap(fileSizebuffer)
                totalFileSizeInBytes = fileSizeBuffer.int

                val baos = ByteArrayOutputStream()
                val buffer = ByteArray(1024)
                var read: Int
                var totalBytesRead = 0
                read = inputStream.read(buffer, 0, buffer.size)
                while (read != -1) {
                    baos.write(buffer, 0, read)
                    totalBytesRead += read
                    if (totalBytesRead == totalFileSizeInBytes) {
                        break
                    }
                    read = inputStream.read(buffer, 0, buffer.size)
                }
                baos.flush()

                val saveFile = getPublicStorageDir(fileName)
                if (saveFile.exists()) {
                    saveFile.delete()
                }
                val fos = FileOutputStream(saveFile.path)
                fos.write(baos.toByteArray())
                fos.close()
            }
            sleep(5000)
            inputStream.close()
            outputStream.close()
            socket.close()
        }
    }
}

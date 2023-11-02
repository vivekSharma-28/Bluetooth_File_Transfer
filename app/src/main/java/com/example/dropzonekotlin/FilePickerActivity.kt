package com.example.dropzonekotlin

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.EXTRA_DEVICE
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.AlarmClock.EXTRA_MESSAGE
import android.provider.Settings.Global
import kotlinx.android.synthetic.main.activity_file_selector.*
import org.jetbrains.anko.toast
import android.support.v7.app.AlertDialog
import java.io.File

class FilePickerActivity : AppCompatActivity() {

    private var device : BluetoothDevice? = null


    private var fileURI : String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_selector)

        supportActionBar?.hide()


        device = intent.getParcelableExtra(EXTRA_DEVICE)
        if (device!!.name == null) {
            user_info_name_value.text = device!!.address
        } else {
            user_info_name_value.text = device!!.name
        }

        file_select_button.setOnClickListener{ filePicker() }

        file_selector_send.setOnClickListener{ send() }
    }


    private fun filePicker() {
        val mimeTypes : Array<String> = arrayOf("image/*", "video/*", "application/pdf", "audio/*")


        val intent = Intent()
            .setType("*/*")
            .setAction(Intent.ACTION_GET_CONTENT) // ACTION_GET_CONTENT refers to the built-in file picker
            .addCategory(Intent.CATEGORY_OPENABLE)
            .putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)


        startActivityForResult(Intent.createChooser(intent, "Choose a file"), 111)
    }

    private fun send() {
        if (fileURI == "") {
            toast("Please choose a file first")
        } else {
            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder.setTitle("Sending Confirmation")
            alertDialogBuilder.setMessage("Are you sure you wish to send this file?")
            alertDialogBuilder.setPositiveButton(R.string.send) { _, _ -> checkLessThan5MB(fileURI) }
            alertDialogBuilder.setNegativeButton(R.string.cancel) { _, _ -> toast("File sending cancelled") }
            alertDialogBuilder.show()
        }
    }

    private fun checkLessThan5MB(fileURI : String) {
        val file = File(fileURI)

        if (file.readBytes().size > (1024 * 1024 * 5)) {

            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder.setTitle("File too large")
            alertDialogBuilder.setMessage("This file is larger than the 5MB Limit")
            alertDialogBuilder.setPositiveButton("OK") { _, _ ->
                toast("File sending failed")
            }
            alertDialogBuilder.show()
        }
        else {


            val intent = Intent(this, SendActivity::class.java)
            intent.putExtra(EXTRA_DEVICE, device!!)
            intent.putExtra(EXTRA_MESSAGE, fileURI)
            startActivity(intent)
            finish()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 111) {
            if (resultCode == RESULT_OK) {
                val selectedFile = data?.data

                val selectedFilePath = FilePickerHelper.getPath(this, selectedFile!!)

                file_info_name_value.text = selectedFilePath

                fileURI = selectedFilePath!!

            } else if (resultCode == RESULT_CANCELED) {
                toast("File choosing cancelled")
            } else {
                toast("Error with choosing this file")
            }
        }
    }
}

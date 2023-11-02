package com.example.dropzonekotlin

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.EXTRA_DEVICE
import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock.EXTRA_MESSAGE
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SendActivity : AppCompatActivity() {

    private var sendingResult: Boolean? = null
    private var device: BluetoothDevice? = null
    private var fileURI: String? = null
    private lateinit var sendLoading: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressPercent:TextView

    private val coroutineScope = CoroutineScope(Job() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send)
        sendLoading = findViewById(R.id.send_loading)
        progressBar = findViewById(R.id.progressBar)
        progressPercent=findViewById(R.id.progressPercent)

        device = intent.getParcelableExtra(EXTRA_DEVICE)
        fileURI = intent.getStringExtra(EXTRA_MESSAGE)

        coroutineScope.launch {
            waitFileSend()
        }
    }

    private suspend fun waitFileSend() {
        sendingResult = withContext(Dispatchers.IO) {
            BluetoothConnectionService().startClient(device!!, fileURI!!, progressBar,progressPercent)
        }
        sendingResultNew(sendingResult == true)
    }

    private fun sendingResultNew(value: Boolean) {
        progressBar.visibility = View.GONE
        progressPercent.visibility=View.GONE
        sendLoading.text = if (value) getString(R.string.success) else getString(R.string.failure)
//        startActivity(Intent(this@SendActivity,MainActivity::class.java))
    }

    override fun onDestroy() {
        coroutineScope.cancel()
        super.onDestroy()
    }
}


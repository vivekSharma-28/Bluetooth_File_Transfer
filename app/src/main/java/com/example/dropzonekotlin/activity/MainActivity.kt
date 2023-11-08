package com.example.dropzonekotlin.activity

import android.Manifest
import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.EXTRA_DEVICE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import com.example.dropzonekotlin.R
import com.example.dropzonekotlin.service.BluetoothConnectionService
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {


    private var m_bluetoothAdapter : BluetoothAdapter? = null

    private lateinit var m_pairedDevices : Set<BluetoothDevice>

    private lateinit var m_discoveredDevices : ArrayList<BluetoothDevice>

    private var isInZone : Boolean = false

    private val REQUEST_ENABLE_BLUETOOTH = 1

    private var MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 0

    private var MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 0

    private var MY_PERMISSIONS_REQUEST_READ_MEDIA_IMAGES=0

    private var MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()

        val permissionsTiramisu = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_MEDIA_IMAGES
        )

        val permissionsOther = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val requestCodeAllPermissions = 1

        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsTiramisu
        } else {
            permissionsOther
        }

        val permissionsNotGranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsNotGranted.isNotEmpty()) {
            if (permissionsNotGranted.any {
                    ActivityCompat.shouldShowRequestPermissionRationale(this, it)
                }) {
                Toast.makeText(this@MainActivity,"This app requires multiple permissions to function properly.",
                    Toast.LENGTH_SHORT).show()
            } else {
                ActivityCompat.requestPermissions(this, permissionsNotGranted, requestCodeAllPermissions)
            }
        }


        m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()


        if (m_bluetoothAdapter == null) {
            toast("This device does not support bluetooth")
            this.finish()
            return
        }

        if (!m_bluetoothAdapter!!.isEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH)
        }


        m_discoveredDevices = ArrayList()

        val deviceFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, deviceFilter)

        val deviceNameFilter = IntentFilter(BluetoothDevice.ACTION_NAME_CHANGED)
        registerReceiver(nameReceiver, deviceNameFilter)

        main_enter_zone.setOnClickListener { enterTheZone() }

        main_refresh_user_list.setOnClickListener{ refreshList() }
    }


    private val receiver = object : BroadcastReceiver() {


        override fun onReceive(context: Context, intent: Intent) {
            val action : String? = intent.action
            when(action) {
                BluetoothDevice.ACTION_FOUND -> {

                    val device: BluetoothDevice? = intent.getParcelableExtra(EXTRA_DEVICE)
                    val index = m_discoveredDevices.indexOf(device)
                    if (index == -1) {
                        if (device != null) {
                            m_discoveredDevices.add(device)
                        }
                    } else {
                        if (device != null) {
                            m_discoveredDevices[index] = device
                        }
                    }
                }
            }
        }
    }


    private val nameReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action : String? = intent.action
            when(action) {
                BluetoothDevice.ACTION_NAME_CHANGED -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(EXTRA_DEVICE)
                    val index = m_discoveredDevices.indexOf(device)
                    if (index == -1) {
                        if (device != null) {
                            m_discoveredDevices.add(device)
                        }
                    } else {
                        if (device != null) {
                            m_discoveredDevices[index] = device
                        }
                    }
                }
            }
        }
    }


    private fun refreshList() {
        if (!isInZone) {
            toast("Please enter the zone first")
            return
        }


        m_pairedDevices = m_bluetoothAdapter!!.bondedDevices


        val list : ArrayList<BluetoothDevice> = ArrayList()


        val listDeviceNames : ArrayList<String> = ArrayList()

        if (m_pairedDevices.isNotEmpty()) {
            for (device: BluetoothDevice in m_pairedDevices) {
                list.add(device)
                listDeviceNames.add(device.name)
            }
        } else {
            toast("No paired bluetooth devices found")
        }

        if (m_discoveredDevices.isNotEmpty()) {
            for (device: BluetoothDevice in m_discoveredDevices) {
                list.add(device)


                if (device.name == null) {
                    listDeviceNames.add(device.address)
                } else {
                    listDeviceNames.add(device.name)
                }
            }
        } else {
            toast("No new bluetooth devices found")
        }


        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listDeviceNames)
        main_select_user_list.adapter = adapter


        main_select_user_list.onItemClickListener = AdapterView.OnItemClickListener{_, _, position, _ ->
            val device: BluetoothDevice = list[position]

            val intent = Intent(this, FilePickerActivity::class.java)
            intent.putExtra(EXTRA_DEVICE, device)
            startActivity(intent)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {

            if (resultCode == Activity.RESULT_OK) {
                if (m_bluetoothAdapter!!.isEnabled) {
                    toast("Bluetooth has been enabled")
                } else {
                    toast("Bluetooth has been disabled")
                }
            } else if (resultCode == Activity.RESULT_CANCELED){
                toast("Bluetooth has been cancelled")
            }
        } else if (requestCode.equals(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)) {
            if (resultCode == Activity.RESULT_CANCELED) {
                toast("Bluetooth has been cancelled")
            }
        }
    }


    private fun changeTextToConnected(statusTextView : TextView) {
        statusTextView.setText(R.string.connected)
        statusTextView.setTextColor(Color.GREEN)

        main_enter_zone.setText(R.string.exitZone)
    }


    private fun changeTextToDisconnected(statusTextView : TextView) {
        statusTextView.setText(R.string.disconnected)
        statusTextView.setTextColor(Color.RED)

        main_enter_zone.setText(R.string.enterZone)
    }


    private fun enterTheZone() {
        if (isInZone) { exitTheZone() }
        else {
            if (!m_bluetoothAdapter!!.isEnabled) {
                m_bluetoothAdapter!!.enable()
            }

            val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            }
            startActivity(discoverableIntent)
            BluetoothConnectionService().startServer()

            changeTextToConnected(status_title)
            isInZone = true
            refreshList()
        }
    }


    private fun exitTheZone() {
        BluetoothConnectionService.BluetoothServerController().cancel()
        m_bluetoothAdapter!!.cancelDiscovery()

        main_select_user_list.adapter = null
        toast("Discoverability off")

        changeTextToDisconnected(status_title)
        isInZone = false
    }


    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(receiver)

        unregisterReceiver(nameReceiver)
    }
}

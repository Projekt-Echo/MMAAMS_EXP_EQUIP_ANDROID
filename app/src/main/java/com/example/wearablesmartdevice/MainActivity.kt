package com.example.wearablesmartdevice

import android.R
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.wearablesmartdevice.databinding.MainActivityBinding
import androidx.appcompat.app.AlertDialog
class MainActivity : AppCompatActivity() {
    private lateinit var binding: MainActivityBinding;
    private lateinit var bluetoothPermissionManager: BluetoothPermissionManager;
    private lateinit var bleManager: BLEManager  // 改用BLE管理器
    private var selectedDeviceAddress: String = ""
    private var isConnected = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        bluetoothPermissionManager= BluetoothPermissionManager(this)
        bleManager = BLEManager(this)
        binding.root.postDelayed({
            startPermissionCheck()
        }, 500)
        setupUI()
    }

    private fun startPermissionCheck() {
        bluetoothPermissionManager.checkBluetoothPermissions(object : BluetoothPermissionManager.BluetoothPermissionCallback {
            override fun onPermissionsGranted() {
                // 权限已获得，可以开始蓝牙功能
               loadPairedDevices()
            }

            override fun onPermissionsDenied() {
                // 权限被拒绝，可以显示一些提示或禁用相关功能
                // 这里可以添加您的处理逻辑
            }

            override fun onPermissionsPermanentlyDenied() {
                // 权限被永久拒绝，用户需要去设置中手动开启
                // 这里可以添加您的处理逻辑
            }
        })
    }
    private fun setupUI() {
        // 连接按钮
        binding.btnConnect.setOnClickListener {
            if (selectedDeviceAddress.isNotEmpty()) {
                connectToDevice()
            } else {
                showToast("请先选择设备")
            }
        }

        // 断开连接按钮
        binding.btnDisconnect.setOnClickListener {
            disconnectDevice()
        }

        // 发送字节流按钮
        binding.btnSendData.setOnClickListener {
            sendTextData()
        }
    }
    @SuppressLint("MissingPermission")
    private fun loadPairedDevices() {
        val devices = bleManager.getPairedDevices()
        if (devices.isNotEmpty()) {
            val deviceList = devices.map { device ->
                "${device.name ?: "未知设备"} - ${device.address}"
            }

            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, deviceList)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerDevices.adapter = adapter

            binding.spinnerDevices.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                    if (position >= 0) {
                        val selected = deviceList[position]
                        selectedDeviceAddress = selected.substringAfterLast(" - ")
                    }
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                    selectedDeviceAddress = ""
                }
            }
        } else {
            showToast("没有找到已配对的BLE设备")
        }
    }
    private fun connectToDevice() {
        showToast("正在连接BLE设备...")
        bleManager.connectToDevice(selectedDeviceAddress, object : BLEManager.BLECallback {
            override fun onDevicesLoaded(devices: List<BluetoothDevice>) {}
            override fun onConnectionSuccess(deviceName: String) {
                runOnUiThread {
                    isConnected = true
                    updateConnectionUI(true)
                    showToast("已连接到: $deviceName")
                }

            }
            override fun onConnectionFailed(errorMessage: String) {
                runOnUiThread {
                    isConnected = false
                    updateConnectionUI(false)
                    showToast("连接失败: $errorMessage")
                }

            }
            override fun onDataSent(success: Boolean, message: String) {
                runOnUiThread {
                    showToast(message)
                    if (success) {
                        binding.etDataInput.text.clear()
                    }
                }

            }
            override fun onServicesDiscovered() {
                Log.d("MainActivity", "BLE服务发现完成")
            }
        })
    }
    private fun disconnectDevice() {
        bleManager.disconnect()
        isConnected = false
        updateConnectionUI(false)
        showToast("已断开连接")
    }

    private fun sendTextData() {
        if (!isConnected) {
            showToast("请先连接设备")
            return
        }

        val inputText = binding.etDataInput.text.toString().trim()
        if (inputText.isEmpty()) {
            showToast("请输入要发送的数据")
            return
        }

        bleManager.sendTextData(inputText, object : BLEManager.BLECallback {
            override fun onDevicesLoaded(devices: List<BluetoothDevice>) {}
            override fun onConnectionSuccess(deviceName: String) {}
            override fun onConnectionFailed(errorMessage: String) {}
            override fun onDataSent(success: Boolean, message: String) {
                showToast(message)
                if (success) {
                    binding.etDataInput.text.clear()
                }
            }
            override fun onServicesDiscovered() {}
        })
    }

    private fun updateConnectionUI(connected: Boolean) {
        binding.btnConnect.isEnabled = !connected
        binding.btnDisconnect.isEnabled = connected
        binding.btnSendData.isEnabled = connected

        binding.tvStatus.text = if (connected) {
            "状态: 已连接"
        } else {
            "状态: 未连接"
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // 只需要这一行，将所有权限处理交给管理器
        bluetoothPermissionManager.handlePermissionResult(requestCode, permissions, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothPermissionManager.cleanup()
        bleManager.disconnect()
    }



}
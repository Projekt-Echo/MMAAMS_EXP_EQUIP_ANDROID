package com.example.wearablesmartdevice
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.io.IOException
import java.io.OutputStream
import java.util.*

class BluetoothConnectionManager(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    companion object {
        private const val TAG = "BluetoothConnection"
        val ESP32_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    interface BluetoothConnectionCallback {
        fun onConnectionSuccess(deviceName: String)
        fun onConnectionFailed(errorMessage: String)
        fun onDataSent(success: Boolean, message: String)
    }

    /**
     * 获取已配对的设备列表
     */
    fun getPairedDevices(): List<BluetoothDevice> {
        return try {
            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            Log.e(TAG, "获取配对设备权限错误: ${e.message}")
            emptyList()
        }
    }

    /**
     * 连接到指定的ESP32设备
     */
    fun connectToDevice(deviceAddress: String, callback: BluetoothConnectionCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
                device?.let { bluetoothDevice ->
                    Log.d(TAG, "尝试连接设备: ${bluetoothDevice.name} - $deviceAddress")

                    socket = bluetoothDevice.createRfcommSocketToServiceRecord(ESP32_UUID)
                    bluetoothAdapter?.cancelDiscovery()
                    socket?.connect()

                    withContext(Dispatchers.Main) {
                        outputStream = socket?.outputStream
                        callback.onConnectionSuccess(bluetoothDevice.name ?: "未知设备")
                    }
                } ?: run {
                    withContext(Dispatchers.Main) {
                        callback.onConnectionFailed("设备未找到")
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "连接失败: ${e.message}")
                withContext(Dispatchers.Main) {
                    callback.onConnectionFailed("连接失败: ${e.message}")
                }
                closeConnection()
            } catch (e: SecurityException) {
                Log.e(TAG, "连接权限错误: ${e.message}")
                withContext(Dispatchers.Main) {
                    callback.onConnectionFailed("蓝牙权限错误")
                }
            }
        }
    }

    /**
     * 发送字节流数据到ESP32
     */
    fun sendByteData(data: ByteArray, callback: BluetoothConnectionCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (outputStream == null || socket?.isConnected != true) {
                    withContext(Dispatchers.Main) {
                        callback.onDataSent(false, "未连接到设备")
                    }
                    return@launch
                }

                outputStream?.write(data)
                outputStream?.flush()

                withContext(Dispatchers.Main) {
                    callback.onDataSent(true, "发送成功 (${data.size} 字节)")
                }
            } catch (e: IOException) {
                Log.e(TAG, "发送失败: ${e.message}")
                withContext(Dispatchers.Main) {
                    callback.onDataSent(false, "发送失败")
                }
                closeConnection()
            } catch (e: Exception) {
                Log.e(TAG, "发送异常: ${e.message}")
                withContext(Dispatchers.Main) {
                    callback.onDataSent(false, "发送异常")
                }
            }
        }
    }

    /**
     * 关闭连接
     */
    fun closeConnection() {
        try {
            outputStream?.close()
            socket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "关闭连接时出错: ${e.message}")
        }
        outputStream = null
        socket = null
    }

    /**
     * 检查是否已连接
     */
    fun isConnected(): Boolean {
        return socket?.isConnected == true
    }

    /**
     * 获取当前连接的设备名称
     */
    @SuppressLint("MissingPermission")
    fun getConnectedDeviceName(): String {
        return try {
            socket?.remoteDevice?.name ?: "未知设备"
        } catch (e: Exception) {
            "未知设备"
        }
    }
}
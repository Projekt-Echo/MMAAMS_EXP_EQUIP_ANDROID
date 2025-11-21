package com.example.wearablesmartdevice
import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.util.*

class BLEManager(private val context: Context) {

    private var bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothGatt: BluetoothGatt? = null
    private var connectedDevice: BluetoothDevice? = null

    companion object {
        private const val TAG = "BLEManager"

        // 根据您的ESP32代码定义UUID
        private val SERVICE_LED_UUID = UUID.fromString("00001000-0000-1000-8000-00805f9b34fb")
        private val CHAR_LED_POWER_UUID = UUID.fromString("00001001-0000-1000-8000-00805f9b34fb")
        private val CHAR_TEXT_INPUT_UUID = UUID.fromString("00004002-0000-1000-8000-00805f9b34fb")
        private val UUID_SERVICE_SYSTEM = UUID.fromString("00004000-0000-1000-8000-00805f9b34fb")
    }

    interface BLECallback {
        fun onDevicesLoaded(devices: List<BluetoothDevice>)
        fun onConnectionSuccess(deviceName: String)
        fun onConnectionFailed(errorMessage: String)
        fun onDataSent(success: Boolean, message: String)
        fun onServicesDiscovered()
    }

    private var bleCallback: BLECallback? = null

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "BLE连接成功")
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "BLE断开连接")
                    bleCallback?.onConnectionFailed("设备断开连接")
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "服务发现成功")
                bleCallback?.onServicesDiscovered()
                connectedDevice?.name?.let { deviceName ->
                    bleCallback?.onConnectionSuccess(deviceName)
                }
            } else {
                Log.e(TAG, "服务发现失败: $status")
                bleCallback?.onConnectionFailed("服务发现失败")
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "数据写入成功")
                bleCallback?.onDataSent(true, "数据发送成功")
            } else {
                Log.e(TAG, "数据写入失败: $status")
                bleCallback?.onDataSent(false, "数据发送失败")
            }
        }
    }

    /**
     * 获取已配对的BLE设备
     */
    fun getPairedDevices(): List<BluetoothDevice> {
        return try {
            bluetoothAdapter?.bondedDevices?.filter { device ->
                device.type == BluetoothDevice.DEVICE_TYPE_LE ||
                        device.type == BluetoothDevice.DEVICE_TYPE_DUAL
            }?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            Log.e(TAG, "获取配对设备权限错误: ${e.message}")
            emptyList()
        }
    }

    /**
     * 连接到BLE设备
     */
    @SuppressLint("MissingPermission")
    fun connectToDevice(deviceAddress: String, callback: BLECallback) {
        this.bleCallback = callback

        try {
            val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
            device?.let { bluetoothDevice ->
                Log.d(TAG, "尝试连接BLE设备: ${bluetoothDevice.name} - $deviceAddress")
                connectedDevice = bluetoothDevice
                bluetoothGatt = bluetoothDevice.connectGatt(context, false, gattCallback)
            } ?: run {
                callback.onConnectionFailed("设备未找到")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "连接权限错误: ${e.message}")
            callback.onConnectionFailed("蓝牙权限错误")
        }
    }

    /**
     * 发送文本数据到ESP32
     */
    @SuppressLint("MissingPermission")
    fun sendTextData(text: String, callback: BLECallback) {
        this.bleCallback = callback

        if (bluetoothGatt == null) {
            callback.onDataSent(false, "未连接到设备")
            return
        }

        try {
            val service = bluetoothGatt?.getService(UUID_SERVICE_SYSTEM)
            if (service == null) {
                callback.onDataSent(false, "未找到LED服务")
                return
            }

            val characteristic = service.getCharacteristic(CHAR_TEXT_INPUT_UUID)
            if (characteristic == null) {
                // 打印所有可用的特征，帮助调试
                val allCharacteristics = service.characteristics
                Log.e(TAG, "未找到文本输入特征，可用的特征有:")
                allCharacteristics.forEach { char ->
                    Log.e(TAG, "特征UUID: ${char.uuid}, 属性: ${char.properties}")
                }
                callback.onDataSent(false, "未找到文本输入特征 (UUID: $CHAR_TEXT_INPUT_UUID)")
                return
            }

            // 将文本转换为字节数组
            val data = text.toByteArray(Charsets.UTF_8)
            characteristic.value = data
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

            val success = bluetoothGatt?.writeCharacteristic(characteristic) ?: false
            if (!success) {
                callback.onDataSent(false, "写入请求失败")
            }

        } catch (e: Exception) {
            Log.e(TAG, "发送数据异常: ${e.message}")
            callback.onDataSent(false, "发送异常: ${e.message}")
        }
    }

    /**
     * 断开连接
     */
    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        connectedDevice = null
    }

    /**
     * 检查是否已连接
     */
    fun isConnected(): Boolean {
        return bluetoothGatt != null
    }
}
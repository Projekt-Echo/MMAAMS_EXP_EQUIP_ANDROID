package com.example.wearablesmartdevice
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import java.util.UUID


enum class ServiceUUID(val uuid: UUID,val value: String){
    UUID_Service_LED(UUID.fromString("00001000-0000-1000-8000-00805f9b34fb"),"00001000-0000-1000-8000-00805f9b34fb"),
    UUID_Service_FAN(UUID.fromString("00002000-0000-1000-8000-00805f9b34fb"),"00002000-0000-1000-8000-00805f9b34fb"),
    UUID_Service_HEATER(UUID.fromString("00003000-0000-1000-8000-00805f9b34fb"),"00003000-0000-1000-8000-00805f9b34fb"),
    UUID_Service_SYSTEM(UUID.fromString("00004000-0000-1000-8000-00805f9b34fb"),"00004000-0000-1000-8000-00805f9b34fb");

    companion object {
        fun fromUuid(uuid: UUID): ServiceUUID? = values().firstOrNull { it.uuid == uuid }
        fun fromString(value: String): ServiceUUID? = values().firstOrNull { it.value.equals(value, ignoreCase = true) }
    }
}

enum class CharacteristicUUID(val uuid: UUID, val value: String){

    UUID_CHAR_LED_POWER(UUID.fromString("00001001-0000-1000-8000-00805f9b34fb"),"00001001-0000-1000-8000-00805f9b34fb"),
    UUID_CHAR_LED_RED(UUID.fromString("00001002-0000-1000-8000-00805f9b34fb"),"00001002-0000-1000-8000-00805f9b34fb"),
    UUID_CHAR_LED_GREEN(UUID.fromString("00001003-0000-1000-8000-00805f9b34fb"),"00001003-0000-1000-8000-00805f9b34fb"),
    UUID_CHAR_LED_BLUE(UUID.fromString("00001004-0000-1000-8000-00805f9b34fb"),"00001004-0000-1000-8000-00805f9b34fb"),

    UUID_CHAR_FAN_POWER(UUID.fromString("00002001-0000-1000-8000-00805f9b34fb"),"00002001-0000-1000-8000-00805f9b34fb"),
    UUID_CHAR_FAN_SPEED(UUID.fromString("00002002-0000-1000-8000-00805f9b34fb"),"00002002-0000-1000-8000-00805f9b34fb"),

    UUID_CHAR_HEATER_POWER(UUID.fromString("00003001-0000-1000-8000-00805f9b34fb"),"00003001-0000-1000-8000-00805f9b34fb"),

    UUID_CHAR_SYSTEM_EMERGENCY(UUID.fromString("00004001-0000-1000-8000-00805f9b34fb"),"00004001-0000-1000-8000-00805f9b34fb"),
    UUID_CHAR_SYSTEM_TEXTIN(UUID.fromString("00004002-0000-1000-8000-00805f9b34fb"),"00004002-0000-1000-8000-00805f9b34fb");

    companion object {
        fun fromUuid(uuid: UUID): CharacteristicUUID? = values().firstOrNull { it.uuid == uuid }
        fun fromString(value: String): CharacteristicUUID? = values().firstOrNull { it.value.equals(value, ignoreCase = true) }
    }

}

class BLEManager(private val context: Context) {

    private var bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothGatt: BluetoothGatt? = null
    private var connectedDevice: BluetoothDevice? = null

    companion object {
        private const val TAG = "BLEManager"
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
            val service = bluetoothGatt?.getService(ServiceUUID.UUID_Service_LED.uuid)
            if (service == null) {
                callback.onDataSent(false, "未找到LED服务")
                return
            }

            val characteristic = service.getCharacteristic(CharacteristicUUID.UUID_CHAR_SYSTEM_TEXTIN.uuid)
            if (characteristic == null) {
                // 打印所有可用的特征，帮助调试
                val allCharacteristics = service.characteristics
                Log.e(TAG, "未找到文本输入特征，可用的特征有:")
                allCharacteristics.forEach { char ->
                    Log.e(TAG, "特征UUID: ${char.uuid}, 属性: ${char.properties}")
                }
                callback.onDataSent(false, "未找到文本输入特征 (UUID: $CharacteristicUUID.UUID_CHAR_SYSTEM_TEXTIN.uuid)")
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
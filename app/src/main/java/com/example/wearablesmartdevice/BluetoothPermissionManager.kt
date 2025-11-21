package com.example.wearablesmartdevice
import android.Manifest
import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

class BluetoothPermissionManager(private val context: Context) : EasyPermissions.PermissionCallbacks {

    private var permissionCallback: BluetoothPermissionCallback? = null
    private var permissionDialog: AlertDialog? = null
    private var isCheckingPermissions = false

    companion object {
        const val RC_BLUETOOTH_PERMISSIONS = 1001

        // 蓝牙所需权限
        val BLUETOOTH_PERMISSIONS = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,    // 精确定位
            Manifest.permission.ACCESS_COARSE_LOCATION,  // 模糊定位 - Android 12+ 必须同时请求
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
    }

    interface BluetoothPermissionCallback {
        fun onPermissionsGranted()
        fun onPermissionsDenied()
        fun onPermissionsPermanentlyDenied()
    }

    /**
     * 检查是否已有所需权限
     */
    fun hasBluetoothPermissions(): Boolean {
        return EasyPermissions.hasPermissions(context, *BLUETOOTH_PERMISSIONS)
    }

    /**
     * 开始完整的权限检查流程
     */
    fun checkBluetoothPermissions(callback: BluetoothPermissionCallback) {
        if (isCheckingPermissions) {
            Log.d("BluetoothPermission", "权限检查正在进行中，跳过重复请求")
            return
        }

        this.permissionCallback = callback
        isCheckingPermissions = true

        Log.d("BluetoothPermission", "开始蓝牙权限检查流程")

        if (hasBluetoothPermissions()) {
            Log.d("BluetoothPermission", "已有权限，直接回调")
            isCheckingPermissions = false
            callback.onPermissionsGranted()
            return
        }

        showToast("正在检查蓝牙权限...")
        showPermissionExplanationDialog()
    }

    /**
     * 显示权限说明对话框
     */
    private fun showPermissionExplanationDialog() {
        Log.d("BluetoothPermission", "显示权限说明对话框")

        permissionDialog?.dismiss()

        permissionDialog = AlertDialog.Builder(context)
            .setTitle("蓝牙权限申请")
            .setMessage("应用需要以下蓝牙权限来连接ESP32设备：\n\n" +
                    "• 蓝牙连接权限\n" +
                    "• 位置权限（用于扫描蓝牙设备）\n\n" +
                    "这些权限仅用于与您的ESP32设备通信，不会用于其他目的。")
            .setPositiveButton("同意并继续") { dialog, _ ->
                dialog.dismiss()
                Log.d("BluetoothPermission", "用户同意，开始系统权限请求")
                requestSystemPermissions()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
                Log.d("BluetoothPermission", "用户取消权限请求")
                handlePermissionDenied()
            }
            .setCancelable(false)
            .setOnDismissListener {
                Log.d("BluetoothPermission", "权限说明对话框被关闭")
            }
            .create()

        permissionDialog?.show()
    }

    /**
     * 请求系统权限
     */
    private fun requestSystemPermissions() {
        Log.d("BluetoothPermission", "请求系统权限")

        try {
            EasyPermissions.requestPermissions(
                context as Activity,
                "请允许蓝牙权限以连接ESP32设备",
                RC_BLUETOOTH_PERMISSIONS,
                *BLUETOOTH_PERMISSIONS
            )
        } catch (e: Exception) {
            Log.e("BluetoothPermission", "请求权限时出错: ${e.message}", e)
            handlePermissionDenied()
        }
    }

    /**
     * 显示重试对话框
     */
    private fun showRetryDialog() {
        Log.d("BluetoothPermission", "显示重试对话框")

        permissionDialog?.dismiss()

        permissionDialog = AlertDialog.Builder(context)
            .setTitle("权限被拒绝")
            .setMessage("蓝牙权限被拒绝，无法使用蓝牙功能。是否重新申请权限？")
            .setPositiveButton("重新申请") { dialog, _ ->
                dialog.dismiss()
                Log.d("BluetoothPermission", "用户选择重新申请权限")
                requestSystemPermissions()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
                Log.d("BluetoothPermission", "用户取消重新申请")
                handlePermissionDenied()
            }
            .setCancelable(false)
            .create()

        permissionDialog?.show()
    }

    /**
     * 显示永久拒绝设置对话框
     */
    private fun showPermanentlyDeniedDialog() {
        Log.d("BluetoothPermission", "显示永久拒绝对话框")

        permissionDialog?.dismiss()

        permissionDialog = AlertDialog.Builder(context)
            .setTitle("权限被永久拒绝")
            .setMessage("蓝牙权限被永久拒绝，无法连接ESP32设备。\n\n" +
                    "请在应用设置中手动授予权限")
            .setPositiveButton("去设置") { dialog, _ ->
                dialog.dismiss()
                showAppSettingsDialog()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
                handlePermissionDenied()
            }
            .setCancelable(false)
            .create()

        permissionDialog?.show()
    }

    /**
     * 显示系统应用设置对话框
     */
    private fun showAppSettingsDialog() {
        AppSettingsDialog.Builder(context as Activity)
            .setTitle("需要权限")
            .setRationale("蓝牙权限被永久拒绝，请在应用设置中手动授予权限")
            .setPositiveButton("去设置")
            .setNegativeButton("取消")
            .build()
            .show()
    }

    /**
     * 处理权限被拒绝
     */
    private fun handlePermissionDenied() {
        isCheckingPermissions = false
        permissionCallback?.onPermissionsDenied()
    }

    /**
     * 处理权限被永久拒绝
     */
    private fun handlePermissionPermanentlyDenied() {
        isCheckingPermissions = false
        permissionCallback?.onPermissionsPermanentlyDenied()
    }

    /**
     * 处理权限授予
     */
    private fun handlePermissionGranted() {
        isCheckingPermissions = false
        permissionCallback?.onPermissionsGranted()
    }

    /**
     * 显示Toast
     */
    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * 处理权限请求结果（需要在Activity中调用）
     */
    fun handlePermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Log.d("BluetoothPermission", "处理权限结果，requestCode: $requestCode")
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    // EasyPermissions.PermissionCallbacks 实现
    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        Log.d("BluetoothPermission", "权限已授予，requestCode: $requestCode, perms: $perms")
        when (requestCode) {
            RC_BLUETOOTH_PERMISSIONS -> {
                showToast("蓝牙权限已授予")
                handlePermissionGranted()
            }
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        Log.d("BluetoothPermission", "权限被拒绝，requestCode: $requestCode, perms: $perms")
        when (requestCode) {
            RC_BLUETOOTH_PERMISSIONS -> {
                if (EasyPermissions.somePermissionPermanentlyDenied(context as Activity, perms)) {
                    Log.d("BluetoothPermission", "权限被永久拒绝")
                    showPermanentlyDeniedDialog()
                } else {
                    Log.d("BluetoothPermission", "权限被临时拒绝")
                    showRetryDialog()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        // 空实现，由 EasyPermissions 处理
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        isCheckingPermissions = false
        permissionDialog?.dismiss()
        permissionDialog = null
        permissionCallback = null
    }
}
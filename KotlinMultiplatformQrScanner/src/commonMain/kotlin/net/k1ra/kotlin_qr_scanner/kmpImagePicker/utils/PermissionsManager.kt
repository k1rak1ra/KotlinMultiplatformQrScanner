package net.k1ra.kotlin_qr_scanner.kmpImagePicker.utils

interface PermissionCallback {
    fun onPermissionStatus(permissionType: PermissionType, status: PermissionStatus)
}


package net.k1ra.kotlin_qr_scanner.kmpImagePicker.utils

import androidx.compose.runtime.Composable

interface PermissionHandler {
    @Composable
    fun askPermission(permission: PermissionType)

    @Composable
    fun isPermissionGranted(permission: PermissionType): Boolean

    @Composable
    fun launchSettings()

}
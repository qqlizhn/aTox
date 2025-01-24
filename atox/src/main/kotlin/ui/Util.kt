// SPDX-FileCopyrightText: 2019-2022 Robin Lindén <dev@robinlinden.eu>
// SPDX-FileCopyrightText: 2021-2022 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.TypedValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import ltd.evilcorp.atox.R
import ltd.evilcorp.core.vo.ConnectionStatus
import ltd.evilcorp.core.vo.Contact
import ltd.evilcorp.core.vo.UserStatus

internal fun colorFromStatus(context: Context, status: UserStatus) = when (status) {
    UserStatus.None -> ContextCompat.getColor(context, R.color.statusAvailable)
    UserStatus.Away -> ContextCompat.getColor(context, R.color.statusAway)
    UserStatus.Busy -> ContextCompat.getColor(context, R.color.statusBusy)
}

internal sealed interface Size

@JvmInline
internal value class Px(val px: Int) : Size

@JvmInline
internal value class Dp(val dp: Float) : Size {
    fun asPx(res: Resources): Px =
        Px(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, res.displayMetrics).toInt())
}

internal fun contactListSorter(contact: Contact) = when {
    contact.lastMessage != 0L -> contact.lastMessage
    contact.connectionStatus == ConnectionStatus.None -> -1000L
    else -> -contact.status.ordinal.toLong()
}


fun hasStoragePermissions(activity: Activity): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        return Environment.isExternalStorageManager()
    } else {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED
    }
}


val PERMISSION_REQUEST_CODE: Int = 100
fun checkAndRequestPermissions(activity: Activity): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (!Environment.isExternalStorageManager()) {
            val intent: Intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.setData(Uri.parse("package:" + activity.getPackageName()))
            activity.startActivityForResult(intent, PERMISSION_REQUEST_CODE)
            return false
        } else {
            return true
//                Log.d(TAG, "All files access permission is already granted.")
        }
    } else {
        // 对于 Android 11 以下的版本，请求常规的存储权限
        val permissions = arrayOf<String>(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )

        var allPermissionsGranted = true
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    permission,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                allPermissionsGranted = false
                break
            }
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(activity, permissions, PERMISSION_REQUEST_CODE)
            return false
        } else {
            return true
//                Log.d(TAG, "All storage permissions are already granted.")
        }
    }
}

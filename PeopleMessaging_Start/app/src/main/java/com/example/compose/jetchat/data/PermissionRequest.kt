package com.example.compose.jetchat.data

import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope

sealed interface PermissionStatus {
    object Granted : PermissionStatus
    class Denied(val shouldShowRationale: Boolean) : PermissionStatus
}

class PermissionRequest(
    private val fragment: Fragment,
    private val permission: String
) {

    private val _status = MutableLiveData<PermissionStatus>().also {
        fragment.lifecycleScope.launchWhenStarted {
            it.value = fragment.requireActivity().checkPermissionStatus(permission)
        }
    }

    val status: LiveData<PermissionStatus> = _status

    private val launcher = fragment.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        _status.value = if (granted) {
            PermissionStatus.Granted
        } else {
            PermissionStatus.Denied(
                ActivityCompat.shouldShowRequestPermissionRationale(
                    fragment.requireActivity(),
                    permission
                )
            )
        }
    }

    fun launch() {
        launcher.launch(permission)
    }
}

private fun Activity.checkPermissionStatus(permission: String): PermissionStatus {
    val check = ContextCompat.checkSelfPermission(this, permission)
    return if (check == PackageManager.PERMISSION_GRANTED) {
        PermissionStatus.Granted
    } else {
        PermissionStatus.Denied(
            ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
        )
    }
}
package uz.ojtest.facedetectiontest.base

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.checkSelfPermission
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.fragment.app.Fragment

@JvmInline
value class PermissionResult(val result: ActivityResultLauncher<Array<String>>)

enum class PermissionState {
    Granted, Denied, PermanentlyDenied
}

private fun getPermissionState(
    activity: Activity,
    result: Map<String, Boolean>
): PermissionState = result.filter { it.value.not() }.keys.run {
    when {
        isEmpty() -> PermissionState.Granted
        any { !shouldShowRequestPermissionRationale(activity, it) } -> PermissionState.PermanentlyDenied
        else -> PermissionState.Denied
    }
}

fun Fragment.registerPermission(onPermissionResult: (PermissionState) -> Unit): PermissionResult {
    return PermissionResult(
        this.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            onPermissionResult(getPermissionState(requireActivity(), it))
        }
    )
}

fun AppCompatActivity.registerPermission(onPermissionResult: (PermissionState) -> Unit): PermissionResult {
    return PermissionResult(
        this.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            onPermissionResult(getPermissionState(this, it))
        }
    )
}

@Suppress("UNCHECKED_CAST")
fun PermissionResult.request(vararg perm: String) {
    this.result.launch(arrayOf(*perm))
}

inline fun Fragment.checkPermission(
    permResult: PermissionResult,
    vararg perm: String,
    showRationale: (Array<out String>) -> Unit
) {
    when {
        perm.all { checkSelfPermission(requireContext(), it) == PERMISSION_GRANTED } -> permResult.request(*perm)
        perm.any { shouldShowRequestPermissionRationale(it) } -> showRationale(perm)
        else -> permResult.request(*perm)
    }
}

inline fun Activity.checkPermission(
    permResult: PermissionResult,
    vararg perm: String,
    showRationale: (Array<out String>) -> Unit
) {
    when {
        perm.all { checkSelfPermission(this, it) == PERMISSION_GRANTED } -> permResult.request(*perm)
        perm.any { shouldShowRequestPermissionRationale(it) } -> showRationale(perm)
        else -> permResult.request(*perm)
    }
}
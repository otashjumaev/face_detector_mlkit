package uz.ojtest.facedetectiontest

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import uz.ojtest.facedetectiontest.base.PermissionState
import uz.ojtest.facedetectiontest.base.checkPermission
import uz.ojtest.facedetectiontest.base.registerPermission
import uz.ojtest.facedetectiontest.base.startActivity
import uz.ojtest.facedetectiontest.ui.FaceDetectorActivity

class MainActivity : AppCompatActivity() {

    private val cameraPer = registerPermission(::onPermissionResult)
    private val requiredPerms =
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<AppCompatButton>(R.id.btn_start).setOnClickListener {
            checkCameraPer()
        }
        checkCameraPer()
    }

    private fun checkCameraPer() {
        checkPermission(cameraPer, *requiredPerms) {
            Toast.makeText(this, "${it.joinToString()} permissions needed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onPermissionResult(state: PermissionState) {
        when (state) {
            PermissionState.Denied -> Toast.makeText(this, "Permission needed to continue", Toast.LENGTH_SHORT).show()
            PermissionState.Granted -> onGranted()
            PermissionState.PermanentlyDenied -> {
                val intent = Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
        }
    }

    private fun onGranted() {
        startActivity<FaceDetectorActivity>()
    }
}

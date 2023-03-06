/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uz.ojtest.facedetectiontest.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.CompoundButton
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.common.MlKitException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uz.ojtest.facedetectiontest.base.Fps
import uz.ojtest.facedetectiontest.databinding.ActivityVisionCameraxLivePreviewBinding
import uz.ojtest.facedetectiontest.ui.vm.FaceDetectorViewModel
import uz.ojtest.facedetectiontest.utils.processor.FaceDetectorProcessor

@ExperimentalGetImage
/** Live preview demo app for ML Kit APIs using CameraX. */
class FaceDetectorActivity : AppCompatActivity(), CompoundButton.OnCheckedChangeListener {

    private lateinit var binding: ActivityVisionCameraxLivePreviewBinding
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageProcessor: FaceDetectorProcessor? = null

    private val viewModel by viewModels<FaceDetectorViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVisionCameraxLivePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.facingSwitch.setOnCheckedChangeListener(this)
        viewModel.processCameraProvider.observe(this) { provider: ProcessCameraProvider? ->
            cameraProvider = provider
            bindAllCameraUseCases(CameraSelector.Builder().requireLensFacing(viewModel.lensFacing).build())
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        cameraProvider ?: return
        val newLensFacing =
            if (viewModel.lensFacing == CameraSelector.LENS_FACING_FRONT) CameraSelector.LENS_FACING_BACK
            else CameraSelector.LENS_FACING_FRONT
        try {
            val newCameraSelector = CameraSelector.Builder().requireLensFacing(newLensFacing).build()
            if (cameraProvider!!.hasCamera(newCameraSelector)) {
                Log.d(TAG, "Set facing to $newLensFacing")
                viewModel.lensFacing = newLensFacing
                bindAllCameraUseCases(newCameraSelector)
                return
            }
        } catch (e: CameraInfoUnavailableException) {
            Log.d(TAG, "onCheckedChanged: $e")
        }
    }

    public override fun onResume() {
        super.onResume()
        bindAllCameraUseCases(CameraSelector.Builder().requireLensFacing(viewModel.lensFacing).build())
    }

    override fun onPause() {
        super.onPause()
        imageProcessor?.stop()
    }

    public override fun onDestroy() {
        super.onDestroy()
        imageProcessor?.stop()
    }


    @SuppressLint("SetTextI18n")
    private fun bindAllCameraUseCases(cameraSelector: CameraSelector) {
        cameraProvider?.let { camProvider ->
            imageProcessor?.stop()

            imageProcessor = FaceDetectorProcessor(viewModel.getFaceDetectorOptions())

            camProvider.unbindAll()

            val targetResolution = viewModel.getCameraXTargetResolution()
            val previewUC = Preview.Builder().run {
                if (targetResolution != null) setTargetResolution(targetResolution)
                build()
            }
            previewUC.setSurfaceProvider(binding.previewView.surfaceProvider)


            val analysisUC = ImageAnalysis.Builder().run {
                if (targetResolution != null) setTargetResolution(targetResolution)
                setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                build()
            }

            var needUpdateGraphicOverlayImageSourceInfo = true

            val fps = Fps()
            analysisUC.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy: ImageProxy ->
                if (needUpdateGraphicOverlayImageSourceInfo) {
                    val isImageFlipped = viewModel.lensFacing == CameraSelector.LENS_FACING_FRONT
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    if (rotationDegrees == 0 || rotationDegrees == 180) {
                        binding.graphicOverlay.setImageSourceInfo(imageProxy.width, imageProxy.height, isImageFlipped)
                    } else {
                        binding.graphicOverlay.setImageSourceInfo(imageProxy.height, imageProxy.width, isImageFlipped)
                    }
                    needUpdateGraphicOverlayImageSourceInfo = false
                }
                try {
                    imageProcessor?.processImageProxy(imageProxy, binding.graphicOverlay)
                } catch (e: MlKitException) {
                    Log.e(TAG, "Failed to process image. Error: " + e.localizedMessage)
                }

                fps.calculate()?.let { binding.textFps.post { binding.textFps.text = "$it fps" } }
            }

            camProvider.bindToLifecycle(this, cameraSelector, previewUC, analysisUC)
        }
    }

    companion object {
        private const val TAG = "TAG_CameraXLivePreview"
    }
}
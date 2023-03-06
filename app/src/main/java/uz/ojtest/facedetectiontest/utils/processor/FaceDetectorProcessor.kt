package uz.ojtest.facedetectiontest.utils.processor

import android.util.Log
import android.widget.Toast
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskExecutors
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import uz.ojtest.facedetectiontest.utils.graphic.FaceGraphic
import uz.ojtest.facedetectiontest.utils.graphic.GraphicOverlay
import java.util.*
import java.util.concurrent.Executors

class FaceDetectorProcessor(detectorOptions: FaceDetectorOptions) {

    private val detector: FaceDetector = FaceDetection.getClient(detectorOptions)
    private val executor = ScopedExecutor(TaskExecutors.MAIN_THREAD)
    private var isShutdown = false

    @ExperimentalGetImage
    fun processImageProxy(image: ImageProxy, graphicOverlay: GraphicOverlay) {
        if (isShutdown) return
        setUpListener(detectInImage(image), graphicOverlay).addOnCompleteListener {
            image.close()
        }
    }

    private fun setUpListener(task: Task<List<Face>>, graphicOverlay: GraphicOverlay): Task<List<Face>> =
        task.addOnSuccessListener(executor) { results ->
            graphicOverlay.clear()
            for (i in results.indices) {
                Log.v(TAG, "face-$i: ${results[i]}")
                graphicOverlay.add(FaceGraphic(graphicOverlay, results[i]))
            }
            graphicOverlay.postInvalidate()
        }.addOnFailureListener(executor) { e ->
            graphicOverlay.clear()
            graphicOverlay.postInvalidate()
            Toast.makeText(graphicOverlay.context, "Failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            Log.e(TAG + "FACE", "Face detection failed $e")
        }

    fun stop() {
        executor.shutdown()
        isShutdown = true
        detector.close()
    }

    @ExperimentalGetImage
    private fun detectInImage(image: ImageProxy): Task<List<Face>> {
        return detector.process(InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees))
    }

    companion object {
        private const val TAG = "TAG_FaceDetectorProcessor"
    }
}

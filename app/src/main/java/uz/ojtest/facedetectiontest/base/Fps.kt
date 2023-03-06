package uz.ojtest.facedetectiontest.base

import android.util.Log

class Fps {
    private val frameCount = 10
    private var frameCounter = 0
    private var lastFpsTimestamp = System.currentTimeMillis()
    private var now = System.currentTimeMillis()
    private var delta = now - lastFpsTimestamp
    private var fps = ""


    fun calculate(): String? {
        if (++frameCounter % frameCount == 0) {
            frameCounter = 0
            now = System.currentTimeMillis()
            delta = now - lastFpsTimestamp
            fps = "%.02f".format(1000 * frameCount.toFloat() / delta)
            Log.d("TAG_FPS", "FPS new: $fps")
            lastFpsTimestamp = now
            return fps
        }
        return null
    }

}
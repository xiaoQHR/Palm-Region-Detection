package com.heiko.mycameraxtest

import android.Manifest
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.heiko.mycameraxtest.databinding.ActivityMainBinding

/**
 * 切换摄像头
 */
class CameraActivity2 : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraProvider: ProcessCameraProvider
    private var preview: Preview? = null
    private var camera: Camera? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
            ), 1
        )

        binding.btnSwitchCamera.visibility = View.VISIBLE
        binding.btnCameraCapture.visibility = View.GONE

        setUpCamera(binding.previewView)

        binding.btnSwitchCamera.setOnClickListener {
            // Disable the button until the camera is set up
            //it.isEnabled = false

            switchCamera()
        }
    }

    //切换 前后摄像头
    private fun switchCamera() {
        lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }

        bindPreview(cameraProvider, binding.previewView)
    }

    private fun setUpCamera(previewView: PreviewView) {
        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
            ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindPreview(cameraProvider, previewView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindPreview(
        cameraProvider: ProcessCameraProvider,
        previewView: PreviewView
    ) {
        // 绑定前确保解除了所有绑定，防止CameraProvider重复绑定到Lifecycle发生异常
        cameraProvider.unbindAll()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing).build()
        preview = Preview.Builder().build()
        camera = cameraProvider.bindToLifecycle(
            this,
            cameraSelector, preview
        )
        preview?.setSurfaceProvider(previewView.surfaceProvider)
    }
}
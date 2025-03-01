package com.heiko.mycameraxtest

import android.Manifest
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.heiko.mycameraxtest.databinding.ActivityMainBinding

/**
 * 预览
 */
class CameraActivity1 : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraProvider: ProcessCameraProvider
    private var preview: Preview? = null
    private var camera: Camera? = null

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

        binding.btnSwitchCamera.visibility = View.GONE
        binding.btnCameraCapture.visibility = View.GONE

        setUpCamera(binding.previewView)
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
        preview = Preview.Builder().build()
        camera = cameraProvider.bindToLifecycle(
            this,
            CameraSelector.DEFAULT_BACK_CAMERA, preview
        )
        preview?.setSurfaceProvider(previewView.surfaceProvider)
    }
}
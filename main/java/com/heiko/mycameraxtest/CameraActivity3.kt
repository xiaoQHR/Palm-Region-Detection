package com.heiko.mycameraxtest

import android.Manifest
import android.os.Bundle
import android.view.SoundEffectConstants
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.heiko.mycameraxtest.databinding.ActivityMainBinding


/**
 * 对焦
 */
class CameraActivity3 : AppCompatActivity() {
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

            lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
            // Re-bind use cases to update selected camera
            //bindCameraUseCases()
            //setUpCamera(binding.viewFinder)
            bindPreview(cameraProvider, binding.previewView)
        }

        binding.previewView.setOnTouchListener { view, event ->
            val action = FocusMeteringAction.Builder(
                binding.previewView.getMeteringPointFactory()
                    .createPoint(event.getX(), event.getY())
            ).build();
            showTapView(event.x.toInt(), event.y.toInt())
            camera?.getCameraControl()?.startFocusAndMetering(action)
            true
        }
    }

    private fun showTapView(x: Int, y: Int) {
        val popupWindow = PopupWindow(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val imageView = ImageView(this)
        imageView.setImageResource(R.drawable.ic_focus_view)
        popupWindow.contentView = imageView
        popupWindow.showAsDropDown(binding.previewView, x, y)
        binding.previewView.postDelayed({ popupWindow.dismiss() }, 600)
        binding.previewView.playSoundEffect(SoundEffectConstants.CLICK)
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
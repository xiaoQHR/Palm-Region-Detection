package com.heiko.mycameraxtest

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.SoundEffectConstants
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import com.google.common.util.concurrent.ListenableFuture
import com.heiko.mycameraxtest.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


/**
 * 拍照
 */
class CameraActivity4 : AppCompatActivity() {
    private var imageCapture: ImageCapture? = null
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
        binding.btnCameraCapture.visibility = View.VISIBLE

        setUpCamera(binding.previewView)

        binding.btnSwitchCamera.setOnClickListener {

            lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }

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

        binding.btnCameraCapture.setOnClickListener {
            takePictureSaveToDisk()
        }
    }
    private fun takePictureSaveToDisk() {
        imageCapture?.let { imageCapture ->

            val photoFile = createFile(getOutputDirectory(this), FILENAME, PHOTO_EXTENSION)
            Log.i(TAG, "photoFile:$photoFile")
            val metadata = ImageCapture.Metadata().apply {
                isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
            }
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                .setMetadata(metadata)
                .build()
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                        Log.d(TAG, "Photo capture succeeded: $savedUri")

                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                            application.sendBroadcast(
                                Intent(android.hardware.Camera.ACTION_NEW_PICTURE, savedUri)
                            )
                        }
                        val mimeType = MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(savedUri.toFile().extension)
                        MediaScannerConnection.scanFile(
                            application,
                            arrayOf(savedUri.toFile().absolutePath),
                            arrayOf(mimeType)
                        ) { _, uri ->
                            Log.d(TAG, "Image capture scanned into media store: $uri")
                        }
                    }
                })
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                binding.root.postDelayed({
                    binding.root.foreground = ColorDrawable(Color.WHITE)
                    binding.root.postDelayed(
                        { binding.root.foreground = null }, 50L
                    )
                }, 100L)
            }
        }
    }
    private fun takePicture() {
        imageCapture?.let { imageCapture ->
            val mainExecutor = ContextCompat.getMainExecutor(this)
            imageCapture.takePicture(mainExecutor, object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                }
            })
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                binding.root.postDelayed({
                    binding.root.foreground = ColorDrawable(Color.WHITE)
                    binding.root.postDelayed(
                        { binding.root.foreground = null }, 50L
                    )
                }, 100L)
            }
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
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)

            .build()

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
        cameraProvider.unbindAll()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing).build()
        preview = Preview.Builder().build()
        camera = cameraProvider.bindToLifecycle(
            this,
            cameraSelector, preview, imageCapture
        )
        preview?.setSurfaceProvider(previewView.surfaceProvider)
    }
    private fun createFile(baseFolder: File, format: String, extension: String) =
        File(
            baseFolder, SimpleDateFormat(format, Locale.US)
                .format(System.currentTimeMillis()) + extension
        )
    fun getOutputDirectory(context: Context): File {
        val appContext = context.applicationContext
        val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else appContext.filesDir
    }
    companion object {
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"
        private const val TAG = "Z-Main"
    }
}
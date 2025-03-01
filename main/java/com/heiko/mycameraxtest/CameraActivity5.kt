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
import android.util.Size
import android.view.SoundEffectConstants
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.PopupWindow
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
 * 图像分析
 */
class CameraActivity5 : AppCompatActivity() {
    private lateinit var imageAnalysis: ImageAnalysis
    private lateinit var imageCapture: ImageCapture
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

        binding.btnCameraCapture.setOnClickListener {
            //takePicture()
            takePictureSaveToDisk()
        }
    }

    //进行拍照并保存到本地
    private fun takePictureSaveToDisk() {
        imageCapture?.let { imageCapture ->

            // Create output file to hold the image
            val photoFile = createFile(getOutputDirectory(this), FILENAME, PHOTO_EXTENSION)
            Log.i(TAG, "photoFile:$photoFile")

            // Setup image capture metadata
            val metadata = ImageCapture.Metadata().apply {
                // Mirror image when using the front camera
                isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
            }

            // Create output options object which contains file + metadata
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                .setMetadata(metadata)
                .build()

            // Setup image capture listener which is triggered after photo has been taken
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

                        /*
                        // We can only change the foreground Drawable using API level 23+ API
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            // Update the gallery thumbnail with latest picture taken
                            setGalleryThumbnail(savedUri)
                        }*/

                        // Implicit broadcasts will be ignored for devices running API level >= 24
                        // so if you only target API level 24+ you can remove this statement
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                            application.sendBroadcast(
                                Intent(android.hardware.Camera.ACTION_NEW_PICTURE, savedUri)
                            )
                        }

                        // If the folder selected is an external media directory, this is
                        // unnecessary but otherwise other apps will not be able to access our
                        // images unless we scan them using [MediaScannerConnection]
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

            // We can only change the foreground Drawable using API level 23+ API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Display flash animation to indicate that photo was captured
                binding.root.postDelayed({
                    binding.root.foreground = ColorDrawable(Color.WHITE)
                    binding.root.postDelayed(
                        { binding.root.foreground = null }, 50L
                    )
                }, 100L)
            }
        }
    }

    //进行拍照
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

            // 让画面闪一下，营造拍照的感觉
            // We can only change the foreground Drawable using API level 23+ API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Display flash animation to indicate that photo was captured
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
            //.setTargetAspectRatio(screenAspectRatio)
            //.setTargetRotation(binding.previewView.display.rotation)
            .build()

        imageAnalysis = ImageAnalysis.Builder()
            //.setTargetRotation(previewView.display.rotation)
            .setTargetResolution(Size(720, 1440))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), ImageAnalysis.Analyzer { imageProxy ->
            Log.i(TAG, "onAnalyzer:$imageProxy")
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            // insert your code here.
            // ...
            // after done, release the ImageProxy object
            imageProxy.close()
        })

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
            cameraSelector, preview, imageCapture, imageAnalysis
        )
        preview?.setSurfaceProvider(previewView.surfaceProvider)
    }

    /** Helper function used to create a timestamped file */
    private fun createFile(baseFolder: File, format: String, extension: String) =
        File(
            baseFolder, SimpleDateFormat(format, Locale.US)
                .format(System.currentTimeMillis()) + extension
        )

    /** Use external media if it is available, our app's file directory otherwise */
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
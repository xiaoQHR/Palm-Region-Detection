package com.heiko.mycameraxtest

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.SoundEffectConstants
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
import com.heiko.mycameraxtest.databinding.ActivityMainVideoBinding
import java.util.*


/**
 * 视频录制
 */
class CameraActivity6 : AppCompatActivity() {
    private lateinit var videoCapture: VideoCapture
    private lateinit var binding: ActivityMainVideoBinding
    private lateinit var cameraProvider: ProcessCameraProvider
    private var preview: Preview? = null
    private var camera: Camera? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var recording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainVideoBinding.inflate(layoutInflater)
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

        binding.previewView.setOnTouchListener { view, event ->
            val action = FocusMeteringAction.Builder(
                binding.previewView.getMeteringPointFactory()
                    .createPoint(event.getX(), event.getY())
            ).build();
            showTapView(event.x.toInt(), event.y.toInt())
            camera?.getCameraControl()?.startFocusAndMetering(action)
            true
        }

        binding.btnStartRecord.setOnClickListener {
            startRecording()
        }
        binding.btnStopRecord.setOnClickListener {
            stopRecording()
        }
    }

    @SuppressLint("RestrictedApi")
    private fun startRecording() {
        val contentValues = ContentValues()
        contentValues.put(
            MediaStore.MediaColumns.DISPLAY_NAME,
            RECORDED_FILE_NAME + "_" + System.currentTimeMillis()
        )
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, RECORDED_FILE_NAME_END)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            //没有权限
            return
        }
        val outputFileOptions = VideoCapture.OutputFileOptions.Builder(
            getContentResolver(),
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues
        ).build()
        videoCapture.startRecording(
            outputFileOptions,
            ContextCompat.getMainExecutor(this),
            object : VideoCapture.OnVideoSavedCallback {
                override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                    Log.i(TAG, "视频保存成功:${outputFileResults.savedUri}")
                }

                override fun onError(
                    videoCaptureError: Int,
                    message: String,
                    cause: Throwable?
                ) {
                    Log.i(TAG, "当出现异常 videoCaptureError:$videoCaptureError message:$message cause:$cause")
                }
            }
        )
        recording = true
        Log.i(TAG, "startRecording")
    }


    @SuppressLint("RestrictedApi")
    private fun stopRecording() {
        if (recording) {
            videoCapture.stopRecording()
            recording = false
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

    @SuppressLint("RestrictedApi")
    private fun setUpCamera(previewView: PreviewView) {
        videoCapture = VideoCapture.Builder()
            //.setTargetRotation(previewView.getDisplay().getRotation())
            .setVideoFrameRate(25)
            .setBitRate(3 * 1024 * 1024)
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
        // 绑定前确保解除了所有绑定，防止CameraProvider重复绑定到Lifecycle发生异常
        cameraProvider.unbindAll()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing).build()
        preview = Preview.Builder().build()
        camera = cameraProvider.bindToLifecycle(
            this,
            cameraSelector, preview, videoCapture
        )
        preview?.setSurfaceProvider(previewView.surfaceProvider)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
    }

    private val RECORDED_FILE_NAME = "recorded_video"
    private val RECORDED_FILE_NAME_END = "video/mp4"
    private val TAG = "Z-Main"
}
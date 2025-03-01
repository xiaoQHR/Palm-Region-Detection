package com.heiko.mycameraxtest

import android.Manifest
import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.Display
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.heiko.mycameraxtest.databinding.ActivityListBinding
import com.heiko.mycameraxtest.databinding.ActivityMainBinding
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 更多功能详见官网 : https://developer.android.google.cn/training/camerax
 */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
            ), 1
        )

        binding.btnPreview.setOnClickListener {
            startActivity(CameraActivity1::class.java)
        }
        binding.btnSwitchCamera.setOnClickListener {
            startActivity(CameraActivity2::class.java)
        }
        binding.btnFocusing.setOnClickListener {
            startActivity(CameraActivity3::class.java)
        }
        binding.btnTakePicture.setOnClickListener {
            startActivity(CameraActivity4::class.java)
        }
        binding.btnImageAnalysis.setOnClickListener {
            startActivity(CameraActivity5::class.java)
        }
        binding.btnVideoCapture.setOnClickListener {
            startActivity(CameraActivity6::class.java)
        }
    }

    private fun startActivity(clazz: Class<*>) {
        val intent = Intent(this, clazz)
        startActivity(intent)
    }
}
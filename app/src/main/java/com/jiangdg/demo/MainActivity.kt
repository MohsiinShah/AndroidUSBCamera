/*
 * Copyright 2017-2022 Jiangdg
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
package com.jiangdg.demo

import android.Manifest.permission.*
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import com.jiangdg.ApiViewModel
import com.jiangdg.ausbc.utils.ToastUtils
import com.jiangdg.ausbc.utils.Utils
import com.jiangdg.demo.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlin.system.exitProcess

/**
 * Demos of camera usage
 *
 * @author Created by jiangdg on 2021/12/27
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private var mWakeLock: PowerManager.WakeLock? = null
    private lateinit var viewBinding: ActivityMainBinding

    private val viewModel: ApiViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        replaceDemoFragment(DemoFragment())

//        viewModel.data.observe(this){
//            Log.d("TAG", "onCreate: $it")
//        }
//        viewModel.fetchData()
    }

    override fun onStart() {
        super.onStart()
        mWakeLock = Utils.wakeLock(this)
    }

    override fun onStop() {
        super.onStop()
        mWakeLock?.apply {
            Utils.wakeUnLock(this)
        }
    }

/*
    private fun replaceDemoFragment(fragment: Fragment) {
        Log.d("TAG", "replaceDemoFragment: here")
        val hasCameraPermission = PermissionChecker.checkSelfPermission(this, CAMERA)
        val hasStoragePermission =
            PermissionChecker.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
        val hasStorageReadPermission = PermissionChecker.checkSelfPermission(this, READ_EXTERNAL_STORAGE)

        if (hasCameraPermission != PermissionChecker.PERMISSION_GRANTED || hasStoragePermission != PermissionChecker.PERMISSION_GRANTED
            || hasStorageReadPermission != PermissionChecker.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA)) {
                ToastUtils.show(R.string.permission_tip)
            }
            ActivityCompat.requestPermissions(
                this,
                arrayOf(CAMERA, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, RECORD_AUDIO),
                REQUEST_CAMERA
            )
            Log.d("TAG", "replaceDemoFragment: return")

            return
        }
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commitAllowingStateLoss()
        Log.d("TAG", "replaceDemoFragment: done")

    }
*/


/*    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CAMERA -> {
                val hasCameraPermission = PermissionChecker.checkSelfPermission(this, CAMERA)
                if (hasCameraPermission == PermissionChecker.PERMISSION_DENIED) {
                    ToastUtils.show(R.string.permission_tip)
                    return
                }
//                replaceDemoFragment(DemoMultiCameraFragment())
                replaceDemoFragment(DemoFragment())
//                replaceDemoFragment(GlSurfaceFragment())
            }
            REQUEST_STORAGE -> {
                val hasCameraPermission =
                    PermissionChecker.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
                if (hasCameraPermission == PermissionChecker.PERMISSION_DENIED) {
                    ToastUtils.show(R.string.permission_tip)
                    return
                }
                // todo
            }
            else -> {
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }*/

    private fun replaceDemoFragment(fragment: Fragment) {
        Log.d("TAG", "replaceDemoFragment: here")
        val hasCameraPermission = PermissionChecker.checkSelfPermission(this, CAMERA) == PermissionChecker.PERMISSION_GRANTED
        val hasAudioPermission = PermissionChecker.checkSelfPermission(this, RECORD_AUDIO) == PermissionChecker.PERMISSION_GRANTED
        val hasMediaPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: Check granular media permissions for accessing non-app media
            PermissionChecker.checkSelfPermission(this, READ_MEDIA_IMAGES) == PermissionChecker.PERMISSION_GRANTED &&
                    PermissionChecker.checkSelfPermission(this, READ_MEDIA_VIDEO) == PermissionChecker.PERMISSION_GRANTED &&
                    PermissionChecker.checkSelfPermission(this, READ_MEDIA_AUDIO) == PermissionChecker.PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true // MediaStore doesn't require permissions for app-created files
        } else {
            // Android 9 and below: Check legacy storage permissions
            PermissionChecker.checkSelfPermission(this, READ_EXTERNAL_STORAGE) == PermissionChecker.PERMISSION_GRANTED &&
                    PermissionChecker.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) == PermissionChecker.PERMISSION_GRANTED
        }

        if (!hasCameraPermission || !hasAudioPermission || !hasMediaPermissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA)) {
                ToastUtils.show(R.string.permission_tip)
            }
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(CAMERA, RECORD_AUDIO, READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_AUDIO)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                arrayOf(CAMERA, RECORD_AUDIO)
            } else {
                arrayOf(CAMERA, RECORD_AUDIO, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE)
            }
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CAMERA)
            Log.d("TAG", "replaceDemoFragment: permissions not granted, requesting")
            return
        }

        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commitAllowingStateLoss()
        Log.d("TAG", "replaceDemoFragment: done")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CAMERA -> {
                val hasCameraPermission = PermissionChecker.checkSelfPermission(this, CAMERA) == PermissionChecker.PERMISSION_GRANTED
                val hasAudioPermission = PermissionChecker.checkSelfPermission(this, RECORD_AUDIO) == PermissionChecker.PERMISSION_GRANTED
                val hasMediaPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PermissionChecker.checkSelfPermission(this, READ_MEDIA_IMAGES) == PermissionChecker.PERMISSION_GRANTED &&
                            PermissionChecker.checkSelfPermission(this, READ_MEDIA_VIDEO) == PermissionChecker.PERMISSION_GRANTED &&
                            PermissionChecker.checkSelfPermission(this, READ_MEDIA_AUDIO) == PermissionChecker.PERMISSION_GRANTED
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    true
                } else {
                    PermissionChecker.checkSelfPermission(this, READ_EXTERNAL_STORAGE) == PermissionChecker.PERMISSION_GRANTED &&
                            PermissionChecker.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) == PermissionChecker.PERMISSION_GRANTED
                }

                if (!hasCameraPermission || !hasAudioPermission || !hasMediaPermissions) {
                    ToastUtils.show(R.string.permission_tip)
                    return
                }
              //  replaceDemoFragment(DemoFragment())
//                finishAffinity()
                try {
                    finishAndRemoveTask()
                    exitProcess(0)
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
            REQUEST_STORAGE -> {
                val hasMediaPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PermissionChecker.checkSelfPermission(this, READ_MEDIA_IMAGES) == PermissionChecker.PERMISSION_GRANTED &&
                            PermissionChecker.checkSelfPermission(this, READ_MEDIA_VIDEO) == PermissionChecker.PERMISSION_GRANTED &&
                            PermissionChecker.checkSelfPermission(this, READ_MEDIA_AUDIO) == PermissionChecker.PERMISSION_GRANTED
                } else {
                    PermissionChecker.checkSelfPermission(this, READ_EXTERNAL_STORAGE) == PermissionChecker.PERMISSION_GRANTED &&
                            PermissionChecker.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) == PermissionChecker.PERMISSION_GRANTED
                }
                if (!hasMediaPermissions) {
                    ToastUtils.show(R.string.permission_tip)
                    return
                }
            }
        }
    }


    companion object {
        private const val REQUEST_CAMERA = 0
        private const val REQUEST_STORAGE = 1

    }
}
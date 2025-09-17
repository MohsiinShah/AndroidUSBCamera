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
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import android.text.InputFilter
import android.text.InputType
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.jiangdg.ApiViewModel
import com.jiangdg.ausbc.utils.ToastUtils
import com.jiangdg.ausbc.utils.Utils
import com.jiangdg.demo.databinding.ActivityMainBinding
import com.jiangdg.utils.Constants
import com.jiangdg.utils.DatastoreManager
import com.jiangdg.worker.DailyAdSyncWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
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

    @Inject
    lateinit var dataStore: DatastoreManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        lifecycleScope.launch {
            val deviceID = dataStore.getData(Constants.USER_DEVICE_ID, -1).first()
            if(deviceID == -1){
                showDeviceIDInputDialog(this@MainActivity) { result ->
                    if (result != null) {
                        lifecycleScope.launch {
                            dataStore.saveData(Constants.USER_DEVICE_ID, result)
                            checkPermissionsAndMove()
                        }
                    } else{
                        ToastUtils.show("Device ID is mandatory")
                    }
                }
            }else {
                checkPermissionsAndMove()
            }
        }

    }

    fun showDeviceIDInputDialog(
        ctx: Context,
        title: String = "Enter Device ID",
        hint: String = "",
        maxDigits: Int? = 6,
        onResult: (Int?) -> Unit
    ) {
        val editText = EditText(ctx).apply {
            inputType = InputType.TYPE_CLASS_NUMBER // only digits
            this.hint = hint
            maxDigits?.let { filters = arrayOf(InputFilter.LengthFilter(it)) }
            isSingleLine = true
            setSelectAllOnFocus(true)
        }

        val dialog = AlertDialog.Builder(ctx)
            .setTitle(title)
            .setView(editText)
            .setCancelable(false)
            .setPositiveButton("Done") { d, _ ->
                val raw = editText.text?.toString()?.trim().orEmpty()
                val value = raw.takeIf { it.isNotEmpty() }?.toIntOrNull()
                onResult(value) // null if not a valid integer
                d.dismiss()
            }
            .create()

        dialog.setOnShowListener {
            editText.requestFocus()
            val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }

        dialog.show()
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


    private fun checkPermissionsAndMove() {

        val request = OneTimeWorkRequestBuilder<DailyAdSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED) // needs internet
                    .build()
            )
            .build()

        WorkManager.getInstance(this)
            .enqueueUniqueWork(
                "DailyAdSyncWork",
                ExistingWorkPolicy.KEEP, // don’t replace if already enqueued
                request
            )



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
        }else{
            goToPreview()
        }
    }

    fun goToPreview(){
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, DemoFragment())
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
//                    finishAndRemoveTask()
//                    exitProcess(0)
                    goToPreview()
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
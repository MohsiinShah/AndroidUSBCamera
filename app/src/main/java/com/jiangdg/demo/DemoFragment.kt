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

import com.jiangdg.ausbc.utils.bus.BusKey
import com.jiangdg.ausbc.utils.bus.EventBus

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Typeface
import android.hardware.usb.UsbDevice
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupWindow
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.bumptech.glide.Glide
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.base.BaseBottomDialog
import com.jiangdg.ausbc.base.CameraFragment
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.ICaptureCallBack
import com.jiangdg.ausbc.callback.IPlayCallBack
import com.jiangdg.ausbc.camera.CameraUVC
import com.jiangdg.ausbc.render.effect.EffectBlackWhite
import com.jiangdg.ausbc.render.effect.EffectSoul
import com.jiangdg.ausbc.render.effect.EffectZoom
import com.jiangdg.ausbc.render.effect.bean.CameraEffect
import com.jiangdg.ausbc.utils.*
import com.jiangdg.ausbc.widget.*
import com.jiangdg.db.AppDatabase
import com.jiangdg.db.slotDateTime
import com.jiangdg.db.toModel
import com.jiangdg.demo.EffectListDialog.Companion.KEY_ANIMATION
import com.jiangdg.demo.EffectListDialog.Companion.KEY_FILTER
import com.jiangdg.demo.databinding.DialogMoreBinding
import com.jiangdg.demo.databinding.FragmentDemoBinding
import com.jiangdg.models.Ad
import com.jiangdg.utils.MMKVUtils
import com.jiangdg.utils.imageloader.ILoader
import com.jiangdg.utils.imageloader.ImageLoaders
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class DemoFragment : CameraFragment(), View.OnClickListener, CaptureMediaView.OnViewClickListener {
    private var mMultiCameraDialog: MultiCameraDialog? = null
    private lateinit var mMoreBindingView: DialogMoreBinding
    private var mMoreMenu: PopupWindow? = null
    private var isCapturingVideoOrAudio: Boolean = false
    private var isPlayingMic: Boolean = false
    private var mRecTimer: Timer? = null
    private var mRecSeconds = 0
    private var mRecMinute = 0
    private var mRecHours = 0
    private var adJob: Job? = null
    private var currentAd: Ad? = null // Track current ad for Confirm action
    private var isLBannerActive: Boolean = false // Track L-banner state

    @Inject
    lateinit var appDatabase: AppDatabase

    private val mCameraModeTabMap = mapOf(
        CaptureMediaView.CaptureMode.MODE_CAPTURE_PIC to R.id.takePictureModeTv,
        CaptureMediaView.CaptureMode.MODE_CAPTURE_VIDEO to R.id.recordVideoModeTv,
        CaptureMediaView.CaptureMode.MODE_CAPTURE_AUDIO to R.id.recordAudioModeTv
    )

    private val mEffectDataList by lazy {
        arrayListOf(
            CameraEffect.NONE_FILTER,
            CameraEffect(
                EffectBlackWhite.ID,
                "BlackWhite",
                CameraEffect.CLASSIFY_ID_FILTER,
                effect = EffectBlackWhite(requireActivity()),
                coverResId = R.mipmap.filter0
            ),
            CameraEffect.NONE_ANIMATION,
            CameraEffect(
                EffectZoom.ID,
                "Zoom",
                CameraEffect.CLASSIFY_ID_ANIMATION,
                effect = EffectZoom(requireActivity()),
                coverResId = R.mipmap.filter2
            ),
            CameraEffect(
                EffectSoul.ID,
                "Soul",
                CameraEffect.CLASSIFY_ID_ANIMATION,
                effect = EffectSoul(requireActivity()),
                coverResId = R.mipmap.filter1
            )
        )
    }

    private val mTakePictureTipView: TipView by lazy {
        mViewBinding.takePictureTipViewStub.inflate() as TipView
    }

    private val mMainHandler: Handler by lazy {
        Handler(Looper.getMainLooper()) {
            when (it.what) {
                WHAT_START_TIMER -> {
                    if (mRecSeconds % 2 != 0) {
                        mViewBinding.recStateIv.visibility = View.GONE
                    } else {
                        mViewBinding.recStateIv.visibility = View.GONE
                    }
                    mViewBinding.recTimeTv.text = calculateTime(mRecSeconds, mRecMinute)
                }
                WHAT_STOP_TIMER -> {
                    mViewBinding.recTimerLayout.visibility = View.GONE
                    mViewBinding.recTimeTv.text = calculateTime(0, 0)
                }
            }
            true
        }
    }

    private var mCameraMode = CaptureMediaView.CaptureMode.MODE_CAPTURE_PIC

    private lateinit var mViewBinding: FragmentDemoBinding

    override fun initView() {
        super.initView()
        mViewBinding.lensFacingBtn1.setOnClickListener(this)
        mViewBinding.effectsBtn.setOnClickListener(this)
        mViewBinding.cameraTypeBtn.setOnClickListener(this)
        mViewBinding.settingsBtn.setOnClickListener(this)
        mViewBinding.voiceBtn.setOnClickListener(this)
        mViewBinding.resolutionBtn.setOnClickListener(this)
        mViewBinding.albumPreviewIv.setOnClickListener(this)
        mViewBinding.captureBtn.setOnViewClickListener(this)
        mViewBinding.albumPreviewIv.setTheme(PreviewImageView.Theme.DARK)
        switchLayoutClick()
        startAdScheduler()
        setupConfirmButton()
    }

    private fun setupConfirmButton() {
        // Create an invisible button for Confirm action
        val confirmButton = Button(requireContext()).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
            ).apply {
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            }
            visibility = View.GONE // Initially invisible
            setBackgroundColor(0x00000000) // Transparent background
            setOnClickListener {
                if (isLBannerActive && currentAd != null) {
                    showFullscreenAd(currentAd!!)
                }
            }
            // Ensure button is pre-focused for remote Confirm action
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
        }
        mViewBinding.root.addView(confirmButton)
    }

    private fun startAdScheduler() {
        adJob = lifecycleScope.launch(Dispatchers.IO) {
            appDatabase.adDao().observeAllAds().collect { ads ->
                if (ads.isEmpty()) return@collect

                while (isActive) {
                    val now = ZonedDateTime.now()
                    val nextSlot = now.withSecond(0).withNano(0)
                        .plusMinutes((5 - now.minute % 5).toLong())
                    Log.d(TAG, "Next slot: $nextSlot")
                    val adForSlot = ads.firstOrNull { ad ->
                        val slot = ad.slotDateTime()
                        slot.withSecond(0).withNano(0) == nextSlot.withSecond(0).withNano(0)
                    }
                    Log.d(TAG, "Ad for slot: $adForSlot")

                    if (adForSlot != null) {
                        val delayMs = Duration.between(now, nextSlot).toMillis()
                        if (delayMs > 0) delay(delayMs)
                        withContext(Dispatchers.Main) {
                            currentAd = adForSlot.toModel()
                            isLBannerActive = true
                            showLBanner(currentAd!!)
                            // Show Confirm button while L-banner is active
                            mViewBinding.root.findViewById<Button>(mViewBinding.root.children.last().id).visibility = View.VISIBLE
                        }
                        adForSlot.isDisplayed = true
                        appDatabase.adDao().update(adForSlot)
                        delay(10_000)
                        withContext(Dispatchers.Main) {
                            hideLBanner()
                            isLBannerActive = false
                            // Hide Confirm button when L-banner is hidden
                            mViewBinding.root.findViewById<Button>(mViewBinding.root.children.last().id).visibility = View.GONE
                        }
                    } else {
                        val delayMs = Duration.between(now, nextSlot).toMillis()
                        if (delayMs > 0) delay(delayMs)
                    }
                }
            }
        }
    }

    private fun showLBanner(ad: Ad) {
        Log.d(TAG, "Showing L-banner")
        mViewBinding.apply {
            adFullscreen.visibility = View.GONE
            qrCodeView.visibility = View.GONE
            adLeft.visibility = View.VISIBLE
            adBottom.visibility = View.VISIBLE

            // Initialize ads with zero alpha and offset
            adLeft.alpha = 0f
            adLeft.translationX = -adLeft.width.toFloat()
            adBottom.alpha = 0f
            adBottom.translationY = adBottom.height.toFloat()

            // Create animation for adLeft (fade in and slide from left)
            val leftFadeIn = ObjectAnimator.ofFloat(adLeft, "alpha", 0f, 1f)
            val leftSlideIn = ObjectAnimator.ofFloat(adLeft, "translationX", -adLeft.width.toFloat(), 0f)
            val leftAnimatorSet = AnimatorSet().apply {
                playTogether(leftFadeIn, leftSlideIn)
                duration = 500
            }

            // Create animation for adBottom (fade in and slide from bottom)
            val bottomFadeIn = ObjectAnimator.ofFloat(adBottom, "alpha", 0f, 1f)
            val bottomSlideIn = ObjectAnimator.ofFloat(adBottom, "translationY", adBottom.height.toFloat(), 0f)
            val bottomAnimatorSet = AnimatorSet().apply {
                playTogether(bottomFadeIn, bottomSlideIn)
                duration = 500
            }

            // Start animations
            leftAnimatorSet.start()
            bottomAnimatorSet.start()

            Glide.with(requireContext()).load(ad.urlLeft).into(adLeft)
            Glide.with(requireContext()).load(ad.urlBottom).into(adBottom)
        }
    }

    private fun hideLBanner() {
        Log.d(TAG, "Hiding L-banner")
        mViewBinding.apply {
            // Create animation for adLeft (fade out and slide to left)
            val leftFadeOut = ObjectAnimator.ofFloat(adLeft, "alpha", 1f, 0f)
            val leftSlideOut = ObjectAnimator.ofFloat(adLeft, "translationX", 0f, -adLeft.width.toFloat())
            val leftAnimatorSet = AnimatorSet().apply {
                playTogether(leftFadeOut, leftSlideOut)
                duration = 500
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        adLeft.visibility = View.GONE
                    }
                })
            }

            // Create animation for adBottom (fade out and slide to bottom)
            val bottomFadeOut = ObjectAnimator.ofFloat(adBottom, "alpha", 1f, 0f)
            val bottomSlideOut = ObjectAnimator.ofFloat(adBottom, "translationY", 0f, adBottom.height.toFloat())
            val bottomAnimatorSet = AnimatorSet().apply {
                playTogether(bottomFadeOut, bottomSlideOut)
                duration = 500
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        adBottom.visibility = View.GONE
                    }
                })
            }

            // Start animations
            leftAnimatorSet.start()
            bottomAnimatorSet.start()
        }
    }


    //    private fun showLBanner(ad: Ad) {
//        Log.d(TAG, "Showing L-banner")
//        mViewBinding.apply {
//            adFullscreen.visibility = View.GONE
//            qrCodeView.visibility = View.GONE
//            adLeft.visibility = View.VISIBLE
//            adBottom.visibility = View.VISIBLE
//            Glide.with(requireContext()).load(ad.urlLeft).into(adLeft)
//            Glide.with(requireContext()).load(ad.urlBottom).into(adBottom)
//        }
//    }
//
//    private fun hideLBanner() {
//        Log.d(TAG, "Hiding L-banner")
//        mViewBinding.apply {
//            adLeft.visibility = View.GONE
//            adBottom.visibility = View.GONE
//
//        }
//    }
    private fun showFullscreenAd(ad: Ad) {
        Log.d(TAG, "Showing full-screen ad")
        mViewBinding.apply {
            adFullscreen.visibility = View.VISIBLE
            qrCodeView.visibility = View.VISIBLE

            Glide.with(requireContext()).load(ad.urlFullscreen).into(adFullscreen)
            try {
                val qrBitmap = generateQRCode(ad.qrLink ?: "", 200, 200)
                qrCodeView.setImageBitmap(qrBitmap)
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to generate QR code", e)
                ToastUtils.show("Failed to generate QR code")
                qrCodeView.visibility = View.GONE
            }

            root.findViewById<Button>(root.children.last().id).visibility = View.GONE

            Handler(Looper.getMainLooper()).postDelayed({
                adFullscreen.visibility = View.GONE
                qrCodeView.visibility = View.GONE
                // Re-enable Confirm button if L-banner is still active
                if (isLBannerActive) {
                    root.findViewById<Button>(root.children.last().id).visibility = View.VISIBLE
                }
            }, 30_000) // 30 seconds
        }
    }

    private fun generateQRCode(url: String, width: Int, height: Int): Bitmap {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(url, BarcodeFormat.QR_CODE, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        return bitmap
    }

    override fun onViewCreated(view: View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Set up back press handling using OnBackPressedDispatcher
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (mViewBinding.adFullscreen.visibility == View.VISIBLE) {
                    mViewBinding.adFullscreen.visibility = View.GONE
                    mViewBinding.qrCodeView.visibility = View.GONE
                    // Re-enable Confirm button if L-banner is still active
                    if (isLBannerActive) {
                        mViewBinding.root.findViewById<Button>(mViewBinding.root.children.last().id).visibility = View.VISIBLE
                    }
                } else {
                    // Let the system handle the back press (e.g., pop fragment or close activity)
                    isEnabled = false // Disable this callback to allow default back press behavior
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun isNetworkAvailable(): Boolean {
        return true // Placeholder; implement actual network check if needed
    }

    override fun initData() {
        super.initData()
        EventBus.with<Int>(BusKey.KEY_FRAME_RATE).observe(this, {
            mViewBinding.frameRateTv.text = "frame rate: $it fps"
        })

        EventBus.with<Boolean>(BusKey.KEY_RENDER_READY).observe(this, { ready ->
            if (!ready) return@observe
            getDefaultEffect()?.apply {
                when (getClassifyId()) {
                    CameraEffect.CLASSIFY_ID_FILTER -> {
                        val animId = MMKVUtils.getInt(KEY_ANIMATION, -99)
                        if (animId != -99) {
                            mEffectDataList.find { it.id == animId }?.also {
                                if (it.effect != null) {
                                    addRenderEffect(it.effect!!)
                                }
                            }
                        }
                        val filterId = MMKVUtils.getInt(KEY_FILTER, -99)
                        if (filterId != -99) {
                            removeRenderEffect(this)
                            mEffectDataList.find { it.id == filterId }?.also {
                                if (it.effect != null) {
                                    addRenderEffect(it.effect!!)
                                }
                            }
                            return@apply
                        }
                        MMKVUtils.set(KEY_FILTER, getId())
                    }
                    CameraEffect.CLASSIFY_ID_ANIMATION -> {
                        val filterId = MMKVUtils.getInt(KEY_ANIMATION, -99)
                        if (filterId != -99) {
                            mEffectDataList.find { it.id == filterId }?.also {
                                if (it.effect != null) {
                                    addRenderEffect(it.effect!!)
                                }
                            }
                        }
                        val animId = MMKVUtils.getInt(KEY_ANIMATION, -99)
                        if (animId != -99) {
                            removeRenderEffect(this)
                            mEffectDataList.find { it.id == animId }?.also {
                                if (it.effect != null) {
                                    addRenderEffect(it.effect!!)
                                }
                            }
                            return@apply
                        }
                        MMKVUtils.set(KEY_ANIMATION, getId())
                    }
                    else -> throw IllegalStateException("Unsupported classify")
                }
            }
        })
    }

    override fun onCameraState(
        self: MultiCameraClient.ICamera,
        code: ICameraStateCallBack.State,
        msg: String?
    ) {
        when (code) {
            ICameraStateCallBack.State.OPENED -> handleCameraOpened()
            ICameraStateCallBack.State.CLOSED -> handleCameraClosed()
            ICameraStateCallBack.State.ERROR -> handleCameraError(msg)
        }
    }

    private fun handleCameraError(msg: String?) {
        mViewBinding.uvcLogoIv.visibility = View.VISIBLE
        mViewBinding.frameRateTv.visibility = View.GONE
        ToastUtils.show("Camera opened error: $msg")
    }

    private fun handleCameraClosed() {
        mViewBinding.uvcLogoIv.visibility = View.VISIBLE
        mViewBinding.frameRateTv.visibility = View.GONE
        ToastUtils.show("Camera closed success")
    }

    private fun handleCameraOpened() {
        mViewBinding.uvcLogoIv.visibility = View.GONE
        mViewBinding.frameRateTv.visibility = View.VISIBLE
        mViewBinding.brightnessSb.max = (getCurrentCamera() as? CameraUVC)?.getBrightnessMax() ?: 100
        mViewBinding.brightnessSb.progress = (getCurrentCamera() as? CameraUVC)?.getBrightness() ?: 0
        Logger.i(TAG, "max = ${mViewBinding.brightnessSb.max}, progress = ${mViewBinding.brightnessSb.progress}")
        mViewBinding.brightnessSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                (getCurrentCamera() as? CameraUVC)?.setBrightness(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        ToastUtils.show("Camera opened success")
        playMic()
    }

    private fun switchLayoutClick() {
        mViewBinding.takePictureModeTv.setOnClickListener {
            if (mCameraMode == CaptureMediaView.CaptureMode.MODE_CAPTURE_PIC) {
                return@setOnClickListener
            }
            mCameraMode = CaptureMediaView.CaptureMode.MODE_CAPTURE_PIC
            updateCameraModeSwitchUI()
        }
        mViewBinding.recordVideoModeTv.setOnClickListener {
            if (mCameraMode == CaptureMediaView.CaptureMode.MODE_CAPTURE_VIDEO) {
                return@setOnClickListener
            }
            mCameraMode = CaptureMediaView.CaptureMode.MODE_CAPTURE_VIDEO
            updateCameraModeSwitchUI()
        }
        mViewBinding.recordAudioModeTv.setOnClickListener {
            if (mCameraMode == CaptureMediaView.CaptureMode.MODE_CAPTURE_AUDIO) {
                return@setOnClickListener
            }
            mCameraMode = CaptureMediaView.CaptureMode.MODE_CAPTURE_AUDIO
            updateCameraModeSwitchUI()
        }
        updateCameraModeSwitchUI()
        showRecentMedia()
    }

    override fun getCameraView(): IAspectRatio {
        return AspectRatioTextureView(requireContext())
    }

    override fun getCameraViewContainer(): ViewGroup {
        return mViewBinding.cameraViewContainer
    }

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View {
        mViewBinding = FragmentDemoBinding.inflate(inflater, container, false)
        return mViewBinding.root
    }

    override fun getGravity(): Int = Gravity.CENTER

    override fun onViewClick(mode: CaptureMediaView.CaptureMode?) {
        if (!isCameraOpened()) {
            ToastUtils.show("Camera not worked!")
            return
        }
        when (mode) {
            CaptureMediaView.CaptureMode.MODE_CAPTURE_PIC -> captureImage()
            CaptureMediaView.CaptureMode.MODE_CAPTURE_AUDIO -> captureAudio()
            else -> captureVideo()
        }
    }

    private fun captureAudio() {
        if (isCapturingVideoOrAudio) {
            captureAudioStop()
            return
        }
        captureAudioStart(object : ICaptureCallBack {
            override fun onBegin() {
                isCapturingVideoOrAudio = true
                mViewBinding.captureBtn.setCaptureVideoState(CaptureMediaView.CaptureVideoState.DOING)
                mViewBinding.modeSwitchLayout.visibility = View.GONE
                mViewBinding.toolbarGroup.visibility = View.GONE
                mViewBinding.albumPreviewIv.visibility = View.GONE
                mViewBinding.lensFacingBtn1.visibility = View.GONE
                mViewBinding.recTimerLayout.visibility = View.GONE
                startMediaTimer()
            }

            override fun onError(error: String?) {
                ToastUtils.show(error ?: "Unknown error")
                isCapturingVideoOrAudio = false
                mViewBinding.captureBtn.setCaptureVideoState(CaptureMediaView.CaptureVideoState.UNDO)
                stopMediaTimer()
            }

            override fun onComplete(path: String?) {
                isCapturingVideoOrAudio = false
                mViewBinding.captureBtn.setCaptureVideoState(CaptureMediaView.CaptureVideoState.UNDO)
                mViewBinding.recTimerLayout.visibility = View.GONE
                stopMediaTimer()
                ToastUtils.show(path ?: "Error")
            }
        })
    }

    private fun captureVideo() {
        if (isCapturingVideoOrAudio) {
            captureVideoStop()
            return
        }
        captureVideoStart(object : ICaptureCallBack {
            override fun onBegin() {
                isCapturingVideoOrAudio = true
                mViewBinding.captureBtn.setCaptureVideoState(CaptureMediaView.CaptureVideoState.DOING)
                mViewBinding.modeSwitchLayout.visibility = View.GONE
                mViewBinding.toolbarGroup.visibility = View.GONE
                mViewBinding.albumPreviewIv.visibility = View.GONE
                mViewBinding.lensFacingBtn1.visibility = View.GONE
                mViewBinding.recTimerLayout.visibility = View.GONE
                startMediaTimer()
            }

            override fun onError(error: String?) {
                ToastUtils.show(error ?: "Unknown error")
                isCapturingVideoOrAudio = false
                mViewBinding.captureBtn.setCaptureVideoState(CaptureMediaView.CaptureVideoState.UNDO)
                stopMediaTimer()
            }

            override fun onComplete(path: String?) {
                ToastUtils.show(path ?: "")
                isCapturingVideoOrAudio = false
                mViewBinding.captureBtn.setCaptureVideoState(CaptureMediaView.CaptureVideoState.UNDO)
                mViewBinding.recTimerLayout.visibility = View.GONE
                showRecentMedia(false)
                stopMediaTimer()
            }
        })
    }

    private fun captureImage() {
        captureImage(object : ICaptureCallBack {
            override fun onBegin() {
                mTakePictureTipView.show("", 100)
                mViewBinding.albumPreviewIv.showImageLoadProgress()
                mViewBinding.albumPreviewIv.setNewImageFlag(true)
            }

            override fun onError(error: String?) {
                ToastUtils.show(error ?: "Unknown error")
                mViewBinding.albumPreviewIv.cancelAnimation()
                mViewBinding.albumPreviewIv.setNewImageFlag(false)
            }

            override fun onComplete(path: String?) {
                showRecentMedia(true)
                mViewBinding.albumPreviewIv.setNewImageFlag(false)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mMultiCameraDialog?.hide()
        adJob?.cancel()
    }

    override fun onClick(v: View?) {
        clickAnimation(v!!, object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                when (v) {
                    mViewBinding.lensFacingBtn1 -> {
                        getCurrentCamera()?.let { strategy ->
                            if (strategy is CameraUVC) {
                                showUsbDevicesDialog(getDeviceList(), strategy.getUsbDevice())
                                return
                            }
                        }
                    }
                    mViewBinding.effectsBtn -> showEffectDialog()
                    mViewBinding.cameraTypeBtn -> {}
                    mViewBinding.settingsBtn -> showMoreMenu()
                    mViewBinding.voiceBtn -> playMic()
                    mViewBinding.resolutionBtn -> showResolutionDialog()
                    mViewBinding.albumPreviewIv -> goToGalley()
                    mMoreBindingView.multiplex, mMoreBindingView.multiplexText -> goToMultiplexActivity()
                    mMoreBindingView.contact, mMoreBindingView.contactText -> showContactDialog()
                    else -> {}
                }
            }
        })
    }

    @SuppressLint("CheckResult")
    private fun showUsbDevicesDialog(usbDeviceList: MutableList<UsbDevice>?, curDevice: UsbDevice?) {
        if (usbDeviceList.isNullOrEmpty()) {
            ToastUtils.show("Get USB device failed")
            return
        }
        val list = arrayListOf<String>()
        var selectedIndex: Int = -1
        for (index in usbDeviceList.indices) {
            val dev = usbDeviceList[index]
            val devName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !dev.productName.isNullOrEmpty()) {
                "${dev.productName}(${dev.deviceId})"
            } else {
                dev.deviceName
            }
            val curDevName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !curDevice?.productName.isNullOrEmpty()) {
                "${curDevice!!.productName}(${curDevice.deviceId})"
            } else {
                curDevice?.deviceName
            }
            if (devName == curDevName) {
                selectedIndex = index
            }
            list.add(devName)
        }
        MaterialDialog(requireContext()).show {
            listItemsSingleChoice(
                items = list,
                initialSelection = selectedIndex
            ) { _, index, _ ->
                if (selectedIndex == index) return@listItemsSingleChoice
                switchCamera(usbDeviceList[index])
            }
        }
    }

    private fun showEffectDialog() {
        EffectListDialog(requireActivity()).apply {
            setData(mEffectDataList, object : EffectListDialog.OnEffectClickListener {
                override fun onEffectClick(effect: CameraEffect) {
                    mEffectDataList.find { it.id == effect.id }?.also {
                        if (it == null) {
                            ToastUtils.show("Set effect failed!")
                            return@also
                        }
                        updateRenderEffect(it.classifyId, it.effect)
                        if (effect.classifyId == CameraEffect.CLASSIFY_ID_ANIMATION) {
                            KEY_ANIMATION
                        } else {
                            KEY_FILTER
                        }.also { key ->
                            MMKVUtils.set(key, effect.id)
                        }
                    }
                }
            })
            show()
        }
    }

    @SuppressLint("CheckResult")
    private fun showResolutionDialog() {
        mMoreMenu?.dismiss()
        getAllPreviewSizes().let { previewSizes ->
            if (previewSizes.isNullOrEmpty()) {
                ToastUtils.show("Get camera preview size failed")
                return
            }
            val list = arrayListOf<String>()
            var selectedIndex: Int = -1
            for (index in previewSizes.indices) {
                val w = previewSizes[index].width
                val h = previewSizes[index].height
                getCurrentPreviewSize()?.apply {
                    if (width == w && height == h) {
                        selectedIndex = index
                    }
                }
                list.add("$w x $h")
            }
            MaterialDialog(requireContext()).show {
                listItemsSingleChoice(
                    items = list,
                    initialSelection = selectedIndex
                ) { _, index, _ ->
                    if (selectedIndex == index) return@listItemsSingleChoice
                    updateResolution(previewSizes[index].width, previewSizes[index].height)
                }
            }
        }
    }

    private fun goToMultiplexActivity() {
        mMoreMenu?.dismiss()
        mMultiCameraDialog = MultiCameraDialog()
        mMultiCameraDialog?.setOnDismissListener(object : BaseBottomDialog.OnDismissListener {
            override fun onDismiss() {
                registerMultiCamera()
            }
        })
        mMultiCameraDialog?.show(childFragmentManager, "multiRoadCameras")
        unRegisterMultiCamera()
    }

    private fun showContactDialog() {
        mMoreMenu?.dismiss()
        MaterialDialog(requireContext()).show {
            title(R.string.dialog_contact_title)
            message(text = getString(R.string.dialog_contact_message, getVersionName()))
        }
    }

    private fun getVersionName(): String? {
        context ?: return null
        val packageManager = requireContext().packageManager
        try {
            val packageInfo = packageManager.getPackageInfo(requireContext().packageName, 0)
            return packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return null
    }

    private fun goToGalley() {
        try {
            Intent(
                Intent.ACTION_VIEW,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            ).apply {
                startActivity(this)
            }
        } catch (e: Exception) {
            ToastUtils.show("Open error: ${e.localizedMessage}")
        }
    }

    private fun playMic() {
        if (isPlayingMic) {
            stopPlayMic()
            return
        }
        startPlayMic(object : IPlayCallBack {
            override fun onBegin() {
                mViewBinding.voiceBtn.setImageResource(R.mipmap.camera_voice_on)
                isPlayingMic = true
                Toast.makeText(requireContext(), "Audio started", Toast.LENGTH_SHORT).show()
            }

            override fun onError(error: String) {
                mViewBinding.voiceBtn.setImageResource(R.mipmap.camera_voice_off)
                isPlayingMic = false
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }

            override fun onComplete() {
                mViewBinding.voiceBtn.setImageResource(R.mipmap.camera_voice_off)
                isPlayingMic = false
                Toast.makeText(requireContext(), "Audio completed", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showRecentMedia(isImage: Boolean? = null) {
        lifecycleScope.launch(Dispatchers.IO) {
            context ?: return@launch
            if (!isFragmentAttached()) return@launch
            try {
                val path = if (isImage != null) {
                    MediaUtils.findRecentMedia(requireContext(), isImage)
                } else {
                    MediaUtils.findRecentMedia(requireContext())
                }
                path?.also {
                    val size = Utils.dp2px(requireContext(), 38F)
                    ImageLoaders.of(this@DemoFragment)
                        .loadAsBitmap(it, size, size, object : ILoader.OnLoadedResultListener {
                            override fun onLoadedSuccess(bitmap: Bitmap?) {
                                lifecycleScope.launch(Dispatchers.Main) {
                                    mViewBinding.albumPreviewIv.canShowImageBorder = true
                                    mViewBinding.albumPreviewIv.setImageBitmap(bitmap)
                                }
                            }

                            override fun onLoadedFailed(error: Exception?) {
                                lifecycleScope.launch(Dispatchers.Main) {
                                    mViewBinding.albumPreviewIv.cancelAnimation()
                                }
                            }
                        })
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    ToastUtils.show("${e.localizedMessage}")
                }
                Logger.e(TAG, "showRecentMedia failed", e)
            }
        }
    }

    private fun updateCameraModeSwitchUI() {
        mViewBinding.modeSwitchLayout.children.forEach { it ->
            val tabTv = it as TextView
            val isSelected = tabTv.id == mCameraModeTabMap[mCameraMode]
            val typeface = if (isSelected) Typeface.BOLD else Typeface.NORMAL
            tabTv.typeface = Typeface.defaultFromStyle(typeface)
            tabTv.setTextColor(if (isSelected) 0xFFFFFFFF.toInt() else 0xFFD7DAE1.toInt())
            tabTv.setShadowLayer(
                Utils.dp2px(requireContext(), 1F).toFloat(),
                0F,
                0F,
                0xBF000000.toInt()
            )
            TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(
                tabTv,
                0,
                0,
                0,
                if (isSelected) R.mipmap.camera_preview_dot_blue else R.drawable.camera_bottom_dot_transparent
            )
            tabTv.compoundDrawablePadding = 1
        }
        mViewBinding.captureBtn.setCaptureViewTheme(CaptureMediaView.CaptureViewTheme.THEME_WHITE)
        val height = mViewBinding.controlPanelLayout.height
        mViewBinding.captureBtn.setCaptureMode(mCameraMode)
        if (mCameraMode == CaptureMediaView.CaptureMode.MODE_CAPTURE_PIC) {
            val translationX = ObjectAnimator.ofFloat(
                mViewBinding.controlPanelLayout,
                "translationY",
                height.toFloat(),
                0.0f
            )
            translationX.duration = 600
            translationX.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    mViewBinding.controlPanelLayout.visibility = View.GONE
                }
            })
            translationX.start()
        } else {
            val translationX = ObjectAnimator.ofFloat(
                mViewBinding.controlPanelLayout,
                "translationY",
                0.0f,
                height.toFloat()
            )
            translationX.duration = 600
            translationX.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    mViewBinding.controlPanelLayout.visibility = View.GONE
                }
            })
            translationX.start()
        }
    }

    private fun clickAnimation(v: View, listener: Animator.AnimatorListener) {
        val scaleXAnim = ObjectAnimator.ofFloat(v, "scaleX", 1.0f, 0.4f, 1.0f)
        val scaleYAnim = ObjectAnimator.ofFloat(v, "scaleY", 1.0f, 0.4f, 1.0f)
        val alphaAnim = ObjectAnimator.ofFloat(v, "alpha", 1.0f, 0.4f, 1.0f)
        val animatorSet = AnimatorSet()
        animatorSet.duration = 150
        animatorSet.addListener(listener)
        animatorSet.playTogether(scaleXAnim, scaleYAnim, alphaAnim)
        animatorSet.start()
    }

    private fun showMoreMenu() {
        if (mMoreMenu == null) {
            layoutInflater.inflate(R.layout.dialog_more, null).apply {
                mMoreBindingView = DialogMoreBinding.bind(this)
                mMoreBindingView.multiplex.setOnClickListener(this@DemoFragment)
                mMoreBindingView.multiplexText.setOnClickListener(this@DemoFragment)
                mMoreBindingView.contact.setOnClickListener(this@DemoFragment)
                mMoreBindingView.contactText.setOnClickListener(this@DemoFragment)
                mMoreBindingView.resolution.setOnClickListener(this@DemoFragment)
                mMoreBindingView.resolutionText.setOnClickListener(this@DemoFragment)
                mMoreBindingView.contract.setOnClickListener(this@DemoFragment)
                mMoreBindingView.contractText.setOnClickListener(this@DemoFragment)
                mMoreMenu = PopupWindow(
                    this,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
                ).apply {
                    isOutsideTouchable = true
                    setBackgroundDrawable(
                        ContextCompat.getDrawable(requireContext(), R.mipmap.camera_icon_one_inch_alpha)
                    )
                }
            }
        }
        try {
            mMoreMenu?.showAsDropDown(mViewBinding.settingsBtn, 0, 0, Gravity.START)
        } catch (e: Exception) {
            Logger.e(TAG, "showMoreMenu fail", e)
        }
    }

    private fun startMediaTimer() {
        val pushTask: TimerTask = object : TimerTask() {
            override fun run() {
                mRecSeconds++
                if (mRecSeconds >= 60) {
                    mRecSeconds = 0
                    mRecMinute++
                }
                if (mRecMinute >= 60) {
                    mRecMinute = 0
                    mRecHours++
                    if (mRecHours >= 24) {
                        mRecHours = 0
                        mRecMinute = 0
                        mRecSeconds = 0
                    }
                }
                mMainHandler.sendEmptyMessage(WHAT_START_TIMER)
            }
        }
        if (mRecTimer != null) {
            stopMediaTimer()
        }
        mRecTimer = Timer()
        mRecTimer?.schedule(pushTask, 1000, 1000)
    }

    private fun stopMediaTimer() {
        mRecTimer?.cancel()
        mRecTimer = null
        mRecHours = 0
        mRecMinute = 0
        mRecSeconds = 0
        mMainHandler.sendEmptyMessage(WHAT_STOP_TIMER)
    }

    private fun calculateTime(seconds: Int, minute: Int, hour: Int? = null): String {
        val mBuilder = StringBuilder()
        if (hour != null) {
            if (hour < 10) mBuilder.append("0").append(hour) else mBuilder.append(hour)
            mBuilder.append(":")
        }
        if (minute < 10) mBuilder.append("0").append(minute) else mBuilder.append(minute)
        mBuilder.append(":")
        if (seconds < 10) mBuilder.append("0").append(seconds) else mBuilder.append(seconds)
        return mBuilder.toString()
    }

    companion object {
        private const val TAG = "DemoFragment"
        private const val WHAT_START_TIMER = 0x00
        private const val WHAT_STOP_TIMER = 0x01
    }
}
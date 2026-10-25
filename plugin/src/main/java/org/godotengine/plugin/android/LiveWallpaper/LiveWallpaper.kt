@file:Suppress("PackageName")

package org.godotengine.plugin.android.LiveWallpaper

import android.app.WallpaperManager
import android.app.WallpaperManager.FLAG_LOCK
import android.app.WallpaperManager.FLAG_SYSTEM
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.WindowInsets
import android.view.WindowInsets.Type.systemBars
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot
import kotlin.math.ceil

class LiveWallpaperService : WallpaperService() {
    companion object {
        private var instance: LiveWallpaperService? = null

        fun getInstance(): LiveWallpaperService? {
            return instance
        }

        var windowInsets: WindowInsets? = null
    }

    private var mSurfaceNeedsUpdate: Boolean = false
    private var mGodotWallpaper: GodotWallpaper? = null
    var topEngine: LiveWallpaperEngine? = null

    private var engineRun: Int = 0

    override fun onCreate() {
        Logwp("[Service] onCreate")
        super.onCreate()
        if (instance == null) {
            instance = this
        }
        mGodotWallpaper = GodotWallpaper(applicationContext)
    }

    override fun onCreateEngine(): Engine {
        engineRun++
        Logwp("[Service] engineRun:$engineRun")
        topEngine = LiveWallpaperEngine()
        return topEngine!!
    }

    override fun onDestroy() {
        Logwp("[Service] onDestroy")
        super.onDestroy()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        mGodotWallpaper?.wpPlugin?.emitMemoryTrim(level)
    }

    inner class LiveWallpaperEngine : Engine() {
        var mSurfaceHolder: SurfaceHolder? = null

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            Logwp("[Engine$engineRun] onCreate")

            super.onCreate(surfaceHolder)
            if (engineRun == 1) {
                mGodotWallpaper?.onCreate()
                mGodotWallpaper?.InitNativeEngine()
            }
        }

        override fun onSurfaceCreated(surfaceHolder: SurfaceHolder) {
            Logwp("[Engine$engineRun] onSurfaceCreated")
            super.onSurfaceCreated(surfaceHolder)
            mSurfaceHolder = surfaceHolder
            mGodotWallpaper?.SetSurfaceHolder(mSurfaceHolder!!)
            if (engineRun == 1) {
                mGodotWallpaper?.InitRenderView()
                return
            }
            mGodotWallpaper?.SurfaceUpdated()
        }

        override fun onSurfaceChanged(
            surfaceHolder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(surfaceHolder, format, width, height)
            mSurfaceHolder = surfaceHolder
            mGodotWallpaper?.SurfaceUpdated()
            Logwp("[Engine$engineRun] onSurfaceChanged")
        }

        override fun onSurfaceDestroyed(surfaceHolder: SurfaceHolder) {
            super.onSurfaceDestroyed(surfaceHolder)
            mSurfaceNeedsUpdate = true
            Logwp("[Engine$engineRun] onSurfaceDestroyed")
            if (engineRun == 1) {
                mGodotWallpaper?.Destroy()
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                Logwp("[Engine$engineRun] visible")
                if (this !== topEngine || mSurfaceNeedsUpdate) {
                    mGodotWallpaper?.SetSurfaceHolder(mSurfaceHolder!!)
                    mGodotWallpaper?.SurfaceUpdated()
                    mSurfaceNeedsUpdate = false
                    topEngine = this
                }
                mGodotWallpaper?.Resume()
            } else {
                Logwp("[Engine$engineRun] not visible")
                mGodotWallpaper?.Pause()
            }

            if (this === topEngine) {
                mGodotWallpaper?.wpPlugin?.emitVisibilityChanged(visible)
            }
        }

        override fun onApplyWindowInsets(insets: WindowInsets?) {
            windowInsets = insets
            insets?.let {
                val (top: Int, bottom: Int, left: Int, right: Int) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    with(it.getInsets(systemBars())) {
                        arrayOf(top, bottom, left, right)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    arrayOf(
                        it.systemWindowInsetTop,
                        it.systemWindowInsetBottom,
                        it.systemWindowInsetLeft,
                        it.systemWindowInsetRight
                    )
                }
                mGodotWallpaper?.wpPlugin?.emitInsetSignal(left, right, top, bottom)
            }

            super.onApplyWindowInsets(insets)
        }

        override fun onTouchEvent(event: MotionEvent?) {
            super.onTouchEvent(event)
            if (event != null) {
                mGodotWallpaper?.onTouchEvent(event)
            }
        }

        override fun onCommand(
            action: String?,
            x: Int,
            y: Int,
            z: Int,
            extras: Bundle?,
            resultRequested: Boolean
        ): Bundle {
            mGodotWallpaper?.wpPlugin?.emitOnCommand(action ?: "null", x, y, z, resultRequested)
            return Bundle()
        }

        override fun onOffsetsChanged(
            xOffset: Float,
            yOffset: Float,
            xOffsetStep: Float,
            yOffsetStep: Float,
            xPixelOffset: Int,
            yPixelOffset: Int
        ) {
            super.onOffsetsChanged(
                xOffset,
                yOffset,
                xOffsetStep,
                yOffsetStep,
                xPixelOffset,
                yPixelOffset
            )
            val homeScreenCountx = if (xOffsetStep > 0) ceil(1.0 / xOffsetStep).toInt() + 1 else 1
            val homeScreenCounty = if (yOffsetStep > 0) ceil(1.0 / yOffsetStep).toInt() + 1 else 1
            mGodotWallpaper?.wpPlugin?.emitOnHomeScreenCountUpdated(homeScreenCountx, homeScreenCounty)
            mGodotWallpaper?.wpPlugin?.emitOnOffsetsChanged(xOffset, yOffset, xPixelOffset, yPixelOffset)
        }

        override fun onDestroy() {
            Logwp("[Engine$engineRun] onDestroy")
            super.onDestroy()
            engineRun--
        }
    }
}

class LiveWallpaper(godot: Godot) : GodotPlugin(godot) {

    companion object {
        private var instance: LiveWallpaper? = null

        @Suppress("unused")
        fun getInstance(): LiveWallpaper? {
            return instance
        }
    }

    init {
        instance = this
    }

    override fun getPluginName() = "LiveWallpaper"

    override fun getPluginSignals(): Set<SignalInfo> {
        Logwp("[Plugin] getPluginSignals")
        val signal = mutableSetOf<SignalInfo>()
        signal.add(SignalInfo("TrimMemory", Int::class.javaObjectType))
        signal.add(
            SignalInfo(
                "ApplyWindowInsets",
                Int::class.javaObjectType,
                Int::class.javaObjectType,
                Int::class.javaObjectType,
                Int::class.javaObjectType
            )
        )
        signal.add(SignalInfo("VisibilityChanged", Boolean::class.javaObjectType))
        signal.add(
            SignalInfo(
                "OnCommand",
                String::class.java,
                Int::class.javaObjectType,
                Int::class.javaObjectType,
                Int::class.javaObjectType,
                Boolean::class.javaObjectType
            )
        )
        signal.add(
            SignalInfo(
                "onOffsetsChanged",
                Float::class.javaObjectType,
                Float::class.javaObjectType,
                Int::class.javaObjectType,
                Int::class.javaObjectType
            )
        )
        signal.add(
            SignalInfo(
                "onHomeScreenCountUpdated",
                Int::class.javaObjectType,
                Int::class.javaObjectType
            )
        )
        return signal
    }

    @UsedByGodot
    @Suppress("unused")
    fun startWallpaperService(): Int {
        activity?.let { context ->
            if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_LIVE_WALLPAPER)) {
                return 0
            }

            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(context, LiveWallpaperService::class.java)
                )
            }
            context.startActivity(intent)
        }
        return 1
    }

    @UsedByGodot
    @Suppress("unused")
    fun isLiveWallpaperInUse(): Boolean {
        val currentActivity = activity ?: return false
        val wallpaperManager = WallpaperManager.getInstance(currentActivity)
        val currentWallpaperService = wallpaperManager.wallpaperInfo
        return currentWallpaperService?.packageName == currentActivity.packageName
    }

    @UsedByGodot
    @Suppress("FunctionName", "unused")
    fun IsPreview(): Boolean {
        return LiveWallpaperService.getInstance()?.topEngine?.isPreview ?: false
    }

    @UsedByGodot
    @Suppress("FunctionName", "unused")
    fun ResetToDefaultWallpaper() {
        val currentActivity = activity ?: return
        val wallpaperManager = WallpaperManager.getInstance(currentActivity)
        try {
            wallpaperManager.clear(FLAG_LOCK or FLAG_SYSTEM)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @UsedByGodot
    @Suppress("FunctionName", "unused")
    fun IsLiveWallpaper(): Boolean {
        return LiveWallpaperService.getInstance() != null
    }

    fun emitInsetSignal(left: Int, right: Int, top: Int, bottom: Int) {
        emitSignal("ApplyWindowInsets", left, right, top, bottom)
    }

    fun emitMemoryTrim(level: Int) {
        emitSignal("TrimMemory", level)
    }

    fun emitVisibilityChanged(isVisible: Boolean) {
        emitSignal("VisibilityChanged", isVisible)
    }

    fun emitOnCommand(action: String, x: Int, y: Int, z: Int, result: Boolean) {
        emitSignal("OnCommand", action, x, y, z, result)
    }

    fun emitOnOffsetsChanged(xOffset: Float, yOffset: Float, xPixelOffset: Int, yPixelOffset: Int) {
        emitSignal("onOffsetsChanged", xOffset, yOffset, xPixelOffset, yPixelOffset)
    }

    fun emitOnHomeScreenCountUpdated(countx: Int, county: Int) {
        emitSignal("onHomeScreenCountUpdated", countx, county)
    }
}

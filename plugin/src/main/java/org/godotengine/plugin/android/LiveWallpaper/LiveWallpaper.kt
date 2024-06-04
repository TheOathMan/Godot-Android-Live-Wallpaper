package org.godotengine.plugin.android.LiveWallpaper

import android.annotation.TargetApi
import android.app.Activity
import android.app.WallpaperManager
import android.app.WallpaperManager.FLAG_LOCK
import android.app.WallpaperManager.FLAG_SYSTEM
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsets.Type.systemBars
import android.widget.Toast
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot
import javax.microedition.khronos.opengles.GL10

class LiveWallpaperService : WallpaperService() {
    private val TAG = "godot"
    companion object {
        private var instance: LiveWallpaperService? = null

        fun getInstance(): LiveWallpaperService? {
            return instance
        }

        fun initialize(liveWallpaperService: LiveWallpaperService) {
            if (instance == null) {
                instance = liveWallpaperService
            }
        }
        var windowInsets:WindowInsets?=null
    }

    var pathToSecondaryWP:String?=null
    var godotWallpaper:GodotWallpaper?=null

    var liveWallpaperEngine:LiveWallpaperEngine?=null

    private var EngineRun:Int=0

    override fun onCreate() {
        super.onCreate()
        initialize(this)
        godotWallpaper = GodotWallpaper(applicationContext)
        Log.v(TAG,"WallpaperService onCreate")
    }
    override fun onCreateEngine(): Engine {
        EngineRun++;
        Log.v(TAG, "EngineRun:$EngineRun") //debug
        return LiveWallpaperEngine()
    }
    override fun onDestroy() {
        Log.v(TAG,"WallpaperService onDestroy")
        godotWallpaper?.terminateGodotLiveWallpaperService()
        super.onDestroy()
    }
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        /*
         #  TRIM_MEMORY_COMPLETE: The system is experiencing a severe memory shortage and might start killing processes.
         #  TRIM_MEMORY_MODERATE: Memory is getting tight, and the system suggests freeing up as much memory as possible.
         #  TRIM_MEMORY_BACKGROUND: Memory is low, and background processes are strong candidates for termination.
         #  TRIM_MEMORY_RUNNING_MODERATE: Similar to TRIM_MEMORY_MODERATE but applies to foreground processes.
         #  TRIM_MEMORY_RUNNING_CRITICAL: System is in a critical memory state and might even terminate foreground processes.
         */

        godotWallpaper?.wpPlugin?.EmitMemoryTrim(level)
    }

    inner class LiveWallpaperEngine : Engine(){

        var Visible: Boolean=false
        var mSurfaceHolder: SurfaceHolder?=null
        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            if(EngineRun==1) {
                godotWallpaper?.onCreate()
                godotWallpaper?.InitNativeEngine()
            }
        }

        override fun onSurfaceCreated(surfaceHolder: SurfaceHolder) {
            super.onSurfaceCreated(surfaceHolder)
            mSurfaceHolder = surfaceHolder
            godotWallpaper?.SetSurfaceHolder(mSurfaceHolder!!)

            if(EngineRun==1) {
                godotWallpaper?.InitRenderEngine()
                godotWallpaper?.InitPlugins()
            }
            Log.v(TAG,"onSurfaceCreated")
        }

        override fun onSurfaceChanged(surfaceHolder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(surfaceHolder, format, width, height)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            Visible=visible
            godotWallpaper?.wpPlugin?.EmitVisibilityChanged(visible)
            if (!visible) {
                Log.v(TAG,"not visible")
                godotWallpaper?.Pause()
            } else {
                Log.v(TAG,"visible")
                if (godotWallpaper?.godotGLRenderViewLW?.view?.holder!=mSurfaceHolder) {
                    godotWallpaper?.SetSurfaceHolder(mSurfaceHolder!!)
                }
                godotWallpaper?.Resume()
            }
        }

        override fun onApplyWindowInsets(insets: WindowInsets?) {
            //TODO: is all devices call this function at startup? because we need a WindowInsets with
            // Godot 4.3. Failing to obtain this before Engine's onCreate(), might result in Error for
            // our ProxyWindow class.
            windowInsets=insets
            insets?.let {
                val (top:Int, bottom:Int, left:Int, right:Int) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    with(it.getInsets(systemBars())) {
                        arrayOf(top, bottom, left, right)
                    }
                } else {
                    arrayOf(it.systemWindowInsetTop, it.systemWindowInsetBottom, it.systemWindowInsetLeft, it.systemWindowInsetRight)
                }
                godotWallpaper?.wpPlugin?.EmitInsetSignal(left,right,top,bottom)
            }
            super.onApplyWindowInsets(insets)

        }


        override fun onTouchEvent(event: MotionEvent?) {
            super.onTouchEvent(event)
            if (event != null) {
                godotWallpaper?.onTouchEvent(event)
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
            Log.v(TAG, " onCommand(" + action + " " + x + " " + y + " " + z + " " + extras
                    + " " + resultRequested + ")");
            //return super.onCommand(action, x, y, z, extras, resultRequested)
            return Bundle()
        }

        override fun onSurfaceDestroyed(surfaceHolder: SurfaceHolder) {
            Log.v(TAG,"onSurfaceDestroyed")

            if (EngineRun==1) {
                godotWallpaper?.Destroy()
            }

            super.onSurfaceDestroyed(surfaceHolder)
        }

        override fun onDestroy() {
            Log.v(TAG,"LiveWallpaperEngine onDestroy")
            super.onDestroy()
            EngineRun--;
        }
    }

}


class LiveWallpaper(godot: Godot): GodotPlugin(godot) {
    private val TAG = "godot"
    override fun getPluginName() = "LiveWallpaper"

    override fun getPluginSignals(): Set<SignalInfo> {
        val signal = mutableSetOf<SignalInfo>()
        signal.add(SignalInfo("TrimMemory",Integer::class.java))
        signal.add(SignalInfo("ApplyWindowInsets",Integer::class.java,Integer::class.java,Integer::class.java,Integer::class.java))
        signal.add(SignalInfo("VisibilityChanged",java.lang.Boolean::class.java))
        return signal
    }

    override fun onMainCreate(activity: Activity?): View? {
        Log.v(TAG,"onMainCreate GodotPlugin")
        //Log.v(TAG,"Is Wallpaper in use? "+isLiveWallpaperInUse().toString())
        return super.onMainCreate(activity)
    }

    override fun onMainDestroy() {
        Log.v(TAG,"onMainDestroy")
        super.onMainDestroy()
    }

    @TargetApi(Build.VERSION_CODES.N)
    override fun onMainResume() {
        Log.v(TAG,"onMainResume")
        super.onMainResume()
    }

    override fun onMainPause() {
        super.onMainPause()
        Log.v(TAG,"onMainPause")
    }

    override fun onGLDrawFrame(gl: GL10?) {
        //Log.v(TAG,"draw---------------")
        super.onGLDrawFrame(gl)
    }

    @UsedByGodot
    private fun startWallpaperService() {
        activity?.let { context ->
            if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_LIVE_WALLPAPER)) {
                runOnUiThread {
                    Toast.makeText(
                        context,
                        "Live Wallpaper is not supported by this device",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return
            }

            // TODO[Optional]: show wallpaper CHOOSER if the wallpaper is already in use.
            //val wallpaperManager = WallpaperManager.getInstance(context)
            //val currentWallpaperComponent = wallpaperManager.wallpaperInfo?.component
            //val newWallpaperComponent = ComponentName(context, LiveWallpaperService::class.java)
            //if (currentWallpaperComponent == newWallpaperComponent) {
            //    // Open the settings or preview of the active live wallpaper
            //    val intent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
            //    context.startActivity(intent)
            //    return
            //}

            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(context, LiveWallpaperService::class.java)
                )
            }
            context.startActivity(intent)
        }
    }



    @UsedByGodot
    fun isLiveWallpaperInUse(): Boolean {
        val wallpaperManager = WallpaperManager.getInstance(activity)
        val wallpaperInfo = wallpaperManager.wallpaperInfo
        return wallpaperInfo != null
    }

    @UsedByGodot
    private fun IsPreview():Boolean{
        return LiveWallpaperService.getInstance()?.liveWallpaperEngine?.isPreview?:false
    }

    @UsedByGodot
    fun ResetToDefaultWallpaper() {
        val wallpaperManager = WallpaperManager.getInstance(activity)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wallpaperManager.clear(FLAG_LOCK or FLAG_SYSTEM)
                return
            } // This will reset the wallpaper to the system default
            wallpaperManager.clear()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @UsedByGodot
    fun IsLiveWallpaper(): Boolean {
        return LiveWallpaperService.getInstance() != null
    }

    @UsedByGodot
    fun SetSecondWallpaperImage(filepath:String){
        LiveWallpaperService.getInstance()?.pathToSecondaryWP=filepath
    }

    fun EmitInsetSignal(L:Int,R:Int,U:Int,D:Int){
        emitSignal("ApplyWindowInsets",L,R,U,D)
    }

    fun EmitMemoryTrim(level:Int){
        emitSignal("TrimMemory",level)
    }

    fun EmitVisibilityChanged(isVisible:Boolean){
        emitSignal("VisibilityChanged",isVisible)
    }

}

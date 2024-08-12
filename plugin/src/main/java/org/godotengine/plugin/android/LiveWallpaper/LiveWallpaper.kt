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
    companion object {
        private var instance: LiveWallpaperService? = null
        fun getInstance(): LiveWallpaperService? {
            return instance
        }
        var windowInsets:WindowInsets?=null
    }

    private var mSurfaceNeedsUpdate: Boolean=false
    private var mGodotWallpaper:GodotWallpaper?=null
    var TopEngine:LiveWallpaperEngine?=null


    private var EngineRun:Int=0

    override fun onCreate() {
        Logwp("[Service] onCreate")
        super.onCreate()
        if (instance == null) {
            instance = this
        }
        mGodotWallpaper = GodotWallpaper(applicationContext)
    }
    override fun onCreateEngine(): Engine {
        EngineRun++
        Logwp( "[Service] EngineRun:$EngineRun") //debug
        TopEngine=LiveWallpaperEngine()
        return TopEngine!!
    }
    override fun onDestroy() {
        Logwp("[Service] onDestroy")
        mGodotWallpaper?.terminateGodotLiveWallpaperService()
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

        mGodotWallpaper?.wpPlugin?.EmitMemoryTrim(level)
    }

    inner class LiveWallpaperEngine : Engine(){

        var mSurfaceHolder: SurfaceHolder?=null
        override fun onCreate(surfaceHolder: SurfaceHolder) {
            Logwp("[Engine$EngineRun] onCreate")
            super.onCreate(surfaceHolder)
            if(EngineRun==1) {
                mGodotWallpaper?.onCreate()
                mGodotWallpaper?.InitNativeEngine()
            }
        }

        override fun onSurfaceCreated(surfaceHolder: SurfaceHolder) {
            Logwp("[Engine$EngineRun] onSurfaceCreated")
            super.onSurfaceCreated(surfaceHolder)
            mSurfaceHolder = surfaceHolder
            mGodotWallpaper?.SetSurfaceHolder(mSurfaceHolder!!)
            if(EngineRun==1) {
                mGodotWallpaper?.InitRenderEngine()
                return
            }
            mGodotWallpaper?.SurfaceUpdated()
        }

        override fun onSurfaceChanged(surfaceHolder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(surfaceHolder, format, width, height)
            mSurfaceHolder = surfaceHolder
            mGodotWallpaper?.SurfaceUpdated()
            Logwp("[Engine$EngineRun] onSurfaceChanged")
        }

        override fun onSurfaceDestroyed(surfaceHolder: SurfaceHolder) {
            super.onSurfaceDestroyed(surfaceHolder)
            mSurfaceNeedsUpdate=true
            Logwp("[Engine$EngineRun] onSurfaceDestroyed")
            if (EngineRun==1) {
                mGodotWallpaper?.Destroy()
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                Logwp("[Engine$EngineRun] visible")
                if(this !== TopEngine || mSurfaceNeedsUpdate) {
                    mGodotWallpaper?.SetSurfaceHolder(mSurfaceHolder!!)
                    mGodotWallpaper?.SurfaceUpdated()
                    mSurfaceNeedsUpdate=false
                    TopEngine=this
                }
                mGodotWallpaper?.Resume()
            }else{
                Logwp("[Engine$EngineRun] not visible")
                mGodotWallpaper?.Pause()
            }

            // only send visibility signals when this is top engine. This is to avoid sending mixed signals
            if(this === TopEngine)
                mGodotWallpaper?.wpPlugin?.EmitVisibilityChanged(visible)

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
                mGodotWallpaper?.wpPlugin?.EmitInsetSignal(left,right,top,bottom)
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
            mGodotWallpaper?.wpPlugin?.EmitOnCommand(action?:"null",x,y,z,resultRequested)
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
            mGodotWallpaper?.wpPlugin?.EmitonOffsetsChanged(xOffset, yOffset, xPixelOffset, yPixelOffset)
        }

        override fun onDestroy() {
            Logwp("[Engine$EngineRun] onDestroy")
            super.onDestroy()
            EngineRun--
        }
    }

}


class LiveWallpaper(godot: Godot): GodotPlugin(godot) {

    override fun onGLDrawFrame(gl: GL10?) {
        super.onGLDrawFrame(gl)
        //Logwp("[Plugin] onGLDrawFrame")
    }
    override fun getPluginName() = "LiveWallpaper"

    override fun getPluginSignals(): Set<SignalInfo> {
        Logwp("[Plugin] getPluginSignals")
        val signal = mutableSetOf<SignalInfo>()
        signal.add(SignalInfo("TrimMemory",Integer::class.java))
        signal.add(SignalInfo("ApplyWindowInsets",Integer::class.java,Integer::class.java,Integer::class.java,Integer::class.java))
        signal.add(SignalInfo("VisibilityChanged",java.lang.Boolean::class.java))
        signal.add(SignalInfo("OnCommand", String::class.java, Integer::class.java, Integer::class.java, Integer::class.java, java.lang.Boolean::class.java))
        signal.add(SignalInfo("onOffsetsChanged", java.lang.Float::class.java,java.lang.Float::class.java, Integer::class.java,Integer::class.java))
        return signal
    }

    override fun onMainCreate(activity: Activity?): View? {
        //Logwp("[Plugin] onMainCreate")
        return super.onMainCreate(activity)
    }

    override fun onMainDestroy() {
        //Logwp("[Plugin] onMainDestroy")
        super.onMainDestroy()
    }

    @TargetApi(Build.VERSION_CODES.N)
    override fun onMainResume() {
        //Logwp("[Plugin] onMainResume")
        super.onMainResume()
    }

    override fun onMainPause() {
        //Logwp("[Plugin] onMainPause")
        super.onMainPause()
    }

    @UsedByGodot
    private fun startWallpaperService(): Int {
        activity?.let { context ->
            if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_LIVE_WALLPAPER)) {
                return 0
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
        return 1
    }

    @UsedByGodot
    fun isLiveWallpaperInUse(): Boolean {
        val wallpaperManager = WallpaperManager.getInstance(activity!!)
        // Check if wallpaper service is set (indirect approach)
        val currentWallpaperService = wallpaperManager.wallpaperInfo
        if (currentWallpaperService != null && currentWallpaperService.packageName == activity!!.packageName) {
            return true
        }
        return false
    }

    @UsedByGodot
    private fun IsPreview():Boolean{
        return LiveWallpaperService.getInstance()?.TopEngine?.isPreview?:false
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


    fun EmitInsetSignal(L:Int,R:Int,U:Int,D:Int){
        emitSignal("ApplyWindowInsets",L,R,U,D)
    }

    fun EmitMemoryTrim(level:Int){
        emitSignal("TrimMemory",level)
    }

    fun EmitVisibilityChanged(isVisible:Boolean){
        emitSignal("VisibilityChanged",isVisible)
    }

    fun EmitOnCommand(action: String,x:Int,y:Int,z:Int,result:Boolean){
        emitSignal("OnCommand",action,x,y,z,result)
    }

    fun EmitonOffsetsChanged(xOffset: Float,yOffset: Float,xPixelOffset: Int,yPixelOffset: Int){
        emitSignal("onOffsetsChanged",xOffset,yOffset,xPixelOffset,yPixelOffset)
    }
}

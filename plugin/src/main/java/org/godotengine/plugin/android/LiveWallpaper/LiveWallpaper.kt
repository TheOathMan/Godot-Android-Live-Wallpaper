package org.godotengine.plugin.android.LiveWallpaper

//import android.opengl.GLSurfaceView
//import MyGLWallpaperService

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsets.Type.systemBars
import android.widget.Toast
import org.godotengine.godot.Godot
import org.godotengine.godot.GodotHost
import org.godotengine.godot.GodotLib
import org.godotengine.godot.gl.GLSurfaceView
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.GodotPluginRegistry
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot
import org.godotengine.godot.utils.ProcessPhoenix
import org.godotengine.godot.xr.XRMode
import javax.microedition.khronos.opengles.GL10


class LiveWallpaperService : WallpaperService() {
    private val TAG = "godot"

    companion object {
        private var instance: LiveWallpaperService? = null
        // Function to get the instance
        fun getInstance(): LiveWallpaperService? {
            return instance
        }
        // Function to initialize the instance
        fun initialize(liveWallpaperService: LiveWallpaperService) {

            if (instance == null) {
                instance = liveWallpaperService
            }
        }
        var windowInsets:WindowInsets?=null
    }

    lateinit var m_godot:Godot
    var godotGLRenderViewLW:GodotGLRenderViewLW?=null
    var liveWallpaperEngine:LiveWallpaperEngine?=null
    var wpPlugin:LiveWallpaper?=null
    private var EngineRun:Int=0

    override fun onCreate() {
        super.onCreate()
        initialize(this)
        Log.v(TAG,"WallpaperService onCreate")
        //Log.v(TAG,"test")
    }

    override fun onCreateEngine(): Engine {
        EngineRun++;
        Log.v(TAG, "EngineRun:$EngineRun") //debug
        liveWallpaperEngine=LiveWallpaperEngine()
        return liveWallpaperEngine!!
    }

    override fun onDestroy() {
        super.onDestroy()
        GodotLib.ondestroy()
        Log.v(TAG,"WallpaperService onDestroy")
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
        wpPlugin?.EmitMemoryTrim(level)
    }


    // TODO: if the main engine is active, and an app opens this wallpaper service,
    //  maybe show an empty engine with an image we set from Godot. This is needed
    //  because we can't instantiate the engine twice from one process.
    inner class DummyEngine : Engine() {
            private fun drawFrame() {
                val holder = surfaceHolder
                val canvas: Canvas? = holder.lockCanvas()
                if (canvas != null) {
                    canvas.save()
                    canvas.drawColor(Color.BLUE)
                    canvas.restore()
                    holder.unlockCanvasAndPost(canvas)
                }
            }
    }

    inner class LiveWallpaperEngine : Engine() ,GodotHost {


        private var proxyActivity: ProxyActivity? = null
        override fun onCreate(surfaceHolder: SurfaceHolder) {
            //resources.getDrawable(R.drawable.icon)
            super.onCreate(surfaceHolder)
            //surfaceHolder.surfaceFrame?.inset()
            if(EngineRun==1) {
                m_godot = Godot(applicationContext)
                Log.v(TAG, "LiveWallpaperEngine onCreate")
                Log.v(TAG, "isPreview value: $isPreview()")
                val displayContextCompat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    displayContext?:applicationContext // Use displayContext directly for newer versions
                } else {
                    ContextWrapper(applicationContext)
                }

                //displayContextCompat.get
                proxyActivity = ProxyActivity(applicationContext, displayContextCompat);
                m_godot.onCreate(this)

                if (!m_godot.onInitNativeLayer(this)) {
                    throw IllegalStateException("Unable to initialize engine native layer");
                }

                godotGLRenderViewLW = object :
                    GodotGLRenderViewLW(applicationContext, this, m_godot, XRMode.REGULAR) {
                    override fun getHolder(): SurfaceHolder {
                        return surfaceHolder
                    }
                }.apply {
                    PreRender()
                    setEGLContextClientVersion(2);
                    setRenderer(GetRenderer())
                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                }
                m_godot.renderView = godotGLRenderViewLW

                m_godot.renderView!!.queueOnRenderThread {
                    for (plugin in GodotPluginRegistry.getPluginRegistry().allPlugins) {
                        plugin.onRegisterPluginWithGodotNative()
                        Log.v(TAG, "PluginName:"+plugin.pluginName)
                        if (plugin.pluginName=="LiveWallpaper") {
                            wpPlugin= plugin as LiveWallpaper?
                        }

                    }
                }
            }
        }

        override fun onSurfaceCreated(surfaceHolder: SurfaceHolder) {
            super.onSurfaceCreated(surfaceHolder)
            Log.v(TAG,"onSurfaceCreated")
        }

        override fun onVisibilityChanged(visible: Boolean) {
            wpPlugin?.EmitVisibilityChanged(visible)
            if (!visible) {
                //Log.v(TAG,"not visible")
                m_godot.onPause(this)
            } else {
                //Log.v(TAG,"visible")
                m_godot.onResume(this)
            }
        }

        override fun onApplyWindowInsets(insets: WindowInsets?) {
            windowInsets=insets
            Log.v(TAG,"onApplyWindowInsets=================================")
            //TODO: send to Godot to inform user about screen insets
            insets?.let {
                val (top:Int, bottom:Int, left:Int, right:Int) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    with(it.getInsets(systemBars())) {
                        arrayOf(top, bottom, left, right)
                    }
                } else {
                    arrayOf(it.systemWindowInsetTop, it.systemWindowInsetBottom, it.systemWindowInsetLeft, it.systemWindowInsetRight)
                }
                wpPlugin?.EmitInsetSignal(left,right,top,bottom)
            }
            super.onApplyWindowInsets(insets)

        }

        override fun onSurfaceChanged(surfaceHolder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(surfaceHolder, format, width, height)
        }

        override fun onSurfaceDestroyed(surfaceHolder: SurfaceHolder) {
            Log.v(TAG,"onSurfaceDestroyed")
            super.onSurfaceDestroyed(surfaceHolder)
        }

        override fun onTouchEvent(event: MotionEvent?) {
            super.onTouchEvent(event)
            if (event != null) {
                godotGLRenderViewLW?.onTouchEvent(event)
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            Log.v(TAG,"LiveWallpaperEngine onDestroy")
            godotGLRenderViewLW?.onPause()
            ProcessPhoenix.forceQuit(activity)
        }

        //==================== GodotHost Requirements ====================\\

        override fun getActivity(): Activity? {
            return proxyActivity
            //return ContextWrapper(applicationContext)
        }

        override fun getGodot(): Godot {
            return m_godot
        }

        //override fun onGodotForceQuit(instance: Godot?) {
        //    super.onGodotForceQuit(instance)
        //    ProcessPhoenix.forceQuit(activity)
        //}

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

    override fun onMainResume() {
        Log.v(TAG,"onMainResume")
       super.onMainResume()
    }

    override fun onMainPause() {
        super.onMainPause()
        Log.v(TAG,"onMainPause")
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
        if(isLiveWallpaperInUse()) {
            val wallpaperManager = WallpaperManager.getInstance(activity)
            try {
                wallpaperManager.clear() // This will reset the wallpaper to the system default
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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

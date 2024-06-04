

package org.godotengine.plugin.android.LiveWallpaper

//import org.godotengine.godot.utils.ProcessPhoenix
//import com.jakewharton.processphoenix.PhoenixService

import android.app.Activity
import android.content.Context
import android.os.Process
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import org.godotengine.godot.Godot
import org.godotengine.godot.GodotHost
import org.godotengine.godot.GodotLib
import org.godotengine.godot.gl.GLSurfaceView
import org.godotengine.godot.plugin.GodotPluginRegistry
import org.godotengine.godot.xr.XRMode


class GodotWallpaper(private val context: Context) : GodotHost {

    private val TAG = "godot"
    private var mGodot:Godot = Godot(context)
    private var proxyActivity: ProxyActivity = ProxyActivity(context)
    var godotGLRenderViewLW:GodotGLRenderViewLW?=null
    var wpPlugin:LiveWallpaper?=null
    private var mSurfaceHolder: SurfaceHolder?=null
    protected var view: SurfaceHolder.Callback2? = null

    private val lock = Any()

    fun onCreate(){
        mGodot.onCreate(this)
    }

    fun InitNativeEngine(){
        if (!mGodot.onInitNativeLayer(this)) {
            throw IllegalStateException("Unable to initialize engine native layer");
        }
    }

    fun SetSurfaceHolder(holder: SurfaceHolder){
        mSurfaceHolder=holder
    }

    fun InitRenderEngine(){
        godotGLRenderViewLW = object : GodotGLRenderViewLW(context, this, mGodot, XRMode.REGULAR ) {
            override fun getHolder(): SurfaceHolder {
                return mSurfaceHolder!!
            }
        }.apply {
            PreRender()
            setEGLContextClientVersion(2);
            setRenderer(GetRenderer())
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
        mGodot.renderView = godotGLRenderViewLW
    }

    private fun NewRender(){
        view = godotGLRenderViewLW?.view as SurfaceHolder.Callback2
        view?.surfaceDestroyed(mSurfaceHolder!!)
        view?.surfaceCreated(mSurfaceHolder!!)

        godotGLRenderViewLW?.requestRender()
    }

    fun InitPlugins(){
        mGodot.renderView?.queueOnRenderThread {
            for (plugin in GodotPluginRegistry.getPluginRegistry().allPlugins) {
                plugin.onRegisterPluginWithGodotNative()
                Log.v(TAG, "PluginName:"+plugin.pluginName)
                if (plugin.pluginName=="LiveWallpaper") {
                    wpPlugin= plugin as LiveWallpaper?
                }
            }
        }
    }

    fun onTouchEvent(event: MotionEvent?) {
        if (event != null) {
            godotGLRenderViewLW?.onTouchEvent(event)
        }
    }

    fun Pause(){
        mGodot.onPause(this)
    }

    fun Resume(){
        NewRender()
        mGodot.onResume(this)
    }

    fun Destroy(){
        for (plugin in GodotPluginRegistry.getPluginRegistry().allPlugins) {
            plugin.onMainDestroy()
        }
        mGodot.runOnRenderThread {
            Pause()
            GodotLib.ondestroy()
            godotGLRenderViewLW?.preserveEGLContextOnPause=false
            godotGLRenderViewLW?.onPause()
        }
    }

    private fun Kill(){
        synchronized(lock) {
            Process.killProcess(Process.myPid())
            Runtime.getRuntime().exit(0)
        }
    }

    fun terminateGodotLiveWallpaperService() {
        Log.v(TAG, "Force quitting Godot instance")
        Kill()
    }


    override fun getActivity(): Activity {
        return proxyActivity
    }

    override fun getGodot(): Godot {
        return mGodot
    }

}
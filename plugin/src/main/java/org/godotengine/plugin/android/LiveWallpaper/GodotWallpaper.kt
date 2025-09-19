package org.godotengine.plugin.android.LiveWallpaper

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import org.godotengine.godot.Godot
import org.godotengine.godot.GodotHost
import org.godotengine.godot.gl.GLSurfaceView
import org.godotengine.godot.plugin.GodotPluginRegistry

fun Logwp(msg:String){
    Log.v("godotwp",msg)
}
class GodotWallpaper(private val context: Context) : GodotHost {

    private val mGodot: Godot by lazy { Godot.getInstance(context) }
    var godotGLRenderViewLW:GodotGLRenderViewWP?=null
    var wpPlugin:LiveWallpaper?=null
    private var mSurfaceHolder: SurfaceHolder?=null

    fun onCreate(){
        mGodot.onStart(this)
    }

    fun InitNativeEngine(){
        if (!mGodot.initEngine(this, emptyList())) {
            throw IllegalStateException("Unable to initialize engine native layer");
        }
    }

    fun SetSurfaceHolder(holder: SurfaceHolder){
        mSurfaceHolder=holder
    }

    fun InitRenderEngine(){
        godotGLRenderViewLW = object : GodotGLRenderViewWP(context,  mGodot) {
            override fun getHolder(): SurfaceHolder {
                return mSurfaceHolder!!
            }
        }.apply {
            PreRender()
            setRenderer(GetRenderer())
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
        mGodot.renderView = godotGLRenderViewLW
        InitPlugins()
    }

    fun SurfaceUpdated(){
        godotGLRenderViewLW?.surfaceDestroyed(mSurfaceHolder!!)
        godotGLRenderViewLW?.surfaceCreated(mSurfaceHolder!!)
    }

    fun InitPlugins(){
        mGodot.renderView?.queueOnRenderThread {
            for (plugin in GodotPluginRegistry.getPluginRegistry().allPlugins) {
                plugin.onRegisterPluginWithGodotNative()
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
//        godotGLRenderViewLW!!.GetRenderer().SyncRendState.signalSurfaceReady()
        mGodot.onResume(this)
    }

    fun Destroy(){
        mGodot.onDestroy(this)
    }


    override fun getActivity() = null

    override fun getGodot(): Godot {
        return mGodot
    }

}
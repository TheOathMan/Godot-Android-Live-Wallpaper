

package org.godotengine.plugin.android.LiveWallpaper

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import org.godotengine.godot.Godot
import org.godotengine.godot.GodotHost
import org.godotengine.godot.gl.GLSurfaceView
import org.godotengine.godot.plugin.GodotPluginRegistry
import org.godotengine.godot.xr.XRMode

class GodotWallpaper(context: Context) : GodotHost {

    private val TAG = "godot"
    private var mGodot:Godot = Godot(context)
    private var proxyActivity: ProxyActivity = ProxyActivity(context)
    var godotGLRenderViewLW:GodotGLRenderViewLW?=null
    var wpPlugin:LiveWallpaper?=null

    fun onCreate(){
        mGodot.onCreate(this)
    }

    fun InitNativeEngine(){
        if (!mGodot.onInitNativeLayer(this)) {
            throw IllegalStateException("Unable to initialize engine native layer");
        }
    }

    fun InitRenderEngine(surfaceHolder:SurfaceHolder,context: Context){
        godotGLRenderViewLW = object :
            GodotGLRenderViewLW(context, this, mGodot, XRMode.REGULAR) {
            override fun getHolder(): SurfaceHolder {
                return surfaceHolder
            }
        }.apply {
            PreRender()
            setEGLContextClientVersion(2);
            setRenderer(GetRenderer())
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
        mGodot.renderView = godotGLRenderViewLW
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
        mGodot.onResume(this)
    }

    override fun getActivity(): Activity {
        return proxyActivity
    }

    override fun getGodot(): Godot {
        return mGodot
    }


}
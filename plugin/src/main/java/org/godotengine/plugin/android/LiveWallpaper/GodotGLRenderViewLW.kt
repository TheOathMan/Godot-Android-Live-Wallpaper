/**************************************************************************/
/*  Godot.kt                                                              */
/**************************************************************************/
/*                         This file is part of:                          */
/*                             GODOT ENGINE                               */
/*                        https://godotengine.org                         */
/**************************************************************************/
/* Copyright (c) 2014-present Godot Engine contributors (see AUTHORS.md). */
/* Copyright (c) 2007-2014 Juan Linietsky, Ariel Manzur.                  */
/*                                                                        */
/* Permission is hereby granted, free of charge, to any person obtaining  */
/* a copy of this software and associated documentation files (the        */
/* "Software"), to deal in the Software without restriction, including    */
/* without limitation the rights to use, copy, modify, merge, publish,    */
/* distribute, sublicense, and/or sell copies of the Software, and to     */
/* permit persons to whom the Software is furnished to do so, subject to  */
/* the following conditions:                                              */
/*                                                                        */
/* The above copyright notice and this permission notice shall be         */
/* included in all copies or substantial portions of the Software.        */
/*                                                                        */
/* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,        */
/* EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF     */
/* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. */
/* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY   */
/* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,   */
/* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE      */
/* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.                 */
/**************************************************************************/

package org.godotengine.plugin.android.LiveWallpaper

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.text.TextUtils
import android.util.SparseArray
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.PointerIcon
import android.view.SurfaceView
import org.godotengine.godot.Godot
import org.godotengine.godot.GodotLib
import org.godotengine.godot.GodotRenderView
import org.godotengine.godot.input.GodotInputHandler
import org.godotengine.plugin.android.LiveWallpaper.gl.GLSurfaceViewWP
import org.godotengine.plugin.android.LiveWallpaper.gl.GodotRenderer

/**
 * A simple GLSurfaceView sub-class that demonstrate how to perform
 * OpenGL ES 2.0 rendering into a GL Surface. Note the following important
 * details:
 *
 * - The class must use a custom context factory to enable 2.0 rendering.
 * See ContextFactory class definition below.
 *
 * - The class must use a custom EGLConfigChooser to be able to select
 * an EGLConfig that supports 3.0. This is done by providing a config
 * specification to eglChooseConfig() that has the attribute
 * EGL10.ELG_RENDERABLE_TYPE containing the EGL_OPENGL_ES2_BIT flag
 * set. See ConfigChooser class definition below.
 *
 * - The class must select the surface's format, then choose an EGLConfig
 * that matches it exactly (with regards to red/green/blue/alpha channels
 * bit depths). Failure to do so would result in an EGL_BAD_MATCH error.
 */



@SuppressLint("ViewConstructor")
open class GodotGLRenderViewLW(
    private val context:Context,
    private val godot: Godot
) :
    GLSurfaceViewWP(context), GodotRenderView {

    private val inputHandler: GodotInputHandler = GodotInputHandler(context,godot)
    private val godotRenderer: GodotRenderer =
        GodotRenderer()
    private val customPointerIcons = SparseArray<PointerIcon>()

    override fun getView(): SurfaceView {
        return this
    }

//    override fun initInputDevices() {
//        inputHandler.initInputDevices()
//    }

    override fun queueOnRenderThread(event: Runnable) {
        queueEvent(event)
    }

    override fun onActivityPaused() {
        queueEvent {
            GodotLib.focusout()
            // Pause the renderer
            godotRenderer.onActivityPaused()
        }
    }

    override fun onActivityStopped() {
        pauseGLThread()
    }

    override fun onActivityStarted() {
        resumeGLThread()
    }

    override fun onActivityDestroyed() {
        requestRenderThreadExitAndWait();
    }

    override fun onActivityResumed() {
        queueEvent {

            // Resume the renderer
            godotRenderer.onActivityResumed()
            GodotLib.focusin()
        }
    }



//    override fun onBackPressed() {
//        godot.onBackPressed()
//    }

    override fun getInputHandler(): GodotInputHandler {
        return inputHandler
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        return inputHandler.onTouchEvent(event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return inputHandler.onKeyUp(keyCode, event) || super.onKeyUp(keyCode, event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return inputHandler.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        return inputHandler.onGenericMotionEvent(event) || super.onGenericMotionEvent(event)
    }

    override fun onCapturedPointerEvent(event: MotionEvent): Boolean {
        return inputHandler.onGenericMotionEvent(event)
    }

    override fun onPointerCaptureChange(hasCapture: Boolean) {
        super.onPointerCaptureChange(hasCapture)
        inputHandler.onPointerCaptureChange(hasCapture)
    }

    override fun requestPointerCapture() {
        if (canCapturePointer()) {
            super.requestPointerCapture()
            inputHandler.onPointerCaptureChange(true)
        }
    }

    override fun releasePointerCapture() {
        super.releasePointerCapture()
        inputHandler.onPointerCaptureChange(false)
    }

    /**
     * Used to configure the PointerIcon for the given type.
     *
     * Called from JNI
     */
    //@Keep
    override fun configurePointerIcon(
        pointerType: Int,
        imagePath: String,
        hotSpotX: Float,
        hotSpotY: Float
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                var bitmap: Bitmap? = null
                if (!TextUtils.isEmpty(imagePath)) {
                    if (godot.directoryAccessHandler.filesystemFileExists(imagePath)) {
                        // Try to load the bitmap from the file system
                        bitmap = BitmapFactory.decodeFile(imagePath)
                    } else if (godot.directoryAccessHandler.assetsFileExists(imagePath)) {
                        // Try to load the bitmap from the assets directory
                        val am = context.assets
                        val imageInputStream = am.open(imagePath)
                        bitmap = BitmapFactory.decodeStream(imageInputStream)
                    }
                }
                val customPointerIcon = PointerIcon.create(bitmap!!, hotSpotX, hotSpotY)
                customPointerIcons.put(pointerType, customPointerIcon)
            } catch (e: Exception) {
                // Reset the custom pointer icon
                customPointerIcons.delete(pointerType)
            }
        }
    }

    /**
     * called from JNI to change pointer icon
     */
    //@Keep
    override fun setPointerIcon(pointerType: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            var pointerIcon = customPointerIcons[pointerType]
            if (pointerIcon == null) {
                pointerIcon = PointerIcon.getSystemIcon(context, pointerType)
            }
            setPointerIcon(pointerIcon)
        }
    }

    override fun onResolvePointerIcon(me: MotionEvent, pointerIndex: Int): PointerIcon {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            pointerIcon
        } else super.onResolvePointerIcon(me, pointerIndex)
    }

    fun PreRender() {
        preserveEGLContextOnPause = true
        isFocusableInTouchMode = false

        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        setEGLContextClientVersion(2);
    }

    override fun startRenderer() {
        /* Set the renderer responsible for frame rendering */
        setRenderer(godotRenderer)
    }

    fun GetRenderer(): GodotRenderer {
        return godotRenderer
    }
}


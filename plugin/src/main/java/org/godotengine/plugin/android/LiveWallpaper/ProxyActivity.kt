package org.godotengine.plugin.android.LiveWallpaper

import android.app.Activity
import android.app.NotificationManager
import android.app.WallpaperManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.InputQueue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsAnimation
import android.view.WindowManager
import java.io.File

class ProxyActivity(private val serviceContext: Context) : Activity() {

    override fun getResources(): Resources {
        //displayContext.getRootWindowInsets
        return serviceContext.resources
    }
    override fun getWindow(): Window {
        return ProxyWindow(serviceContext)
    }

    override fun getContentResolver(): ContentResolver? {
        return serviceContext.contentResolver
    }

    override fun getPackageManager(): PackageManager {
        return serviceContext.packageManager
    }

    override fun getPackageName(): String {
        return serviceContext.packageName
    }

    override fun getApplicationContext(): Context {
        // Override getApplicationContext to return the serviceContext
        return serviceContext
    }

    override fun getBaseContext(): Context {
        // Override getBaseContext to return the serviceContext
        return serviceContext
    }

    override fun attachBaseContext(newBase: Context) {
        // Attach the new base context if needed
        super.attachBaseContext(serviceContext)
    }

    override fun getAssets(): AssetManager {
        return serviceContext.assets
    }

    override fun getFilesDir(): File {
        return serviceContext.filesDir
    }

    override fun getClassLoader(): ClassLoader {
        return serviceContext.classLoader
    }

    override fun getSystemService(name: String): Any {
        return serviceContext.getSystemService(name)
    }

    override fun sendBroadcast(intent: Intent?) {
        serviceContext.sendBroadcast(intent)
    }

    override fun getApplicationInfo(): ApplicationInfo {
        return serviceContext.applicationInfo
    }

    override fun checkCallingOrSelfPermission(permission: String): Int {
        return serviceContext.checkCallingOrSelfPermission(permission)
    }

    override fun isRestricted(): Boolean {
        return serviceContext.isRestricted
    }

    override fun getLocalClassName(): String {
        return serviceContext.toString()
    }

    override fun getSystemServiceName(serviceClass: Class<*>): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            serviceContext.getSystemServiceName(serviceClass)
        } else {
            when (serviceClass) {
                WallpaperManager::class.java -> Context.WALLPAPER_SERVICE
                WindowManager::class.java -> Context.WINDOW_SERVICE
                NotificationManager::class.java -> Context.NOTIFICATION_SERVICE
                else -> null // Not supported on this version
            }
        }
    }

    override fun getAttributionTag(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            serviceContext.attributionTag
        } else {
            Log.w("AttributionTag", "Not available on versions below Android 11")
            null
        }
    }

    override fun finishAndRemoveTask() {
        super.finishAndRemoveTask()
    }

    /*
    * HIDDEN FUNCTIONS. DON"T DELETE OR MODIFY
    */
    fun getUserID():Int{
        return 0
    }

    fun canLoadUnsafeResources(): Boolean {
        return true;
    }

    fun getDisplayId():Int{
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            serviceContext.display!!.displayId
        } else {
            Log.w("DisplayId", "Not supported on versions below Android 11")
            -1 // Or any other default value
        }
    }
}

class ProxyWindow(context: Context?) : Window(context){

    //TODO: maybe omit window flags function?
//    override fun addFlags(flags: Int) {
//        //No window flags
//    }
    override fun getDecorView(): View {
        return ProxyView(context)
    }
    override fun takeSurface(p0: SurfaceHolder.Callback2?) {
        TODO("Not yet needed")
    }

    override fun takeInputQueue(p0: InputQueue.Callback?) {
        TODO("Not yet needed")
    }

    override fun isFloating(): Boolean {
        TODO("Not yet needed")
    }

    override fun setContentView(p0: Int) {
        TODO("Not yet needed")
    }

    override fun setContentView(p0: View?) {
        TODO("Not yet needed")
    }

    override fun setContentView(p0: View?, p1: ViewGroup.LayoutParams?) {
        TODO("Not yet needed")
    }

    override fun addContentView(p0: View?, p1: ViewGroup.LayoutParams?) {
        TODO("Not yet needed")
    }

    override fun getCurrentFocus(): View? {
        TODO("Not yet needed")
    }

    override fun getLayoutInflater(): LayoutInflater {
        TODO("Not yet needed")
    }

    override fun setTitle(p0: CharSequence?) {
        TODO("Not yet needed")
    }

    override fun setTitleColor(p0: Int) {
        TODO("Not yet needed")
    }

    override fun openPanel(p0: Int, p1: KeyEvent?) {
        TODO("Not yet needed")
    }

    override fun closePanel(p0: Int) {
        TODO("Not yet needed")
    }

    override fun togglePanel(p0: Int, p1: KeyEvent?) {
        TODO("Not yet needed")
    }

    override fun invalidatePanelMenu(p0: Int) {
        TODO("Not yet needed")
    }

    override fun performPanelShortcut(p0: Int, p1: Int, p2: KeyEvent?, p3: Int): Boolean {
        TODO("Not yet needed")
    }

    override fun performPanelIdentifierAction(p0: Int, p1: Int, p2: Int): Boolean {
        TODO("Not yet needed")
    }

    override fun closeAllPanels() {
        TODO("Not yet needed")
    }

    override fun performContextMenuIdentifierAction(p0: Int, p1: Int): Boolean {
        TODO("Not yet needed")
    }

    override fun onConfigurationChanged(p0: Configuration?) {
        TODO("Not yet needed")
    }

    override fun setBackgroundDrawable(p0: Drawable?) {
        TODO("Not yet needed")
    }

    override fun setFeatureDrawableResource(p0: Int, p1: Int) {
        TODO("Not yet needed")
    }

    override fun setFeatureDrawableUri(p0: Int, p1: Uri?) {
        TODO("Not yet needed")
    }

    override fun setFeatureDrawable(p0: Int, p1: Drawable?) {
        TODO("Not yet needed")
    }

    override fun setFeatureDrawableAlpha(p0: Int, p1: Int) {
        TODO("Not yet needed")
    }

    override fun setFeatureInt(p0: Int, p1: Int) {
        TODO("Not yet needed")
    }

    override fun takeKeyEvents(p0: Boolean) {
        TODO("Not yet needed")
    }

    override fun superDispatchKeyEvent(p0: KeyEvent?): Boolean {
        TODO("Not yet needed")
    }

    override fun superDispatchKeyShortcutEvent(p0: KeyEvent?): Boolean {
        TODO("Not yet needed")
    }

    override fun superDispatchTouchEvent(p0: MotionEvent?): Boolean {
        TODO("Not yet needed")
    }

    override fun superDispatchTrackballEvent(p0: MotionEvent?): Boolean {
        TODO("Not yet needed")
    }

    override fun superDispatchGenericMotionEvent(p0: MotionEvent?): Boolean {
        TODO("Not yet needed")
    }

    override fun peekDecorView(): View {
        TODO("Not yet needed")
    }

    override fun saveHierarchyState(): Bundle {
        TODO("Not yet needed")
    }

    override fun restoreHierarchyState(p0: Bundle?) {
        TODO("Not yet needed")
    }

    override fun onActive() {
        TODO("Not yet needed")
    }

    override fun setChildDrawable(p0: Int, p1: Drawable?) {
        TODO("Not yet needed")
    }

    override fun setChildInt(p0: Int, p1: Int) {
        TODO("Not yet needed")
    }

    override fun isShortcutKey(p0: Int, p1: KeyEvent?): Boolean {
        TODO("Not yet needed")
    }

    override fun setVolumeControlStream(p0: Int) {
        TODO("Not yet needed")
    }

    override fun getVolumeControlStream(): Int {
        TODO("Not yet needed")
    }

    override fun getStatusBarColor(): Int {
        TODO("Not yet needed")
    }

    override fun setStatusBarColor(p0: Int) {
        TODO("Not yet needed")
    }

    override fun getNavigationBarColor(): Int {
        TODO("Not yet needed")
    }

    override fun setNavigationBarColor(p0: Int) {
        TODO("Not yet needed")
    }

    override fun setDecorCaptionShade(p0: Int) {
        TODO("Not yet needed")
    }

    override fun setResizingCaptionDrawable(p0: Drawable?) {
        TODO("Not yet needed")
    }
}

class ProxyView(private val serviceContext: Context) : View(serviceContext) {
    override fun getRootWindowInsets(): WindowInsets {
        return LiveWallpaperService.windowInsets!!
    }
}



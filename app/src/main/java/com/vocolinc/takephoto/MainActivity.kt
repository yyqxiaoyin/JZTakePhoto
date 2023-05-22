package com.vocolinc.takephoto

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.vocolinc.takephoto.app.TakePhoto
import com.vocolinc.takephoto.app.TakePhotoImpl
import com.vocolinc.takephoto.compress.CompressConfig
import com.vocolinc.takephoto.model.InvokeParam
import com.vocolinc.takephoto.model.TContextWrap
import com.vocolinc.takephoto.model.TResult
import com.vocolinc.takephoto.permission.InvokeListener
import com.vocolinc.takephoto.permission.PermissionManager
import com.vocolinc.takephoto.permission.TakePhotoInvocationHandler
import java.io.File

class MainActivity : AppCompatActivity(),TakePhoto.TakeResultListener, InvokeListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.btn_test).setOnClickListener {
            Log.d("yyqxiaoyin","点击了按钮")
            val cameraRes = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

            val writeRes = if (Build.VERSION.SDK_INT >= 33) {
                ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            } else{
                ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }

            if (!cameraRes){
                Log.d("yyqxiaoyin","没有相机权限")
            }
            if (!writeRes){
                Log.d("yyqxiaoyin","没有读写权限")
            }

            if (!cameraRes || !writeRes){
                val uri = Uri.fromParts("package", packageName, null)
                val myIntent = Intent(Intent.ACTION_VIEW, uri)
                myIntent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                startActivity(myIntent)
                return@setOnClickListener
            }

            takeNewPhoto()
        }
    }

    var takePhoto : TakePhoto? = null
    var action_type = 1
    var invokeParam : InvokeParam? = null

    fun getTakePhoto() {
        //获得TakePhoto实例
        if (takePhoto == null) {
            takePhoto = TakePhotoInvocationHandler.of(this).bind(TakePhotoImpl(this, this)) as TakePhoto
        }
        //设置压缩规则，最大500kb
        takePhoto?.onEnableCompress(CompressConfig.Builder().setMaxSize(50 * 1024).create(), true)
    }

    fun takeNewPhoto(){
        getTakePhoto()
        val file = File(filesDir, "48df8f41-c2e8-460e-8c18-c98a9ced7667.png"/*"temp.png"*/)
        val uri = Uri.fromFile(file)
        //takePhoto?.onPickFromCaptureWithCrop(uri,cropOptions)//Cannot load the image.
        action_type = 0
        takePhoto?.onPickFromCapture(uri)
    }

    override fun takeSuccess(result: TResult?) {
        Toast.makeText(this,"takeSuccess", Toast.LENGTH_SHORT).show();
    }

    override fun takeFail(result: TResult?, msg: String?) {
        Toast.makeText(this,msg, Toast.LENGTH_SHORT).show();
    }

    override fun takeCancel() {
        Toast.makeText(this,"takeCancel", Toast.LENGTH_SHORT).show();
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val type = PermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionManager.handlePermissionsResult(this, type, invokeParam, this)
    }

    override fun invoke(invokeParam: InvokeParam?): PermissionManager.TPermissionType {
        val type = PermissionManager.checkPermission(TContextWrap.of(this), invokeParam!!.method)
        if (PermissionManager.TPermissionType.WAIT == type) {
            this.invokeParam = invokeParam
        }
        return type
    }
}
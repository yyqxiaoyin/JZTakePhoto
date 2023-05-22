package com.vocolinc.takephoto.permission;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.vocolinc.takephoto.R;
import com.vocolinc.takephoto.app.TakePhoto;
import com.vocolinc.takephoto.model.InvokeParam;
import com.vocolinc.takephoto.model.TContextWrap;
import com.vocolinc.takephoto.uitl.TConstant;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Created by penn on 16/9/22.
 */
public class PermissionManager {
    public enum TPermission {
        STORAGE(Manifest.permission.WRITE_EXTERNAL_STORAGE), CAMERA(Manifest.permission.CAMERA),IMAGES(Manifest.permission.READ_MEDIA_IMAGES);
        String stringValue;

        TPermission(String stringValue) {
            this.stringValue = stringValue;
        }

        public String stringValue() {
            return stringValue;
        }
    }


    public enum TPermissionType {
        GRANTED("已授权"), DENIED("未授权"), WAIT("等待授权"), NOT_NEED("无需授权"), ONLY_CAMERA_DENIED("没有拍照权限"), ONLY_STORAGE_DENIED("没有读写SD卡权限");
        String stringValue;

        TPermissionType(String stringValue) {
            this.stringValue = stringValue;
        }

        public String stringValue() {
            return stringValue;
        }
    }


    private final static String[] methodNames =
        {"onPickFromCapture", "onPickFromCaptureWithCrop", "onPickMultiple", "onPickMultipleWithCrop", "onPickFromDocuments",
            "onPickFromDocumentsWithCrop", "onPickFromGallery", "onPickFromGalleryWithCrop", "onCrop"};

    /**
     * 检查当前应用是否被授予相应权限
     *
     * @param contextWrap
     * @param method
     * @return
     */
    public static TPermissionType checkPermission(@NonNull TContextWrap contextWrap, @NonNull Method method) {
        String methodName = method.getName();
        boolean contain = false;
        for (int i = 0, j = methodNames.length; i < j; i++) {
            if (TextUtils.equals(methodName, methodNames[i])) {
                contain = true;
                break;
            }
        }
        if (!contain) {
            return TPermissionType.NOT_NEED;
        }

        boolean cameraGranted = true;
        boolean storageGranted = false;
        if (Build.VERSION.SDK_INT >= 33) {
            storageGranted = ContextCompat.checkSelfPermission(contextWrap.getActivity(),TPermission.IMAGES.stringValue) == PackageManager.PERMISSION_GRANTED;

        }else {
            storageGranted = ContextCompat.checkSelfPermission(contextWrap.getActivity(), TPermission.STORAGE.stringValue())
                    == PackageManager.PERMISSION_GRANTED;
        }

        if (TextUtils.equals(methodName, "onPickFromCapture") || TextUtils.equals(methodName, "onPickFromCaptureWithCrop")) {
            cameraGranted = ContextCompat.checkSelfPermission(contextWrap.getActivity(), TPermission.CAMERA.stringValue())
                    == PackageManager.PERMISSION_GRANTED;
        }

        boolean granted = storageGranted && cameraGranted;
        if (!granted) {
            ArrayList<String> permissions = new ArrayList<>();
            if (!storageGranted) {
                permissions.add(TPermission.STORAGE.stringValue());
            }
            if (!cameraGranted) {
                permissions.add(TPermission.CAMERA.stringValue());
            }
            requestPermission(contextWrap, permissions.toArray(new String[permissions.size()]));
        }
        return granted ? TPermissionType.GRANTED : TPermissionType.WAIT;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static void requestPermission(@NonNull TContextWrap contextWrap, @NonNull String[] permissions) {
        if (contextWrap.getFragment() != null) {
            contextWrap.getFragment().requestPermissions(permissions,TConstant.PERMISSION_REQUEST_TAKE_PHOTO);

        } else {
            ActivityCompat.requestPermissions(contextWrap.getActivity(), permissions, TConstant.PERMISSION_REQUEST_TAKE_PHOTO);
        }
    }

    public static TPermissionType onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == TConstant.PERMISSION_REQUEST_TAKE_PHOTO) {
            boolean cameraGranted = true, storageGranted = true;
            for (int i = 0, j = permissions.length; i < j; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    if (TextUtils.equals(TPermission.STORAGE.stringValue(), permissions[i])) {
                        storageGranted = false;
                    } else if (TextUtils.equals(TPermission.CAMERA.stringValue(), permissions[i])) {
                        cameraGranted = false;
                    }
                }
            }
            if (cameraGranted && storageGranted) {
                return TPermissionType.GRANTED;
            }
            if (!cameraGranted && storageGranted) {
                return TPermissionType.ONLY_CAMERA_DENIED;
            }
            if (!storageGranted && cameraGranted) {
                return TPermissionType.ONLY_STORAGE_DENIED;
            }
            if (!storageGranted && !cameraGranted) {
                return TPermissionType.DENIED;
            }
        }
        return TPermissionType.WAIT;
    }

    public static void handlePermissionsResult(Activity activity, TPermissionType type, InvokeParam invokeParam,
        TakePhoto.TakeResultListener listener) {
        String tip = null;
        switch (type) {
            case DENIED:
                listener.takeFail(null, tip = activity.getResources().getString(R.string.tip_permission_camera_storage));
                break;
            case ONLY_CAMERA_DENIED:
                listener.takeFail(null, tip = activity.getResources().getString(R.string.tip_permission_camera));
                break;
            case ONLY_STORAGE_DENIED:
                listener.takeFail(null, tip = activity.getResources().getString(R.string.tip_permission_storage));
                break;
            case GRANTED:
                try {
                    invokeParam.getMethod().invoke(invokeParam.getProxy(), invokeParam.getArgs());
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.takeFail(null, tip = activity.getResources().getString(R.string.tip_permission_camera_storage));
                }
                break;
            default:
                break;
        }
        if (tip != null) {
            Toast.makeText(activity, tip, Toast.LENGTH_LONG).show();
        }

    }
}

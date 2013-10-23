package org.sipdroid.sipua.ui;

import android.annotation.TargetApi;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.GINGERBREAD) 
public class VideoCameraNew_SDK9 {
	static Camera open() {
		return Camera.open(CameraInfo.CAMERA_FACING_FRONT);
	}
}

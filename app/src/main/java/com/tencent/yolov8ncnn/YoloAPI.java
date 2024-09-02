package com.tencent.yolov8ncnn;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

public class YoloAPI {
    public native boolean Init(AssetManager mgr, int modelid,int cpugpu);

    public class Obj {
        public float x;
        public float y;
        public float w;
        public float h;
        public int label;
        public float prob;
    }

    public native Obj[] Detect(Bitmap bitmap, boolean use_gpu);

    static {
        try {
            System.loadLibrary("yolov8ncnn");
            Log.e("YoloAPI", "加载yolov8ncnn成功");
        } catch (UnsatisfiedLinkError e) {
            Log.e("YoloAPI", "Failed to load yolov8ncnn library", e);
        }
    }

}

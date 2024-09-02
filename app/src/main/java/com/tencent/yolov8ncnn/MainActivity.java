
package com.tencent.yolov8ncnn;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class MainActivity extends Activity {
    private static final int PERMISSION_REQUEST_CODE = 0;
    private static final int OPEN_GALLERY_REQUEST_CODE = 1;
    private static final int TAKE_PHOTO_REQUEST_CODE = 2;

    private String[]class_name=new String[]{
            "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat", "traffic light",
            "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep", "cow",
            "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee",
            "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove", "skateboard", "surfboard",
            "tennis racket", "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
            "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
            "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote", "keyboard", "cell phone",
            "microwave", "oven", "toaster", "sink", "refrigerator", "book", "clock", "vase", "scissors", "teddy bear",
            "hair drier", "toothbrush"
    };
    private Button btn_select_image;
    private Button btn_recognize;
    private TextView tv_result;
    private YoloAPI yoloAPI;
    private Bitmap bitmap;
    private ImageView imageView;

    @SuppressLint("MissingInflatedId")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        yoloAPI = new YoloAPI();
        /*gpu选择1,cpu选择0*/
        boolean init = yoloAPI.Init(getAssets(), 1, 1);
        if (init) Toast.makeText(this, "模型加载成功", Toast.LENGTH_SHORT).show();
        else Toast.makeText(this, "模型加载失败", Toast.LENGTH_SHORT).show();

        btn_select_image = (Button) findViewById(R.id.btn_select_image);
        btn_recognize = (Button) findViewById(R.id.btn_recognize);
        tv_result = (TextView) findViewById(R.id.tv_result);
        imageView = (ImageView) findViewById(R.id.imageView);


        btn_select_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                applyPermission();
            }
        });
        btn_recognize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (init && bitmap != null) {
                    tv_result.setText("result:");
                    YoloAPI.Obj[] detect = yoloAPI.Detect(bitmap, true);
                    if (detect != null && detect.length!=0) {
                        String res = tv_result.getText().toString();
                        res += class_name[detect[0].label] + " " + detect[0].prob;
                        tv_result.setText(res);
                    } else {
                        String res = tv_result.getText().toString() + "无结果";
                        tv_result.setText(res);
                    }
                } else if (bitmap == null)
                    Toast.makeText(getApplicationContext(), "请选择图片", Toast.LENGTH_SHORT).show();
                else if (!init)
                    Toast.makeText(getApplicationContext(), "模型加载失败", Toast.LENGTH_SHORT).show();
            }
        });

    }

    /**
     * 申请动态权限
     */
    private void applyPermission() {
        //检测权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // 如果没有权限，则申请需要的权限
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        } else {
            openGallery();
        }
    }

    /**
     * 用户选择是否开启权限操作后的回调；TODO 同意/拒绝
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户同样授权
                openGallery();
            } else {
                // 用户拒绝授权
                Toast.makeText(this, "你拒绝使用存储权限！", Toast.LENGTH_SHORT).show();
                Log.d("HL", "你拒绝使用存储权限！");
            }
        }

    }

    /**
     * 打开相册
     */
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intent, OPEN_GALLERY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OPEN_GALLERY_REQUEST_CODE) { // 检测请求码
            if (resultCode == Activity.RESULT_OK && data != null) {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(data.getData());
                    Bitmap bitmap2 = BitmapFactory.decodeStream(inputStream);
                    // TODO 把获取到的图片放到ImageView上
                    imageView.setImageBitmap(bitmap2);
                    bitmap = bitmap2;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}

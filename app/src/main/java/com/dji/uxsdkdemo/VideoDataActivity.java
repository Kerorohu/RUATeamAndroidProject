package com.dji.uxsdkdemo;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import dji.common.airlink.PhysicalSource;
import dji.midware.usb.P3.UsbAccessoryService;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.sdkmanager.DJISDKManager;

import org.opencv.aruco.*;
import org.opencv.core.MatOfInt;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class VideoDataActivity extends AppCompatActivity {
    static {
        System.loadLibrary("native-lib");
    }

    public native double[] exeAruco(Bitmap input, int width, int height);

    public native String stringFromJNI();

    private static final String TAG = "YDSDemo::Aruco";
    private TextView editText;
    private DJICodecManager codecManager = null;
    private ImageView imageView;
    Bitmap bitmap;
    private VideoFeeder.VideoDataListener videoDataListener = null;
    private AtomicLong lastReceivedFrameTime = new AtomicLong(0);
    private VideoFeeder.PhysicalSourceListener sourceListener;
    private VideoFeedView primaryVideoFeed;
    private TextView textView;
    private boolean isPrimaryVideoFeed;
    private Button button;
    String str = new String();
    private Mat resMat, src;
    boolean resT;
    int width, height;

    private void showToast(final String toastMsg) {

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("opencv", "OpenCV loaded successfully");
                    break;
                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_data);
        //editText = findViewById(R.id.test);
        button = findViewById(R.id.button3);
        imageView = findViewById(R.id.imageView);
        primaryVideoFeed = (VideoFeedView) findViewById(R.id.primary_video_feed);
        textView = (TextView) findViewById(R.id.textView);

        if (primaryVideoFeed != null) {
            primaryVideoFeed.changeSourceResetKeyFrame();
        }

        sourceListener = new VideoFeeder.PhysicalSourceListener() {
            @Override
            public void onChange(VideoFeeder.VideoFeed videoFeed, PhysicalSource newPhysicalSource) {
                if (videoFeed == VideoFeeder.getInstance().getPrimaryVideoFeed()) {
                    String newText = "Primary Source: " + newPhysicalSource.toString();
                    //Toast.makeText(getApplicationContext(),newText,Toast.LENGTH_SHORT).show();
                }
                if (videoFeed == VideoFeeder.getInstance().getSecondaryVideoFeed()) {
                    //Toast.makeText(getApplicationContext(),"balabalabala",Toast.LENGTH_SHORT).show();
                }
            }
        };

        if (VideoFeeder.getInstance() == null)
            Toast.makeText(getApplicationContext(), "no instance", Toast.LENGTH_SHORT).show();
        final BaseProduct product = DJISDKManager.getInstance().getProduct();
        if (product != null) {
            VideoFeeder.VideoDataListener primaryVideoDataListener =
                    primaryVideoFeed.registerLiveVideo(VideoFeeder.getInstance().getPrimaryVideoFeed(), true);
            VideoFeeder.getInstance().addPhysicalSourceListener(sourceListener);
        }


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bitmap = primaryVideoFeed.getBitmap();
//                ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                width = bitmap.getWidth();
//                height = bitmap.getHeight();
//                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
//                byte[] datas = baos.toByteArray();

                //detect marker
                double[] res = exeAruco(bitmap, width, height);

//                BitmapFactory.Options options = new BitmapFactory.Options();
//                options.inSampleSize = 2;

                //Bitmap resb = BitmapFactory.decodeByteArray(res, 0, res.length);

                if (res.length == 0) {
                    textView.setText("no detect!");
                } else {
                    //showToast(Integer.toString(res.length));
                    textView.setText("tvecs: ");
                    //textView.append("\n");
                    for (int i = 0; i < res.length; i++) {
                        textView.append(Double.toString(res[i]));
                        textView.append("\n");
                    }
                }
                //Utils.bitmapToMat(bitmap, src);
                //resT = exeAruco(src.getNativeObjAddr(),resMat.getNativeObjAddr());
                //imageView.setImageBitmap(resb);
                //              gray.release();
                //              src.release();
            }
        });


//        byte[] rgbaData = codecManager.getRgbaData(codecManager.getVideoWidth(),codecManager.getVideoHeight());
//
//
////        byte[] bytes = getIntent().getByteArrayExtra("data");
//
//        str = Base64.getEncoder().encodeToString(rgbaData);
//        editText.setText(str);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


}
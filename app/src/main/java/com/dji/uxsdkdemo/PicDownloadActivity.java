package com.dji.uxsdkdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.sdk.media.FetchMediaTask;
import dji.sdk.media.FetchMediaTaskContent;
import dji.sdk.media.FetchMediaTaskScheduler;
import dji.sdk.media.MediaFile;
import dji.sdk.media.MediaManager;

public class PicDownloadActivity extends AppCompatActivity {

    private MediaFile media;
    private MediaManager mediaManager;
    private FetchMediaTaskScheduler taskScheduler;
    private FetchMediaTask.Callback fetchMediaFileTaskCallback;
    private TableLayout tableLayout;
    private List<MediaFile> nowPic;
    private ScrollView scrollView;
    private Button cancel, confirm;
    private TableRow[] childsRow;
    private RadioButton[] childsRadio;
    private TextView[] childsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //UI初始化
        setContentView(R.layout.activity_pic_download);
        scrollView = (ScrollView) findViewById(R.id.id_scrollView);
        scrollView.setVerticalScrollBarEnabled(false);
        tableLayout = findViewById(R.id.picdownload);
        cancel = (Button) findViewById(R.id.cancel);
        confirm = (Button) findViewById(R.id.confirm);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PicDownloadActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        setUpListener();
        if (ModuleVerificationUtil.isCameraModuleAvailable()) {
            if (ModuleVerificationUtil.isMediaManagerAvailable()) {
                if (mediaManager == null) {
                    mediaManager = MApplication.getProductInstance().getCamera().getMediaManager();
                }

                if (taskScheduler == null) {
                    taskScheduler = mediaManager.getScheduler();
                    if (taskScheduler != null && taskScheduler.getState() == FetchMediaTaskScheduler.FetchMediaTaskSchedulerState.SUSPENDED) {
                        taskScheduler.resume(new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {

                                if (djiError != null) {
                                    ToastUtils.setResultToToast("taskScheduler resume failed: " + djiError.getDescription());
                                }

                            }
                        });
                    }
                }

                MApplication.getProductInstance()
                        .getCamera()
                        .setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD,
                                new CommonCallbacks.CompletionCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {
                                        if (null == djiError) {
                                            fetchMediaList();
                                        }
                                    }
                                });
                if (nowPic.size() > 0) {
                    for (int i = 0; i < nowPic.size(); i++) {
                        //addview(nowPic.get(i).getFileName());
                    }
                } else {
                    ToastUtils.showToast("No pic!");
                }
            } else {
                ToastUtils.showToast(String.valueOf(R.string.not_support_mediadownload));
            }
        }

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TO-DO
                childsRow = new TableRow[tableLayout.getChildCount()];
                childsText = new TextView[tableLayout.getChildCount()];
                childsRadio = new RadioButton[tableLayout.getChildCount()];

                Set<Integer> count = new HashSet<>();

                for (int i = 0; i < childsRow.length; i++) {
                    childsRow[i] = (TableRow) tableLayout.getChildAt(i);
                    childsText[i] = (TextView) childsRow[i].getChildAt(0);
                    childsRadio[i] = (RadioButton) childsRow[i].getChildAt(1);
                    if (childsRadio[i].isChecked()) {
                        count.add(i);
                    }
                }
                Iterator<Integer> it = count.iterator();
                MediaFile mediaFile;
                while (it.hasNext()) {
                    mediaFile = nowPic.get((Integer) it.next().intValue());
                    if (ModuleVerificationUtil.isCameraModuleAvailable()
                            && mediaFile != null
                            && mediaManager != null) {
                        File destDir = new File(Environment.getExternalStorageDirectory().
                                getPath() + "/Dji_Pic/");
                        if (!destDir.exists())
                            destDir.mkdir();
                        mediaFile.fetchFileData(destDir, mediaFile.getFileName(), new DownloadHandler<String>());
                    }
                }
            }
        });

    }


    private void setUpListener() {
        // Example of Listener
        fetchMediaFileTaskCallback = new FetchMediaTask.Callback() {
            @Override
            public void onUpdate(MediaFile mediaFile, FetchMediaTaskContent fetchMediaTaskContent, DJIError djiError) {

                if (djiError == null) {
                    Bitmap bitmap = null;
                    if (FetchMediaTaskContent.PREVIEW == fetchMediaTaskContent) {
                        bitmap = mediaFile.getPreview();
                    }
                    if (FetchMediaTaskContent.THUMBNAIL == fetchMediaTaskContent) {
                        bitmap = mediaFile.getThumbnail();
                    }
                } else {
                    ToastUtils.setResultToToast("fetch media failed: " + djiError.getDescription());
                }
            }
        };
    }

    private void fetchMediaList() {
        if (ModuleVerificationUtil.isMediaManagerAvailable()) {
            if (mediaManager != null) {
                mediaManager.refreshFileListOfStorageLocation(SettingsDefinitions.StorageLocation.SDCARD, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        String str;
                        if (null == djiError) {
                            List<MediaFile> djiMedias = mediaManager.getSDCardFileListSnapshot();
                            nowPic = djiMedias;
                            if (null != djiMedias) {
                                if (!djiMedias.isEmpty()) {
                                    str = "fetch list success";
                                    ToastUtils.setResultToToast(str);
                                } else {
                                    str = "No Media in SD Card";
                                    ToastUtils.setResultToToast(str);
                                }
                            }
                        } else {
                            ToastUtils.setResultToToast(djiError.getDescription());
                        }
                    }
                });
            }
        }
    }

    private void addview(String str) {
        TableRow tableRow = new TableRow(this);
        TextView textView = new TextView(this);
        RadioButton radioButton = new RadioButton(this);

        textView.setText(str);

        tableRow.addView(textView);
        tableRow.addView(radioButton);

        tableLayout.addView(tableRow);
    }

}
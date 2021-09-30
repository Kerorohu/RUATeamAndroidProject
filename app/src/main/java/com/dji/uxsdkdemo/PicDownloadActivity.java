package com.dji.uxsdkdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

    private MediaManager mediaManager;
    private FetchMediaTaskScheduler taskScheduler;
    private FetchMediaTask.Callback fetchMediaFileTaskCallback;
    private List<MediaFile> nowPic;
    private Button cancel, confirm;
    private List<String> nowPicName;
    private ListView listView;
    private MediaAdapter mediaAdapter;
    private SparseBooleanArray stateCheckedMap = new SparseBooleanArray();
    //    private List<String> mCheckedData = new ArrayList<>();
    private List<Integer> count = new ArrayList<>();
    private LinearLayout mLlEditBar;
//    private Map<String,MediaFile> mediaFileMap = new HashMap<>();

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

        nowPic = new ArrayList<MediaFile>();
        nowPicName = new ArrayList<>();

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
            } else {
                ToastUtils.showToast(String.valueOf(R.string.not_support_mediadownload));
            }
        }

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TO-DO
                if (!count.isEmpty()) {
                    //ToastUtils.showToast("onclick sucess");
                    Iterator<Integer> it = count.iterator();
                    MediaFile mediaFile;
                    while (it.hasNext()) {
                        mediaFile = nowPic.get(it.next());
                        if (ModuleVerificationUtil.isCameraModuleAvailable()
                                && mediaFile != null
                                && mediaManager != null) {
                            String dir = getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/downloadMedia/";
                            File destDir = new File(dir);
                            if (!destDir.exists()) {
                                destDir.mkdir();
                            }
                            //ToastUtils.showToast(mediaFile.getFileName());
                            mediaFile.fetchFileData(destDir, mediaFile.getFileName().substring(0, mediaFile.getFileName().indexOf(".")), new DownloadHandler<String>());
                        }
                    }
                } else {
                    Log.d("download", "data empty");
                }
            }
        });

        setOnListViewItemClickListener();

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
                            nowPicName.clear();
                            if (!nowPic.isEmpty()) {
                                str = "fetch list success";
                                Log.d("download", str);
                                for (int i = 0; i < nowPic.size(); i++) {
                                    int finalI = i;
                                    nowPicName.add(nowPic.get(finalI).getFileName());
//                                    mediaFileMap.put(nowPic.get(finalI).getFileName(),nowPic.get(finalI));
                                }
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mediaAdapter = new MediaAdapter(nowPicName, PicDownloadActivity.this, stateCheckedMap);
                                        listView.setAdapter(mediaAdapter);
                                        //setOnListViewItemLongClickListener();
                                    }
                                });
                            } else {
                                str = "No Media in SD Card";
                                ToastUtils.setResultToToast(str);
                            }
                        } else {
                            ToastUtils.setResultToToast(djiError.getDescription());
                        }
                    }
                });
            }
        }
    }

    private void setStateCheckedMap(boolean isSelectedAll) {
        for (int i = 0; i < nowPicName.size(); i++) {
            stateCheckedMap.put(i, isSelectedAll);
            listView.setItemChecked(i, isSelectedAll);
        }
    }

    private void setOnListViewItemClickListener() {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                updateCheckBoxStatus(view, position);
            }
        });
    }

    /**
     * 如果返回false那么click仍然会被调用,,先调用Long click，然后调用click。
     * 如果返回true那么click就会被吃掉，click就不会再被调用了
     * 在这里click即setOnItemClickListener
     */
//    private void setOnListViewItemLongClickListener() {
//        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
//            @Override
//            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//                mLlEditBar.setVisibility(View.VISIBLE);//显示下方布局
//                mediaAdapter.setShowCheckBox(true);//CheckBox的那个方框显示
//                updateCheckBoxStatus(view, position);
//                return true;
//            }
//        });
//    }
    private void updateCheckBoxStatus(View view, int position) {
        MediaAdapter.ViewHolder holder = (MediaAdapter.ViewHolder) view.getTag();
        holder.checkBox.toggle();//反转CheckBox的选中状态
        listView.setItemChecked(position, holder.checkBox.isChecked());//长按ListView时选中按的那一项
        stateCheckedMap.put(position, holder.checkBox.isChecked());//存放CheckBox的选中状态
        if (holder.checkBox.isChecked()) {
//            mCheckedData.add(nowPicName.get(position));//CheckBox选中时，把这一项的数据加到选中数据列表
            count.add(position);
        } else {
//            mCheckedData.remove(nowPicName.get(position));//CheckBox未选中时，把这一项的数据从选中数据列表移除
            count.remove(position);
        }
        mediaAdapter.notifyDataSetChanged();
    }


//    private void addview(String str) {
//        TableRow tableRow = new TableRow(this);
//        TextView textView = new TextView(this);
//        RadioButton radioButton = new RadioButton(this);
//
//        textView.setText(str);
//        textView.setGravity(Gravity.CENTER);
//
//        radioButton.setGravity(Gravity.CENTER);
//
//        tableRow.addView(textView);
//        tableRow.addView(radioButton);
//
//
//        //tableLayout.addView(tableRow);
//    }

}
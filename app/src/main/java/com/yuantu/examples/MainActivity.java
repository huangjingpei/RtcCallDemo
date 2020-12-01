package com.yuantu.examples;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.tencent.bugly.crashreport.CrashReport;
import com.yuantu.call.CallStatus;
import com.yuantu.call.RTCEngine;
import com.yuantu.call.RTCEngineEventHandler;
import com.yuantu.call.RTCEngineFactory;
import com.yuantu.call.RTCEngineContext;
import com.yuantu.call.RTCViewRenderer;


import org.webrtc.EglBase;
import org.webrtc.RendererCommon;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class MainActivity extends AppCompatActivity implements View.OnClickListener ,
        CompoundButton.OnCheckedChangeListener,
        RTCEngineEventHandler {
    public static final String TAG = "MainActivity";
    private static final String[] MANDATORY_PERMISSIONS = {"android.permission.CAMERA",
            "android.permission.RECORD_AUDIO", "android.permission.WRITE_EXTERNAL_STORAGE"};
    private boolean isSdkInit = false;
    private boolean isStartPreview = false;
    private boolean isSwitchCamera = false;
    private boolean isStartCall = false;
    private String peerName;

    private TextView userEdit;

    private Button sdkInitBtn;
    private Button startorstopPreviewBtn;
    private Button startorstopCallBtn;
    private Button switchCameraBtn;
    private Button startorstopPushBtn;

    private CheckBox videoCallDisabledCheckBox;
    private CheckBox useOpenSLESCheckBox;
    private CheckBox disableBuiltInAECCheckBox;
    private CheckBox disableBuiltInAGCCheckBox;
    private CheckBox disableBuiltInNSCheckBox;
    private CheckBox useLegacyAudioDeviceCheckBox;


    private LinearLayout remoteFeedContainer;
    private LinearLayout remoteHoriFeedContainer;
    private LinearLayout feedContainer;
    private RelativeLayout localSurfaceLayout;
    private RTCViewRenderer localRenderer;
    private RTCViewRenderer remoteRenderer;

    private RTCEngineContext rtcEngineCtx;
    private RTCEngine rtcEngine;

    private EglBase eglBase;

    private LayoutInflater inflater;
    private boolean doRepeatTest = false;

    private AlertDialog incomingDialog;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private ConcurrentHashMap<String, ViewGroup> feedWindows = new ConcurrentHashMap<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        sharedPreferences = getSharedPreferences("data",Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        boolean value = sharedPreferences.getBoolean("videoCallDisabled",false);
        RTCEngineContext.videoCallEnabled = !value;
        value = sharedPreferences.getBoolean("useOpenSLES", false);
        RTCEngineContext.useOpenSLES = value;
        value = sharedPreferences.getBoolean("useLegacyAudioDevice",false);
        RTCEngineContext.useLegacyAudioDevice = value;
        value = sharedPreferences.getBoolean("disableBuiltInAEC",false);
        RTCEngineContext.disableBuiltInAEC = value;
        value = sharedPreferences.getBoolean("disableBuiltInAGC",false);
        RTCEngineContext.disableBuiltInAGC = value;
        value = sharedPreferences.getBoolean("disableBuiltInNS",false);
        RTCEngineContext.disableBuiltInNS = value;


        initRes();

        CrashReport.initCrashReport(this, "2639476540", true);
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        editor.putString("useId", userEdit.getText().toString());
        editor.commit();
        super.onDestroy();
        Log.e(TAG, "onDestroy Leave.");
        onHangup();
        if (isSdkInit) {
            RTCEngineFactory.destoryEngine();
            isSdkInit = false;
        }
    }


    private void initRes() {
        inflater = LayoutInflater.from(this);

        userEdit = (TextView) findViewById(R.id.userid);
        String useId = sharedPreferences.getString("useId", "0918");
        userEdit.setText(useId);
        sdkInitBtn = (Button) findViewById(R.id.SDKInit);
        sdkInitBtn.setOnClickListener(this);

        startorstopPreviewBtn = (Button) findViewById(R.id.startPrevieworstopPreview);
        startorstopPreviewBtn.setOnClickListener(this);

        startorstopCallBtn = (Button) findViewById(R.id.startCallorStopCall);
        startorstopCallBtn.setOnClickListener(this);

        switchCameraBtn = (Button) findViewById(R.id.switchCamera);
        switchCameraBtn.setOnClickListener(this);

        videoCallDisabledCheckBox = (CheckBox) findViewById(R.id.videoCallDisabled);
        videoCallDisabledCheckBox.setOnCheckedChangeListener(this);
        videoCallDisabledCheckBox.setChecked(!RTCEngineContext.videoCallEnabled);


        useOpenSLESCheckBox = (CheckBox) findViewById(R.id.useOpenSLES);
        useOpenSLESCheckBox.setOnCheckedChangeListener(this);
        if(RTCEngineContext.useOpenSLES) {
            useOpenSLESCheckBox.setChecked(true);
        }

        disableBuiltInAECCheckBox = (CheckBox) findViewById(R.id.disableBuiltInAEC);
        disableBuiltInAECCheckBox.setOnCheckedChangeListener(this);
        if (RTCEngineContext.disableBuiltInAEC) {
            disableBuiltInAECCheckBox.setChecked(true);
        }

        disableBuiltInAGCCheckBox = (CheckBox) findViewById(R.id.disableBuiltInAGC);
        disableBuiltInAGCCheckBox.setOnCheckedChangeListener(this);
        if (RTCEngineContext.disableBuiltInAGC) {
            disableBuiltInAGCCheckBox.setChecked(true);
        }

        disableBuiltInNSCheckBox = (CheckBox) findViewById(R.id.disableBuiltInNS);
        disableBuiltInNSCheckBox.setOnCheckedChangeListener(this);
        if (RTCEngineContext.disableBuiltInNS) {
            disableBuiltInNSCheckBox.setChecked(true);
        }

        useLegacyAudioDeviceCheckBox = (CheckBox) findViewById(R.id.useLegacyAudioDevice);
        useLegacyAudioDeviceCheckBox.setOnCheckedChangeListener(this);
        if (RTCEngineContext.useLegacyAudioDevice) {
            useLegacyAudioDeviceCheckBox.setChecked(true);
        }

        localSurfaceLayout = (RelativeLayout)findViewById(R.id.rtc_local_surfaceview);
        remoteFeedContainer = (LinearLayout)findViewById(R.id.rtc_remote_feeds_container);
        remoteHoriFeedContainer = (LinearLayout)findViewById(R.id.rtc_hori_remote_feeds_container);
        feedContainer = remoteFeedContainer;
    }

    private void initEngineCtx() {
        eglBase = EglBase.create(null /* sharedContext */, EglBase.CONFIG_PLAIN);
        rtcEngineCtx = new RTCEngineContext(this.getApplicationContext(),
                eglBase,
                userEdit.getText().toString(),
                "",
                0,
                0);
    }

    @Override
    public void onClick(View view) {


        if (view.getId() == R.id.SDKInit) {
            if (!isSdkInit &&!isStartPreview ){
                initEngineCtx();
                requestDangerousPermissions(MANDATORY_PERMISSIONS, 1);

                rtcEngine = RTCEngineFactory.createEngine(rtcEngineCtx, this);
                sdkInitBtn.setText("注销");
                //sdkInitBtn.setEnabled(false);
                isSdkInit = true;
                userEdit.setEnabled(false);
            }else{
                if(isSdkInit && !isStartPreview ){
                	RTCEngineFactory.destoryEngine();
                    //mInfoReport.unregisterInfoObserver(this);
                    sdkInitBtn.setText("注册");
                    userEdit.setEnabled(true);
                    isSdkInit=false;
                }
            }
        }

        if (view.getId() == R.id.startPrevieworstopPreview) {
            if (!isStartPreview && isSdkInit){
                if (RTCEngineContext.videoCallEnabled) {
                    WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
                    Point point = new Point();
                    wm.getDefaultDisplay().getSize(point);
                    wm.getDefaultDisplay().getSize(point);
                    int displayWidth = point.x;
                    int displayHeight = point.y;
                    RTCViewRenderer surfacePreview = new RTCViewRenderer(this);
                    surfacePreview.setKeepScreenOn(true);
                    surfacePreview.setZOrderMediaOverlay(true);
                    surfacePreview.setZOrderOnTop(false);
                    surfacePreview.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
                    surfacePreview.init(eglBase.getEglBaseContext(), null);
                    rtcEngine.startPreview(160, 120, 30, surfacePreview);

                    localRenderer = surfacePreview;
                    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(displayWidth, displayHeight);
                    surfacePreview.setLayoutParams(layoutParams);
                    localSurfaceLayout.setVisibility(View.VISIBLE);
                    localSurfaceLayout.addView(surfacePreview);
                }
                startorstopPreviewBtn.setText("停止预览");
                isStartPreview=true;
            }else{
                if(isSdkInit){
                    if (RTCEngineContext.videoCallEnabled) {
                        rtcEngine.stopPreview();
                        localSurfaceLayout.removeView(localRenderer);
                        localRenderer.release();
                        if (localRenderer != null) {
                            localRenderer.release();
                        }
                    }

                    startorstopPreviewBtn.setText("开始预览");
                    isStartPreview = false;

                }
            }
        }

        if (view.getId() == R.id.startCallorStopCall) {
            if (doRepeatTest) {
                if(!isStartCall) {
                    peerName = "123";
                    remoteRenderer = new RTCViewRenderer(MainActivity.this);
                    remoteRenderer.setKeepScreenOn(true);
                    remoteRenderer.setZOrderMediaOverlay(true);
                    remoteRenderer.setZOrderOnTop(false);
                    remoteRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
                    remoteRenderer.init(eglBase.getEglBaseContext(), null);
                    rtcEngine.callPeer(peerName, remoteRenderer);
                    addFeedWindow(peerName, remoteRenderer);
                    isStartCall = true;
                    startorstopCallBtn.setText("停止呼叫");
                } else {
                    peerName = null;
                    rtcEngine.hangup();
                    isStartCall = false;
                    startorstopCallBtn.setText("开始呼叫");
                }

            } else {
                if (!isStartCall) {
                    EditText inputUserId = new EditText(MainActivity.this);
                    inputUserId.setInputType(EditorInfo.TYPE_CLASS_TEXT);
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("请输入你要呼的账号");
                    //builder.setIcon(R.drawable.ic_launcher_foreground);
                    builder.setView(inputUserId);
                    builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String userName = inputUserId.getText().toString();
                            if (RTCEngineContext.videoCallEnabled) {
                                remoteRenderer = new RTCViewRenderer(MainActivity.this);
                                remoteRenderer.setKeepScreenOn(true);
                                remoteRenderer.setZOrderMediaOverlay(true);
                                remoteRenderer.setZOrderOnTop(true);
                                remoteRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
                                remoteRenderer.init(eglBase.getEglBaseContext(), null);
                                rtcEngine.callPeer(userName, remoteRenderer);
                                addFeedWindow(userName, remoteRenderer);
                            } else {
                                rtcEngine.callPeer(userName, null);
                            }
                            isStartCall = true;
                            startorstopCallBtn.setText("停止呼叫");
                        }
                    });
                    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
                    builder.show();
                } else {
                    deleteFeedWindow(peerName);
                    peerName = null;
                    rtcEngine.hangup();
                    isStartCall = false;
                    startorstopCallBtn.setText("开始呼叫");
                }
            }
        }

        if (view.getId() == R.id.switchCamera) {
            rtcEngine.switchCamera();
        }

        if (view.getId() == R.id.videoCallDisabled) {
            boolean checked = videoCallDisabledCheckBox.isChecked();


        }
    }

    public int dip2px(Context context,float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    private void addFeedWindow(String streamName, RTCViewRenderer surfaceView) {
        if (!feedWindows.containsKey(streamName)) {
            RelativeLayout feedWindow = (RelativeLayout) inflater.inflate(R.layout.feed_window, feedContainer, false);
            ViewGroup.LayoutParams params = new RelativeLayout.LayoutParams(dip2px(this,160),dip2px(this,288));
            surfaceView.setLayoutParams(params);
            surfaceView.setZOrderMediaOverlay(true);
            //surfaceView.setBackgroundColor(Color.TRANSPARENT);
            feedWindow.addView(surfaceView,0);
            feedContainer.addView(feedWindow);
            feedWindows.put(streamName,feedWindow);
            TextView streamNameTV = feedWindow.findViewById(R.id.streamName_tv);
            streamNameTV.setText(streamName);
            Button subscribeBtn = feedWindow.findViewById(R.id.subscribe_btn);
            subscribeBtn.setOnClickListener(new View.OnClickListener() {
                private boolean subscribed = false;
                @Override
                public void onClick(View v) {

                }
            });

            CheckBox muteAudioChkBox = feedWindow.findViewById(R.id.muteAudio_chkbox);
            muteAudioChkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                   
                }
            });

            CheckBox muteVideoChkBox = feedWindow.findViewById(R.id.muteVideo_chkbox);
            muteVideoChkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                  
                }
            });
        } else {
            Log.e(TAG, "addFeedWindow fail repeat streamName = " + streamName);
        }
    }

    private void deleteFeedWindow(String streamName) {
        if (streamName == null) {
            return;
        }
        if (feedWindows.containsKey(streamName)) {
            ViewGroup viewGroup = feedWindows.get(streamName);
            viewGroup.removeAllViewsInLayout();
            feedWindows.remove(streamName);
            feedContainer.removeView(viewGroup);


        } else {
            Log.e(TAG,"deleteFeedWindow fail no uid = "+streamName);
        }
        if (remoteRenderer != null) {
            remoteRenderer.release();
        }

    }


    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (compoundButton.getId() == R.id.videoCallDisabled) {
            editor.putBoolean("videoCallDisabled", b);
            RTCEngineContext.videoCallEnabled = !b;
            if(b == true) {
                startorstopPreviewBtn.setVisibility(View.GONE);
            } else {
                startorstopPreviewBtn.setVisibility(View.VISIBLE);
            }
        } else if (compoundButton.getId() == R.id.useOpenSLES) {
            editor.putBoolean("useOpenSLES", b);
            RTCEngineContext.useOpenSLES = b;
        } else if (compoundButton.getId() == R.id.disableBuiltInAEC) {
            editor.putBoolean("disableBuiltInAEC", b);
            RTCEngineContext.disableBuiltInAEC = b;
        } else if (compoundButton.getId() == R.id.disableBuiltInAGC) {
            editor.putBoolean("disableBuiltInAGC", b);
            RTCEngineContext.disableBuiltInAGC = b;
        } else if (compoundButton.getId() == R.id.disableBuiltInNS) {
            editor.putBoolean("disableBuiltInNS", b);
            RTCEngineContext.disableBuiltInNS = b;
        } else if (compoundButton.getId() == R.id.useLegacyAudioDevice) {
            editor.putBoolean("useLegacyAudioDevice", b);
            RTCEngineContext.useLegacyAudioDevice = b;
        }
        editor.commit();
     }


    /**
     * 请求权限
     */
    public void requestDangerousPermissions(String[] permissions, int requestCode) {
        if (checkDangerousPermissions(permissions)){
            handlePermissionResult(requestCode, true);
            return;
        }
        ActivityCompat.requestPermissions(this, permissions, requestCode);
    }

    /**
     * 检查是否已被授权危险权限
     * @param permissions
     * @return
     */
    private boolean checkDangerousPermissions(String[] permissions) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                return false;
            }
        }
        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean granted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                granted = false;
            }
        }
        boolean finish = handlePermissionResult(requestCode, granted);
        if (!finish){
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * 处理请求危险权限的结果
     * @return
     */
    private boolean handlePermissionResult(int requestCode, boolean granted) {
        //Notice 这里要自定义处理权限申请。
        return false;
    }

    @Override
    public void onCalling() {

    }

    @Override
    public void onIncomingCall(String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("请选择");
                builder.setPositiveButton("接听", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        peerName = s;
                        if (RTCEngineContext.videoCallEnabled) {
                            RTCViewRenderer surfacePreview = new RTCViewRenderer(MainActivity.this);
                            surfacePreview.setKeepScreenOn(true);
                            surfacePreview.setZOrderMediaOverlay(true);
                            surfacePreview.setZOrderOnTop(false);
                            surfacePreview.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
                            surfacePreview.init(eglBase.getEglBaseContext(), null);
                            addFeedWindow(s, surfacePreview);
                            rtcEngine.accept(surfacePreview);

                        } else {
                            rtcEngine.accept(null);
                        }
                        startorstopCallBtn.setText("停止呼叫");
                        isStartCall = true;
                    }
                });
                builder.setNegativeButton("挂断", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        rtcEngine.hangup();
                    }
                });
                incomingDialog = builder.show();
                incomingDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.RED);
                incomingDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.GREEN);


            }
        });

    }

    @Override
    public void onAccepted() {

    }

    @Override
    public void onUserJoin(String s) {

    }

    @Override
    public void onUserLeave(String s) {

    }

    @Override
    public void onRegistered(String s, Boolean aBoolean) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (aBoolean) {
                    editor.putString("useId", userEdit.getText().toString());
                    editor.commit();
                    sdkInitBtn.setText("注销");
                    Toast.makeText(getApplicationContext(), "账号注册成功", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "账号注册失败", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onAcctError(String s, Integer integer) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (integer == -1) {
                    sdkInitBtn.setText("注册");
                    Toast.makeText(getApplicationContext(), "账号已经被注册", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onCallStatus(CallStatus callStatus) {
        Log.e(TAG, "onCallStatus rx audio delay " + callStatus.rxAudioDelayMs + " rx video delay " + callStatus.rxVideoDelayMs);
    }

    @Override
    public void onHangup() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (incomingDialog != null) {
                    incomingDialog.dismiss();
                    incomingDialog = null;
                }
                deleteFeedWindow(peerName);
                if (isStartPreview) {
                    isStartPreview = false;
                    rtcEngine.stopPreview();
                    localSurfaceLayout.removeView(localRenderer);
                    localRenderer.release();
                    if (localRenderer != null) {
                        localRenderer.release();
                        localRenderer = null;
                    }
                    startorstopPreviewBtn.setText("开始预览");
                }
                if (isStartCall) {
                    rtcEngine.hangup();
                    isStartCall = false;
                    startorstopCallBtn.setText("开始呼叫");
                }
                if (remoteRenderer != null) {
                    remoteRenderer.release();
                    remoteRenderer = null;
                }
            }
        });
    }

    @Override
    public void onRuntimeError(String s, Integer integer, String s1) {

    }

    @Override
    public void onMissingCall(String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String text = "你有一个新的未接听来电" + s;
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onKeepAlive(Integer integer) {

    }
}

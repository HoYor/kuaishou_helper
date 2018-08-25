package com.fuyun.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * Created by yym on 2018/8/20.
 */

public class RobotService extends AccessibilityService {

    private final String TAG = "RobotService";
    public static String mSendMsg = "我把所有视频都评论了一遍 ~ ";
    public static boolean isAllowPlay = true;
    private int step = 0;
    private int mItemCount = -1;
    private int mCurItem = -1;
    private boolean isRunning = false;
    private boolean isOpen = false;
    private Disposable disposable;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        int eventType = accessibilityEvent.getEventType();
        Log.d(TAG, "onAccessibilityEvent-eventType:"+eventType);
        if(eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED){
            Log.d(TAG, "onAccessibilityEvent: TYPE_WINDOW_STATE_CHANGED");
            if(!isAllowPlay)return;
            if(!isOpen) {
                isOpen = true;
                disposable = startFix();
            }
            ComponentName activityComponentName = new ComponentName(
                    accessibilityEvent.getPackageName().toString(),
                    accessibilityEvent.getClassName().toString());
            dispatchStep(activityComponentName);
        }
    }

    private void test() {
        Observable.interval(5,TimeUnit.SECONDS)
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
                        List<AccessibilityNodeInfo> recycler = nodeInfo.findAccessibilityNodeInfosByViewId(
                                "com.smile.gifmaker:id/recycler_view");
                        if(recycler == null || recycler.size() == 0)return;
                        recycler.get(0).performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                    }
                });
    }

    private void dispatchStep(final ComponentName activityComponentName) {
        isRunning = true;
        Observable.timer(1000,TimeUnit.MILLISECONDS)
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        switch (step){
                            case 0:
                                step1();
                                break;
                            case 1:
//                        "com.yxcorp.gifshow.detail.PhotoDetailActivity"
                                step2();
                                break;
                            case 2:
                                step3();
                                break;
                            case 3:
                                if(activityComponentName == null){
                                    step = 0;
                                    return;
                                }
                                try {
                                    String activityName = getPackageManager().getActivityInfo
                                            (activityComponentName, 0).name;
                                    Log.d(TAG, "activityName: " + activityName);
                                    if (activityName != null &&
                                            "com.yxcorp.gifshow.HomeActivity".equals(activityName)) {
                                        return;
                                    }
                                } catch (PackageManager.NameNotFoundException e) {
                                    e.printStackTrace();
                                }
                                step4();
                                break;
                        }
                    }
                });
    }

    private Disposable startFix() {
        return Observable.interval(5,TimeUnit.SECONDS)
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        if(!isAllowPlay){
                            disposable.dispose();
                            disposable = null;
                        }else if(disposable == null){
                            disposable = startFix();
                        }
                        if(isRunning){
                            isRunning = false;
                        }else{
                            dispatchStep(null);
                        }
                    }
                });
    }

    private void step1(){
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if(nodeInfo == null)return;
        List<AccessibilityNodeInfo> recyclers = nodeInfo.findAccessibilityNodeInfosByViewId(
                "com.smile.gifmaker:id/recycler_view");
        if(recyclers != null && recyclers.size() > 0) {
            AccessibilityNodeInfo recycler = recyclers.get(0);
            if(mItemCount == -1 && mCurItem == -1) {
                mItemCount = recycler.getChildCount();
                mCurItem = 0;
            }else{
                mCurItem++;
            }
            if(mCurItem < recycler.getChildCount()) {
                step = 1;
                recycler.getChild(mCurItem).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
    }

    private void step2(){
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if(nodeInfo == null)return;
        List<AccessibilityNodeInfo> holders = nodeInfo.findAccessibilityNodeInfosByViewId(
                "com.smile.gifmaker:id/editor_holder");
        if(holders == null || holders.size() == 0){
            step4();
        }else {
            step = 2;
            holders.get(holders.size()-1).performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }

    private void step3(){
        final AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if(nodeInfo == null)return;
        List<AccessibilityNodeInfo> editor = nodeInfo.findAccessibilityNodeInfosByViewId(
                "com.smile.gifmaker:id/editor");
        if(editor == null || editor.size()==0)return;
        AccessibilityNodeInfo editInfo = editor.get(editor.size()-1);
        Bundle bundle = new Bundle();
        bundle.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,mSendMsg);
        editInfo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,bundle);
//        editInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);

        Observable.timer(1,TimeUnit.SECONDS)
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        List<AccessibilityNodeInfo> send = nodeInfo.findAccessibilityNodeInfosByViewId(
                                "com.smile.gifmaker:id/finish_button");
                        if(send == null || send.size()==0)return;
                        step = 3;
                        send.get(send.size()-1).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                });
    }

    private void step4() {
        performGlobalAction(GLOBAL_ACTION_BACK);
        Observable.timer(500,TimeUnit.MILLISECONDS)
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
                        if(nodeInfo == null) {
//                            step1();
                            return;
                        }
                        List<AccessibilityNodeInfo> recycler = nodeInfo.findAccessibilityNodeInfosByViewId(
                                "com.smile.gifmaker:id/recycler_view");
                        if(recycler == null || recycler.size() == 0)return;
                        if(mCurItem >= mItemCount-1) {
                            mItemCount = -1;
                            mCurItem = -1;
                            recycler.get(0).performAction(AccessibilityNodeInfo
                                    .ACTION_SCROLL_FORWARD);
                            Observable.timer(500,TimeUnit.MILLISECONDS)
                                    .subscribe(new Consumer<Long>() {
                                        @Override
                                        public void accept(Long aLong) throws Exception {
                                            step1();
                                        }
                                    });
                        }else{
                            step1();
                        }
                    }
                });
    }

    private void slideUp() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Path path = new Path();
            path.moveTo(400,1000);
            path.lineTo(400,100);
            GestureDescription gestureDescription = new GestureDescription.Builder()
                    .addStroke(new GestureDescription.StrokeDescription(path,100,300))
                    .build();
            if(dispatchGesture(gestureDescription, new GestureResultCallback() {
                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    Log.d(TAG, "onCancelled: ");
                }

                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    step = 0;
                    step1();
                }
            },null)){
//                isAllowPlay = false;
            }
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "onInterrupt: ");
    }


    private static int tabcount = -1;
    private static StringBuilder sb;

    public static void printPacketInfo(AccessibilityNodeInfo root) {
        sb = new StringBuilder();
        tabcount = 0;
        int[] is = {};
        analysisPacketInfo(root, is);
        Log.d("RobotService",sb.toString());
    }

    //打印此时的界面状况,便于分析
    private static void analysisPacketInfo(AccessibilityNodeInfo info, int... ints) {
        if (info == null) {
            return;
        }
        if (tabcount > 0) {
            for (int i = 0; i < tabcount; i++) {
                sb.append("\t\t");
            }
        }
        if (ints != null && ints.length > 0) {
            StringBuilder s = new StringBuilder();
            for (int j = 0; j < ints.length; j++) {
                s.append(ints[j]).append(".");
            }
            sb.append(s).append(" ");
        }
        String name = info.getClassName().toString();
        String[] split = name.split("\\.");
        name = split[split.length - 1];
        if ("TextView".equals(name)) {
            CharSequence text = info.getText();
            sb.append("text:").append(text);
        } else if ("Button".equals(name)) {
            CharSequence text = info.getText();
            sb.append("Button:").append(text);
        } else {
            sb.append(name);
        }
        sb.append("\n");

        int count = info.getChildCount();
        if (count > 0) {
            tabcount++;
            int len = ints.length + 1;
            int[] newInts = Arrays.copyOf(ints, len);

            for (int i = 0; i < count; i++) {
                newInts[len - 1] = i;
                analysisPacketInfo(info.getChild(i), newInts);
            }
            tabcount--;
        }

    }
}

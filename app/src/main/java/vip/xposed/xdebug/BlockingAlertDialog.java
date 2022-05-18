package vip.xposed.xdebug;

import android.app.Activity;


import java.util.logging.LogRecord;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.widget.EditText;

/**
 * Created by Administrator on 2022/5/18.
 */

public class BlockingAlertDialog {

    private Context context;
    private String content = "";
    private Handler handler;
    public static final int CONTINUE = 1;//继续执行
    public static final int FINISH = 0;//结束进程
    private int result = CONTINUE;//默认继续执行


    public BlockingAlertDialog(Context context, String content) {
        this.context = context;
        this.content = content;
    }

    public int show() {
        this.handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                throw new RuntimeException();
            }
        };


        AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
        builder.setTitle("等待调试器");
        builder.setCancelable(false);//禁止 在点击窗口外的界面时关闭对话框
        builder.setMessage(this.content);
        builder.setPositiveButton("继续执行", new DialogInterface.OnClickListener() {//添加确定按钮
            @Override
            public void onClick(DialogInterface dialog, int which) {//确定按钮的响应事件，点击事件没写，自己添加
                result = CONTINUE;
                handler.handleMessage(handler.obtainMessage());
            }
        });
        builder.setNegativeButton("结束进程", new DialogInterface.OnClickListener() {//添加返回按钮
            @Override
            public void onClick(DialogInterface dialog, int which) {//响应事件，点击事件没写，自己添加
                result = FINISH;
            }

        });
        builder.create();
        builder.show();
        try {
            Looper.myLooper().loop();// 这里 循环是可以 但 程序 还会继续执行下去？ 是 因为 其他包的原因？
        } catch (RuntimeException e) {
        }

        return result;

    }


}

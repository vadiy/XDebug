package vip.xposed.xdebug;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;


public class PopupWindowDialog {

    private Context context;
    private Button bContinue;
    private Button bFinish;
    private TextView tContent;
    private String content = "";
    public static final int CONTINUE = 1;//继续执行
    public static final int FINISH = 0;//结束进程
    private int result = CONTINUE;//默认继续执行

    public PopupWindowDialog(Context context, String content) {
        this.context = context;
        this.content = content;
    }

    public int show() {

        View contentView = LayoutInflater.from(this.context).inflate(R.layout.popup_window, null);
        tContent = contentView.findViewById(R.id.tContent);
        tContent.setText(this.content);
        bContinue = contentView.findViewById(R.id.bContinue);
        bFinish = contentView.findViewById(R.id.bFinish);

        bContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("PopupWindowDialog", "按下 bContinue");
                result = CONTINUE;
            }
        });

        bFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("PopupWindowDialog", "按下 bFinish");
                result = FINISH;
            }
        });


        contentView.setBackgroundColor(Color.BLACK);

        final PopupWindow popupWindow = new PopupWindow(contentView);
        popupWindow.setContentView(contentView);
        popupWindow.setFocusable(false);
        popupWindow.setOutsideTouchable(false);
        popupWindow.showAtLocation(contentView, Gravity.BOTTOM, 0, 0);
        //Intent intent = new Intent();
        //context.startActivity(intent);

        return result;
    }
}

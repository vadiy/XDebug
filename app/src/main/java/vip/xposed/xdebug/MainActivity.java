package vip.xposed.xdebug;

import android.app.Activity;
import android.app.Application;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity {
    SharedPreferences mSharedPreferences;

    List<ApplicationInfo> packages;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSharedPreferences = getSharedPreferences("config", Activity.MODE_WORLD_READABLE);
        packages = getPackages();
        setOnItemEvent();



    }

    //监听列表事件
    private void setOnItemEvent(){
        ListView lv = (ListView) findViewById(R.id.listView1);
        lv.setAdapter(new MyAdapter());
        //监听点击事件
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,long id) {
                ApplicationInfo appInfo = packages.get(position);
                mSharedPreferences.edit().putString(PreferencesUtils.DEBUGPACKAGENAME,appInfo.packageName).commit();
                Intent intent = getPackageManager().getLaunchIntentForPackage(
                        appInfo.packageName);
                startActivity(intent);
            }
        });

        //监听长按事件
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                ClipboardManager cm =(ClipboardManager)getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ApplicationInfo appInfo = packages.get(position);
                cm.setPrimaryClip(ClipData.newPlainText(null, appInfo.packageName));
                Toast.makeText(MainActivity.this, appInfo.packageName+"\n已复制到剪切板", Toast.LENGTH_SHORT).show();
                return true;//返回 true 拦截消息
            }
        });
    }
    //过滤系统app
    private List<ApplicationInfo> getPackages(){
        List<ApplicationInfo> napps = new ArrayList<ApplicationInfo>();
        List<ApplicationInfo> apps = getPackageManager().getInstalledApplications(0);
        for (int i=0;i<apps.size();i++){
            ApplicationInfo app = apps.get(i);
            if ((ApplicationInfo.FLAG_SYSTEM & app.flags)!=0){
            continue;
            }
            napps.add(app);
        }

        sortData(napps);
        return napps;
    }

    //按时间排序
    private void sortData(List<ApplicationInfo> apps){
        Collections.sort(apps, new Comparator<ApplicationInfo>() {
            @Override
            public int compare(ApplicationInfo o1, ApplicationInfo o2) {
                try {
                    PackageInfo i1 = getPackageManager().getPackageInfo(o1.packageName, 0);
                    PackageInfo i2 = getPackageManager().getPackageInfo(o2.packageName, 0);
                    if (i1.firstInstallTime > i2.firstInstallTime) {
                        return -1;
                    } else {
                        return 1;
                    }
                } catch (PackageManager.NameNotFoundException e) {

                }
                return 0;
            }


        });



    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    List<Map<String, Object>> list;

    class MyAdapter extends BaseAdapter {

        public void Query() {

        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return packages.size();
        }

        @Override
        public Object getItem(int arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getItemId(int arg0) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public View getView(int arg0, View arg1, ViewGroup arg2) {
            View v = getLayoutInflater().inflate(R.layout.item, null);
            ImageView ivAppIcon = (ImageView) v.findViewById(R.id.ivAppIcon);
            TextView textviewAppName = (TextView) v.findViewById(R.id.textviewAppName);
            TextView textviewPackageName = (TextView) v.findViewById(R.id.textviewPackageName);

            ApplicationInfo app = (ApplicationInfo) packages.get(arg0);
            // 获得包名
            String packageName = app.packageName;
            textviewPackageName.setText(packageName);

            // 获得图标
            Drawable dr = getPackageManager().getApplicationIcon(app);
            ivAppIcon.setImageDrawable(dr);

            String label = "";
            try {
                //获得应用名
                label = getPackageManager().getApplicationLabel(app).toString();
                textviewAppName.setText(label);
            } catch (Exception e) {
            }
            return v;
        }

    }
}

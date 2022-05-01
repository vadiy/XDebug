package vip.xposed.xdebug;

import android.os.Process;
import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by Administrator on 2022/4/30.
 */

public class XDebuggable implements IXposedHookLoadPackage,IXposedHookZygoteInit{


    private static final int DEBUG_ENABLE_DEBUGGER = 0x1;

    private XC_MethodHook debugAppsHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param)
                throws Throwable {
            //在系统进程 system_process 中可以查看到日志
            if (PreferencesUtils.getDebugPackageName().equals(param.args[1])){
                Log.i("调试","-- before  niceName :" + param.args[1] +" 目标:"+PreferencesUtils.getDebugPackageName()+ " debugFlags : " + param.args[5]);
                int flags = (Integer) param.args[5];
                // 修改类android.os.Process的start函数的第6个传入参数
                if ((flags & DEBUG_ENABLE_DEBUGGER) == 0) {
                    // 增加开启Android调试选项的标志
                    flags |= DEBUG_ENABLE_DEBUGGER;
                }
                param.args[5] = flags;
                Log.i("调试","-- before niceName :" + param.args[1]+ " debugFlags : " + param.args[5]);
            }
        }

        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if (PreferencesUtils.getDebugPackageName().equals(param.args[1])){
                Log.i("调试","-- after niceName :" + param.args[1]+ " debugFlags : " + param.args[5]);
            }
        }
    };

    // 实现的接口IXposedHookZygoteInit的函数
    @Override
    public void initZygote(final IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        // /frameworks/base/core/java/android/os/Process.java
        // Hook类android.os.Process的start函数
        Log.i("调试","-- hook initZygote");
        XposedBridge.hookAllMethods(Process.class, "start", debugAppsHook);
        XposedBridge.hookAllMethods(Process.class, "startWebView", debugAppsHook);

    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        //Log.i("调试","-- hook handleLoadPackage");

    }
}


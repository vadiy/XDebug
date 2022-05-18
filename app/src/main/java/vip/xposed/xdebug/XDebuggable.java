package vip.xposed.xdebug;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AndroidAppHelper;
import android.content.Context;
import android.os.Process;

import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by Administrator on 2022/4/30.
 */

public class XDebuggable implements IXposedHookLoadPackage, IXposedHookZygoteInit {


    private static final String TAG = "XDebug";
    private static final int DEBUG_ENABLE_DEBUGGER = 0x1;
    private int appPid = 0;
    private static Object lock = new Object();

    private static void log(String text) {
        XposedBridge.log(TAG + "=>" + text);
    }

    // 实现的接口IXposedHookZygoteInit的函数
    @Override
    public void initZygote(final IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {

        // /frameworks/base/core/java/android/os/Process.java
        // Hook类android.os.Process的start函数
        log("-- hook initZygote");
        XposedBridge.hookAllMethods(Process.class, "start", debugAppsHook);
        XposedBridge.hookAllMethods(Process.class, "startWebView", debugAppsHook);

    }

    private XC_MethodHook debugAppsHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param)
                throws Throwable {
            //在系统进程 system_process 中可以查看到日志
            if (PreferencesUtils.getDebugPackageName().equals(param.args[1])) {
                log("-- before  niceName :" + param.args[1] + " 目标:" + PreferencesUtils.getDebugPackageName() + " debugFlags : " + param.args[5]);
                int flags = (Integer) param.args[5];
                // 修改类android.os.Process的start函数的第6个传入参数
                if ((flags & DEBUG_ENABLE_DEBUGGER) == 0) {
                    // 增加开启Android调试选项的标志
                    flags |= DEBUG_ENABLE_DEBUGGER;
                }
                param.args[5] = flags;
                log("-- before niceName :" + param.args[1] + " debugFlags : " + param.args[5]);

            }
        }

        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            if (PreferencesUtils.getDebugPackageName().equals(param.args[1])) {
                Object processStartResult = param.getResult();
                int pid = XposedHelpers.getIntField(processStartResult, "pid");
                log("-- after niceName :" + param.args[1] + " debugFlags : " + param.args[5] + " appPID : " + pid);
            }
        }
    };

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (PreferencesUtils.getDebugPackageName().equals(lpparam.packageName)) {
            //hookDefineClass(lpparam);// 先拦截到这个 ,拦截的包不全,先停用 等 后面有需要的app 在开启
            //hookLoadClass(lpparam);//后拦截到这个
            //methodHook(lpparam);
        }
    }

    //用于个别app 的拦截
    public void hookDefineClass(final XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            /*get DexFile Class*/
            final Class clazz = lpparam.classLoader.loadClass("dalvik.system.DexFile");
            Method[] methods = clazz.getDeclaredMethods();
            for (int i = 0; i < methods.length; i++) {
                String name = methods[i].getName();
                if (name.equalsIgnoreCase("defineClass")) {
                    XposedBridge.hookMethod(methods[i], new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Class clz = (Class) param.getResult();
                            if (clz != null) {
                                log("hookDefineClass 拦截到包 " + lpparam.packageName + " " + clz.getName());

                                methodHook(lpparam);
                            }
                        }
                    });
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    //用于个别app 的拦截
    public void hookLoadClass(final XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(ClassLoader.class, "loadClass", String.class, new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.hasThrowable()) {
                        return;
                    }
                    Class clz = (Class) param.getResult();
                    if (clz != null) {
                        //log("hookLoadClass 拦截到包 " + lpparam.packageName + " " + clz.getName());

                        methodHook(lpparam);
                    }

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void methodHook(XC_LoadPackage.LoadPackageParam lpparam) {
        //XposedBridge.hookAllMethods(Activity.class, "attach", attachHook);//这个获取的参数比较多
        XposedBridge.hookAllMethods(Activity.class, "performCreate", performCreateHook);
    }

    private XC_MethodHook attachHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            Activity activity = (Activity) param.thisObject;
            Context context = (Context) param.args[0];
            Activity parentActivity = (Activity) param.args[9];
            log("attachHook 进来 " + context.getClass().getName() + " " + context.getClass().getCanonicalName() + " " + context.getClass().getSimpleName());
            synchronized (lock) {
                if (context != null && appPid == 0) {// && appPid == 0 不判断是否取到进程的话就可以  && !"Main".equals(activity.getClass().getSimpleName())
                    appPid = getPidFromContext(context);
                    String appName = context.getPackageManager().getApplicationLabel(context.getApplicationInfo()).toString();
                    String msg = "应用名:" + appName + "\n进程名:" + context.getApplicationInfo().packageName + "\n进程ID:" + appPid + "\n等待调试器附加...";
                    log(msg);
                    //Toast.makeText(context,msg,Toast.LENGTH_LONG).show();

                    waittingForDebugger(context, msg);
                }
            }
            log("attachHook 出来");

        }

        @Override
        protected void afterHookedMethod(MethodHookParam param) throws Throwable {

        }
    };


    private XC_MethodHook performCreateHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            Activity activity = (Activity) param.thisObject;
            log("performCreateHook 进来" + activity.getClass().getName() + " " + activity.getClass().getCanonicalName() + " " + activity.getClass().getSimpleName());
            //activity.getCallingActivity().wait();
            synchronized (lock) {
                if (activity != null && appPid == 0) {// && appPid == 0 不判断是否取到进程的话就可以  && !"Main".equals(activity.getClass().getSimpleName())
                    appPid = getPidFromContext(activity);
                    String appName = activity.getPackageManager().getApplicationLabel(activity.getApplicationInfo()).toString();
                    String msg = "应用名:" + appName + "\n进程名:" + activity.getPackageName() + "\n进程ID:" + appPid + "\n等待调试器附加...";
                    log(msg);
                    //Toast.makeText(context,msg,Toast.LENGTH_LONG).show();

                    waittingForDebugger(activity, msg);
                }
            }
            log("performCreateHook 出来");
        }

        ;
    };


    private int getPidFromContext(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo appProcessInfo : activityManager.getRunningAppProcesses()) {
            if (appProcessInfo.processName.equals(context.getPackageName())) {
                return appProcessInfo.pid;
            }
        }
        return 0;
    };

    private void waittingForDebugger(final Context context, final String msg) {
        log("begin Runnable");
//        if (new BlockingAlertDialog(context,msg).show() == BlockingAlertDialog.FINISH){
//            Process.killProcess(getPidFromContext(context));
//        }
        if (new PopupWindowDialog(context,msg).show() == PopupWindowDialog.FINISH){
            Process.killProcess(getPidFromContext(context));
        }

    };

}


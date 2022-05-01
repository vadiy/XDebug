package vip.xposed.xdebug;

import de.robv.android.xposed.XSharedPreferences;

//不能在程序界面使用,只能在xposed模块中使用,否则出异常
public class PreferencesUtils {

    public static final String DEBUGPACKAGENAME = "DEBUGPACKAGENAME";

    private static XSharedPreferences instanc = null;

    //new File(Environment.getDataDirectory(), "data/" + packageName + "/shared_prefs/" + prefFileName + ".xml");

    private static XSharedPreferences getInstanc() {
        if (instanc == null) {
            instanc = new XSharedPreferences(PreferencesUtils.class.getPackage().getName(), "config");
            instanc.makeWorldReadable();
        } else {
            instanc.reload();
        }
        return instanc;
    }

    //用于设置连接服务器的地址
    public static String getDebugPackageName() {
        return getInstanc().getString(DEBUGPACKAGENAME, "");
    }


}

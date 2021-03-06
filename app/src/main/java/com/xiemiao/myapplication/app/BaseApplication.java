package com.xiemiao.myapplication.app;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.view.WindowManager;

import com.jess.arms.base.App;
import com.jess.arms.base.delegate.AppDelegate;
import com.jess.arms.base.delegate.AppLifecycles;
import com.jess.arms.di.component.AppComponent;
import com.jess.arms.integration.IRepositoryManager;
import com.jess.arms.utils.ArmsUtils;
import com.xiemiao.myapplication.BuildConfig;
import com.xiemiao.myapplication.common.mvp.model.api.service.CommonService;
import com.xiemiao.myapplication.common.mvp.model.bean.ConfigInfoResult;
import com.xiemiao.myapplication.net.CommonObserver;
import com.xiemiao.myapplication.net.ExceptionHandle;
import com.xiemiao.myapplication.net.RxUtils;
import com.xiemiao.myapplication.utils.UIUtils;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import me.jessyan.rxerrorhandler.handler.RetryWithDelay;
import timber.log.Timber;

/**
 * Title:基类Application,做一些应用启动初始化 <br />
 * Description: <br />
 * author:xiemiao <br />
 * date: 2017-09-30  <br />
 * version 1.0 <br />
 */
public class BaseApplication extends Application implements App {
    private AppLifecycles mAppDelegate;
    private IRepositoryManager mRepositoryManager;

    private static Context mContext;
    private static Handler mHandler;
    private static Thread mMainThread;
    private static int mMainThreadId;

    /**
     * 屏幕宽度
     */
    private int screenWidth;
    /**
     * 屏幕高度
     */
    private int screenHeight;

    private static BaseApplication instance;

    /**
     * 这里会在 {@link BaseApplication#onCreate} 之前被调用,可以做一些较早的初始化
     * 常用于 MultiDex 以及插件化框架的初始化
     *
     * @param base
     */
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        this.mAppDelegate = new AppDelegate(base);
        this.mAppDelegate.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mAppDelegate.onCreate(this);
        instance = this;
        // 获取上下文
        mContext = getApplicationContext();
        // 获取消息对象
        mHandler = new Handler();
        // 获取主线程
        mMainThread = Thread.currentThread();
        // 获取主线程ID
        mMainThreadId = android.os.Process.myTid();
        // 在使用 SDK 各组间之前初始化 context 信息，传入 ApplicationContext
        final WindowManager windowManager = (WindowManager) getBaseContext().getSystemService
                (Context.WINDOW_SERVICE);
        // 屏幕高度、宽度
        screenWidth = windowManager.getDefaultDisplay().getWidth();
        screenHeight = windowManager.getDefaultDisplay().getHeight();

        //网络请求管理类
        mRepositoryManager = getAppComponent().repositoryManager();

        //Timber日志打印
        if (BuildConfig.LOG_DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        //请求获取全局配置信息
        requestGetConfigInfo();

    }

    private void requestGetConfigInfo() {
        Observable<ConfigInfoResult> configInfo = mRepositoryManager.obtainRetrofitService(CommonService.class).getConfigInfo();
        configInfo.retryWhen(new RetryWithDelay(3, 2))
                .compose(RxUtils.<ConfigInfoResult>schedulersTransformer())//线程调度
                .compose(RxUtils.<ConfigInfoResult>httpTransformer())//统一返回数据过滤 异常处理
                .subscribe(new CommonObserver<ConfigInfoResult>() {
                    @Override
                    public void onNext(@NonNull ConfigInfoResult configInfoResult) {
                        //保存全局配置
                        UIUtils.putConfigInfoResult(configInfoResult);
                    }

                    @Override
                    public void onError(ExceptionHandle.ResponeThrowable e) {
                        ArmsUtils.snackbarText(e.message);
                    }
                });

    }

    /**
     * 在模拟环境中程序终止时会被调用
     */
    @Override
    public void onTerminate() {
        super.onTerminate();
        if (mAppDelegate != null)
            this.mAppDelegate.onTerminate(this);
    }

    /**
     * 将AppComponent返回出去,供其它地方使用, AppComponent接口中声明的方法返回的实例,在getAppComponent()拿到对象后都可以直接使用
     *
     * @return
     */
    @Override
    public AppComponent getAppComponent() {
        return ((App) mAppDelegate).getAppComponent();
    }

    public static BaseApplication getInstance() {
        return instance;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public static Handler getHandler() {
        return mHandler;
    }

    public static Context getContext() {
        return mContext;
    }

    public static Thread getMainThread() {
        return mMainThread;
    }

    public static int getMainThreadId() {
        return mMainThreadId;
    }
}

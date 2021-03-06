package com.amobletool.bluetooth.le.downexample.mvp;

/**
 * MVPPlugin
 * 邮箱 784787081@qq.com
 *
 * @author xuyan
 */

public interface BasePresenter<V extends BaseView> {
    void attachView(V view);

    void detachView();
}

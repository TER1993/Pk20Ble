package com.amobletool.bluetooth.le.downexample.mvp;

/**
 * MVPPlugin
 * 邮箱 784787081@qq.com
 *
 * @author xuyan
 */

public class BasePresenterImpl<V extends BaseView> implements BasePresenter<V> {
    protected V mView;

    @Override
    public void attachView(V view) {
        mView = view;
    }

    @Override
    public void detachView() {
        mView = null;
    }
}

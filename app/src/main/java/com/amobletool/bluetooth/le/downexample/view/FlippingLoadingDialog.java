
package com.amobletool.bluetooth.le.downexample.view;

import android.app.Dialog;
import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

import com.amobletool.bluetooth.le.R;


/**
 * @author xuyan
 */
public class FlippingLoadingDialog extends Dialog {

    private ImageView mFivIcon;
    private TextView mHtvText;
    private String mText = "";

    public FlippingLoadingDialog(Context context) {
        super(context, R.style.Theme_Light_FullScreenDialogAct);
        init();

    }

    public FlippingLoadingDialog(Context context, String text) {
        super(context, R.style.Theme_Light_FullScreenDialogAct);
        mText = text;
        init();

    }

    private void init() {
        setContentView(R.layout.diloag_flipping_loading);
        mFivIcon = findViewById(R.id.toast_icon);
        mHtvText = findViewById(R.id.toast_text);
        mHtvText.setText(mText);
        setCanceledOnTouchOutside(false);
    }


    public void setTouchCancel(boolean cancel) {
        setCanceledOnTouchOutside(cancel);
    }

    public void setText(String text) {
        mText = text;
        mHtvText.setText(mText);
    }

    @Override
    public void show() {
        try {
            super.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void dismiss() {
        try {
            if (isShowing()) {
                super.dismiss();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

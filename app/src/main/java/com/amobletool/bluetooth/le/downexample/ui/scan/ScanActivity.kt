package com.amobletool.bluetooth.le.downexample.ui.scan

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import cn.bingoogolapple.qrcode.core.QRCodeView
import com.amobletool.bluetooth.le.R
import kotlinx.android.synthetic.main.activity_scan.*




/**
 * @author :Reginer in  2018/10/18 13:38.
 * 联系方式:QQ:282921012
 * 功能描述:扫二维码
 */
@SuppressLint("Registered")
class ScanActivity : AppCompatActivity(), QRCodeView.Delegate {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)
        qrView.setDelegate(this)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }
    }

    override fun onStart() {
        super.onStart()
        qrView.startCamera()
        qrView.startSpot()
    }

    override fun onStop() {
        qrView.stopCamera()
        super.onStop()
    }

    override fun onDestroy() {
        qrView.onDestroy()
        super.onDestroy()
    }

    override fun onScanQRCodeSuccess(s: String) {
        setResult(Activity.RESULT_OK, Intent().putExtra("SCAN", s))
        finish()
    }

    override fun onScanQRCodeOpenCameraError() {
    }

}

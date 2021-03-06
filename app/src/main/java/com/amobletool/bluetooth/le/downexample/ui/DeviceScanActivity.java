/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amobletool.bluetooth.le.downexample.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.amobletool.bluetooth.le.R;
import com.amobletool.bluetooth.le.downexample.MyApp;
import com.amobletool.bluetooth.le.downexample.ui.scan.ScanActivity;
import com.scandecode.ScanDecode;
import com.scandecode.inf.ScanInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 *
 * @author xuyan
 */
@SuppressLint("NewApi")
public class DeviceScanActivity extends ListActivity {

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CAMERA_SCAN = 1001;
    private static final String SCAN = "SCAN";
    // 5秒后停止查找搜索.
    private static final long SCAN_PERIOD = 3000;
    private BluetoothLeScanner mBluetoothLeScanner;
    private static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 1;

    private ScanInterface scanDecode;
    private List<BluetoothDevice> mList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getActionBar()).setTitle(R.string.title_devices);
        mHandler = new Handler();

        // 检查当前手机是否支持ble 蓝牙,如果不支持退出程序
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // 初始化 Bluetooth adapter, 通过蓝牙管理器得到一个参考蓝牙适配器(API必须在以上android4.3或以上和版本)
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = Objects.requireNonNull(bluetoothManager).getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        // 检查设备上是否支持蓝牙
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//如果 API level 是大于等于 23(Android 6.0) 时
            //判断是否具有权限
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //判断是否需要向用户解释为什么需要申请该权限
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION);
                //showToast("自Android 6.0开始需要打开位置权限才可以搜索到Ble设备");
                //请求权限
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_CODE_ACCESS_COARSE_LOCATION);
            }
        }

        scanDecode = new ScanDecode(this);
        try {
            scanDecode.initService("true");
        } catch (Exception e) {
            e.printStackTrace();
        }

        scanDecode.getBarCode(new ScanInterface.OnScanListener() {
            @Override
            public void getBarcode(String s) {
                scanScan(s);
            }

            @Override
            public void getBarcodeByte(byte[] bytes) {

            }
        });
        s = getIntent().getStringExtra("SCAN");
        if (s != null && s.length() == 4) {
            firstScan(s);
            reScan(s);
        }

    }

    private void reScan(String scan) {
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            /*
             *要执行的操作 0.1秒后执行Runnable中的run方法
             */
            if (!stopscan) {
                runOnUiThread(() -> {
                    mLeDeviceListAdapter.notifyDataSetChanged();
                    scanLeDevice(true);
                    firstScan(scan);
                });
            }

        }, 4000);

    }

    private void firstScan(String scan) {
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            /*
             *要执行的操作  0.1秒后执行Runnable中的run方法
             */
            if (!stopscan) {
                runOnUiThread(() -> {
                    mLeDeviceListAdapter.notifyDataSetChanged();
                    scanResult(scan);
                });
            }
        }, 1000);
    }

    private void scanScan(String scan) {
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            /*
             *要执行的操作 0.1秒后执行Runnable中的run方法
             */
            if (!stopscan) {
                runOnUiThread(() -> {
                    scanResult(scan);
                    reScan(scan);
                });
            }
        }, 1000);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.menu_camera).setVisible(true);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
            case R.id.menu_camera:
                //页面跳转
                startActivityForResult(new Intent(DeviceScanActivity.this, ScanActivity.class), REQUEST_CAMERA_SCAN);
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 为了确保设备上蓝牙能使用, 如果当前蓝牙设备没启用,弹出对话框向用户要求授予权限来启用
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Initializes list view adapter.

        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);
        stopscan = false;
    }

    private String s;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }

        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CAMERA_SCAN) {
            s = data.getStringExtra(SCAN);
            if (s != null && s.length() == 4) {
                firstScan(s);
                reScan(s);
            }
        }

    }

    private void scanResult(String s) {
        mList = mLeDeviceListAdapter.getmLeDevices();
        mLeDeviceListAdapter.notifyDataSetChanged();
        for (int i = 0; i < mList.size(); i++) {
            String s1 = mList.get(i).getAddress();
            String s2 = s1.substring(s1.length() - 5).replaceAll("[[\\s-:punct:]]", "");
            if (s2.equals(s)) {
                if (mScanning) {
                    scanLeDevice(false);
                }
                MyApp.getInstance().getDeviceName(mList.get(i));
                finish();
            }
        }
    }

    private boolean stopscan = true;

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
        stopscan = true;
    }

    //点击列表跳转到DeviceControlActivity
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        System.out.println("==position==" + position);
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) {
            return;
        }
//        final Intent intent = new Intent(this, DeviceControlActivity.class);
//        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
//        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {
//            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mBluetoothLeScanner.stopScan(mScanCallback);
            mScanning = false;
        }
        MyApp.getInstance().getDeviceName(device);
        this.finish();
//        startActivity(intent);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(() -> {
                mScanning = false;
//                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                mBluetoothLeScanner.stopScan(mScanCallback);
                invalidateOptionsMenu();
            }, SCAN_PERIOD);

            mScanning = true;
//            mBluetoothAdapter.startLeScan(mLeScanCallback);
            mBluetoothLeScanner.startScan(mScanCallback);
        } else {
            mScanning = false;
//            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mBluetoothLeScanner.stopScan(mScanCallback);
        }
        invalidateOptionsMenu();
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public ArrayList<BluetoothDevice> getmLeDevices() {
            return mLeDevices;
        }

        public void setmLeDevices(ArrayList<BluetoothDevice> mLeDevices) {
            this.mLeDevices = mLeDevices;
        }

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = view.findViewById(R.id.device_address);
                viewHolder.deviceName = view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            if (mLeDevices.size() > i) {
                BluetoothDevice device = mLeDevices.get(i);
                final String deviceName = device.getName();
                if (deviceName != null && deviceName.length() > 0) {
                    viewHolder.deviceName.setText(deviceName);
                } else {
                    viewHolder.deviceName.setText(R.string.unknown_device);
                }
                viewHolder.deviceAddress.setText(device.getAddress());
            }
            return view;
        }
    }

    // Device scan callback.返蓝牙信息更新到界面
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(() -> {
                mLeDeviceListAdapter.addDevice(device);
                mLeDeviceListAdapter.notifyDataSetChanged();
                scanResult(s);
            });
        }
    };

    // 5.0+.返蓝牙信息更新到界面
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            mLeDeviceListAdapter.addDevice(result.getDevice());
            mLeDeviceListAdapter.notifyDataSetChanged();
            scanResult(s);
        }

//        @Override
//        public void onBatchScanResults(List<ScanResult> results) {
//            super.onBatchScanResults(results);
//            for (int i = 0; i < results.size(); i++) {
//                mLeDeviceListAdapter.addDevice(results.get(i).getDevice());
//                mLeDeviceListAdapter.notifyDataSetChanged();
//            }
//        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };


    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    @Override
    protected void onDestroy() {
        try {
            scanDecode.onDestroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }
}
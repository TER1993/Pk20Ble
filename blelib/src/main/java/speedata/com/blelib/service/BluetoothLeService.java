package speedata.com.blelib.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import speedata.com.blelib.bean.LWHData;
import speedata.com.blelib.bean.PK20Data;
import speedata.com.blelib.bean.SampleGattAttributes;
import speedata.com.blelib.utils.ByteUtils;
import speedata.com.blelib.utils.DataManageUtils;
import speedata.com.blelib.utils.PK20Utils;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
@SuppressLint("NewApi")
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA";
    public final static String NOTIFICATION_DATA = "com.example.bluetooth.le.NOTIFICATION_DATA";
    public final static String NOTIFICATION_DATA_LWH = "com.example.bluetooth.le.NOTIFICATION_DATA_LWH";
    public final static String NOTIFICATION_DATA_ERR = "com.example.bluetooth.le.NOTIFICATION_DATA_ERR";

    public final static String TEST_DATA = "com.example.bluetooth.le.TEST_DATA";
    public final static String NOTIFICATION_DATA_G = "com.example.bluetooth.le.NOTIFICATION_DATA_G";


    private boolean mData1 = false;
    private boolean mData2 = false;
    private boolean mData3 = false;
    private boolean mData4 = false;
    private boolean mData5 = false;
    private boolean mData6 = false;
    private boolean mData7 = false;
    private boolean mData8 = false;

    private byte[] c1;
    private byte[] c2;
    private byte[] c3;
    private byte[] c4;
    private byte[] c5;
    private byte[] c6;
    private byte[] c7;
    private byte[] c8;
    private byte[] c;

    private String lenth1;
    private int allLenth;
    private int l1;
    private int l2;
    private int l3;
    private int l4;
    private int l5;
    private int l6;
    private int l7;
    private int l8;

    public final static UUID UUID_HEART_RATE_MEASUREMENT = UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                mBluetoothGatt.close();
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        //discoverServices 搜索连接设备所支持的service。
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        //readCharacteristic 读取指定的characteristic。
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        //setCharacteristicNotification 设置当指定characteristic值变化时，发出通知。
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        //写入回调
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Handler handler = new Handler(Looper.getMainLooper());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                setCharacteristicNotification(characteristic, true);
            } else if (status == BluetoothGatt.GATT_FAILURE) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        boolean cn = "CN".equals(BluetoothLeService.this.getResources().getConfiguration().locale.getCountry());
                        if (cn) {
                            Toast.makeText(BluetoothLeService.this, "写入失败", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(BluetoothLeService.this, "Write failed", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            } else if (status == BluetoothGatt.GATT_WRITE_NOT_PERMITTED) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        boolean cn = "CN".equals(BluetoothLeService.this.getResources().getConfiguration().locale.getCountry());
                        if (cn) {
                            Toast.makeText(BluetoothLeService.this, "没有权限", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(BluetoothLeService.this, "No permission", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }
    };
    private List<byte[]> mByteList = new ArrayList<>();

    public void wirteCharacteristic(BluetoothGattCharacteristic characteristic) {

        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        mBluetoothGatt.writeCharacteristic(characteristic);
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
            sendBroadcast(intent);
        } else if ("0000fff6-0000-1000-8000-00805f9b34fb".equals(characteristic.getUuid().toString())) {
            final byte[] data = characteristic.getValue();
            String result = ByteUtils.toHexString(data);
            Log.d("ZM", "接收数据: " + result);

            //eyJDdXN0Q29kZSI6IlRUIiwiQ2F0ZWdvcnkiOiIxMTAxIiwiVmFsdWUiOiJUTjE5MTIwMDAzIn0=$ET_01_55B8
            if (data.length == 19 && data[0] == (byte) 0xB1) {
                lenth1 = Objects.requireNonNull(result).substring(2, 4);
                allLenth = Integer.parseInt(lenth1, 16);
                Log.d("ZM", "0xB1: " + allLenth);

                //处理各个数据的长度了l1-l8,以及初始化各个数据状态，然后判断每个数据状态都为true即可
                getAllLenth();

                Log.d("ZM", "l1: " + l1);
                c1 = new byte[l1];
                System.arraycopy(data, 2, c1, 0, l1);
                mData1 = true;
                Log.d("ZM", "mData1: " + mData1);

                if (mData1 && mData2 && mData3 && mData4 && mData5 && mData6 && mData7 && mData8) {
                    c = new byte[allLenth];
                    System.arraycopy(c1, 0, c, 0, l1);
                } else {
                    return;
                }

                String realBarcode = ByteUtils.toAsciiString(c);
                Log.d("ZM", "realBarcode: " + realBarcode);
                intent.putExtra(TEST_DATA, realBarcode);
                sendBroadcast(intent);

            } else if (data.length == 19 && data[0] == (byte) 0xB2) {

                if (l2 == 0) {
                    Log.d("ZM", "l1: " + l1);
                    return;
                }

                c2 = new byte[l2];
                System.arraycopy(data, 1, c2, 0, l2);
                mData2 = true;
                Log.d("ZM", "mData2: " + mData2);

                if (mData1 && mData2 && mData3 && mData4 && mData5 && mData6 && mData7 && mData8) {
                    c = new byte[allLenth];
                    System.arraycopy(c1, 0, c, 0, l1);
                    System.arraycopy(c2, 0, c, 15, l2);

                } else {
                    return;
                }

                String realBarcode = ByteUtils.toAsciiString(c);
                Log.d("ZM", "realBarcode: " + realBarcode);
                intent.putExtra(TEST_DATA, realBarcode);
                sendBroadcast(intent);

            } else if (data.length == 19 && data[0] == (byte) 0xB3) {

                if (l3 == 0) {
                    Log.d("ZM", "l2: " + l2);
                    return;
                }

                c3 = new byte[l3];
                System.arraycopy(data, 1, c3, 0, l3);
                mData3 = true;
                Log.d("ZM", "mData3: " + mData3);

                if (mData1 && mData2 && mData3 && mData4 && mData5 && mData6 && mData7 && mData8) {
                    c = new byte[allLenth];
                    System.arraycopy(c1, 0, c, 0, l1);
                    System.arraycopy(c2, 0, c, 15, l2);
                    System.arraycopy(c3, 0, c, 31, l3);

                } else {
                    return;
                }

                String realBarcode = ByteUtils.toAsciiString(c);
                Log.d("ZM", "realBarcode: " + realBarcode);
                intent.putExtra(TEST_DATA, realBarcode);
                sendBroadcast(intent);

            } else if (data.length == 19 && data[0] == (byte) 0xB4) {

                if (l4 == 0) {
                    Log.d("ZM", "l3: " + l3);
                    return;
                }

                c4 = new byte[l4];
                System.arraycopy(data, 1, c4, 0, l4);
                mData4 = true;
                Log.d("ZM", "mData4: " + mData4);

                if (mData1 && mData2 && mData3 && mData4 && mData5 && mData6 && mData7 && mData8) {
                    c = new byte[allLenth];
                    System.arraycopy(c1, 0, c, 0, l1);
                    System.arraycopy(c2, 0, c, 15, l2);
                    System.arraycopy(c3, 0, c, 31, l3);
                    System.arraycopy(c4, 0, c, 47, l4);

                } else {
                    return;
                }

                String realBarcode = ByteUtils.toAsciiString(c);
                Log.d("ZM", "realBarcode: " + realBarcode);
                intent.putExtra(TEST_DATA, realBarcode);
                sendBroadcast(intent);

            } else if (data.length == 19 && data[0] == (byte) 0xB5) {

                if (l5 == 0) {
                    Log.d("ZM", "l4: " + l4);
                    return;
                }

                c5 = new byte[l5];
                System.arraycopy(data, 1, c5, 0, l5);
                mData5 = true;
                Log.d("ZM", "mData5: " + mData5);

                if (mData1 && mData2 && mData3 && mData4 && mData5 && mData6 && mData7 && mData8) {
                    c = new byte[allLenth];
                    System.arraycopy(c1, 0, c, 0, l1);
                    System.arraycopy(c2, 0, c, 15, l2);
                    System.arraycopy(c3, 0, c, 31, l3);
                    System.arraycopy(c4, 0, c, 47, l4);
                    System.arraycopy(c5, 0, c, 63, l5);

                } else {
                    return;
                }

                String realBarcode = ByteUtils.toAsciiString(c);
                Log.d("ZM", "realBarcode: " + realBarcode);
                intent.putExtra(TEST_DATA, realBarcode);
                sendBroadcast(intent);

            } else if (data.length == 19 && data[0] == (byte) 0xB6) {

                if (l6 == 0) {
                    Log.d("ZM", "l5: " + l5);
                    return;
                }

                c6 = new byte[l6];
                System.arraycopy(data, 1, c6, 0, l6);
                mData6 = true;
                Log.d("ZM", "mData6: " + mData6);

                if (mData1 && mData2 && mData3 && mData4 && mData5 && mData6 && mData7 && mData8) {
                    c = new byte[allLenth];
                    System.arraycopy(c1, 0, c, 0, l1);
                    System.arraycopy(c2, 0, c, 15, l2);
                    System.arraycopy(c3, 0, c, 31, l3);
                    System.arraycopy(c4, 0, c, 47, l4);
                    System.arraycopy(c5, 0, c, 63, l5);
                    System.arraycopy(c6, 0, c, 79, l6);

                } else {
                    return;
                }

                String realBarcode = ByteUtils.toAsciiString(c);
                Log.d("ZM", "realBarcode: " + realBarcode);
                intent.putExtra(TEST_DATA, realBarcode);
                sendBroadcast(intent);

            } else if (data.length == 19 && data[0] == (byte) 0xB7) {

                if (l7 == 0) {
                    Log.d("ZM", "l6: " + l6);
                    return;
                }

                c7 = new byte[l7];
                System.arraycopy(data, 1, c7, 0, l7);
                mData7 = true;
                Log.d("ZM", "mData7: " + mData7);

                if (mData1 && mData2 && mData3 && mData4 && mData5 && mData6 && mData7 && mData8) {
                    c = new byte[allLenth];
                    System.arraycopy(c1, 0, c, 0, l1);
                    System.arraycopy(c2, 0, c, 15, l2);
                    System.arraycopy(c3, 0, c, 31, l3);
                    System.arraycopy(c4, 0, c, 47, l4);
                    System.arraycopy(c5, 0, c, 63, l5);
                    System.arraycopy(c6, 0, c, 79, l6);
                    System.arraycopy(c7, 0, c, 95, l7);

                } else {
                    return;
                }

                String realBarcode = ByteUtils.toAsciiString(c);
                Log.d("ZM", "realBarcode: " + realBarcode);
                intent.putExtra(TEST_DATA, realBarcode);
                sendBroadcast(intent);

            } else if (data.length == 19 && data[0] == (byte) 0xB8) {

                if (l8 == 0) {
                    Log.d("ZM", "l7: " + l7);
                    return;
                }

                c8 = new byte[l8];
                System.arraycopy(data, 1, c8, 0, l8);
                mData8 = true;
                Log.d("ZM", "mData8: " + mData8);

                if (mData1 && mData2 && mData3 && mData4 && mData5 && mData6 && mData7 && mData8) {
                    c = new byte[allLenth];
                    System.arraycopy(c1, 0, c, 0, l1);
                    System.arraycopy(c2, 0, c, 15, l2);
                    System.arraycopy(c3, 0, c, 31, l3);
                    System.arraycopy(c4, 0, c, 47, l4);
                    System.arraycopy(c5, 0, c, 63, l5);
                    System.arraycopy(c6, 0, c, 79, l6);
                    System.arraycopy(c7, 0, c, 95, l7);
                    System.arraycopy(c8, 0, c, 111, l8);

                } else {
                    return;
                }

                String realBarcode = ByteUtils.toAsciiString(c);
                Log.d("ZM", "realBarcode: " + realBarcode);
                intent.putExtra(TEST_DATA, realBarcode);
                sendBroadcast(intent);

            } else if (data.length == 9 && data[0] == (byte) 0xAA) {
                //长宽高 b1b2长，b3b4宽，b5b6高
                String itemL = "";
                String itemW = "";
                String itemH = "";
                itemL = Objects.requireNonNull(result).substring(2, 6);
                itemW = result.substring(6, 10);
                itemH = result.substring(10, 14);

                itemL = String.valueOf((double) Integer.parseInt(itemL, 16) / 10);
                itemW = String.valueOf((double) Integer.parseInt(itemW, 16) / 10);
                itemH = String.valueOf((double) Integer.parseInt(itemH, 16) / 10);

                LWHData lwhData = new LWHData(itemL, itemW, itemH);
                intent.putExtra(NOTIFICATION_DATA_LWH, lwhData);
                sendBroadcast(intent);

            } else if(data.length == 10 && data[0] == (byte) 0xAA && data[1] == (byte) 0x0D){
                PK20Utils.analysisData(BluetoothLeService.this, intent, data);
            }
        }
    }

    private void getAllLenth() {
        if (allLenth > 111) {

            l1 = 15;
            l2 = 16;
            l3 = 16;
            l4 = 16;
            l5 = 16;
            l6 = 16;
            l7 = 16;
            l8 = allLenth - 111;

            mData1 = false;
            mData2 = false;
            mData3 = false;
            mData4 = false;
            mData5 = false;
            mData6 = false;
            mData7 = false;
            mData8 = false;

        } else if (allLenth > 95) {

            l1 = 15;
            l2 = 16;
            l3 = 16;
            l4 = 16;
            l5 = 16;
            l6 = 16;
            l7 = allLenth - 95;
            l8 = 0;

            mData1 = false;
            mData2 = false;
            mData3 = false;
            mData4 = false;
            mData5 = false;
            mData6 = false;
            mData7 = false;
            mData8 = true;

        } else if (allLenth > 79) {

            l1 = 15;
            l2 = 16;
            l3 = 16;
            l4 = 16;
            l5 = 16;
            l6 = allLenth - 79;
            l7 = 0;
            l8 = 0;

            mData1 = false;
            mData2 = false;
            mData3 = false;
            mData4 = false;
            mData5 = false;
            mData6 = false;
            mData7 = true;
            mData8 = true;

        } else if (allLenth > 63) {

            l1 = 15;
            l2 = 16;
            l3 = 16;
            l4 = 16;
            l5 = allLenth - 63;
            l6 = 0;
            l7 = 0;
            l8 = 0;

            mData1 = false;
            mData2 = false;
            mData3 = false;
            mData4 = false;
            mData5 = false;
            mData6 = true;
            mData7 = true;
            mData8 = true;

        } else if (allLenth > 47) {

            l1 = 15;
            l2 = 16;
            l3 = 16;
            l4 = allLenth - 47;
            l5 = 0;
            l6 = 0;
            l7 = 0;
            l8 = 0;

            mData1 = false;
            mData2 = false;
            mData3 = false;
            mData4 = false;
            mData5 = true;
            mData6 = true;
            mData7 = true;
            mData8 = true;

        } else if (allLenth > 31) {

            l1 = 15;
            l2 = 16;
            l3 = allLenth - 31;
            l4 = 0;
            l5 = 0;
            l6 = 0;
            l7 = 0;
            l8 = 0;

            mData1 = false;
            mData2 = false;
            mData3 = false;
            mData4 = true;
            mData5 = true;
            mData6 = true;
            mData7 = true;
            mData8 = true;

        } else if (allLenth > 15) {

            l1 = 15;
            l2 = allLenth - 15;
            l3 = 0;
            l4 = 0;
            l5 = 0;
            l6 = 0;
            l7 = 0;
            l8 = 0;

            mData1 = false;
            mData2 = false;
            mData3 = true;
            mData4 = true;
            mData5 = true;
            mData6 = true;
            mData7 = true;
            mData8 = true;

        } else {

            l1 = allLenth;
            l2 = 0;
            l3 = 0;
            l4 = 0;
            l5 = 0;
            l6 = 0;
            l7 = 0;
            l8 = 0;

            mData1 = false;
            mData2 = true;
            mData3 = true;
            mData4 = true;
            mData5 = true;
            mData6 = true;
            mData7 = true;
            mData8 = true;

        }
    }

    //发送信道3长宽高信息
    private void sendLWHData(Intent intent, byte[] data) {
        String itemW = "";
        String itemH = "";
        String itemL = "";
        String length = DataManageUtils.getLWH(data, DataManageUtils.L3);
        int l = 0;
        if (!"ffff".equals(length)) {
            l = Integer.parseInt(Objects.requireNonNull(length), 16);
            double result = (double) l / 10;
            itemL = result + "";
        }

        String wStr = DataManageUtils.getLWH(data, DataManageUtils.W3);
        int w = 0;
        if (!"ffff".equals(wStr)) {
            w = Integer.parseInt(Objects.requireNonNull(wStr), 16);
            double result = (double) w / 10;
            itemW = result + "";
        }

        String hStr = DataManageUtils.getLWH(data, DataManageUtils.H3);
        int h = 0;
        if (!"ffff".equals(hStr)) {
            h = Integer.parseInt(Objects.requireNonNull(hStr), 16);
            double result = (double) h / 10;
            itemH = result + "";
        }

        LWHData lwhData = new LWHData(itemL, itemW, itemH);
        intent.putExtra(NOTIFICATION_DATA_LWH, lwhData);
        sendBroadcast(intent);
    }


    class mThread extends Thread {
        List<byte[]> mByteNewList;
        BluetoothGattCharacteristic characteristic;
        Intent intent;

        public mThread(List<byte[]> mByteNewList, BluetoothGattCharacteristic characteristic, Intent intent) {
            this.mByteNewList = mByteNewList;
            this.characteristic = characteristic;
            this.intent = intent;
        }

        @Override
        public void run() {
            String expressCode = DataManageUtils.getExpressCode(mByteNewList.get(3), mByteNewList.get(4));
            String barCode = "";
            if (!"ffffffffffffffffffffffffffffffffffffffff".equals(expressCode)) {
                barCode = DataManageUtils.convertHexToString(Objects.requireNonNull(expressCode)).replace("\u0000", "");
            }

            String branchCode = DataManageUtils.getBranchCode(mByteNewList.get(0));
            String wangDian = "";
            if (!"ffffffffffffffffffff".equals(branchCode)) {
                wangDian = DataManageUtils.convertHexToString(Objects.requireNonNull(branchCode)).replace("\u0000", "");
            }

            String centerCode = DataManageUtils.getCenterCode(mByteNewList.get(0), mByteNewList.get(1));
            String center = "";
            if (!"ffffffffffffffffffff".equals(centerCode)) {
                center = DataManageUtils.convertHexToString(Objects.requireNonNull(centerCode)).replace("\u0000", "");
            }

            String muDiCode = DataManageUtils.getMuDiCode(mByteNewList.get(1));
            String muDi = "";
            if (!"ffffffffffffffffffff".equals(muDiCode)) {
                muDi = DataManageUtils.convertHexToString(Objects.requireNonNull(muDiCode)).replace("\u0000", "");
            }

            String use = DataManageUtils.getUse(mByteNewList.get(1));
            String liuCheng = DataManageUtils.convertHexToString(Objects.requireNonNull(use));

            String mac = DataManageUtils.getMac(mByteNewList.get(6), mByteNewList.get(7));
            String identify = DataManageUtils.getIdentify(mByteNewList.get(7));
            String itemL = "";
            String length = DataManageUtils.getLWHG(mByteNewList.get(2), DataManageUtils.L);
            int l = 0;
            if (!"ffff".equals(length)) {
                l = Integer.parseInt(Objects.requireNonNull(length), 16);
                double result = (double) l / 10;
                itemL = result + "";
            }

            String itemW = "";
            String itemH = "";
            String itemV = "";

            if ("00".equals(identify)) {
                //长宽高数据都有效(体积）
                String wStr = DataManageUtils.getLWHG(mByteNewList.get(2), DataManageUtils.W);
                int w = 0;
                if (!"ffff".equals(wStr)) {
                    w = Integer.parseInt(Objects.requireNonNull(wStr), 16);
                    double result = (double) w / 10;
                    itemW = result + "";
                }

                String hStr = DataManageUtils.getLWHG(mByteNewList.get(2), DataManageUtils.H);
                int h = 0;
                if (!"ffff".equals(hStr)) {
                    h = Integer.parseInt(Objects.requireNonNull(hStr), 16);
                    double result = (double) h / 10;
                    itemH = result + "";
                }

                String vStr = DataManageUtils.getV(mByteNewList.get(2));
                long v = 0;
                if (!"ffffffff".equals(vStr)) {
                    v = Long.parseLong(Objects.requireNonNull(vStr), 16);
                    double tijizhong = (double) v / 100;
                    itemV = tijizhong + "";
                }
            }

            String gStr = DataManageUtils.getLWHG(mByteNewList.get(2), DataManageUtils.G);
            String itemG = "";
            int g = 0;
            if (!"ffff".equals(gStr)) {
                g = Integer.parseInt(Objects.requireNonNull(gStr), 16);
                itemG = g + "";
            }

            String time = DataManageUtils.getTime(mByteNewList.get(2));
            String timeResult = "";
            if (!"ffffffffffff".equals(time)) {
                timeResult = "20" + Objects.requireNonNull(time).substring(10, 12) + "-"
                        + time.substring(8, 10) + "-"
                        + time.substring(6, 8) + " "
                        + time.substring(4, 6) + ":"
                        + time.substring(2, 4) + ":"
                        + time.substring(0, 2);
            }

            String mainCode = DataManageUtils.getMainCode(mByteNewList.get(4), mByteNewList.get(5));
            String zhu = "";
            if (!"ffffffffffffffffffffffffffffffffffffffff".equals(mainCode)) {
                zhu = DataManageUtils.convertHexToString(Objects.requireNonNull(mainCode)).replace("\u0000", "");
            }

            String sonCode = DataManageUtils.getSonCode(mByteNewList.get(5), mByteNewList.get(6));
            String zi = "";
            if (!"ffffffffffffffffffffffffffffffffffffffff".equals(sonCode)) {
                zi = DataManageUtils.convertHexToString(Objects.requireNonNull(sonCode)).replace("\u0000", "");
            }

            String flag = DataManageUtils.getFlag(mByteNewList.get(8));
            String biaoji = "";
            if (!"ff".equals(flag)) {
                biaoji = DataManageUtils.convertHexToString(Objects.requireNonNull(flag));
            }

            String name = DataManageUtils.getName(mByteNewList.get(7), mByteNewList.get(8));
            PK20Data mData = new PK20Data(barCode, wangDian, center, muDi, liuCheng, itemL, itemW, itemH, itemV
                    , itemG, timeResult, zhu, zi, mac, identify, biaoji, name);

            intent.putExtra(NOTIFICATION_DATA, mData);
            sendBroadcast(intent);
        }
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     * 连接到在蓝牙LE设备上托管的GATT服务器。
     *
     * @param address The device address of the destination device.目标设备的设备地址。
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.以前连接设备。尝试重新连接。
        if (address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.我们想要直接连接到设备上，所以我们设置了自动连接 参数为false。
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.在使用了一个可使用的设备之后，应用程序必须调用这个方法来确保资源的使用。
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     * 启用或禁用给定特性的通知。
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        boolean isEnableNotification = mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        if (isEnableNotification) {
            List<BluetoothGattDescriptor> descriptorList = characteristic.getDescriptors();
            if (descriptorList != null && descriptorList.size() > 0) {
                for (BluetoothGattDescriptor descriptor : descriptorList) {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    mBluetoothGatt.writeDescriptor(descriptor);
                }
            }
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     * 检索连接设备上支持的GATT服务的列表
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) {
            return null;
        }

        return mBluetoothGatt.getServices();
    }
}

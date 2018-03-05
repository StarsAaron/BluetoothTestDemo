package com.aaron.bluetoothdemo.ble;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.aaron.bluetoothdemo.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 5.0 新接口 startScan
 */
@SuppressLint("NewApi")
public class CenterActivity extends Activity {
    private static final String TAG = CenterActivity.class.getSimpleName();
    private static final long SCAN_PERIOD = 10000;                    // 10秒后停止查找搜索.
    private LeDeviceList mLeDeviceList = new LeDeviceList();          // 自定义一个用来存储搜索到的蓝牙设备的List
    private MyHandler mHandler= new MyHandler();                      // 搜索时用于延时的
    private boolean mScanning;                                        // 标记是否正在搜索
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;                       // 蓝牙适配器

    private TextView tv_dev;

    //静态内部类，防止内存溢出
    private static class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_center);

        initView();
        initBluetooth();
    }

    /**
     * 视图初始化
     */
    private void initView() {
        Button btnScan = (Button) findViewById(R.id.button1);
        btnScan.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                scanLeDevice(true);
            }
        });

        Button btnJump = (Button) findViewById(R.id.button2);
        btnJump.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                btn2_click();
            }
        });

        tv_dev = (TextView)findViewById(R.id.tv_dev);
    }

    /**
     * 初始化蓝牙环境
     */
    private void initBluetooth() {
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        // 或者
        // mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(!isSupportBle()){
            toast("不支持Ble");
            finish();
        }
    }

    /**
     * 是否支持蓝牙模块
     *
     * @return
     */
    public boolean isSupportBle() {
        // hasSystemFeature 需要<uses-feature android:name="android.hardware.bluetooth_le" android:required="true"/>
        // required 设置为true
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * 点击button将搜到的设备名和设备地址传送给DeviceControlActivity
     */
    void btn2_click() {

        final Intent intent = new Intent(this, DeviceControlActivity.class);

        //将所有搜索到的设备传输给DeviceControlActivity
        int num_device = mLeDeviceList.getCount();
        intent.putExtra("num_device", Integer.toString(num_device));
        for (int i = 0; i < num_device; i++) {
            final BluetoothDevice device = mLeDeviceList.getDevice(i);
            intent.putExtra("device_name: " + i, device.getName());
            intent.putExtra("device_address: " + i, device.getAddress());
        }

        if (mScanning) {
            stopScan();
            // 4.3(API 18) 蓝牙搜索使用下面
            // mBluetoothAdapter.stopLeScan(mLeScanCallback);
            // mScanning = false;
        }
        startActivity(intent);
    }

    /**
     * 用来搜索蓝牙设备的函数SCAN_PERIOD=10s周期
     * 信息接收在mLeScanCallback回调函数中
     *
     * @param enable
     */
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // 延时关闭蓝牙搜索，减少电量开销
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScan();
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);
            startScan();
        } else {
            stopScan();
        }
    }

    /**
     * 开始搜索Ble
     */
    private void startScan() {
        mScanning = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //5.0 之后使用android.bluetooth.le 包新接口
            BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            bluetoothLeScanner.startScan(scanCallback);
        }
        // 4.3(API 18) 蓝牙搜索使用下面
//        mBluetoothAdapter.startLeScan(mLeScanCallback);
    }

    /**
     * 带过滤的搜索
     */
    private void startScanWithFilters(ParcelUuid uuid) {
        // add a filter to only scan for advertisers with the given service UUID
        List<ScanFilter> bleScanFilters = new ArrayList<>();
        bleScanFilters.add(
                new ScanFilter.Builder().setServiceUuid(uuid).build()
        );

        BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        ScanSettings bleScanSettings = new ScanSettings.Builder().build();

        Log.d(TAG, "Starting scanning with settings:" + bleScanSettings + " and filters:" + bleScanFilters);

        // tell the BLE controller to initiate scan
        bluetoothLeScanner.startScan(bleScanFilters, bleScanSettings, scanCallback);

        // 5.0之前只能通过service UUID去搜索，其他条件都不行
        // public boolean startLeScan(final UUID[] serviceUuids, final LeScanCallback callback)
    }

    /**
     * 关闭搜索Ble
     */
    private void stopScan() {
        mScanning = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            bluetoothLeScanner.stopScan(scanCallback);
        }
        // mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        stopScan();
    }

    /**
     * 新添加的android.bluetooth.le 包新接口回调
     */
    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            super.onScanResult(callbackType, result);

            if (result == null || result.getDevice() == null
                    || TextUtils.isEmpty(result.getDevice().getName())) {
                Log.d("Main", "没有搜索到蓝牙设备");
                return;
            }

            BluetoothDevice device = result.getDevice();
            mLeDeviceList.addDevice(device);

            String temp = tv_dev.getText().toString();
            if(temp.equals("[log]")){
                tv_dev.setText(device.getName() + " " + device.getAddress()+"\n");
            }else{
                if(!temp.contains(device.getName())){//过滤重复的
                    tv_dev.setText(temp+"\n"+device.getName() + " " + device.getAddress());
                }
            }

            Log.d(TAG, "Device name: " + device.getName());
            Log.d(TAG, "Device address: " + device.getAddress());
            Log.d(TAG, "Device service UUIDs: " + device.getUuids());

            ScanRecord record = result.getScanRecord();
            Log.d(TAG, "Record advertise flags: 0x" + Integer.toHexString(record.getAdvertiseFlags()));
            Log.d(TAG, "Record Tx power level: " + record.getTxPowerLevel());
            Log.d(TAG, "Record device name: " + record.getDeviceName());
            Log.d(TAG, "Record service UUIDs: " + record.getServiceUuids());
            Log.d(TAG, "Record service data: " + record.getServiceData());

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                }
            });
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    /**
     * 通讯回调
     */
    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback(){
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                //broadcastUpdate(intentAction);//发送状态
                Log.d("连接", "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.d("", "Attempting to start service discovery:" );

                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("", "Disconnected from GATT server.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.e("discovered1", "onServicesDiscovered received: " +status);
            }else{
                //Log.e("discovered0", "onServicesDiscovered received: " +status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                byte[] data = characteristic.getValue();
                Log.e("onCharacteristicRead", "onCharacteristicRead received: " +
                        new String(data));
                //gatt.readCharacteristic(characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] data = characteristic.getValue();
            Log.e("onCharacteristicChanged", "onCharacteristicChangedreceived: " + new String(data));
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }
    };

    /**
     * 通讯
     * @param device
     */
    private void connect(BluetoothDevice device){
        BluetoothGatt bluetoothGatt = device.connectGatt(this,false,bluetoothGattCallback);
    }

    /**
     * 旧回调，参数scanRecord是数据的字节数组，不够直观，要手动解析数据
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLeDeviceList.addDevice(device);
                    toast(device.getName() + device.getAddress());
                    Log.i(TAG, device.getName() + device.getAddress());
                    //这里的参数 scanRecord 就是广播数据，这里同时包含 广播数据 和 扫描响应数据 （如果有的话），所以长度一般就是 62 字节。
                    parseData(scanRecord);
                }
            });
        }
    };

    // 自定义数据解析类
    class ParsedAd {
        byte flags;
        List<UUID> uuids;
        String localName;
        short manufacturer;
    }

    /**
     * 解析广播和响应数据
     *
     * @param adv_data
     * @return
     */
    public ParsedAd parseData(byte[] adv_data) {
        ParsedAd parsedAd = new ParsedAd();
        ByteBuffer buffer = ByteBuffer.wrap(adv_data).order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.remaining() > 2) {
            byte length = buffer.get();
            if (length == 0)
                break;
            byte type = buffer.get();
            length -= 1;
            switch (type) {
                case 0x01: // Flags
                    parsedAd.flags = buffer.get();
                    length--;
                    break;
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                case 0x14: // List of 16-bit Service Solicitation UUIDs
                    while (length >= 2) {
                        parsedAd.uuids.add(UUID.fromString(String.format(
                                "%08x-0000-1000-8000-00805f9b34fb", buffer.getShort())));
                        length -= 2;
                    }
                    break;
                case 0x04: // Partial list of 32 bit service UUIDs
                case 0x05: // Complete list of 32 bit service UUIDs
                    while (length >= 4) {
                        parsedAd.uuids.add(UUID.fromString(String.format(
                                "%08x-0000-1000-8000-00805f9b34fb", buffer.getInt())));
                        length -= 4;
                    }
                    break;
                case 0x06: // Partial list of 128-bit UUIDs
                case 0x07: // Complete list of 128-bit UUIDs
                case 0x15: // List of 128-bit Service Solicitation UUIDs
                    while (length >= 16) {
                        long lsb = buffer.getLong();
                        long msb = buffer.getLong();
                        parsedAd.uuids.add(new UUID(msb, lsb));
                        length -= 16;
                    }
                    break;
                case 0x08: // Short local device name
                case 0x09: // Complete local device name
                    byte sb[] = new byte[length];
                    buffer.get(sb, 0, length);
                    length = 0;
                    parsedAd.localName = new String(sb).trim();
                    break;
                case (byte) 0xFF: // Manufacturer Specific Data
                    parsedAd.manufacturer = buffer.getShort();
                    length -= 2;
                    break;
                default: // skip
                    break;
            }
            if (length > 0) {
                buffer.position(buffer.position() + length);
            }
        }
        return parsedAd;
    }

    /**
     * @author LiTao
     *         自定义的一个用来存储搜索到的蓝牙设备的一个类
     *         Adapter for holding devices found through scanning.
     */
    private class LeDeviceList {
        private ArrayList<BluetoothDevice> mLeDevices;

        public LeDeviceList() {
            mLeDevices = new ArrayList<BluetoothDevice>();
        }

        public void addDevice(BluetoothDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
                Log.i(TAG, device.getName() + " " + device.getAddress());
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        public int getCount() {
            return mLeDevices.size();
        }
    }

    /**
     * 提示Toast
     * @param msg
     */
    private void toast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}

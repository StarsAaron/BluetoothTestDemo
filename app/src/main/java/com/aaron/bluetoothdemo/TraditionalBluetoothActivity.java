package com.aaron.bluetoothdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

/**
 * 蓝牙使用例子
 * 传统蓝牙startDiscovery方式
 */
public class TraditionalBluetoothActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private TextView tv_bondeddevices,tv_devices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_traditional);

        tv_bondeddevices = (TextView)findViewById(R.id.tv_bondeddevices);
        tv_devices = (TextView)findViewById(R.id.tv_devices);

        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // 设置广播信息过滤
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        // 注册广播接收器，接收并处理搜索结果
        registerReceiver(receiver, intentFilter);

        Button btn_search = (Button)findViewById(R.id.btn_search);
        btn_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScan();
            }
        });

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "不支持蓝牙", Toast.LENGTH_SHORT).show();
            finish();
        }

        getBondedDevices();
    }

    /**
     * 获取之前匹配过的设备信息
     */
    private void getBondedDevices() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            StringBuilder builder = new StringBuilder();
            for (BluetoothDevice device : pairedDevices) {
                builder.append(device.getName() + " " + device.getAddress()+"\n");
                Log.i("BlueTooth--", device.getName() + " " + device.getAddress());
            }
            tv_bondeddevices.setText(builder.toString());
        } else {
            tv_bondeddevices.setText("");
            Toast.makeText(this, "没有找到已匹对的设备", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * 开始扫描
     */
    private void startScan() {
        if (mBluetoothAdapter.isDiscovering()) {
            return;
        }
        // 寻找蓝牙设备，android会将查找到的设备以广播形式发出去
        // startDiscovery 方法可以查找到传统蓝牙和Ble设备但是startDiscovery的回调无法返回
        // Ble的广播，所以无法通过广播识别设备，且startDiscovery扫描Ble的效率比StartLeScan低很多。
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetoothAdapter.cancelDiscovery();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(TraditionalBluetoothActivity.this, "搜索完成", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        },10000);
        mBluetoothAdapter.startDiscovery();
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String temp = tv_devices.getText().toString();
                if(temp.equals("[log]")){
                    tv_devices.setText(device.getName() + " " + device.getAddress()+"\n");
                }else{
                    if(!temp.contains(device.getName())){//过滤重复的
                        tv_devices.setText(temp+"\n"+device.getName() + " " + device.getAddress());
                    }
                }
                Log.i("BroadcastReceiver--", device.getName());
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

}

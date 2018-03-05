package com.aaron.bluetoothdemo.ble;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;

import com.aaron.bluetoothdemo.R;

public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    //保存从MainActivity中传来的设备信息
    private int num_device;
    private String[] mDeviceName = new String[8]; // 8 slaver;
    private String[] mDeviceAddress = new String[8];

    private BluetoothLeService mBluetoothLeService;//新建一个BluetoothLeService蓝牙服务的类
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();//用来存储所有的服务

    private TextView tv_cha;

    /**
     * Code to manage Service lifecycle.
     * 和蓝牙服务建立连接对象（相当于回调函数，在onCreate最后调用bind函数建立
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {//初始化该对象
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress, num_device);//调用连接函数连接远程设备
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     * 初始化接收从MainActivity传来的设备信息并保存，同时调用
     * bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);建立一个和蓝牙服务之间的连接，
     * 其中mServiceConnection类似于回调函数（所以onCreate后要看这个回调函数）
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_characteristics);

        tv_cha = (TextView)findViewById(R.id.tv_cha);

        final Intent intent = getIntent();//从MainActivity中获得intent消息（设备信息,并将其保存在device_name和device_address中
        num_device = Integer.parseInt(intent.getStringExtra("num_device"));
        Log.i(TAG, "++onCreate++");
        Log.i(TAG, Integer.toString(num_device));
        for (int i = 0; i < num_device; i++) {
            mDeviceName[i] = intent.getStringExtra("device_name: " + i);
            mDeviceAddress[i] = intent.getStringExtra("device_address: " + i);
            Log.i(TAG, mDeviceName[i] + " " + mDeviceAddress[i]);
        }

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        registerReceiver(mGattUpdateReceiver,intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mGattUpdateReceiver);
        unbindService(mServiceConnection);
    }

    /**
     * 上面讲了函数从oncreate的bind建立和蓝牙server之间的连接，
     * 然后由类似于回调函数的mServiceConnection调用connnect函数进行连接远程设备
     * 在蓝牙服务文件中当对每一个设备进行mBluetoothGatt[i] = device[i].connectGatt(this, false, mGattCallback);时会产生一个mGattCallback回调函数
     * 该函数通过广播将一些消息广播出来，下面是接收广播的函数：
     * Handles various events fired by the Service.
     * ACTION_GATT_CONNECTED: connected to a GATT server.
     * ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
     * ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
     * ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
     * or notification operations.
     */
    private int xxxx = 0;
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {//连接
                Log.i(TAG, "ACTION_GATT_CONNECTED");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {//断开
                Log.i(TAG, "ACTION_GATT_DISCONNECTED");
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {//GATT服务搜索完毕
                // Show all the supported services and characteristics on the user interface.
                Log.i(TAG, "ACTION_GATT_SERVICES_DISCOVERED");
                displayGattServices(mBluetoothLeService.getSupportedGattServices((xxxx++) % 3));
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {//有数据传送过来！！！
                Log.i(TAG, "ACTION_DATA_AVAILABLE: " + intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };


    //获取每个设备的所有服务的characteristic保存在mGattCharacteristics
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();//保存所有的characteristic（直接保存characteristic）用于系统调用

        StringBuilder builder = new StringBuilder(tv_cha.getText().toString());
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {//遍历gattServices中的每一个gattService（即遍历每个服务）
            List<BluetoothGattCharacteristic> gattCharacteristics =//获取当前一个服务的所有的characteristic
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =//[保存每个服务的characteristic]
                    new ArrayList<BluetoothGattCharacteristic>();

            builder.append("\n Service UUID:"+gattService.getUuid()+"\n Characteristic size："
                    +gattCharacteristics.size()+"\n Characteristic:");

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {//对每个服务的characteristic进行遍历
                charas.add(gattCharacteristic);
                builder.append(gattCharacteristic.getUuid()+"\n");
            }
            tv_cha.setText(builder.toString());
            mGattCharacteristics.add(charas);//这里添加！！！
        }
    }

/*
  输出：
CenterActivity: Note6 63:C1:EE:B5:1B:08
CenterActivity: Device name: Note6
CenterActivity: Device address: 63:C1:EE:B5:1B:08
CenterActivity: Device service UUIDs: null
CenterActivity: Record advertise flags: 0x1a
CenterActivity: Record Tx power level: -7
CenterActivity: Record device name: Note6
CenterActivity: Record service UUIDs: [0000fff0-0000-1000-8000-00805f9b34fb]
CenterActivity: Record service data: {}
CenterActivity: Device name: Note6
CenterActivity: Device address: 63:C1:EE:B5:1B:08
CenterActivity: Device service UUIDs: null
CenterActivity: Record advertise flags: 0x1a
CenterActivity: Record Tx power level: -7
CenterActivity: Record device name: Note6
CenterActivity: Record service UUIDs: [0000fff0-0000-1000-8000-00805f9b34fb]
CenterActivity: Record service data: {}

I/DeviceControlActivity: ++onCreate++
I/DeviceControlActivity: 1
I/DeviceControlActivity: Note6 63:C1:EE:B5:1B:08
D/BluetoothGatt: connect() - device: 63:C1:EE:B5:1B:08, auto: false
D/BluetoothGatt: registerApp()
D/BluetoothGatt: registerApp() - UUID=dd3c808d-f644-414f-ba89-fcb4c37f8e0b
D/BluetoothGatt: onClientRegistered() - status=0 clientIf=5
D/BluetoothLeService: Trying to create a new connection.
I/Choreographer: Skipped 121 frames!  The application may be doing too much work on its main thread.
D/BluetoothGatt: onClientConnectionState() - status=0 clientIf=5 device=63:C1:EE:B5:1B:08
I/DeviceControlActivity: ACTION_GATT_CONNECTED
I/BluetoothLeService: Connected to GATT server.
D/BluetoothGatt: discoverServices() - device: 63:C1:EE:B5:1B:08
I/BluetoothLeService: Attempting to start service discovery: 0 true
D/BluetoothGatt: discoverServices() - device: 63:C1:EE:B5:1B:08
D/BluetoothGatt: onSearchComplete() = Device=63:C1:EE:B5:1B:08 Status=0
I/DeviceControlActivity: ACTION_GATT_SERVICES_DISCOVERED
I/BluetoothLeService: getSupportedGattServices: 0
*/

}

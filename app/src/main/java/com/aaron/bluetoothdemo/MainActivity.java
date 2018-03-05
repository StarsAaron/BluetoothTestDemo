package com.aaron.bluetoothdemo;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.aaron.bluetoothdemo.ble.CenterActivity;
import com.aaron.bluetoothdemo.ble.PeripheralActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * 主界面
 * (1) 传统蓝牙使用方式
 *
 * (2) BLE蓝牙的两种方式：
 *  (a)中心设备模式
 *  (b)外围设备模式
 *
 *  内容：
 *  主要是进行权限申请
 */
public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 0x111;
    private static final int REQUEST_CODE_PERMISSION_LOCATION = 2;
    private static final int REQUEST_CODE_OPEN_GPS = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager bluetoothManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);

        initView();

        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        //1.检查是否支持Ble蓝牙
        if (mBluetoothAdapter != null) {
            //2.检查蓝牙是否开启
            // 为了确保设备上蓝牙能使用, 如果当前蓝牙设备没启用,弹出对话框向用户要求授予权限来启用
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }

            //3.定位权限检查
            // 6.0 以上动态申请定位权限，有的手机需要开启定位权限才能使用蓝牙
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android M Permission check
                checkPermissions();
            }
        } else {
            //不支持蓝牙模块处理
            Toast.makeText(this, "不支持蓝牙", Toast.LENGTH_SHORT).show();
        }
    }

    private void initView(){
        Button btn_traditional = (Button)findViewById(R.id.btn_traditional);
        Button btn_center = (Button)findViewById(R.id.btn_center);
        Button btn_peripheral = (Button)findViewById(R.id.btn_peripheral);

        btn_traditional.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goActivity(TraditionalBluetoothActivity.class);
            }
        });

        btn_center.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goActivity(CenterActivity.class);
            }
        });

        btn_peripheral.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goActivity(PeripheralActivity.class);
            }
        });
    }

    /**
     * 使本机蓝牙在300秒内可被搜索
     */
    public static void ensureDiscoverable(Context context) {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            context.startActivity(discoverableIntent);
        }
    }

    /**
     * 静默打开蓝牙
     *
     * @return
     */
    private boolean autoOpenBlueTooth() {
        return mBluetoothAdapter.enable();
    }

    /**
     * 静默关闭蓝牙
     *
     * @return
     */
    private boolean autoCloseBlueTooth() {
        return mBluetoothAdapter.disable();
    }

    private void goActivity(Class name){
        Intent intent = new Intent(this,name);
        startActivity(intent);
    }

    @Override
    public final void onRequestPermissionsResult(int requestCode,
                                                 @NonNull String[] permissions,
                                                 @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION_LOCATION:
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            // 定位权限申请成功
                            toast("定位权限申请成功");
                            onPermissionGranted(permissions[i]);
                        }
                    }
                }
                break;
        }
    }

    private void checkPermissions() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
        List<String> permissionDeniedList = new ArrayList<>();
        for (String permission : permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted(permission);
            } else {
                permissionDeniedList.add(permission);
            }
        }
        if (!permissionDeniedList.isEmpty()) {
            String[] deniedPermissions = permissionDeniedList.toArray(new String[permissionDeniedList.size()]);
            ActivityCompat.requestPermissions(this, deniedPermissions, REQUEST_CODE_PERMISSION_LOCATION);
        }
    }

    private void onPermissionGranted(String permission) {
        switch (permission) {
            case Manifest.permission.ACCESS_FINE_LOCATION:
                if (!checkGPSIsOpen()) {
                    new AlertDialog.Builder(this)
                            .setTitle("提示")
                            .setMessage("需要开启定位")
                            .setNegativeButton("取消",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                        }
                                    })
                            .setPositiveButton("去设置",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                            startActivityForResult(intent, REQUEST_CODE_OPEN_GPS);
                                        }
                                    })

                            .setCancelable(false)
                            .show();
                }
                break;
        }
    }

    /**
     * 检查定位是否开启
     * @return
     */
    private boolean checkGPSIsOpen() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null)
            return false;
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN_GPS) {
            if (checkGPSIsOpen()) {
                // 打开了定位
                toast("打开了定位成功");
            }
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

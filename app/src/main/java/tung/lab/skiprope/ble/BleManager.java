package tung.lab.skiprope.ble;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.text.TextUtils;

/**
 * Created by Administrator on 2017/4/11.
 */

public class BleManager {
    private static BleManager ourInstance;
    private String address;
    private BleService bleService;
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {// TODO Auto-generated method stub
            bleService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bleService = ((BleService.LocalBinder) service).getService();
            if(!TextUtils.isEmpty(address)){
                bleService.initBluetoothDevice(address,context);
            }
        }
    };
    private Intent serviceIntent;
    BluetoothAdapter bluetoothAdapter;
    Context context;

    private BleManager(Context context) {
        this.context = context;
        if (serviceIntent == null) {
            serviceIntent = new Intent(context, BleService.class);
            context.bindService(serviceIntent, serviceConnection,
                    Service.BIND_AUTO_CREATE);
        }
        BluetoothManager bluetoothManager = (BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter=bluetoothManager.getAdapter();
    }

    public static void init(Context context){
        if (ourInstance == null) {
            synchronized (BleManager.class) {
                if (ourInstance == null) {
                    ourInstance = new BleManager(context);
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    public  boolean isBleEnable(){
        return bluetoothAdapter.enable();
    }
    public static BleManager getInstance() {
        return ourInstance;
    }

    public void connectDevice(String address){
        if(!bluetoothAdapter.isEnabled()|| TextUtils.isEmpty(address)||isConnected())return;

        if(bleService==null){
            this.address=address;
        }else{
            bleService.initBluetoothDevice(address,this.context);
        }
    }



    public void enableNotifaction(){
        if(bleService==null)return;
        bleService.setCharacteristicNotification(true);
    }

    public void writeValue(byte[]value){
        if(bleService==null||ourInstance==null||!isConnected())return;
        bleService.writeValue(value);
    }

    public void offerValue(byte[]data){
        if(bleService==null)return;
        bleService.offerValue(data);
    }

    public void writeValue(){
        if(bleService==null)return;
        bleService.nextQueue();
    }
    public void disconnectDevice(){
        if(bleService==null)return;
        bleService.disconnect();
    }

    public boolean isConnected(){
        if(bleService==null)return false;
        return bleService.isConnected();
    }
}

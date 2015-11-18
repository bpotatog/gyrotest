package com.example.kj.gyrotest;

import java.util.*;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothAdapter;
/**
 * Created by kj on 15/10/30.
 */
public class BluetoothClient extends Thread{
    private BluetoothAdapter myBluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices;
    public BluetoothClient(){
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        pairedDevices = myBluetoothAdapter.getBondedDevices();
    }
}

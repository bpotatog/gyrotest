package com.example.kj.gyrotest;

import com.example.kj.*;
import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.content.Intent;
import android.bluetooth.*;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.os.Handler;
import java.math.*;


public class MainActivity extends Activity implements SensorEventListener{
    private final String KJ = "kj says: ";
    private final String deviceBT = "bikeBT";
    private final float Gvalue = 9.8f;

    private float currentYvalue = 0;
    private float rectifyYvalue;
    private OutputStream mOutStream = null;
    private InputStream mInStream = null;
    private Handler mainHandler;

    private SensorManager mySensorManager;
    private Sensor mySensor;
    private TextView t;
    private TextView t1;
    private TextView t2;
    private TextView t6;
    private Button frontUp, frontDown, rearUp, rearDown, setSlope, resetBtn;
    private final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter myBluetoothAdapter;
    private BluetoothDevice mDevice;
    protected List<Sensor> sensors;
    private Button connectBtn;
    // serial port service class uuid
    private UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private String mac;
    private BluetoothSocket mSocket = null;
    private mOnClickListener mOnClickListener = new mOnClickListener();
    private Toast toast;
    private connectedThread cThread;
//    private connectThread myConnectThread;
//    private connectedThread myConnectedThread;

    private Set<BluetoothDevice> pairedDevices;
    private double defaultAngle = 0;
    DecimalFormat df = new DecimalFormat("##.##");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        t = (TextView) this.findViewById(R.id.textView);
        t1 = (TextView) this.findViewById(R.id.textView1);
//        t2 = (TextView) this.findViewById(R.id.textView2);
        t6 = (TextView) this.findViewById(R.id.textView6);
        frontUp = (Button) this.findViewById(R.id.frontUp);
        frontDown = (Button) this.findViewById(R.id.frontDown);
        rearUp = (Button) this.findViewById(R.id.rearUp);
        rearDown = (Button) this.findViewById(R.id.rearDown);
        setSlope = (Button) this.findViewById(R.id.setSlope);
        resetBtn = (Button) this.findViewById(R.id.resetBtn);

        mySensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        connectBtn = (Button) this.findViewById(R.id.connectBtn);
        df.setRoundingMode(RoundingMode.CEILING);

        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        connectBtn.setOnClickListener(mOnClickListener);
        frontUp.setOnClickListener(mOnClickListener);
        frontDown.setOnClickListener(mOnClickListener);
        rearUp.setOnClickListener(mOnClickListener);
        rearDown.setOnClickListener(mOnClickListener);
        setSlope.setOnClickListener(mOnClickListener);
        resetBtn.setOnClickListener(mOnClickListener);

        mainHandler = new Handler(){
            public void handleMessage(Message msg){
//                Log.wtf(KJ, String.valueOf(msg.getData().get("getData")));
//                t6.append(String.valueOf(msg.getData().get("getData")));
                switch(msg.what){
                    case 1:
                        byte[] readbuf = (byte[]) msg.obj;
                        String readmsg = new String(readbuf, 0, msg.arg1);
                        Log.d(KJ, readmsg);
//                        t6.append(String.valueOf(temp));
                        break;
                }
            }
        };
//        String state = Environment.getExternalStorageState();
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "kj");
        if(dir.mkdirs()){
            File f = new File(dir.getPath()+"/data1.txt");
            try{
                FileWriter fw = new FileWriter(f);
                fw.write("hello moto\n");
                fw.write("GG");
                fw.write("QQ");
                fw.flush();
                fw.close();
            }catch(IOException e){
                Log.wtf(KJ, "can't create file writer");
            }
        }
//        Calendar c = Calendar.getInstance();
//        Log.wtf(KJ,String.valueOf(c.getTime()));
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent intent){
        if(resultCode == Activity.RESULT_OK){
            Log.wtf(KJ, "wtf");
            if(connect()) {
                connectBtn.setText("Close");
            }
        }
        else{
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume(){
        super.onResume();
        sensors = mySensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if(sensors.size() > 0){
            mySensor = sensors.get(0);
            mySensorManager.registerListener(this, mySensor, mySensorManager.SENSOR_DELAY_NORMAL);
        }
//        t6.append("On resume");
//        t6.append(myBluetoothAdapter.getAddress());
        if(mSocket == null && connectBtn.getText().equals("Close")){
            if(!connect()){
                connectBtn.setText("Connect");
            }
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (mOutStream != null) {
            try {
                mOutStream.flush();
                mOutStream = null;
            } catch (IOException e) {
                Log.d(KJ, "can't flush output stream, shit!!", e);
            }
        }
        if (mSocket != null) {
            try {
                mSocket.close();
                mSocket = null;
            } catch (IOException e) {
                Log.d(KJ, "can't close socket", e);
            }
        }
    }
    @Override
    protected void onStop(){
        super.onStop();
        if (mOutStream != null) {
            try {
                mOutStream.flush();
                mOutStream = null;
            } catch (IOException e) {
                Log.d(KJ, "can't flush output stream, shit!!", e);
            }
        }
        if (mSocket != null) {
            try {
                mSocket.close();
                mSocket = null;
            } catch (IOException e) {
                Log.d(KJ, "can't close socket", e);
            }
        }
    }
    // to string method String.valueOf()
    public void onSensorChanged(SensorEvent event){
        float[] values = event.values;
        currentYvalue = values[1];
//        t.setText(String.valueOf(event.values.length));
//        t.setText(String.valueOf(df.format(values[0])));
        double rectifyAngle = 0;
        if(setSlope.getVisibility() == View.INVISIBLE){
            rectifyAngle = Math.toDegrees(Math.acos(rectifyYvalue / Gvalue));
        }
        double angle = 0.0;
        double cosValue = values[1] / Gvalue;
        angle = Math.toDegrees(Math.acos(cosValue));
        if(angle <= rectifyAngle && setSlope.getVisibility() == View.INVISIBLE) {
            t1.setText("+"+String.valueOf(df.format(rectifyAngle - angle))+"˚");
        }
        else if(angle > rectifyAngle && setSlope.getVisibility() == View.INVISIBLE){
            t1.setText("-"+String.valueOf(df.format(angle - rectifyAngle))+"˚");
        }
//        t2.setText(String.valueOf(df.format(values[2])));
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy){

    }

    public boolean connect(){
        pairedDevices = myBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            Iterator<BluetoothDevice> ite = pairedDevices.iterator();
            while(ite.hasNext()) {
                BluetoothDevice btd = ite.next();
//                t6.append(btd.getName()+","+btd.getAddress()+"\n");
                if(btd.getName().equals(deviceBT)){
                    mac = btd.getAddress();
                    mDevice = btd;
                    break;
                }
            }
            try{
                mSocket = mDevice.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e){
                Log.d(KJ, "can't create socket", e);
            }
            myBluetoothAdapter.cancelDiscovery();
            try{
                mSocket.connect();
            }catch (IOException e){
                Log.wtf(KJ, "kill me");
                try{
                    mSocket.close();
                }catch(IOException e2){
                    Log.d(KJ, "can't close socket", e);
                }
                return false;
            }
            try{
                mOutStream = mSocket.getOutputStream();
            } catch (IOException e){
                Log.d(KJ, "can't create outputstream", e);
            }
            // I Got U
            String msg = "IGU";
            byte[] msgbuf = msg.getBytes();
            try{
                mOutStream.write(msgbuf);
            } catch (IOException e){
                Log.d(KJ, "test failed.....", e);
                return false;
            }
            cThread = new connectedThread(mSocket);
            cThread.start();
            return true;
        }
        else {
            Log.wtf(KJ, "NO paired DEVICES");
            return false;
        }
    }
    public class mOnClickListener implements View.OnClickListener{
        @Override
        public void onClick(View v){
            if(v.getId() == R.id.connectBtn){
                if(!myBluetoothAdapter.isEnabled()){
                    Intent openBluetooth  = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(openBluetooth, REQUEST_ENABLE_BT);
                }
                else{
                    if(mSocket != null && connectBtn.getText().equals("Close")){
                        try{
                            mSocket.close();
                        } catch(IOException e){
                            Log.wtf(KJ, "can't close socket");
                        }
                        mSocket = null;
                        connectBtn.setText("Connect");
                    }
                    else if(mSocket == null && connectBtn.getText().equals("Connect")){
                        if(connect()) {
                            connectBtn.setText("Close");
                        }
                        else{
                            toast.makeText(getApplicationContext(), "can't connect device", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
            else if(v.getId() == R.id.frontUp && connectBtn.getText().equals("Close")){
                String msg = "0up";
                byte[] msgbuf = msg.getBytes();
                try{
                    mOutStream.write(msgbuf);
                } catch (IOException e){

                }
            }
            else if(v.getId() == R.id.frontDown && connectBtn.getText().equals("Close")){
                String msg = "0dn";
                byte[] msgbuf = msg.getBytes();
                try{
                    mOutStream.write(msgbuf);
                } catch (IOException e){

                }
            }else if(v.getId() == R.id.rearUp && connectBtn.getText().equals("Close")){
                String msg = "1up";
                byte[] msgbuf = msg.getBytes();
                try{
                    mOutStream.write(msgbuf);
                } catch (IOException e){

                }
            }
            else if(v.getId() == R.id.rearDown && connectBtn.getText().equals("Close")){
                String msg = "1dn";
                byte[] msgbuf = msg.getBytes();
                try{
                    mOutStream.write(msgbuf);
                } catch (IOException e){

                }
            }
            else if(v.getId() == R.id.setSlope){
//                Log.wtf(KJ, "check set slope button");
                rectifyYvalue = currentYvalue;
                setSlope.setVisibility(View.INVISIBLE);
                resetBtn.setVisibility(View.VISIBLE);
            }
            else if(v.getId() == R.id.resetBtn) {
//                Log.wtf(KJ, "check reset button");
                t1.setText("not set yet");
                setSlope.setVisibility(View.VISIBLE);
                resetBtn.setVisibility(View.INVISIBLE);
            }
        }
    }
    private class connectedThread extends Thread{
        private BluetoothSocket mSocket;
        public connectedThread(BluetoothSocket socket){
            Log.wtf(KJ, "creating thread");
//            mSocket = socket;
            InputStream mTempIn = null;
            try{
                mTempIn = socket.getInputStream();
            } catch(IOException e){
                Log.wtf(KJ, "can't get the Input Stream , damn");
            }
            mInStream = mTempIn;
        }
        public void run(){
            Log.wtf(KJ, "starting connected thread");
            byte[] buffer = new byte[64];
            int bytes;
            while(true) {
                try {
                    bytes = mInStream.read(buffer);
                    Log.wtf(KJ, "Got some msg");
                    Log.wtf(KJ, String.valueOf(bytes));
                    mainHandler.obtainMessage(1, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    Log.wtf(KJ, "disconnected");
                    break;
                }
            }
        }
        public void cancel(){
            try{
                mSocket.close();
            }catch(IOException e){
                Log.wtf(KJ, "thread can't close thread");
            }
        }
    }
}

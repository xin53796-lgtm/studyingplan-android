package com.studyplanner.fish;
import android.os.Bundle;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.webkit.JavascriptInterface;
import com.getcapacitor.BridgeActivity;
public class MainActivity extends BridgeActivity implements SensorEventListener {
 private SensorManager sensors; private Sensor accelerometer; private boolean requested=false, foreground=false, listening=false;
 @Override public void onCreate(Bundle state){ registerPlugin(CalendarSync.class); super.onCreate(state); sensors=(SensorManager)getSystemService(SENSOR_SERVICE); accelerometer=sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); getBridge().getWebView().addJavascriptInterface(new MotionBridge(),"StudyMotion"); }
 public class MotionBridge { @JavascriptInterface public void setEnabled(boolean enabled){runOnUiThread(()->{requested=enabled;updateMotion();});} }
 private void updateMotion(){if(sensors==null)return;boolean next=requested&&foreground&&accelerometer!=null;if(next==listening)return;if(next)listening=sensors.registerListener(this,accelerometer,SensorManager.SENSOR_DELAY_GAME);else {sensors.unregisterListener(this);listening=false;}}
 @Override public void onResume(){super.onResume();foreground=true;updateMotion();}
 @Override public void onPause(){foreground=false;updateMotion();super.onPause();}
 @Override public void onDestroy(){foreground=false;updateMotion();super.onDestroy();}
 @Override public void onAccuracyChanged(Sensor sensor,int accuracy){}
 @Override public void onSensorChanged(SensorEvent e){if(!listening)return;float x=e.values[0],y=e.values[1];if(!Float.isFinite(x)||!Float.isFinite(y))return;getBridge().getWebView().evaluateJavascript("window.studyMotionSample && window.studyMotionSample("+x+","+y+")",null);}
}

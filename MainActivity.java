package com.studyplanner.fish;
import android.os.Bundle;
import android.app.*;
import android.content.*;
import android.os.*;
import org.json.*;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.webkit.JavascriptInterface;
import com.getcapacitor.BridgeActivity;
public class MainActivity extends BridgeActivity implements SensorEventListener {
 private SensorManager sensors; private Sensor accelerometer; private boolean requested=false, foreground=false, listening=false;
 @Override public void onCreate(Bundle state){ registerPlugin(CalendarSync.class); super.onCreate(state); sensors=(SensorManager)getSystemService(SENSOR_SERVICE); accelerometer=sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); getBridge().getWebView().addJavascriptInterface(new MotionBridge(),"StudyMotion"); getBridge().getWebView().addJavascriptInterface(new TimerBridge(),"StudyTimerNative"); }
 public class MotionBridge { @JavascriptInterface public void setEnabled(boolean enabled){runOnUiThread(()->{requested=enabled;updateMotion();});} }
 private void updateMotion(){if(sensors==null)return;boolean next=requested&&foreground&&accelerometer!=null;if(next==listening)return;if(next)listening=sensors.registerListener(this,accelerometer,SensorManager.SENSOR_DELAY_GAME);else {sensors.unregisterListener(this);listening=false;}}
 @Override public void onResume(){super.onResume();foreground=true;updateMotion();}
 @Override public void onPause(){foreground=false;updateMotion();super.onPause();}
 @Override public void onDestroy(){foreground=false;updateMotion();super.onDestroy();}
 @Override public void onAccuracyChanged(Sensor sensor,int accuracy){}
 @Override public void onSensorChanged(SensorEvent e){if(!listening)return;float x=e.values[0],y=e.values[1];if(!Float.isFinite(x)||!Float.isFinite(y))return;getBridge().getWebView().evaluateJavascript("window.studyMotionSample && window.studyMotionSample("+x+","+y+")",null);}

 public class TimerBridge {
  @JavascriptInterface public void setState(String value){runOnUiThread(()->{if(android.os.Build.VERSION.SDK_INT>=33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED && !value.equals("null"))requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},321); TimerService.set(MainActivity.this,value);});}
  @JavascriptInterface public String snapshot(){return TimerService.snapshot(MainActivity.this);}
  @JavascriptInterface public void ack(String ids){TimerService.ack(MainActivity.this,ids);}
 }
 public static class TimerService extends Service {
  static final String PREF="study_timer_native", RUN="study_running", DONE="study_done";
  static final int ID=2301; final Handler h=new Handler(Looper.getMainLooper());
  static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences(PREF,0);}
  static JSONObject active(Context c){try{return new JSONObject(prefs(c).getString("active","null"));}catch(Exception e){return null;}}
  static JSONArray records(Context c){try{return new JSONArray(prefs(c).getString("records","[]"));}catch(Exception e){return new JSONArray();}}
  static synchronized String snapshot(Context c){try{return new JSONObject().put("active",active(c)==null?JSONObject.NULL:active(c)).put("records",records(c)).toString();}catch(Exception e){return "{}";}}
  static synchronized void ack(Context c,String ids){try{JSONArray a=new JSONArray(ids),r=records(c),keep=new JSONArray();for(int i=0;i<r.length();i++){JSONObject x=r.getJSONObject(i);boolean found=false;for(int j=0;j<a.length();j++)if(x.optString("id").equals(a.optString(j)))found=true;if(!found)keep.put(x);}prefs(c).edit().putString("records",keep.toString()).commit();}catch(Exception e){}}
  static void set(Context c,String value){prefs(c).edit().putString("active",value).commit();Intent i=new Intent(c,TimerService.class);if(value.equals("null")){c.stopService(i);return;}if(Build.VERSION.SDK_INT>=26)c.startForegroundService(i);else c.startService(i);}
  static long elapsed(JSONObject a){long since=a.optLong("since");return a.optLong("elapsed")+(since>0?Math.max(0,System.currentTimeMillis()-since):0);}
  static String fmt(long ms){long s=Math.max(0,ms/1000);return String.format(java.util.Locale.US,"%02d:%02d:%02d",s/3600,s/60%60,s%60);}
  NotificationManager nm(){return getSystemService(NotificationManager.class);}
  @Override public void onCreate(){super.onCreate();if(Build.VERSION.SDK_INT>=26){NotificationChannel r=new NotificationChannel(RUN,"学习计时",NotificationManager.IMPORTANCE_LOW);nm().createNotificationChannel(r);NotificationChannel d=new NotificationChannel(DONE,"计时完成",NotificationManager.IMPORTANCE_HIGH);d.enableVibration(true);d.setVibrationPattern(new long[]{0,400,150,400});nm().createNotificationChannel(d);}}
  PendingIntent action(String a,int code){return PendingIntent.getService(this,code,new Intent(this,TimerService.class).setAction(a),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
  PendingIntent open(){return PendingIntent.getActivity(this,1,new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
  Notification.Builder builder(String channel){return Build.VERSION.SDK_INT>=26?new Notification.Builder(this,channel):new Notification.Builder(this);}
  Notification notice(JSONObject a){Notification.Builder b=builder(RUN).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(a==null?"学习计时":a.optString("name","自由学习")).setContentIntent(open()).setOngoing(true).setOnlyAlertOnce(true);if(a!=null){boolean down=a.optString("mode").equals("down"),run=a.optLong("since")>0;long ms=down?Math.max(0,a.optLong("duration")-elapsed(a)):elapsed(a);b.setContentText((run?"正在专注 · ":"已暂停 · ")+fmt(ms));if(run){b.setWhen(System.currentTimeMillis()+(down?ms:-ms)).setUsesChronometer(true).setChronometerCountDown(down);}b.addAction(android.R.drawable.ic_media_pause,run?"暂停":"继续",action("toggle",2));b.addAction(android.R.drawable.ic_menu_close_clear_cancel,"结束",action("finish",3));}return b.build();}
  synchronized void finish(JSONObject a,boolean auto){try{long ms=elapsed(a);if(a.optString("mode").equals("down"))ms=Math.min(ms,a.optLong("duration"));long end=auto?a.optLong("since")+Math.max(0,a.optLong("duration")-a.optLong("elapsed")):System.currentTimeMillis();JSONArray r=records(this);if(ms>=1000)r.put(new JSONObject().put("id",a.optString("id")).put("name",a.optString("name")).put("mode",a.optString("mode")).put("start",a.optLong("start")).put("end",end).put("ms",ms));prefs(this).edit().putString("active","null").putString("records",r.toString()).commit();if(auto){Notification.Builder b=builder(DONE).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("专注计时已完成").setContentText(a.optString("name")+" · "+fmt(ms)).setContentIntent(open()).setAutoCancel(true).setPriority(Notification.PRIORITY_HIGH);if(a.optBoolean("vib",true)){android.os.Vibrator v=(android.os.Vibrator)getSystemService(VIBRATOR_SERVICE);if(v!=null&&v.hasVibrator())v.vibrate(VibrationEffect.createWaveform(new long[]{0,400,150,400},-1));}else b.setSilent(true);nm().notify(ID+1,b.build());}}catch(Exception e){}stopForeground(true);stopSelf();}
  final Runnable tick=new Runnable(){public void run(){JSONObject a=active(TimerService.this);if(a==null){stopForeground(true);stopSelf();return;}if(a.optString("mode").equals("down")&&a.optLong("since")>0&&elapsed(a)>=a.optLong("duration")){finish(a,true);return;}nm().notify(ID,notice(a));h.postDelayed(this,1000);}};
  @Override public int onStartCommand(Intent i,int flags,int id){JSONObject a=active(this);startForeground(ID,notice(a));if(a!=null&&i!=null){String act=i.getAction();if("finish".equals(act)){finish(a,false);return START_NOT_STICKY;}if("toggle".equals(act)){try{if(a.optLong("since")>0){a.put("elapsed",elapsed(a));a.put("since",0);}else a.put("since",System.currentTimeMillis());prefs(this).edit().putString("active",a.toString()).commit();}catch(Exception e){}}}h.removeCallbacks(tick);h.post(tick);return START_STICKY;}
  @Override public void onDestroy(){h.removeCallbacks(tick);super.onDestroy();}
  @Override public IBinder onBind(Intent i){return null;}
 }
}

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
 @Override public void onCreate(Bundle state){ registerPlugin(CalendarSync.class); super.onCreate(state); sensors=(SensorManager)getSystemService(SENSOR_SERVICE); accelerometer=sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); getBridge().getWebView().addJavascriptInterface(new MotionBridge(),"StudyMotion"); getBridge().getWebView().addJavascriptInterface(new TimerBridge(),"StudyTimerNative"); getBridge().getWebView().addJavascriptInterface(new HapticBridge(),"StudyHaptics"); }
 public class MotionBridge { @JavascriptInterface public void setEnabled(boolean enabled){runOnUiThread(()->{requested=enabled;updateMotion();});} }
 private void updateMotion(){if(sensors==null)return;boolean next=requested&&foreground&&accelerometer!=null;if(next==listening)return;if(next)listening=sensors.registerListener(this,accelerometer,SensorManager.SENSOR_DELAY_GAME);else {sensors.unregisterListener(this);listening=false;}}
 @Override public void onResume(){super.onResume();foreground=true;updateMotion();}
 @Override public void onPause(){foreground=false;updateMotion();super.onPause();}
 @Override public void onDestroy(){foreground=false;updateMotion();super.onDestroy();}
 @Override public void onAccuracyChanged(Sensor sensor,int accuracy){}
 @Override public void onSensorChanged(SensorEvent e){if(!listening)return;float x=e.values[0],y=e.values[1];if(!Float.isFinite(x)||!Float.isFinite(y))return;getBridge().getWebView().evaluateJavascript("window.studyMotionSample && window.studyMotionSample("+x+","+y+")",null);}


 public class HapticBridge {
  private long lastTick=0;
  @JavascriptInterface public void pulse(String kind,String strength){runOnUiThread(()->{
   if(!foreground)return;
   boolean tick="tick".equals(kind); long now=SystemClock.elapsedRealtime();
   if(tick && now-lastTick<45)return; if(tick)lastTick=now;
   // Do not interrupt the existing repeating completion alarm.
   if(TimerService.prefs(MainActivity.this).getBoolean("pendingDone",false))return;
   int duration=35, amplitude=155;
   switch(kind==null?"tap":kind){case "tick":duration=18;amplitude=100;break;case "nav":duration=40;break;case "confirm":duration=55;amplitude=200;break;case "start":duration=85;amplitude=255;break;case "reset":duration=70;amplitude=220;break;case "end":duration=45;amplitude=180;break;}
   float factor="轻柔".equals(strength)?0.55f:"增强".equals(strength)?1.25f:0.85f;
   try { Vibrator v=(Vibrator)getSystemService(VIBRATOR_SERVICE); if(v!=null&&v.hasVibrator()){
    int ms=Math.max(10,Math.round(duration*factor)); int amp=Math.max(1,Math.min(255,Math.round(amplitude*factor)));
    if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createOneShot(ms,v.hasAmplitudeControl()?amp:VibrationEffect.DEFAULT_AMPLITUDE));else v.vibrate(ms);
   }else getBridge().getWebView().performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
   }catch(Exception e){getBridge().getWebView().performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);}
  });}
 }
 public class TimerBridge {
  @JavascriptInterface public void setState(String value){runOnUiThread(()->{if(android.os.Build.VERSION.SDK_INT>=33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED && !value.equals("null"))requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},321); TimerService.set(MainActivity.this,value);});}
  @JavascriptInterface public String snapshot(){return TimerService.snapshot(MainActivity.this);}
  @JavascriptInterface public void confirmDone(){runOnUiThread(()->{Intent i=new Intent(MainActivity.this,TimerService.class).setAction("confirmDone"); if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);});}
  @JavascriptInterface public void ack(String ids){TimerService.ack(MainActivity.this,ids);}
 }
 public static class TimerService extends Service {
  static final String PREF="study_timer_native", RUN="study_running", DONE="study_done", QUIET="study_done_quiet";
  static final int ID=2301; final Handler h=new Handler(Looper.getMainLooper());
  static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences(PREF,0);}
  static JSONObject active(Context c){try{return new JSONObject(prefs(c).getString("active","null"));}catch(Exception e){return null;}}
  static JSONArray records(Context c){try{return new JSONArray(prefs(c).getString("records","[]"));}catch(Exception e){return new JSONArray();}}
  static synchronized String snapshot(Context c){try{return new JSONObject().put("active",active(c)==null?JSONObject.NULL:active(c)).put("records",records(c)).put("pendingDone",prefs(c).getBoolean("pendingDone",false)).toString();}catch(Exception e){return "{}";}}
  static synchronized void ack(Context c,String ids){try{JSONArray a=new JSONArray(ids),r=records(c),keep=new JSONArray();for(int i=0;i<r.length();i++){JSONObject x=r.getJSONObject(i);boolean found=false;for(int j=0;j<a.length();j++)if(x.optString("id").equals(a.optString(j)))found=true;if(!found)keep.put(x);}prefs(c).edit().putString("records",keep.toString()).commit();}catch(Exception e){}}
  static void set(Context c,String value){prefs(c).edit().putString("active",value).commit();Intent i=new Intent(c,TimerService.class);if(value.equals("null")){if(!prefs(c).getBoolean("pendingDone",false))c.stopService(i);return;}if(Build.VERSION.SDK_INT>=26)c.startForegroundService(i);else c.startService(i);}
  static long elapsed(JSONObject a){long since=a.optLong("since");return a.optLong("elapsed")+(since>0?Math.max(0,System.currentTimeMillis()-since):0);}
  static String fmt(long ms){long s=Math.max(0,ms/1000);return String.format(java.util.Locale.US,"%02d:%02d:%02d",s/3600,s/60%60,s%60);}
  NotificationManager nm(){return getSystemService(NotificationManager.class);}
  @Override public void onCreate(){super.onCreate();if(Build.VERSION.SDK_INT>=26){NotificationChannel r=new NotificationChannel(RUN,"学习计时",NotificationManager.IMPORTANCE_LOW);nm().createNotificationChannel(r);NotificationChannel d=new NotificationChannel(DONE,"计时完成",NotificationManager.IMPORTANCE_HIGH);d.enableVibration(true);d.setVibrationPattern(new long[]{0,400,150,400});nm().createNotificationChannel(d);NotificationChannel q=new NotificationChannel(QUIET,"计时完成（静音）",NotificationManager.IMPORTANCE_HIGH);q.enableVibration(false);q.setSound(null,null);nm().createNotificationChannel(q);}}
  PendingIntent action(String a,int code){return PendingIntent.getService(this,code,new Intent(this,TimerService.class).setAction(a),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
  PendingIntent open(){return PendingIntent.getActivity(this,1,new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
  Notification.Builder builder(String channel){return Build.VERSION.SDK_INT>=26?new Notification.Builder(this,channel):new Notification.Builder(this);}
  Notification notice(JSONObject a){Notification.Builder b=builder(RUN).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(a==null?"学习计时":a.optString("name","自由学习")).setContentIntent(open()).setOngoing(true).setOnlyAlertOnce(true);if(a!=null){boolean down=a.optString("mode").equals("down"),run=a.optLong("since")>0;long ms=down?Math.max(0,a.optLong("duration")-elapsed(a)):elapsed(a);b.setContentText((run?"正在专注 · ":"已暂停 · ")+fmt(ms));if(run){b.setWhen(System.currentTimeMillis()+(down?ms:-ms)).setUsesChronometer(true).setChronometerCountDown(down);}b.addAction(android.R.drawable.ic_media_pause,run?"暂停":"继续",action("toggle",2));b.addAction(android.R.drawable.ic_menu_close_clear_cancel,"结束",action("finish",3));}return b.build();}
  synchronized void finish(JSONObject a,boolean auto){try{long ms=elapsed(a);if(a.optString("mode").equals("down"))ms=Math.min(ms,a.optLong("duration"));long end=auto?a.optLong("since")+Math.max(0,a.optLong("duration")-a.optLong("elapsed")):System.currentTimeMillis();JSONArray r=records(this);if(ms>=1000)r.put(new JSONObject().put("id",a.optString("id")).put("name",a.optString("name")).put("mode",a.optString("mode")).put("start",a.optLong("start")).put("end",end).put("ms",ms));prefs(this).edit().putString("active","null").putString("records",r.toString()).commit();if(auto){prefs(this).edit().putBoolean("pendingDone",true).putBoolean("doneVib",a.optBoolean("vib",true)).putString("doneText",a.optString("name")+" · "+fmt(ms)).commit();showDone();return;}}catch(Exception e){}stopForeground(true);stopSelf();}
  void showDone(){Notification.Builder b=builder(QUIET).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("专注计时已完成").setContentText(prefs(this).getString("doneText","学习记录已保存")).setContentIntent(open()).setOngoing(true).setOnlyAlertOnce(true).setPriority(Notification.PRIORITY_HIGH).addAction(0,"确认完成",action("confirmDone",4));startForeground(ID,b.build());if(prefs(this).getBoolean("doneVib",true)){Vibrator v=(Vibrator)getSystemService(VIBRATOR_SERVICE);if(v!=null&&v.hasVibrator())v.vibrate(VibrationEffect.createWaveform(new long[]{0,800,400},0));}}
  void confirmDone(){prefs(this).edit().putBoolean("pendingDone",false).commit();Vibrator v=(Vibrator)getSystemService(VIBRATOR_SERVICE);if(v!=null)v.cancel();nm().cancel(ID+1);stopForeground(true);stopSelf();}
  final Runnable tick=new Runnable(){public void run(){JSONObject a=active(TimerService.this);if(a==null){if(prefs(TimerService.this).getBoolean("pendingDone",false))return;stopForeground(true);stopSelf();return;}if(a.optString("mode").equals("down")&&a.optLong("since")>0&&elapsed(a)>=a.optLong("duration")){finish(a,true);return;}nm().notify(ID,notice(a));h.postDelayed(this,1000);}};
  @Override public int onStartCommand(Intent i,int flags,int id){JSONObject a=active(this);startForeground(ID,notice(a));if(i!=null&&"confirmDone".equals(i.getAction())){confirmDone();return START_NOT_STICKY;}if(prefs(this).getBoolean("pendingDone",false)){showDone();return START_STICKY;}if(a!=null&&i!=null){String act=i.getAction();if("finish".equals(act)){finish(a,false);return START_NOT_STICKY;}if("toggle".equals(act)){try{if(a.optLong("since")>0){a.put("elapsed",elapsed(a));a.put("since",0);}else a.put("since",System.currentTimeMillis());prefs(this).edit().putString("active",a.toString()).commit();}catch(Exception e){}}}h.removeCallbacks(tick);h.post(tick);return START_STICKY;}
  @Override public void onDestroy(){h.removeCallbacks(tick);Vibrator v=(Vibrator)getSystemService(VIBRATOR_SERVICE);if(v!=null)v.cancel();super.onDestroy();}
  @Override public IBinder onBind(Intent i){return null;}
 }
}

package com.studyplanner.fish;
import android.Manifest;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract.*;
import com.getcapacitor.*;
import com.getcapacitor.annotation.*;
import org.json.*;
import java.util.*;
@CapacitorPlugin(name="CalendarSync", permissions={@Permission(alias="calendar",strings={Manifest.permission.READ_CALENDAR,Manifest.permission.WRITE_CALENDAR})})
public class CalendarSync extends Plugin {
 @PluginMethod public void authorize(PluginCall call){if(getPermissionState("calendar")==PermissionState.GRANTED) granted(call);else requestPermissionForAlias("calendar",call,"granted");}
 @PermissionCallback private void granted(PluginCall call){JSObject r=new JSObject();r.put("granted",getPermissionState("calendar")==PermissionState.GRANTED);call.resolve(r);}
 @PluginMethod public void calendars(PluginCall call){try{JSArray a=new JSArray();try(Cursor c=getContext().getContentResolver().query(Calendars.CONTENT_URI,new String[]{Calendars._ID,Calendars.CALENDAR_DISPLAY_NAME,Calendars.ACCOUNT_NAME},Calendars.CALENDAR_ACCESS_LEVEL+">=?",new String[]{String.valueOf(Calendars.CAL_ACCESS_CONTRIBUTOR)},null)){while(c!=null&&c.moveToNext()){JSObject o=new JSObject();o.put("id",c.getString(0));o.put("name",c.getString(1)+" · "+c.getString(2));a.put(o);}}JSObject r=new JSObject();r.put("items",a);call.resolve(r);}catch(Exception e){call.reject(e.toString());}}
 @PluginMethod public synchronized void sync(PluginCall call){try{
 if(getPermissionState("calendar")!=PermissionState.GRANTED){call.reject("需要日历权限");return;}
 long cal=Long.parseLong(call.getString("calendarId")); JSONArray items=call.getArray("items"); int days=call.getInt("days",2); ContentResolver cr=getContext().getContentResolver();
 String prefix="studyplanner.fish:"; Map<String,Long> old=new HashMap<>(); ArrayList<ContentProviderOperation> ops=new ArrayList<>();
 try(Cursor c=cr.query(Events.CONTENT_URI,new String[]{Events._ID,Events.DESCRIPTION,Events.CALENDAR_ID},Events.DESCRIPTION+" LIKE ?",new String[]{prefix+"%"},null)){if(c==null)throw new Exception("无法读取日历");while(c.moveToNext()){String key=c.getString(1);long id=c.getLong(0);if(c.getLong(2)!=cal||old.containsKey(key)){ops.add(ContentProviderOperation.newDelete(ContentUris.withAppendedId(Events.CONTENT_URI,id)).build());}else old.put(key,id);}}
 for(int i=0;i<items.length();i++){JSONObject o=items.getJSONObject(i);String key=prefix+o.getString("id");long at=o.getLong("at");Long id=old.remove(key);ContentValues v=new ContentValues();v.put(Events.CALENDAR_ID,cal);v.put(Events.TITLE,o.getString("title"));v.put(Events.DESCRIPTION,key);v.put(Events.DTSTART,at);v.put(Events.DTEND,at+1800000);v.put(Events.EVENT_TIMEZONE,TimeZone.getDefault().getID());v.put(Events.HAS_ALARM,1);
 int idx=ops.size();if(id==null)ops.add(ContentProviderOperation.newInsert(Events.CONTENT_URI).withValues(v).build());else{ops.add(ContentProviderOperation.newUpdate(ContentUris.withAppendedId(Events.CONTENT_URI,id)).withValues(v).build());ops.add(ContentProviderOperation.newDelete(Reminders.CONTENT_URI).withSelection(Reminders.EVENT_ID+"=?",new String[]{id.toString()}).build());}
 for(int minutes:new int[]{days*1440,0}){if(minutes>0&&at-minutes*60000L<=System.currentTimeMillis())continue;ContentProviderOperation.Builder b=ContentProviderOperation.newInsert(Reminders.CONTENT_URI).withValue(Reminders.MINUTES,minutes).withValue(Reminders.METHOD,Reminders.METHOD_ALERT);if(id==null)b.withValueBackReference(Reminders.EVENT_ID,idx);else b.withValue(Reminders.EVENT_ID,id);ops.add(b.build());}}
 for(long id:old.values())ops.add(ContentProviderOperation.newDelete(ContentUris.withAppendedId(Events.CONTENT_URI,id)).build());
 if(!ops.isEmpty())cr.applyBatch("com.android.calendar",ops);call.resolve();
 }catch(Exception e){call.reject(e.toString());}}
}

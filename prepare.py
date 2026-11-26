from pathlib import Path
import shutil, xml.etree.ElementTree as E
from PIL import Image
p=Path('android/app/src/main'); dest=p/'java/com/studyplanner/fish'; dest.mkdir(parents=True,exist_ok=True)
for f in Path('native').glob('*.java'): shutil.copy(f,dest/f.name)
f=p/'AndroidManifest.xml'; s=f.read_text(); s=s.replace('</manifest>','<uses-permission android:name="android.permission.READ_CALENDAR"/><uses-permission android:name="android.permission.WRITE_CALENDAR"/></manifest>'); f.write_text(s)
for d in (p/'res').glob('mipmap-*'):
 for name in ['ic_launcher','ic_launcher_round','ic_launcher_foreground']:
  f=d/(name+'.png')
  if f.exists():
   size=Image.open(f).size; Image.open('www/icon-512.png').convert('RGBA').resize(size,Image.Resampling.LANCZOS).save(f)
for f in (p/'res').glob('mipmap-anydpi*/ic_launcher*.xml'): f.unlink()

# Android release version
f=Path('android/app/build.gradle')
s=f.read_text()
import re
s,n=re.subn(r'versionCode\s+\d+', 'versionCode 40', s, count=1)
assert n == 1, 'versionCode not found'
s,n=re.subn(r'versionName\s+["\'][^"\']+["\']', 'versionName "3.2.8"', s, count=1)
assert n == 1, 'versionName not found'
f.write_text(s)

f=p/'AndroidManifest.xml'
s=f.read_text().replace('</manifest>','<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/><uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE"/><uses-permission android:name="android.permission.POST_NOTIFICATIONS"/><uses-permission android:name="android.permission.VIBRATE"/></manifest>').replace('</application>','<service android:name=".MainActivity$TimerService" android:exported="false" android:foregroundServiceType="specialUse"><property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" android:value="User initiated study stopwatch and countdown"/></service></application>')
f.write_text(s)

# System-managed APK installation callback.
f=p/'AndroidManifest.xml'
s=f.read_text().replace('</manifest>','<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/></manifest>').replace('</application>','<receiver android:name=".MainActivity$InstallResult" android:exported="false"/></application>')
f.write_text(s)

# Expanded native timer notification layout.
layout=p/'res/layout';layout.mkdir(parents=True,exist_ok=True)
(layout/'study_timer_expanded.xml').write_text('''<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:padding="8dp">
<TextView android:id="@+id/timer_task" android:layout_width="match_parent" android:layout_height="wrap_content" android:textAppearance="@android:style/TextAppearance.Material.Notification.Title" android:maxLines="1" android:ellipsize="end"/>
<TextView android:id="@+id/timer_state" android:layout_width="match_parent" android:layout_height="wrap_content" android:gravity="center" android:textAppearance="@android:style/TextAppearance.Material.Notification"/>
<TextView android:id="@+id/timer_digits" android:layout_width="match_parent" android:layout_height="wrap_content" android:gravity="center" android:textSize="32sp" android:fontFamily="monospace" android:textAppearance="@android:style/TextAppearance.Material.Notification.Title"/>
<LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="horizontal">
<TextView android:id="@+id/timer_toggle" android:layout_width="0dp" android:layout_weight="1" android:layout_height="40dp" android:gravity="center" android:text="暂停" android:textAppearance="@android:style/TextAppearance.Material.Notification.Title"/>
<TextView android:id="@+id/timer_finish" android:layout_width="0dp" android:layout_weight="1" android:layout_height="40dp" android:gravity="center" android:text="结束" android:textAppearance="@android:style/TextAppearance.Material.Notification.Title"/>
<TextView android:id="@+id/timer_open" android:layout_width="0dp" android:layout_weight="1" android:layout_height="40dp" android:gravity="center" android:text="打开" android:textAppearance="@android:style/TextAppearance.Material.Notification.Title"/>
</LinearLayout></LinearLayout>''',encoding='utf-8')

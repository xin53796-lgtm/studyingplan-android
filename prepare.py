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
s,n=re.subn(r'versionCode\s+\d+', 'versionCode 43', s, count=1)
assert n == 1, 'versionCode not found'
s,n=re.subn(r'versionName\s+["\'][^"\']+["\']', 'versionName "3.2.11"', s, count=1)
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
draw=p/'res/drawable';draw.mkdir(parents=True,exist_ok=True)
for name,base in [('toggle','#D5E7DD'),('finish','#F0E2DF'),('open','#E3EBE5')]:
 (draw/('timer_'+name+'.xml')).write_text(f'<ripple xmlns:android="http://schemas.android.com/apk/res/android" android:color="#55728D80"><item><shape android:shape="oval"><solid android:color="{base}"/></shape></item></ripple>')
(layout/'study_timer_expanded.xml').write_text('''<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="vertical" android:gravity="center_horizontal" android:paddingTop="12dp" android:paddingBottom="6dp" android:paddingRight="32dp">
<TextView android:id="@+id/timer_task" android:layout_width="match_parent" android:layout_height="wrap_content" android:gravity="center" android:textSize="15sp" android:textStyle="bold" android:maxLines="1" android:ellipsize="end" android:textAppearance="@android:style/TextAppearance.Material.Notification.Title"/>
<TextView android:id="@+id/timer_digits" android:layout_width="match_parent" android:layout_height="wrap_content" android:gravity="center" android:textSize="32sp" android:fontFamily="sans-serif-medium" android:layout_marginTop="4dp" android:textAppearance="@android:style/TextAppearance.Material.Notification.Title"/>
<TextView android:id="@+id/timer_state" android:layout_width="match_parent" android:layout_height="wrap_content" android:gravity="center" android:textSize="12sp" android:textAppearance="@android:style/TextAppearance.Material.Notification"/>
<LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="horizontal" android:gravity="center" android:layout_marginTop="12dp">
<LinearLayout android:id="@+id/timer_toggle" android:layout_width="0dp" android:layout_weight="1" android:layout_height="wrap_content" android:orientation="vertical" android:gravity="center_horizontal">
<TextView android:id="@+id/timer_toggle_icon" android:layout_width="40dp" android:layout_height="40dp" android:gravity="center" android:background="@drawable/timer_toggle" android:textColor="#507D69" android:text="▶" android:textSize="20sp" android:includeFontPadding="false"/>
<TextView android:id="@+id/timer_toggle_label" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="4dp" android:text="继续" android:textColor="#507D69" android:textSize="12sp"/>
</LinearLayout>
<LinearLayout android:id="@+id/timer_finish" android:layout_width="0dp" android:layout_weight="1" android:layout_height="wrap_content" android:orientation="vertical" android:gravity="center_horizontal">
<TextView android:id="@+id/timer_finish_icon" android:layout_width="40dp" android:layout_height="40dp" android:gravity="center" android:background="@drawable/timer_finish" android:textColor="#986F69" android:text="□" android:textSize="20sp" android:includeFontPadding="false"/>
<TextView android:id="@+id/timer_finish_label" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="4dp" android:text="结束" android:textColor="#986F69" android:textSize="12sp"/>
</LinearLayout>
<LinearLayout android:id="@+id/timer_open" android:layout_width="0dp" android:layout_weight="1" android:layout_height="wrap_content" android:orientation="vertical" android:gravity="center_horizontal">
<TextView android:id="@+id/timer_open_icon" android:layout_width="40dp" android:layout_height="40dp" android:gravity="center" android:background="@drawable/timer_open" android:textColor="#607D70" android:text="↗" android:textSize="20sp" android:includeFontPadding="false"/>
<TextView android:id="@+id/timer_open_label" android:layout_width="wrap_content" android:layout_height="wrap_content" android:layout_marginTop="4dp" android:text="打开" android:textColor="#607D70" android:textSize="12sp"/>
</LinearLayout>
</LinearLayout></LinearLayout>''',encoding='utf-8')

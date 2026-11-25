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
s,n=re.subn(r'versionCode\s+\d+', 'versionCode 33', s, count=1)
assert n == 1, 'versionCode not found'
s,n=re.subn(r'versionName\s+["\'][^"\']+["\']', 'versionName "3.2.1"', s, count=1)
assert n == 1, 'versionName not found'
f.write_text(s)

f=p/'AndroidManifest.xml'
s=f.read_text().replace('</manifest>','<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/><uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE"/><uses-permission android:name="android.permission.POST_NOTIFICATIONS"/><uses-permission android:name="android.permission.VIBRATE"/></manifest>').replace('</application>','<service android:name=".MainActivity$TimerService" android:exported="false" android:foregroundServiceType="specialUse"><property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" android:value="User initiated study stopwatch and countdown"/></service></application>')
f.write_text(s)

# Online update provider, restricted to the APK cache directory.
f=p/'AndroidManifest.xml'
s=f.read_text().replace('</manifest>','<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/></manifest>').replace('</application>','<provider android:name="androidx.core.content.FileProvider" android:authorities="${applicationId}.updateprovider" android:exported="false" android:grantUriPermissions="true"><meta-data android:name="android.support.FILE_PROVIDER_PATHS" android:resource="@xml/update_paths"/></provider></application>')
f.write_text(s)
d=p/'res/xml'; d.mkdir(parents=True,exist_ok=True)
(d/'update_paths.xml').write_text('<paths xmlns:android="http://schemas.android.com/apk/res/android"><cache-path name="updates" path="updates/"/></paths>')

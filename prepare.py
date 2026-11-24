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

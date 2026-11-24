# 小鱼爱学习 · Android 原生日历版

## 云端编译
1. 解压本项目，在 GitHub 创建新的仓库（建议私有），将所有文件及隐藏目录 `.github` 上传到仓库根目录。不要覆盖旧网页版。
2. 打开 Actions，选择 Build Android APK，点击 Run workflow（或推送 main 自动触发）。
3. 成功后打开运行记录，在 Artifacts 下载 study-planner-apk，解压获得 app-debug.apk，传到安卓手机安装。后续安装需使用相同签名；云端 debug 签名可能变化，正式长期使用应配置固定签名。
4. 原网页版先生成备份，在新 APP 的备份面板恢复，再点击授权 / 选择日历，允许读写权限，选择专用可写日历并确认同步。

## 行为
保存后防抖 2 秒同步未来未完成截止任务及倒计时；修改、完成、删除或调整提醒天数时重新协调。已过期事件会在后续同步时清理。同步失败时每 30 秒尝试，APP 返回前台时也会触发；关闭 APP 后不保证尚未提交的同步继续执行。已写入事件由系统日历负责提醒，需开启日历通知。旧 ICS 导入的事件不属于本程序管理，需自行清理以免重复。网页版保留 ICS 功能。

## 本地编译
安装 Node 22、JDK 21、Android SDK 35、Python Pillow。运行 npm install、npx cap add android、npx cap sync android、python prepare.py，然后在 android 目录运行 ./gradlew assembleDebug（Windows 使用 gradlew.bat）。

## 验证状态
网页 JavaScript 已做语法检查；尚未实际编译 APK 或在手机上测试 Calendar Provider。建议先用测试日历验证新增、修改、删除、完成和提醒，再迁移正式数据。

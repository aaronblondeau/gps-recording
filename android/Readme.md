### To connect to emulator

```
adb -d forward tcp:5601 tcp:5601
```

### To debug on real watch
https://developer.android.com/training/wearables/apps/debugging#usb-phone
(don't forget to hit allow on watch)

### also useful sometimes
```
adb kill-server
adb start-server
```

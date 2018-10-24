/*
 * Entry point for the companion app
 */
import { settingsStorage } from "settings";
import * as messaging from "messaging";
import { me } from "companion";

console.log("Companion code started");

// Event fires when a setting is changed
settingsStorage.onchange = function(evt) {
  // Which setting changed
  console.log("key: " + evt.key)

  // What was the old value
  console.log("old value: " + evt.oldValue)

  // What is the new value
  console.log("new value: " + evt.newValue)
  sendSettingsToDevice()
}

if (me.launchReasons.settingsChanged || me.launchReasons.peerAppLaunched) {
  // Settings were changed while the companion was not running
  sendSettingsToDevice()
}

function sendSettingsToDevice() {
    let action = {
        action: 'updateSettings',
        settings: {
            useMetricUnits: settingsStorage.getItem('useMetricUnits'),
            distanceFilterInMeters: settingsStorage.getItem('distanceFilterInMeters'),
            timeFilterInSeconds: settingsStorage.getItem('timeFilterInSeconds')
        }
    }
    if (messaging.peerSocket.readyState === messaging.peerSocket.OPEN) {
        messaging.peerSocket.send(action);
    } else {
        console.log("No peerSocket connection");
    }
}

if(settingsStorage.getItem('distanceFilterInMeters') == null) {
    settingsStorage.setItem('distanceFilterInMeters', 10)
}
if(settingsStorage.getItem('timeFilterInSeconds') == null) {
    settingsStorage.setItem('timeFilterInSeconds', 10)
}

messaging.peerSocket.onmessage = function(evt) {
    if(evt.data.action == 'receivePoints') {
        let points = evt.data.points
        let filename = evt.data.filename
        console.log('~~ new points from device', filename)
        
        // TODO - copy to local fs
        // If this is a "finished" filename, send data to server

        let action = {
            action: 'confirmPoints',
            filename: filename
        }
        if (messaging.peerSocket.readyState === messaging.peerSocket.OPEN) {
            messaging.peerSocket.send(action);
        } else {
            console.log("No peerSocket connection - cannot confirm points");
        }

    }
}

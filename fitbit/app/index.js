/*
 * Entry point for the watch app
 */
import document from "document";
import { geolocation } from "geolocation";
import { me } from "appbit";
import distance from "@turf/distance"
import { point } from "@turf/helpers"
import * as messaging from "messaging";
import { readFileSync, writeFileSync, writeFile } from "fs";

let settings = {
  useMetricUnits: false,
  distanceFilterInMeters: 10,
  timeFilterInSeconds: 10
}

try {
  let settingsText = readFileSync("settings.json", "utf-8");
  if (settingsText) {
    try {
      settings = JSON.parse(settingsText)
      console.log('Read settings.json as', JSON.stringify(settings))
    } catch(e) {
      console.error('Unable to parse settings.json file!', e)
    }
  }
} catch(e) {
  console.warn('Unable to load settings.json file', e)
}

var watchID = null
let locationText = document.getElementById("location");
let altitudeText = document.getElementById("altitude");
let distanceText = document.getElementById("distance");
let elapsedText = document.getElementById("elapsed");
let timeText = document.getElementById("time")
let lastPosition = null
let totalDistanceInKm = 0
let totalTimestamps = 0

function locationSuccess(position) {
  storePosition(position)  
  hasTrack = true
  updateButtons()
}

function locationError(error) {
    console.error("GPS Location Error: " + error.code, "Message: " + error.message);
}

function storePosition(position) {
  

  if (lastPosition) {
    var from = point([lastPosition.coords.longitude, lastPosition.coords.latitude]);
    var to = point([position.coords.longitude, position.coords.latitude]);
    
    let dist = distance(from, to)
    
    // Docs appear to be wrong and these are in ns instead of ms?
    let timems = position.timestamp - lastPosition.timestamp // for simulator : position.timestamp / 1000 - lastPosition.timestamp / 1000
    
    // Only record points if we are more than distance filter or time filter away from last point
    if (((dist * 1000) > settings.distanceFilterInMeters) || ((timems / 1000) > settings.timeFilterInSeconds)) {
      totalDistanceInKm = totalDistanceInKm + dist
      // console.log('~~ distance = ' + totalDistanceInKm + ' (+'+ dist +')')

      totalTimestamps = totalTimestamps + timems
      // console.log('~~ elapsed = ' + totalTimestamps + ' (+'+ timems +')')

      // TODO - save location to file

    }
  }
  lastPosition = position

  updateText()
}

let buttonStartPause = document.getElementById("btn-start-pause");
let buttonFinish = document.getElementById("btn-finish");
setTimeout(function() {
  updateButtons()
}, 500)
let buttonStartPauseIcon = buttonStartPause.getElementById("combo-button-icon");
let buttonStartPauseIconPress = buttonStartPause.getElementById("combo-button-icon-press");
let recording = false
let hasTrack = false
buttonStartPause.onactivate = function(evt) {
  console.log("Start/Pause!");
  if (recording) {
    stopRecording()
  } else {
    startRecording()
  }
}

function stopRecording() {
  if (watchID) {
    geolocation.clearWatch(watchID)
  }
  recording = false
  updateButtons()
  updateText()
}

function startRecording() {
  locationText.text = "Locating ...";

  // TODO - fetch from store
  totalDistanceInKm = 0
  totalTimestamps = 0

  watchID = geolocation.watchPosition(locationSuccess, locationError, {
    enableHighAccuracy: true
  });
  recording = true
  updateButtons()
  updateText()
}

function finishRecording() {
  stopRecording()
  
  //TODO = sync track to companion
  
  hasTrack = false
  updateButtons()
  updateText()
}

function updateText() {
  if (lastPosition) {
    console.log("Latitude: " + lastPosition.coords.latitude, "Longitude: " + lastPosition.coords.longitude, "Timestamp: " + lastPosition.timestamp);
    locationText.text = lastPosition.coords.latitude.toFixed(4) + "," + lastPosition.coords.longitude.toFixed(4)

    if(!settings.useMetricUnits || settings.useMetricUnits === 'false') {
      altitudeText.text = lastPosition.coords.altitude ? ((lastPosition.coords.altitude * 3.28084).toFixed(2) + "ft") : "?"
    } else {
      altitudeText.text = lastPosition.coords.altitude ? (lastPosition.coords.altitude.toFixed(0) + "m") : "?"
    }
    // lastPosition.coords.speed
    // lastPosition.coords.heading
    // lastPosition.coords.altitudeAccuracy
  }

  if(!settings.useMetricUnits || settings.useMetricUnits === 'false') {
    distanceText.text = (totalDistanceInKm * 0.621371).toFixed(2) + "mi"
  } else {
    distanceText.text = totalDistanceInKm.toFixed(2) + "km"
  }

  let date = new Date(null);
  date.setSeconds(totalTimestamps / 1000); // specify value for SECONDS here
  let resultHMS = date.toISOString().substr(11, 8);
  elapsedText.text = resultHMS
}

function updateButtons() {
  if (recording) {
    buttonStartPause.style.fill = "fb-red"
    buttonStartPauseIcon.image = "pause.png"
    buttonStartPauseIconPress.image = "pause_press.png"
  } else {
    buttonStartPause.style.fill = "fb-green"
    buttonStartPauseIcon.image = "start.png"
    buttonStartPauseIconPress.image = "start_press.png"
  }
  
  if(hasTrack) {
    buttonFinish.enable()
  } else {
    buttonFinish.disable()
  }
}

buttonFinish.onactivate = function(evt) {
  console.log("Finish");
  if (hasTrack) {
    finishRecording()
  } else {
    console.error("Tried to finish recording without a track!")
  }
}

// https://dev.fitbit.com/blog/2018-10-05-announcing-fitbit-os-2.2/

me.appTimeoutEnabled = false
console.log("appTimeoutEnabled: " + me.appTimeoutEnabled);

me.onunload = () => {
    console.log("~~ app onunload");
}

messaging.peerSocket.onmessage = function(evt) {
  if(evt.data.action == 'updateSettings') {
    console.log('~~ new settings', JSON.stringify(evt.data.settings))
    settings = evt.data.settings
    // TODO - write settings.json
    writeFileSync('settings.json', JSON.stringify(settings), "utf-8")
    updateText()
  }
}

function formatTwoDigits(n) {
  return n < 10 ? '0' + n : n;
}

function showTime() {
  let date = new Date()
  let hours = date.getHours()
  let ampm = "am"
  if (hours > 12) {
    hours = hours - 12
    ampm = "pm"
  }
  let minutes = date.getMinutes()
  timeText.text = hours + ":" + formatTwoDigits(minutes) + " " + ampm  //date.toTimeString().split(' ')[0]
}

setInterval(function(){
  showTime()
}, 1000)
showTime()

/*
 * Entry point for the watch app
 */
import document from "document";
import { geolocation } from "geolocation";

var watchID = null
let location = document.getElementById("location");
let altitude = document.getElementById("altitude");
let speed = document.getElementById("speed");

function locationSuccess(position) {
    console.log("Latitude: " + position.coords.latitude, "Longitude: " + position.coords.longitude);
    location.text = position.coords.latitude.toFixed(4) + "," + position.coords.longitude.toFixed(4)
    altitude.text = position.coords.altitude ? position.coords.altitude : "?"
    speed.text = position.coords.speed ? position.coords.speed : "?"
  
    hasTrack = true
    updateButtons()
}

function locationError(error) {
    console.error("GPS Location Error: " + error.code, "Message: " + error.message);
}

location.text = "Locating ...";

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
}

function startRecording() {
  watchID = geolocation.watchPosition(locationSuccess, locationError, {
    enableHighAccuracy: true
  });
  recording = true
  updateButtons()
}

function finishRecording() {
  stopRecording()
  
  //TODO = sync track to companion
  
  hasTrack = false
  updateButtons()
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



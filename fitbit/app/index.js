/*
 * Entry point for the watch app
 */
import document from "document";
import { geolocation } from "geolocation";
import { me } from "appbit";
import * as messaging from "messaging";
import fs from "fs";
import distance from "@turf/distance"
import { point } from "@turf/helpers"

let settings = {
  useMetricUnits: false,
  distanceFilterInMeters: 10,
  timeFilterInSeconds: 10
}

try {
  let settingsText = fs.readFileSync("settings.json", "utf-8");
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
let filesText = document.getElementById("files")
let lastPosition = null
let totalDistanceInKm = 0
let totalTimestamps = 0
let points = []
let pointsFileCount = 0
let currentTrackId = ''

try {
  let currentTrackText = fs.readFileSync("current_track.json", "utf-8");
  if (currentTrackText) {
    try {
      let ct = JSON.parse(currentTrackText)
      currentTrackId = ct.currentTrackId
      totalDistanceInKm = ct.totalDistanceInKm
      totalTimestamps = ct.totalTimestamps
      lastPosition = ct.lastPosition
      pointsFileCount = ct.pointsFileCount ? ct.pointsFileCount : 0
      console.log('Read current_track.json as', JSON.stringify(ct))
    } catch(e) {
      console.error('Unable to parse current_track.json file!', e)
    }
  }
} catch(e) {
  console.warn('Unable to load current_track.json file', e)
}

let fileLog = readFileLog()

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

        // save location to file
        points.push({
          lat: position.coords.latitude,
          lng: position.coords.longitude,
          alt: position.coords.altitude,
          ts: position.timestamp
        })

        if(points.length >= 10) {
          storePoints()
        }
    }
  } else {
    // save location to file
    points.push({
      lat: position.coords.latitude,
      lng: position.coords.longitude,
      alt: position.coords.altitude,
      ts: position.timestamp
    })

    if(points.length >= 10) {
      storePoints()
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
  // console.log("Start/Pause!");
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
  points.push({pause: true, ts: new Date().getTime()})
  storePoints()
  updateButtons()
  updateText()
  lastPosition = null
  saveCurrentTrack()
}

function startRecording() {
  locationText.text = "Locating ...";

  if(!currentTrackId) {
    currentTrackId = new Date().getTime()
  }
  saveCurrentTrack()

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

  storePoints()

  // Create a flag file to indicate track id is no longer being recorded to
  let filename = 'finished_track_'+currentTrackId+'.json'
  fs.writeFileSync(filename, JSON.stringify({pointsFileCount:pointsFileCount - 1}), "utf-8")
  appendToFileLog(filename)

  currentTrackId = ''
  totalDistanceInKm = 0
  totalTimestamps = 0
  points = []
  pointsFileCount = 0
  lastPosition = null
  saveCurrentTrack()
  
  hasTrack = false
  updateButtons()
  updateText()
}

function updateText() {
  if (lastPosition) {
    // console.log("Latitude: " + lastPosition.coords.latitude, "Longitude: " + lastPosition.coords.longitude, "Timestamp: " + lastPosition.timestamp);
    locationText.text = lastPosition.coords.latitude.toFixed(4) + "," + lastPosition.coords.longitude.toFixed(4)

    if(!settings.useMetricUnits || settings.useMetricUnits === 'false') {
      altitudeText.text = lastPosition.coords.altitude ? ((lastPosition.coords.altitude * 3.28084).toFixed(2) + "ft") : "?"
    } else {
      altitudeText.text = lastPosition.coords.altitude ? (lastPosition.coords.altitude.toFixed(0) + "m") : "?"
    }

    // lastPosition.coords.accuracy
    // lastPosition.coords.speed
    // lastPosition.coords.heading
    // lastPosition.coords.altitudeAccuracy
  } else {
    locationText.text = ''
    altitudeText.text = ''
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
  // console.log("Finish");
  if (hasTrack) {
    finishRecording()
  } else {
    console.error("Tried to finish recording without a track!")
  }
}

// https://dev.fitbit.com/blog/2018-10-05-announcing-fitbit-os-2.2/

me.appTimeoutEnabled = false
// console.log("appTimeoutEnabled: " + me.appTimeoutEnabled);

me.onunload = () => {
    console.log("~~ app onunload");
    storePoints()
    writeFileLog(fileLog)
}

messaging.peerSocket.onmessage = function(evt) {
  if(evt.data.action == 'updateSettings') {
    // console.log('~~ new settings', JSON.stringify(evt.data.settings))
    settings = evt.data.settings
    // TODO - write settings.json
    fs.writeFileSync('settings.json', JSON.stringify(settings), "utf-8")
    updateText()
  }
  if(evt.data.action == 'confirmPoints') {
    let filename = evt.data.filename
    console.log("confirmPoints", filename)
    fs.unlinkSync(filename)
    pendingFilenames[filename] = false
    removeFileFromLog(filename)
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

function saveCurrentTrack() {
  fs.writeFileSync('current_track.json', JSON.stringify({
    currentTrackId: currentTrackId,
    totalDistanceInKm: totalDistanceInKm,
    totalTimestamps: totalTimestamps,
    lastPosition: lastPosition,
    pointsFileCount: pointsFileCount
  }), "utf-8")
}

function storePoints() {
  if (points.length > 0) {
    let filename = 'track_'+currentTrackId+'_points_'+pointsFileCount+'.json'
    fs.writeFileSync(filename, JSON.stringify(points), "utf-8")
    appendToFileLog(filename)
  }
  points = []
  pointsFileCount = pointsFileCount + 1
}

// These are here because fs.listDirSync does not seem to exist

function readFileLog() {
  try {
    let logText = fs.readFileSync("files.log", "utf-8");
    return logText
  } catch(e) {
    console.warn('Unable to load files.log', e)
  }
  return ''
}

function writeFileLog(text) {
  text = text.replace(/\n\n/g, "\n")  // Cleanup empty lines

  let filenames = text.split("\n")
  let count = 0
  for (let filename of filenames) {
    if (filename) {
      count++
    }
  }
  filesText.text = count + " files to transfer"

  fs.writeFileSync("files.log", text, "utf-8")
}

function appendToFileLog(filename) {
  // TODO - get this working
  // let file = fs.openSync("files.log", "a+");
  // let uint8array = encoder.encode(filename);
  // fs.writeSync(file, uint8array)
  // fs.closeSync(file);

  console.log('logging file ' + filename)

  // let text = readFileLog()
  // text = text + filename + "\n"
  // writeFileLog(text)

  fileLog = fileLog + filename + "\n"
  writeFileLog(fileLog)
}

function removeFileFromLog(filename) {
  console.log('un-logging file ' + filename)

  // let text = readFileLog()
  // text = text.replace(filename + "\n", '')
  // writeFileLog(text)

  fileLog = fileLog.replace(filename + "\n", '')
  writeFileLog(fileLog)
}

setInterval(function() {
  syncFiles()
}, 10000)

let pendingFilenames = {}

function syncFiles() {
  if ((messaging.peerSocket.readyState === messaging.peerSocket.OPEN)) {
    let filenames = fileLog.split("\n")
    for (let filename of filenames) {
      if (messaging.peerSocket.bufferedAmount > 4096) {
        break
      }
      if (filename) {
        // console.log(filename)
        if (!pendingFilenames[filename]){

          try {
            let pointsText = fs.readFileSync(filename, "utf-8");

            // console.log('loaded pointsText', pointsText)

            try {
              let points = JSON.parse(pointsText)
              let action = {
                action: 'receivePoints',
                filename: filename,
                points: points
              }
              messaging.peerSocket.send(action);

              pendingFilenames[filename] = true
            } catch(e) {
              console.error('Unable to parse file ' + filename, e)
              removeFileFromLog(filename)
            }
          } catch(e) {
            console.error('Unable to load file ' + filename, e)
            removeFileFromLog(filename)
          }
        }
      }
    }
  }
}

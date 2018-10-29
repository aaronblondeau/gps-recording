/*
 * Entry point for the companion app
 */
import { settingsStorage } from "settings";
import * as messaging from "messaging";
import { me } from "companion";
import { localStorage } from "local-storage";

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
        
        localStorage.setItem(filename, JSON.stringify(points));

        // If this is a "finished" filename, send data to server
        let matches = filename.match(/^finished_track_(\d+)\.json/)
        if(matches && matches.length == 2) {
            let trackId = matches[1]
            submitTrack(trackId)
        }

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

function submitTrack(trackId) {
    // Get expected file count
    let filesToRemove = []
    let finishedFilename = 'finished_track_'+trackId+'.json'
    let expectedText = localStorage.getItem(finishedFilename)
    filesToRemove.push(finishedFilename)
    try {
        let pointsFileCount = JSON.parse(expectedText).pointsFileCount
        console.log("~~ looking for "+pointsFileCount+" point files for track "+trackId)

        let geojson = {
            "type": "Feature",
            "properties": {
                "stroke": "#ff0000",
                "stroke-width": 4,
                "stroke-opacity": 1,
                "id": trackId
            },
            "geometry": {
                type: "MultiLineString",
                coordinates: []
            }
        }


        var currentCoordinates = []

        // Find each file segment for the track
        for(let i = 0; i < pointsFileCount; i++) {
            let filename = 'track_'+trackId+'_points_'+i+'.json'
            let pointsText = localStorage.getItem(filename)
            // console.log("~~ A" + pointsText)
            if (!pointsText) {
                console.warn("Unable to submit track - missing file " + filename)
                return
            }
            try {
                let points = JSON.parse(pointsText)
                filesToRemove.push(filename)
                for (let point of points) {
                    if(point.pause) {
                        if(currentCoordinates.length > 1) {
                            geojson.geometry.coordinates.push(currentCoordinates)
                        }
                        currentCoordinates = []
                    } else {
                        currentCoordinates.push([
                            point.lng, point.lat, point.alt
                        ])
                    }
                }
                if(currentCoordinates.length > 1) {
                    geojson.geometry.coordinates.push(currentCoordinates)
                }
            } catch (error) {
                console.error("Unable to parse points file " + filename)
                return
            }
        }

        fetch('https://ablondeau.lib.id/fitbit-gps@dev/', {
            method: 'POST',
            mode: 'cors',
            cache: 'no-cache',
            body: JSON.stringify(geojson),
            headers:{
              'Content-Type': 'application/json'
            }
          }).then((response) => {
            console.log('Success submitting GeoJSON')

            // Cleanup all keys after submit success
            for (let fileToRemove of filesToRemove) {
                localStorage.removeItem(fileToRemove)
            }
          })
          .catch((error) => {
              console.error('Track submit error: ' + error)
          });

    } catch (error) {
        console.error('Unable to parse finished track', trackId, expectedText)
    }
}

function scanForFinishedTracks() {
    // Find each file segment for the track
    // console.log('scanForFinishedTracks A')
    console.log('scanForFinishedTracks ' + localStorage.length)
    // console.log('scanForFinishedTracks B')
    for(let i = 0; i < localStorage.length; i++) {
        let key = localStorage.key(i)
        // console.log(key, localStorage.getItem(key))
        let matches = key.match(/^finished_track_(\d+)\.json/)
        if(matches && matches.length == 2) {
            let trackId = matches[1]
            submitTrack(trackId)
        }
        // Dev cleanup
        // localStorage.removeItem(key)
    }
}

setTimeout(function(){
    scanForFinishedTracks()
    setInterval(function() {
        scanForFinishedTracks()
    }, 10000)
}, 2000)

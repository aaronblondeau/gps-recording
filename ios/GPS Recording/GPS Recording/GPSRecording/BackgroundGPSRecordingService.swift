//
//  BackgroundGPSRecordingService.swift
//  GPS Recording
//
//  Created by Aaron Blondeau on 6/22/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import Foundation

class BackgroundGPSRecordingService: GPSRecordingService {
    var recording: Bool = false
    var currentTrack: Track?
    var store: GPSRecordingStore
    
    var currentTrackUrl: String {
        get {
            let defaults = UserDefaults.standard
            if let path = defaults.string(forKey: "currentTrackPath") {
                return path
            }
            return ""
        }
        set {
            let defaults = UserDefaults.standard
            defaults.set(newValue, forKey: "currentTrackPath")
        }
    }
    
    var hasCurrentTrack: Bool {
        get {
            return self.currentTrackUrl != ""
        }
    }
    
    init(_ store: GPSRecordingStore) {
        self.store = store
        if(self.currentTrackUrl != "") {
            let idURL = URL(string: self.currentTrackUrl)
            if let found = store.getTrack(atURL: idURL!) {
                currentTrack = found
            }
        }
    }
    
    init (_ store: GPSRecordingStore, withTrack track: Track) {
        self.store = store
        self.currentTrack = track
        self.currentTrackUrl = track.objectID.uriRepresentation().absoluteString
    }
    
    func start() {
        // Create current track if there isn't one
        // Start location updates
        print("Background GPS Recording Start")
    }
    
    func pause() {
        // Stop location updates
        print("Background GPS Recording Pause")
    }
    
    func resume() {
        // Make sure current track is still valid
        // Start location updates
        print("Background GPS Recording Resume")
    }
    
    func finish() {
        // Stop location updates
        // Unset current track
        print("Background GPS Recording Stop")
    }
}

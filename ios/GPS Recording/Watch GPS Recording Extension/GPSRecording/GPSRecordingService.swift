//
//  GPSRecordingService.swift
//  Watch GPS Recording Extension
//
//  Created by Aaron Blondeau on 7/10/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import Foundation
import CoreLocation
import UserNotifications
import WatchKit

extension Notification.Name {
    static let gpsRecordingStarted = Notification.Name("gpsRecordingStarted")
    static let gpsRecordingStopped = Notification.Name("gpsRecordingStopped")
    static let gpsRecordingNewPoint = Notification.Name("gpsRecordingNewPoint")
    static let gpsRecordingManualPermissionsNeeded = Notification.Name("gpsRecordingManualPermissionsNeeded")
    static let gpsRecordingError = Notification.Name("gpsRecordingError")
    static let gpsRecordingAwaitingPermissions = Notification.Name("gpsRecordingAwaitingPermissions")
}

class GPSRecordingService: NSObject, CLLocationManagerDelegate {
    var recording: Bool = false
    private var _currentTrack: Track?
    let store: GPSRecordingStore
    var shouldStartRecording: Bool = false
    var interfaceController: WKInterfaceController?
    var startTime: Date?
    
    let locationManager = CLLocationManager()
    
    var currentTrack: Track? {
        get {
            return _currentTrack
        }
        set {
            _currentTrack = newValue
            if let track = newValue {
                currentTrackUrl = track.objectID.uriRepresentation().absoluteString
            } else {
                currentTrackUrl = ""
            }
        }
    }
    
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
        super.init()
        if(self.currentTrackUrl != "") {
            let idURL = URL(string: self.currentTrackUrl)
            if let found = store.getTrack(atURL: idURL!) {
                _currentTrack = found
            }
        }
        locationManager.delegate = self
        
        NotificationCenter.default.addObserver(self, selector: #selector(managedObjectContextObjectsDidChange), name: NSNotification.Name.NSManagedObjectContextObjectsDidChange, object: store.container.viewContext)
        
        NotificationCenter.default.addObserver(self, selector: #selector(defaultsChanged), name: UserDefaults.didChangeNotification, object: nil)
    }
    
    @objc func defaultsChanged() {
        locationManager.distanceFilter = Double(Settings.distanceFilterInMeters)
    }
    
    @objc func managedObjectContextObjectsDidChange() {
        if let track = currentTrack {
            if (track.isDeleted) {
                if(recording) {
                    print("~~ Current track deleted while recording!")
                    finish()
                } else {
                    print("~~ Current track deleted!")
                    currentTrack = nil
                }
            }
        }
    }
    
    func start(_ interfaceController: WKInterfaceController) {
        // Start location updates
        print("Background GPS Recording Start")
        
        self.interfaceController = interfaceController
        
        // Start the process by ensuring we have gps recording permissions
        requestPermissions()
    }
    
    func pause() {
        // Stop location updates
        stopRecording()
        print("Background GPS Recording Pause")
    }
    
    func resume() {
        
        if let track = currentTrack {
            do {
                let _ = try store.addLine(toTrack: track)
            } catch {
                print("~~ Failed to add a new line for resume: \(error.localizedDescription)")
                
                // Don't start
                return
            }
        }
        
        print("Background GPS Recording Resume")
        requestPermissions()
    }
    
    func finish() {
        // Stop location updates
        // Unset current track
        print("Background GPS Recording Stop")
        currentTrack = nil
        stopRecording()
    }
    
    private func startRecording() {
        shouldStartRecording = false
        
        // Create a current track if there isn't one
        if currentTrack == nil {
            let formatter = DateFormatter()
            formatter.dateFormat = "yyyy-MM-dd HH:mm"
            let trackName = formatter.string(from: Date())
            
            do {
                currentTrack = try store.createTrack(name: trackName, note: "", activity: nil)
            } catch {
                NotificationCenter.default.post(name: .gpsRecordingError, object: "There was a problem creating a Track : \(error.localizedDescription)")
                return;
            }
            
        }
        
        locationManager.allowsBackgroundLocationUpdates = true
        locationManager.distanceFilter = Double(Settings.distanceFilterInMeters)
        locationManager.desiredAccuracy = kCLLocationAccuracyNearestTenMeters
        
        startTime = Date()
        
        locationManager.startUpdatingLocation()
        recording = true
        NotificationCenter.default.post(name: .gpsRecordingStarted, object: self.store)
    }
    
    private func stopRecording() {
        locationManager.stopUpdatingLocation()
        recording = false
        NotificationCenter.default.post(name: .gpsRecordingStopped, object: self.store)
    }
    
    func requestPermissions() {
        switch CLLocationManager.authorizationStatus() {
        case .notDetermined:
            // We have not asked for permissions yet
            // Ask for it, and begin recording once the user has granted permission
            locationManager.requestAlwaysAuthorization()
            NotificationCenter.default.post(name: .gpsRecordingAwaitingPermissions, object: nil)
            shouldStartRecording = true
            break
            
        case .restricted, .denied:
            // Permission has been denied - show alert directing user to location setttings for app
            showManualPermissionsMessage()
            break
            
        case .authorizedWhenInUse:
            // This is too low of a permission level for background - show alert directing user to location setttings for app
            showManualPermissionsMessage()
            break
            
        case .authorizedAlways:
            // We are good to go
            startRecording()
            break
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        if(status == .authorizedAlways) {
            // The user granted the needed permissions - start recording
            if  (shouldStartRecording) {
                startRecording()
            }
        } else {
            shouldStartRecording = false
            
            // Perms changed while recording!
            if(recording) {
                // Stop recording
                pause()
                
                // Send a local notification to user to inform them that they interrupted recording
                let center = UNUserNotificationCenter.current()
                center.getNotificationSettings { (settings) in
                    if(settings.authorizationStatus == .authorized)
                    {
                        let notification = UNMutableNotificationContent()
                        notification.title = "GPS Recording Interrupted"
                        notification.subtitle = "Permissions Denied"
                        notification.body = "GPS Permissions were removed so recording has been interrupted!"
                        
                        let notificationTrigger = UNTimeIntervalNotificationTrigger(timeInterval: 0.1, repeats: false)
                        let request = UNNotificationRequest(identifier: "notification_recording_permissions_interrupted", content: notification, trigger: notificationTrigger)
                        
                        UNUserNotificationCenter.current().add(request, withCompletionHandler: nil)
                    }
                }
            }
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        if (locations.count > 0) {
            for location in locations {
                print("~~ \(location.coordinate.latitude), \(location.coordinate.longitude) time=\(location.timestamp) acc=\(location.horizontalAccuracy)")
                if let track = currentTrack {
                    // Save the point
                    do {
                        // Don't try to record if track got deleted
                        // Throw out points with low accuracy
                        var pointTimeValid = true
                        if let startTime = self.startTime {
                            // iOS sometimes sends us old points from a previous recording session
                            if location.timestamp.compare(startTime) != .orderedDescending {
                                pointTimeValid = false
                                print("~~ Discarding a point because timestamp \(location.timestamp) is before recording start time \(startTime)")
                            }
                        }
                        if ((!track.isDeleted) && (location.horizontalAccuracy <= 50) && pointTimeValid) {
                            let point = try store.addPoint(toTrack: track, fromLocation: location)
                            NotificationCenter.default.post(name: .gpsRecordingNewPoint, object: point)
                        }
                    } catch {
                        print("~~ Unable to save a point : \(error.localizedDescription)")
                    }
                } else {
                    print("~~ Received a point, but no current track to store it in!")
                }
            }
        }
    }
    
    func showManualPermissionsMessage() {
        NotificationCenter.default.post(name: .gpsRecordingManualPermissionsNeeded, object: nil)
    }
}

//
//  BackgroundGPSRecordingService.swift
//  GPS Recording
//
//  Created by Aaron Blondeau on 6/22/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import Foundation
import CoreLocation
import UIKit
import UserNotifications

class BackgroundGPSRecordingService: NSObject, GPSRecordingService, CLLocationManagerDelegate {
    var recording: Bool = false
    private var _currentTrack: Track?
    let store: GPSRecordingStore
    var shouldStartRecording: Bool = false
    var viewController: UIViewController?
    
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
    
    func start(_ viewController: UIViewController) {
        // Start location updates
        print("Background GPS Recording Start")
        
        self.viewController = viewController
        
        // Start the process by ensuring we have gps recording permissions
        requestPermissions()
    }
    
    func pause() {
        // Stop location updates
        stopRecording()
        print("Background GPS Recording Pause")
    }
    
    func resume() {
        // Make sure current track is still valid
        // Start location updates
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
                if let vc = viewController {
                    let alert = UIAlertController(title: "Unable to create track!", message: "\(error.localizedDescription)", preferredStyle: .alert)
                    alert.addAction(UIAlertAction(title: "Ok", style: .default, handler: nil))
                    vc.present(alert, animated: true)
                }
                return;
            }
            
        }
        
        locationManager.allowsBackgroundLocationUpdates = true
        locationManager.pausesLocationUpdatesAutomatically = false
        locationManager.distanceFilter = 10.0
        locationManager.desiredAccuracy = kCLLocationAccuracyNearestTenMeters
        
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
            shouldStartRecording = true
            break
            
        case .restricted, .denied:
            // Permission has been denied - show alert directing user to location setttings for app
            showManualPermissionsDialog()
            break
            
        case .authorizedWhenInUse:
            // This is too low of a permission level for background - show alert directing user to location setttings for app
            showManualPermissionsDialog()
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
                print("~~ \(location.coordinate.latitude), \(location.coordinate.longitude) acc=\(location.horizontalAccuracy)")
                if let track = currentTrack {
                    // Save the point
                    do {
                        if (!track.isDeleted) {
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
    
    func showManualPermissionsDialog() {
        if let vc = viewController {
            let alert = UIAlertController(title: "GPS Permissions Needed!", message: "This app needs permission to 'Always' access GPS so that it can record even while you are using other apps.  The permission has previously been denied for this app.  You need to go to the device settings for this app and set Location to 'Always'.", preferredStyle: .alert)
            
            alert.addAction(UIAlertAction(title: "Take Me There", style: .default, handler: {
                action in
                self.shouldStartRecording = true
                print("Would try to take user to app settings.")
                if let url = NSURL(string: UIApplicationOpenSettingsURLString) as URL? {
                    UIApplication.shared.open(url, options: [:], completionHandler: nil)
                }
            }))
            alert.addAction(UIAlertAction(title: "Cancel", style: .cancel, handler: {
                action in
                self.shouldStartRecording = false
            }))
            vc.present(alert, animated: true)
        }
    }
}

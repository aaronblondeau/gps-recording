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
    var currentTrack: Track?
    let store: GPSRecordingStore
    var shouldStartRecording: Bool = false
    var viewController: UIViewController?
    
    let locationManager = CLLocationManager()
    
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
                currentTrack = found
            }
        }
        locationManager.delegate = self
    }
    
    func start(_ viewController: UIViewController) {
        // Create current track if there isn't one
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
    }
    
    func finish() {
        // Stop location updates
        // Unset current track
        print("Background GPS Recording Stop")
        stopRecording()
    }
    
    private func startRecording() {
        shouldStartRecording = false
        recording = true
        print("Would start recording")
        
        locationManager.allowsBackgroundLocationUpdates = true
        locationManager.pausesLocationUpdatesAutomatically = false
        locationManager.distanceFilter = 10.0
        locationManager.desiredAccuracy = kCLLocationAccuracyNearestTenMeters
        locationManager.startUpdatingLocation()
        
        NotificationCenter.default.post(name: .gpsRecordingStarted, object: self.store)
    }
    
    private func stopRecording() {
        recording = false
        print("Would stop recording")
        
        locationManager.stopUpdatingLocation()
                
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
        print("~~ didChangeAuthorization status A")
        if(status == .authorizedAlways) {
            print("~~ didChangeAuthorization status B")
            // The user granted the needed permissions - start recording
            if  (shouldStartRecording) {
                startRecording()
            }
        } else {
            print("~~ didChangeAuthorization status C")
            shouldStartRecording = false
            
            // Perms changed while recording!
            if(recording) {
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
                print("~~ \(location.coordinate.latitude), \(location.coordinate.longitude)")
            }
        }
    }
    
    func showManualPermissionsDialog() {
        print("~~ showManualPermissionsDialog")
        
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
        
        if let vc = viewController {
            vc.present(alert, animated: true)
        }
    }
}

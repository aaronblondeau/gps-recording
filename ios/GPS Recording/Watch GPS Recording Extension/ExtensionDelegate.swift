//
//  ExtensionDelegate.swift
//  Watch GPS Recording Extension
//
//  Created by Aaron Blondeau on 7/9/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import WatchKit
import CoreData
import WatchConnectivity

class ExtensionDelegate: NSObject, WKExtensionDelegate, WCSessionDelegate {
    
    var store: GPSRecordingStore?
    var service: GPSRecordingService?

    func applicationDidFinishLaunching() {
        // Perform any final initialization of your application.
        
        // Get access to the recording store
        let bundle = Bundle(for: type(of: self))
        GPSRecordingStore.buildContainer(bundle: bundle) {
            (container: NSPersistentContainer) in
            self.store = GPSRecordingStore(withContainer: container)
            self.service = GPSRecordingService(self.store!)
            
            if let rootController = WKExtension.shared().rootInterfaceController as? InterfaceController {
                rootController.store = self.store
                rootController.service = self.service
            }
            self.sendRecordStatus()
            NotificationCenter.default.post(name: .gpsRecordingStoreReady, object: self.store)
        }
        
        setupWatchConnectivity()
        
        NotificationCenter.default.addObserver(self, selector: #selector(onRecordingStarted(notification:)), name: .gpsRecordingStarted, object: nil)
        
        NotificationCenter.default.addObserver(self, selector: #selector(onRecordingStopped(notification:)), name: .gpsRecordingStopped, object: nil)
    }

    func applicationDidBecomeActive() {
        // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
    }

    func applicationWillResignActive() {
        // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
        // Use this method to pause ongoing tasks, disable timers, etc.
    }

    func handle(_ backgroundTasks: Set<WKRefreshBackgroundTask>) {
        // Sent when the system needs to launch the application in the background to process tasks. Tasks arrive in a set, so loop through and process each one.
        for task in backgroundTasks {
            // Use a switch statement to check the task type
            switch task {
            case let backgroundTask as WKApplicationRefreshBackgroundTask:
                // Be sure to complete the background task once you’re done.
                backgroundTask.setTaskCompletedWithSnapshot(false)
            case let snapshotTask as WKSnapshotRefreshBackgroundTask:
                // Snapshot tasks have a unique completion call, make sure to set your expiration date
                snapshotTask.setTaskCompleted(restoredDefaultState: true, estimatedSnapshotExpiration: Date.distantFuture, userInfo: nil)
            case let connectivityTask as WKWatchConnectivityRefreshBackgroundTask:
                // Be sure to complete the connectivity task once you’re done.
                connectivityTask.setTaskCompletedWithSnapshot(false)
            case let urlSessionTask as WKURLSessionRefreshBackgroundTask:
                // Be sure to complete the URL session task once you’re done.
                urlSessionTask.setTaskCompletedWithSnapshot(false)
            default:
                // make sure to complete unhandled task types
                task.setTaskCompletedWithSnapshot(false)
            }
        }
    }
    
    // MARK: - Watch Connectivity
    
    func setupWatchConnectivity() {
        if WCSession.isSupported() {
            let session  = WCSession.default
            session.delegate = self
            session.activate()
        }
    }
    
    func session(_ session: WCSession, activationDidCompleteWith
        activationState: WCSessionActivationState, error: Error?) {
        if let error = error {
            print("WC Session activation failed with error: \(error.localizedDescription)")
            return
        }
        print("WC Session activated with state: \(activationState.rawValue)")
    }
    
    func session(_ session: WCSession, didReceiveApplicationContext applicationContext:[String:Any]) {
        if let phone_recording = applicationContext["phone_recording"] as? Bool {
            if (phone_recording) {
                print("~~ Watch: phone sent message to say it is recording")
            } else {
                print("~~ Watch: phone sent message to say it is not recording")
            }
        }
    }
    
    @objc func onRecordingStarted(notification: NSNotification) {
        sendRecordStatus()
    }
    
    @objc func onRecordingStopped(notification: NSNotification) {
        sendRecordStatus()
    }
    
    func sendRecordStatus() {
        if WCSession.isSupported() {
        let session = WCSession.default
            do {
                var dictionary = ["watch_recording": false]
                if let service = self.service {
                    dictionary = ["watch_recording": service.recording]
                }
                try session.updateApplicationContext(dictionary)
            } catch {
                print("ERROR: \(error)")
            }
        }
    }

}

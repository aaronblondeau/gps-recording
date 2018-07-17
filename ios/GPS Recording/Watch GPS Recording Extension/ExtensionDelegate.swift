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
    
    var wcUserInfoQueue: [[String : Any]]?

    func applicationDidFinishLaunching() {
        // Perform any final initialization of your application.
        
        wcUserInfoQueue = [[String : Any]]()
        self.setupWatchConnectivity()
        
        // Get access to the recording store
        let bundle = Bundle(for: type(of: self))
        GPSRecordingStore.buildContainer(bundle: bundle) {
            (container: NSPersistentContainer) in
            self.store = GPSRecordingStore(withContainer: container)
            self.service = BackgroundGPSRecordingService(self.store!)
            
            if let rootController = WKExtension.shared().rootInterfaceController as? InterfaceController {
                rootController.store = self.store
                rootController.service = self.service
            }
            NotificationCenter.default.post(name: .gpsRecordingStoreReady, object: self.store)
            self.sendRecordStatus()
        }
        
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
    
    func enqueueWCUserInfo(_ userInfo: [String : Any] = [:]) {
        print("~~ queueing a wc userinfo item")
        wcUserInfoQueue?.append(userInfo)
    }
    
    func processWCUserInfoQueue() {
        if let items = wcUserInfoQueue, let store = self.store {
            print("~~ processing \(items.count) wc userinfo items")
            for item in items {
                processWCUserInfo(store, WCSession.default, item)
            }
        }
    }
    
    func processWCUserInfo(_ store: GPSRecordingStore, _ session : WCSession, _ userInfo: [String : Any] = [:]) {
        if let action = userInfo["action"] as? String {
            if (action == "track_from_watch_result") {
                print("~~ Watch got a track result from the phone")
                if let success = userInfo["success"] as? Bool {
                    if (success) {
                        // TODO - or we can delete the local copy now that it is safely on the phone...
                        if let downstreamId = userInfo["downstreamId"] as? String, let id = userInfo["id"] as? String {
                            if let url = URL(string: id) {
                                if let track = store.getTrack(atURL: url) {
                                    do {
                                        try store.saveDownstreamId(track: track, downstreamId: downstreamId)
                                        
                                        NotificationCenter.default.post(name: .gpsRecordingTransferToPhoneSuccess, object: track)
                                        
                                        // Remove transfer file
                                        if let transferFileUrl = track.getTransferFileUrl() {
                                            do {
                                                let fileManager = FileManager.default
                                                try fileManager.removeItem(at: transferFileUrl)
                                                print("~~ removed temporary transfer file")
                                            } catch {
                                                print("~~ failed to remove temporary transfer file \(error.localizedDescription)")
                                            }
                                        }
                                        
                                    } catch {
                                        print("~~ failed to save downstream id for track!")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if let error = userInfo["error"] as? Bool {
                        print("~~ Phone sent a message to say that it failed to save a track : \(error)")
                    }
                }
            }
        }
    }
    
    func session(_ session: WCSession, didReceiveUserInfo userInfo: [String : Any] = [:]) {
        if let store = self.store {
            processWCUserInfo(store, session, userInfo)
        } else {
            // Store wasn't ready : add to queue
            enqueueWCUserInfo(userInfo)
        }
    }

}

//
//  RecordInterfaceController.swift
//  Watch GPS Recording Extension
//
//  Created by Aaron Blondeau on 7/10/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import WatchKit
import Foundation

class RecordInterfaceController: WKInterfaceController {
    
    var store: GPSRecordingStore?
    var service: GPSRecordingService?
    var observingStore = false
    
    override func awake(withContext context: Any?) {
        super.awake(withContext: context)
        self.setTitle("Record")
        // Configure interface objects here.
        
        NotificationCenter.default.addObserver(self, selector: #selector(onRecordingStarted(notification:)), name: .gpsRecordingStarted, object: nil)
        
        NotificationCenter.default.addObserver(self, selector: #selector(onRecordingStopped(notification:)), name: .gpsRecordingStopped, object: nil)
        
        if context != nil {
            let ctx = context as! GPSRecordingContext
            if let store = ctx.store {
                self.store = store
                observeStore()
                print("~~ Got store from context")
            }
            if let service = ctx.service {
                self.service = service
                print("~~ Got service from context")
            }
        }
    }
    
    override func willActivate() {
        // This method is called when watch view controller is about to be visible to user
        super.willActivate()
    }
    
    override func didDeactivate() {
        // This method is called when watch view controller is no longer visible
        super.didDeactivate()
    }
    
    @IBAction func onCreateTestTrack() {
        let action1 = WKAlertAction(title: "Create", style: .default) {
            do {
                let formatter = DateFormatter()
                formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
                let _ = try self.store?.createTrack(name: formatter.string(from: Date()), note: "Test", activity: nil)
            } catch {
                print("~~ Failed to create test track: \(error.localizedDescription)")
            }
        }
        let action2 = WKAlertAction(title: "Cancel", style: .cancel) {}
        
        presentAlert(withTitle: "Test Track", message: "", preferredStyle: .actionSheet, actions: [action1, action2])
    }
    
    @objc func onStoreLoaded(notification: NSNotification) {
        self.store = notification.object as? GPSRecordingStore
        observeStore()
    }
    
    func observeStore() {
        if let store = self.store {
            if (!observingStore) {
                NotificationCenter.default.addObserver(self, selector: #selector(managedObjectContextObjectsDidChange), name: NSNotification.Name.NSManagedObjectContextObjectsDidChange, object: store.container.viewContext)
                observingStore = true
            }
        }
    }
    
    @objc func managedObjectContextObjectsDidChange() {
        updateStats()
    }
    
    @objc func onRecordingStarted(notification: NSNotification) {
        updateButtons()
    }
    
    @objc func onRecordingStopped(notification: NSNotification) {
        updateButtons()
    }
    
    func updateStats() {
        print("~~ TODO - update stats")
    }
    
    func updateButtons() {
        print("~~ TODO - update buttons")
    }
}

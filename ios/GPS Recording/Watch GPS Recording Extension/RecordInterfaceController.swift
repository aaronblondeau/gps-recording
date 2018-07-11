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
    var finishedTrack: Track?
    
    @IBOutlet var startButton: WKInterfaceButton!
    @IBOutlet var pauseButton: WKInterfaceButton!
    @IBOutlet var resumeButton: WKInterfaceButton!
    @IBOutlet var finishButton: WKInterfaceButton!
    @IBOutlet var labelDistance: WKInterfaceLabel!
    @IBOutlet var labelDuration: WKInterfaceLabel!
    @IBOutlet var labelMessage: WKInterfaceLabel!
    
    override func awake(withContext context: Any?) {
        super.awake(withContext: context)
        self.setTitle("Record")
        // Configure interface objects here.
        
        NotificationCenter.default.addObserver(self, selector: #selector(onRecordingStarted(notification:)), name: .gpsRecordingStarted, object: nil)
        
        NotificationCenter.default.addObserver(self, selector: #selector(onRecordingStopped(notification:)), name: .gpsRecordingStopped, object: nil)
        
        NotificationCenter.default.addObserver(self, selector: #selector(onGPSRecordingError(notification:)), name: .gpsRecordingError, object: nil)

        NotificationCenter.default.addObserver(self, selector: #selector(onGPSRecordingManualPermissionsNeeded(notification:)), name: .gpsRecordingManualPermissionsNeeded, object: nil)
        
        NotificationCenter.default.addObserver(self, selector: #selector(onGPSAwaitingPermissions(notification:)), name: .gpsRecordingAwaitingPermissions, object: nil)
        
        if context != nil {
            let ctx = context as! GPSRecordingContext
            if let service = ctx.service {
                self.service = service
                print("~~ Got service from context")
            }
            if let store = ctx.store {
                self.store = store
                observeStore()
                print("~~ Got store from context")
            }
        }
        
        updateStats()
        updateButtons()
    }
    
    override func willActivate() {
        // This method is called when watch view controller is about to be visible to user
        super.willActivate()
    }
    
    override func didDeactivate() {
        // This method is called when watch view controller is no longer visible
        super.didDeactivate()
    }
    
    @objc func onStoreLoaded(notification: NSNotification) {
        self.store = notification.object as? GPSRecordingStore
        observeStore()
    }
    
    @objc func onGPSRecordingError(notification: NSNotification) {
        if let message = notification.object as? String {
            let action1 = WKAlertAction(title: "Ok", style: .default) {}
            presentAlert(withTitle: "Error", message: message, preferredStyle: .actionSheet, actions: [action1])
        }
    }
    
    @objc func onGPSRecordingManualPermissionsNeeded(notification: NSNotification) {
        labelMessage.setText("Allow 'Always' location permissions for this app via the Settings in your iPhone to enable GPS recording.")
        labelMessage.setHidden(false)
    }
    
    @objc func onGPSAwaitingPermissions(notification: NSNotification) {
        labelMessage.setText("Allow 'Always' location permissions for this app on your iPhone to enable GPS recording.")
        labelMessage.setHidden(false)
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
        if let track = self.service?.currentTrack {
            let formatter = DateComponentsFormatter()
            formatter.allowedUnits = [.hour, .minute, .second]
            formatter.unitsStyle = .full
            let formattedString = formatter.string(from: TimeInterval(track.totalDurationInMilliseconds / 1000))!
            labelDuration.setText("\(formattedString)")
            
            if Settings.useMetricUnits {
                let kilometers = Double(round(100*(track.totalDistanceInMeters / 1000))/100)
                labelDistance.setText("\(kilometers) km")
            } else {
                let miles = Double(round(100*(track.totalDistanceInMeters * 0.000621371))/100)
                labelDistance.setText("\(miles) miles")
            }
        } else {
            if Settings.useMetricUnits {
                labelDuration.setText("0 seconds")
                labelDistance.setText("? km")
            } else {
                labelDuration.setText("0 seconds")
                labelDistance.setText("? miles")
            }
        }
    }
    
    func updateButtons() {
        if let service = self.service {
            startButton.setEnabled(!service.recording)
            startButton.setHidden(service.hasCurrentTrack || service.recording)
            
            pauseButton.setEnabled(service.recording)
            pauseButton.setHidden(!service.recording)
            
            resumeButton.setEnabled(!service.recording && service.hasCurrentTrack)
            resumeButton.setHidden(service.recording || !service.hasCurrentTrack)
            
            finishButton.setEnabled(service.hasCurrentTrack)
            finishButton.setHidden(!service.hasCurrentTrack)
            
            labelDistance.setHidden(!(service.hasCurrentTrack || service.recording))
            labelDuration.setHidden(!(service.hasCurrentTrack || service.recording))
        } else {
            startButton.setEnabled(false)
            pauseButton.setEnabled(false)
            resumeButton.setEnabled(false)
            finishButton.setEnabled(false)
            
            startButton.setHidden(false)
            pauseButton.setHidden(true)
            resumeButton.setHidden(true)
            finishButton.setHidden(true)
            
            labelDistance.setHidden(true)
            labelDistance.setHidden(true)
        }
        labelMessage.setHidden(true)
    }
    
    @IBAction func onStartButton() {
        service?.start(self)
        updateButtons()
    }
    
    @IBAction func onPauseButton() {
        service?.pause()
        updateButtons()
    }
    
    @IBAction func onResumeButton() {
        service?.resume()
        updateButtons()
    }
    
    @IBAction func onFinishButton() {
        finishedTrack = service?.currentTrack
        service?.finish()
        updateButtons()
        
        let context = GPSRecordingContext()
        context.store = self.store
        context.service = self.service
        if let track = self.finishedTrack {
            context.track = track
        }
        
        pushController(withName: "trackDetail", context: context)
    }
    
//    override func contextForSegue(withIdentifier segueIdentifier: String) -> Any? {
//        print("~~ segue (from record) \(segueIdentifier)")
//
//        return context
//    }
    
}

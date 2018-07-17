//
//  TrackInterfaceController.swift
//  Watch GPS Recording Extension
//
//  Created by Aaron Blondeau on 7/10/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import WatchKit
import Foundation
import WatchConnectivity

class TrackInterfaceController: WKInterfaceController {
    
    var track: Track?
    var store: GPSRecordingStore?
    var service: GPSRecordingService?
    
    @IBOutlet var deleteButton: WKInterfaceButton!
    @IBOutlet var durationLabel: WKInterfaceLabel!
    @IBOutlet var distanceLabel: WKInterfaceLabel!
    @IBOutlet var sendToPhoneButton: WKInterfaceButton!
    @IBOutlet var openOnPhoneButton: WKInterfaceButton!
    @IBOutlet var syncMessage: WKInterfaceLabel!
    
    override func awake(withContext context: Any?) {
        super.awake(withContext: context)
        
        syncMessage.setHidden(true)
        sendToPhoneButton.setHidden(true)
        openOnPhoneButton.setHidden(true)
        
        // Configure interface objects here.
        if context != nil {
            let ctx = context as! GPSRecordingContext
            if let track = ctx.track {
                self.track = track
                
                if let downstreamId = track.downstreamId {
                    if (downstreamId == "-pending-") {
                        sendToPhoneButton.setHidden(false)
                        openOnPhoneButton.setHidden(true)
                        syncMessage.setHidden(false)
                        syncMessage.setText("Transfer in progress. Please open app on iPhone to complete.")
                    } else if (downstreamId != "") {
                        sendToPhoneButton.setHidden(true)
                        openOnPhoneButton.setHidden(false)
                    }
                } else {
                    sendToPhoneButton.setHidden(false)
                    openOnPhoneButton.setHidden(true)
                }
            }
            if let service = ctx.service {
                self.service = service
            }
            if let store = ctx.store {
                self.store = store
            }
            updateUI()
        }
        
        NotificationCenter.default.addObserver(self, selector: #selector(onTransferSuccess(notification:)), name: .gpsRecordingTransferToPhoneSuccess, object: nil)
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
    }

    override func willActivate() {
        // This method is called when watch view controller is about to be visible to user
        super.willActivate()
    }

    override func didDeactivate() {
        // This method is called when watch view controller is no longer visible
        super.didDeactivate()
    }
    
    func updateUI() {
        if let track = self.track {
            self.setTitle(track.name)
        }
        
        if store != nil && track != nil {
            deleteButton.setEnabled(true)
        } else {
            deleteButton.setEnabled(false)
        }
        
        updateStats()
    }
    
    func updateStats() {
        if let track = self.track {
            let formatter = DateComponentsFormatter()
            formatter.allowedUnits = [.hour, .minute, .second]
            formatter.unitsStyle = .full
            let formattedString = formatter.string(from: TimeInterval(track.totalDurationInMilliseconds / 1000))!
            durationLabel.setText("\(formattedString)")
            
            if Settings.useMetricUnits {
                let kilometers = Double(round(100*(track.totalDistanceInMeters / 1000))/100)
                distanceLabel.setText("\(kilometers) km")
            } else {
                let miles = Double(round(100*(track.totalDistanceInMeters * 0.000621371))/100)
                distanceLabel.setText("\(miles) miles")
            }
        } else {
            if Settings.useMetricUnits {
                durationLabel.setText("0 seconds")
                durationLabel.setText("? km")
            } else {
                durationLabel.setText("0 seconds")
                durationLabel.setText("? miles")
            }
        }
    }

    @IBAction func onDelete() {
        self.deleteButton.setEnabled(false)
        let action1 = WKAlertAction(title: "Delete", style: .default) {
            if let track = self.track {
                do {
                    try self.store?.delete(track: track)
                    self.pop()
                } catch {
                    print("~~ Failed to delete track \(error.localizedDescription)")
                }
            }
        }
        let action2 = WKAlertAction(title: "Cancel", style: .cancel) {
            self.deleteButton.setEnabled(true)
        }
        
        presentAlert(withTitle: "Delete Track?", message: "", preferredStyle: .actionSheet, actions: [action1, action2])
    }
    
    @objc func onTransferSuccess(notification: NSNotification) {
        if let transferredTrack = notification.object as? Track, let track = self.track {
            if(transferredTrack.objectID == track.objectID) {
                // The viewed track has been transferred to the phone
                sendToPhoneButton.setHidden(true)
                openOnPhoneButton.setHidden(false)
                syncMessage.setHidden(true)
            }
        }
    }
    
    @IBAction func onSendToPhoneButton() {
        sendToPhoneButton.setEnabled(false)
        sendToPhoneButton.setHidden(true)
        
// // Does not work for large tracks
//        if WCSession.isSupported() {
//            if let track = self.track {
//                var dictionary = track.toDict()
//                dictionary["action"] = "track_from_watch"
//                WCSession.default.transferUserInfo(dictionary)
//                print("~~ Track sent to phone")
//                do {
//                    try store?.saveDownstreamId(track: track, downstreamId: "-pending-")
//                    syncMessage.setText("Transfer in progress. Please open app on iPhone to complete.")
//                    syncMessage.setHidden(false)
//                } catch {
//                    print("~~ Failed to set downstream id as pending")
//                }
//            }
//        }
        
        if WCSession.isSupported() {
            if let track = self.track {
                let trackDictionary = track.toDict()
                var metaDictionary = [String:Any]()
                metaDictionary["action"] = "track_from_watch"
                
                
                if let fileUrl = track.getTransferFileUrl() {
                    
                    NSKeyedArchiver.archiveRootObject(trackDictionary, toFile: fileUrl.path)
                    WCSession.default.transferFile(fileUrl, metadata: metaDictionary)
                    
                    print("~~ Track sent to phone via \(fileUrl.absoluteString)")
                    do {
                        try store?.saveDownstreamId(track: track, downstreamId: "-pending-")
                        syncMessage.setText("Transfer in progress. Please open app on iPhone to complete.")
                        syncMessage.setHidden(false)
                    } catch {
                        print("~~ Failed to set downstream id as pending")
                    }
                    
                } else {
                    print("~~ Track could not be sent - unable to generate file url.")
                }
            }
        }
    }
    
    @IBAction func onOpenOnPhone() {
        if WCSession.isSupported() {
            if let track = self.track {
                var dictionary = [String:Any]()
                dictionary["id"] = track.downstreamId
                dictionary["action"] = "open_track_from_watch"
                WCSession.default.transferUserInfo(dictionary)
            }
        }
    }
}

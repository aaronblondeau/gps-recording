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
    
    override func awake(withContext context: Any?) {
        super.awake(withContext: context)
        
        // Configure interface objects here.
        if context != nil {
            let ctx = context as! GPSRecordingContext
            if let track = ctx.track {
                self.track = track
            }
            if let service = ctx.service {
                self.service = service
            }
            if let store = ctx.store {
                self.store = store
            }
            updateUI()
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
    
    @IBAction func onSendToPhoneButton() {
        print("~~ Would send to phone")
        if WCSession.isSupported() {
            if let track = self.track {
                do {
                    let dictionary = ["track": track.objectID.uriRepresentation().absoluteString]
                    try WCSession.default.updateApplicationContext(dictionary)
                } catch {
                    print("ERROR: \(error)")
                }
            }
        }
    }
    
}

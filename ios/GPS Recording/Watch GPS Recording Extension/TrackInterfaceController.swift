//
//  TrackInterfaceController.swift
//  Watch GPS Recording Extension
//
//  Created by Aaron Blondeau on 7/10/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import WatchKit
import Foundation


class TrackInterfaceController: WKInterfaceController {
    
    var track: Track?
    var store: GPSRecordingStore?
    var service: GPSRecordingService?
    
    @IBOutlet var deleteButton: WKInterfaceButton!
    
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
}

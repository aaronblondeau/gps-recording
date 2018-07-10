//
//  InterfaceController.swift
//  Watch GPS Recording Extension
//
//  Created by Aaron Blondeau on 7/9/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import WatchKit
import Foundation

class InterfaceController: WKInterfaceController {
    
    var store: GPSRecordingStore?
    var service: GPSRecordingService?

    @IBOutlet var recordButton: WKInterfaceButton!
    @IBOutlet var listButton: WKInterfaceButton!
    
    override func awake(withContext context: Any?) {
        super.awake(withContext: context)
        
        // Configure interface objects here.
        NotificationCenter.default.addObserver(self, selector: #selector(onStoreLoaded(notification:)), name: .gpsRecordingStoreReady, object: nil)
        
        if store == nil {
            recordButton.setEnabled(false)
            listButton.setEnabled(false)
        }
    }
    
    @objc func onStoreLoaded(notification: NSNotification) {
        print("~~ onStoreLoaded")
        self.store = notification.object as? GPSRecordingStore
        recordButton.setEnabled(true)
        listButton.setEnabled(true)
    }
    
    override func willActivate() {
        // This method is called when watch view controller is about to be visible to user
        super.willActivate()
    }
    
    override func didDeactivate() {
        // This method is called when watch view controller is no longer visible
        super.didDeactivate()
    }
    
    override func contextForSegue(withIdentifier segueIdentifier: String) -> Any? {
        let context = GPSRecordingContext()
        context.store = self.store
        context.service = self.service
        return context
    }

}

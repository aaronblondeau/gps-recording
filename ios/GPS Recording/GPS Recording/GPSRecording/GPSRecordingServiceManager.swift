//
//  GPSRecordingServiceManager.swift
//  GPS Recording
//
//  Created by Aaron Blondeau on 7/4/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import Foundation

class GPSRecordingServiceManager {
    
    let service: GPSRecordingService
    
    init(_ store: GPSRecordingStore) {
        self.service = BackgroundGPSRecordingService(store)
    }
    
    // TODO - and functions to allow switch between Background and Foreground GPS recording services.
    
}

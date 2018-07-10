//
//  ListRowController.swift
//  Watch GPS Recording Extension
//
//  Created by Aaron Blondeau on 7/10/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import WatchKit

class ListRowController: NSObject {

    @IBOutlet var titleLabel: WKInterfaceLabel!
    @IBOutlet var detailLabel: WKInterfaceLabel!
    
    func setTrack(_ track: Track, service: GPSRecordingService) {
        
        if let name = track.name {
            titleLabel.setText(name)
        } else {
            let formatter = DateFormatter()
            formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
            titleLabel.setText(formatter.string(from: track.startedAt))
        }
        
//        if (service.currentTrack?.objectID == track.objectID) {
//            titleLabel.font = UIFont.boldSystemFont(ofSize: 16.0)
//        } else {
//            titleLabel.font = UIFont.systemFont(ofSize: 16.0)
//        }
        
        var labelText = ""
        if Settings.useMetricUnits {
            let kilometers = Double(round(100*(track.totalDistanceInMeters / 1000))/100)
            labelText = "\(kilometers) km"
        } else {
            let miles = Double(round(100*(track.totalDistanceInMeters * 0.000621371))/100)
            labelText = "\(miles) miles"
        }
        
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd HH:mm"
        labelText = "\(labelText) - \(formatter.string(from: track.startedAt))"
        
        detailLabel.setText(labelText)
        
    }
}

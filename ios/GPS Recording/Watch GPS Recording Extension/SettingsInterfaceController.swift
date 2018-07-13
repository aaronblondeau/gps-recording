//
//  SettingsInterfaceController.swift
//  Watch GPS Recording Extension
//
//  Created by Aaron Blondeau on 7/10/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import WatchKit
import Foundation

class SettingsInterfaceController: WKInterfaceController {
    
    @IBOutlet var metricUnitsSwitch: WKInterfaceSwitch!
    @IBOutlet var distanceFilterSlider: WKInterfaceSlider!
    @IBOutlet var distanceFilterValueLabel: WKInterfaceLabel!
    
    override func awake(withContext context: Any?) {
        super.awake(withContext: context)
        self.setTitle("Settings")
        // Configure interface objects here.
        metricUnitsSwitch.setOn(Settings.useMetricUnits)
        distanceFilterSlider.setValue(Settings.distanceFilterInMeters)
        distanceFilterValueLabel.setText("\(Settings.distanceFilterInMeters) meters")
    }
    
    override func willActivate() {
        // This method is called when watch view controller is about to be visible to user
        super.willActivate()
    }
    
    override func didDeactivate() {
        // This method is called when watch view controller is no longer visible
        super.didDeactivate()
    }

    @IBAction func onUnitsChange(_ value: Bool) {
        Settings.useMetricUnits = value
    }
    
    @IBAction func onDistanceFilterChange(_ value: Float) {
        Settings.distanceFilterInMeters = value
        distanceFilterValueLabel.setText("\(Settings.distanceFilterInMeters) meters")
    }
    
}

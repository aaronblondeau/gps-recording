//
//  Settings.swift
//  GPS Recording
//
//  Created by Aaron Blondeau on 5/16/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import Foundation

class Settings {
    
    class var useMetricUnits: Bool {
        get {
            let defaults = UserDefaults.standard
            return defaults.bool(forKey: "useMetricUnits")
        }
        set {
            let defaults = UserDefaults.standard
            defaults.set(newValue, forKey: "useMetricUnits")
        }
    }
    
    class var distanceFilterInMeters: Float {
        get {
            let defaults = UserDefaults.standard
            if defaults.object(forKey: "distanceFilterInMeters") != nil {
                return defaults.float(forKey: "distanceFilterInMeters")
            }
            return 10
        }
        set {
            let defaults = UserDefaults.standard
            defaults.set(newValue, forKey: "distanceFilterInMeters")
        }
    }
    
}

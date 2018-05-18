//
//  Line.swift
//  GPS Recording
//
//  Created by Aaron Blondeau on 5/18/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import Foundation
import CoreData

class Line: NSManagedObject {
    static var entityName = "Line"
    
    @NSManaged var startedAt: Date
    @NSManaged var endedAt: Date
    @NSManaged var totalDistanceInMeters: Double
    @NSManaged var inTrack: Track
    @NSManaged var points: NSSet?
}

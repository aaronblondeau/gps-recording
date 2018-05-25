//
//  Track.swift
//  GPS Recording
//
//  Created by Aaron Blondeau on 5/18/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import Foundation
import CoreData

class Track: NSManagedObject {
    static var entityName = "Track"
    
    @NSManaged var name: String?
    @NSManaged var note: String?
    @NSManaged var activity: String?
    @NSManaged var startedAt: Date
    @NSManaged var endedAt: Date
    @NSManaged var totalDistanceInMeters: Double
    @NSManaged var totalDurationInMilliseconds: Double
    @NSManaged var lines: NSSet?
    
}

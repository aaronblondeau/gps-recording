//
//  Point.swift
//  GPS Recording
//
//  Created by Aaron Blondeau on 5/18/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import Foundation
import CoreData

class Point: NSManagedObject {
    static var entityName = "Point"
    
    @NSManaged var altitude: Double
    @NSManaged var course: Double
    @NSManaged var horizontalAccuracy: Double
    @NSManaged var verticalAccuracy: Double
    @NSManaged var latitude: Double
    @NSManaged var longitude: Double
    @NSManaged var speed: Double
    @NSManaged var timestamp: Date
    @NSManaged var inLine: Line
}

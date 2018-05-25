//
//  GPSRecordingStore.swift
//  GPS Recording
//
//  Created by Aaron Blondeau on 5/18/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import Foundation
import CoreData
import CoreLocation

enum GPSRecordingError: Error {
    case pointOutOfOrder()
}

class GPSRecordingStore {
    
    let container: NSPersistentContainer
    
    static var storeIdentifier = "GPSRecording"
    
    class func buildContainer(containerReadyHandler: @escaping (NSPersistentContainer) -> Void) -> Void {
        let container:NSPersistentContainer = NSPersistentContainer(name: storeIdentifier)
        
        container.loadPersistentStores(completionHandler: {
            persistentStoreDescription, error in
            if let error = error {
                fatalError("Failed to load SQLLite store: \(error)")
            }
            DispatchQueue.main.async {
                containerReadyHandler(container)
            }
        })
    }
    
    class func buildTestContainer(bundle: Bundle, containerReadyHandler: @escaping (NSPersistentContainer) -> Void) -> Void {
        
        // https://medium.com/flawless-app-stories/cracking-the-tests-for-core-data-15ef893a3fee
        
        let managedObjectModel = NSManagedObjectModel.mergedModel(from: [bundle] )!
    
        let container:NSPersistentContainer = NSPersistentContainer(name: storeIdentifier, managedObjectModel: managedObjectModel)
        
        let description = NSPersistentStoreDescription()
        description.type = NSInMemoryStoreType
        description.shouldAddStoreAsynchronously = false // Make it simpler in test env
        
        container.persistentStoreDescriptions = [description]
        
        container.loadPersistentStores(completionHandler: {
            persistentStoreDescription, error in
            precondition( persistentStoreDescription.type == NSInMemoryStoreType )
            if let error = error {
                fatalError("Failed to load in memory store: \(error)")
            }
            DispatchQueue.main.async {
                containerReadyHandler(container)
            }
        })
        
        // Since we have shouldAddStoreAsynchronously = false above, we could skip the callback above and do this here:
        // return container
    }
    
    init (withContainer container: NSPersistentContainer) {
        self.container = container
    }
    
    public func createTrack(name: String?, note: String?, activity: String?) throws -> Track {
        let now = Date()
        let context = container.viewContext
        let track = NSEntityDescription.insertNewObject(forEntityName: Track.entityName, into: context) as! Track
        track.name = name
        track.note = note
        track.activity = activity
        track.startedAt = now
        track.endedAt = now
        track.totalDistanceInMeters = 0
        track.totalDurationInMilliseconds = 0
        try context.save()
        return track
    }
    
    public func getTrack(withId: NSManagedObjectID) -> Track {
        let context = container.viewContext
        let found = context.object(with: withId) as! Track
        return found
    }
    
    public func update(track: Track, name: String?, note: String?, activity: String?) throws {
        let context = container.viewContext
        track.name = name
        track.note = note
        track.activity = activity
        try context.save()
    }
    
    public func delete(track: Track) throws {
        let context = container.viewContext
        context.delete(track)
        try context.save()
    }
    
    public func addLine(toTrack: Track) throws -> Line {
        let now = Date()
        let context = container.viewContext
        let line = NSEntityDescription.insertNewObject(forEntityName: Line.entityName, into: context) as! Line
        line.startedAt = now
        line.endedAt = now
        line.totalDistanceInMeters = 0
        line.inTrack = toTrack
        try context.save()
        return line
    }
    
    // Use this method when client doesn't want to manage lines (just wants one)
    public func addPoint(toTrack: Track, fromLocation: CLLocation) throws -> Point {
        let line: Line
        if let lines = toTrack.lines {
            if (lines.count == 0) {
                line = try addLine(toTrack: toTrack)
            } else {
                line = lines.reversed().first as! Line
            }
        } else {
            line = try addLine(toTrack: toTrack)
        }
        return try addPoint(toLine: line, fromLocation: fromLocation)
    }
    
    public func addPoint(toLine: Line, fromLocation: CLLocation) throws -> Point {
        let context = container.viewContext
        
        if let points = toLine.points {
            if (points.count >= 1) {
                // Additional point in line
                
                // Line : update endedAt
                toLine.endedAt = fromLocation.timestamp

                // Line : And distance
                let lastPoint = toLine.points?.sortedArray(using: [NSSortDescriptor(key: #keyPath(Point.timestamp), ascending: false)]).first as! Point
                
                // Throw error if new point's timestamp is not after last point's timestamp
                if(fromLocation.timestamp < lastPoint.timestamp) {
                    throw GPSRecordingError.pointOutOfOrder()
                }
                
                let lastLocation = CLLocation(latitude: lastPoint.latitude, longitude: lastPoint.longitude)
                toLine.totalDistanceInMeters = toLine.totalDistanceInMeters + fromLocation.distance(from: lastLocation)
                
                // Track : update endedAt and totalDurationInMilliseconds
                toLine.inTrack.endedAt = fromLocation.timestamp
                
                toLine.inTrack.totalDurationInMilliseconds = toLine.inTrack.totalDurationInMilliseconds + (fromLocation.timestamp.timeIntervalSince(lastPoint.timestamp) * 1000)
                
                // Track : Add distance
                toLine.inTrack.totalDistanceInMeters = toLine.inTrack.totalDistanceInMeters + fromLocation.distance(from: lastLocation)
            } else {
                // First point in line
                
                // Line : udpate startedAt and endedAt
                toLine.startedAt = fromLocation.timestamp
                toLine.endedAt = fromLocation.timestamp
                
                // Track : update startedAt (if also on first line) and endedAt
                if (toLine.inTrack.lines?.count == 1) {
                    toLine.inTrack.startedAt = fromLocation.timestamp
                }
                toLine.inTrack.endedAt = fromLocation.timestamp
            }
        }
        
        let point = NSEntityDescription.insertNewObject(forEntityName: Point.entityName, into: context) as! Point
        point.altitude = fromLocation.altitude
        point.course = fromLocation.course
        point.horizontalAccuracy = fromLocation.horizontalAccuracy
        point.verticalAccuracy = fromLocation.verticalAccuracy
        point.latitude = fromLocation.coordinate.latitude
        point.longitude = fromLocation.coordinate.longitude
        point.speed = fromLocation.speed
        point.timestamp = fromLocation.timestamp
        point.inLine = toLine
        
        try context.save()
        
        return point
    }
    
    public func countTracks() -> Int {
        let fetchRequest = NSFetchRequest<Track>(entityName: Track.entityName)
        do {
            let count = try container.viewContext.fetch(fetchRequest).count
            return count
        } catch {
            return 0
        }
    }
    
    public func countLines() -> Int {
        let fetchRequest = NSFetchRequest<Line>(entityName: Line.entityName)
        do {
            let count = try container.viewContext.fetch(fetchRequest).count
            return count
        } catch {
            return 0
        }
    }
    
    public func countLines(inTrack: Track) -> Int {
        if let lines = inTrack.lines {
            return lines.count
        } else {
            return 0
        }
    }
    
    public func countPoints() -> Int {
        let fetchRequest = NSFetchRequest<Point>(entityName: Point.entityName)
        do {
            let count = try container.viewContext.fetch(fetchRequest).count
            return count
        } catch {
            return 0
        }
    }
    
    public func countPoints(inLine: Line) -> Int {
        if let points = inLine.points {
            return points.count
        } else {
            return 0
        }
    }
    
}

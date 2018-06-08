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

extension Notification.Name {
    static let gpsRecordingStoreReady = Notification.Name("gpsRecordingStoreReady")
}

class GPSRecordingStore {
    
    let container: NSPersistentContainer
    
    static var storeIdentifier = "GPSRecording"
    
    /**
     Create a SQLite container for use in initializing a store.
     
     - Parameter containerReadyHandler: Callback that will be called when the container is ready.
     */
    class func buildContainer(bundle: Bundle, containerReadyHandler: @escaping (NSPersistentContainer) -> Void) -> Void {
        
        let managedObjectModel = NSManagedObjectModel.mergedModel(from: [bundle] )!
        let container:NSPersistentContainer = NSPersistentContainer(name: storeIdentifier, managedObjectModel: managedObjectModel)
        
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
    
    /**
     Create a in-memory container for use in initializing a store.  *For tests only!*
     
     - Parameter bundle: Inject the test target's bundle : ```let bundle = Bundle(for: type(of: self))```
     - Parameter containerReadyHandler: Callback that will be called when the container is ready.
     */
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
    
    /**
     Create a new GPSRecordingStore.
     
     - Parameter container: The container to use for this store.  Should be from either buildContainer or buildTestContainer.
     */
    init (withContainer container: NSPersistentContainer) {
        self.container = container
    }
    
    /**
     Create a new Track.
     
     - Warning: Until points are added to the track, startedAt and endedAt will default to the time this method was called.
     
     - Parameter name: The name of the track.
     - Parameter note: A description of the track.
     - Parameter activity: The user's activity (run, bike, etc...).
     
     - Returns: A brand new track.
     */
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
    
    /**
     Retrieve a track by id.
     
     - Parameter withId: A track id that was obtained via the track's objectID property.
     
     - Returns: The specified track.
     */
    public func getTrack(withId: NSManagedObjectID) -> Track? {
        let context = container.viewContext
        let found = context.object(with: withId) as! Track
        return found
    }
    
    /**
     Retrieve a track by URL.
     
     - Parameter atURL: A track id that was obtained via the track's objectID property and then converted to a URL with uriRepresentation.
     
     - Returns: The specified track.
     */
    public func getTrack(atURL: URL) -> Track? {
        if let id = container.persistentStoreCoordinator.managedObjectID(forURIRepresentation: atURL) {
            let context = container.viewContext
            let found = context.object(with: id) as! Track
            return found
        }
        return nil
    }
    
    /**
     Retrieve a line by id.
     
     - Parameter withId: A line id that was obtained via the line's objectID property.
     
     - Returns: The specified line.
     */
    public func getLine(withId: NSManagedObjectID) -> Line? {
        let context = container.viewContext
        let found = context.object(with: withId) as! Line
        return found
    }
    
    /**
     Retrieve a line by URL.
     
     - Parameter atURL: A line id that was obtained via the line's objectID property and then converted to a URL with uriRepresentation.
     
     - Returns: The specified line.
     */
    public func getLine(atURL: URL) -> Line? {
        if let id = container.persistentStoreCoordinator.managedObjectID(forURIRepresentation: atURL) {
            let context = container.viewContext
            let found = context.object(with: id) as! Line
            return found
        }
        return nil
    }
    
    /**
     Update a track's details.
     
     - Parameter name: A new name for the track.
     - Parameter note: A new description of the track.
     - Parameter activity: A new value for the user's activity (run, bike, etc...).
     */
    public func update(track: Track, name: String?, note: String?, activity: String?) throws {
        let context = container.viewContext
        track.name = name
        track.note = note
        track.activity = activity
        try context.save()
    }
    
    /**
     Destroy a track.
     
     - Parameter track: The track to destroy.
     
     All lines and points within the track will also be destroyed.
     */
    public func delete(track: Track) throws {
        let context = container.viewContext
        context.delete(track)
        try context.save()
    }
    
    /**
     Add a new line to a track.
     
     - Parameter toTrack: The track to add a new line to.
     
     - Returns: The new empty Line.
     */
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
    
    /**
     Add a new point to a track.
     
     - Warning: If the track has multiple lines, point will be added to the last line in the track.
     
     - Parameter toTrack: The track to add a new point to.
     - Parameter fromLocation: The GPS location of the point.
     
     - Returns: The new Point.
     
     - throws: If this point has a date before the last point in the track, `GPSRecordingError.pointOutOfOrder` will be thrown.
     
     Use this method when you don't want to manage lines.
     */
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
    
    /**
     Add a new point to a line.
     
     - Parameter toLine: The line to add a new point to.
     - Parameter fromLocation: The GPS location of the point.
     
     - Returns: The new Point.
     
      - throws: If this point has a date before the last point in the track, `GPSRecordingError.pointOutOfOrder` will be thrown.
     */
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
    
    /**
     - Returns: How many tracks are in the store.
     */
    public func countTracks() -> Int {
        let fetchRequest = NSFetchRequest<Track>(entityName: Track.entityName)
        do {
            let count = try container.viewContext.fetch(fetchRequest).count
            return count
        } catch {
            return 0
        }
    }
    
    /**
     - Returns: How many tracks are in the store.
     */
    public func countLines() -> Int {
        let fetchRequest = NSFetchRequest<Line>(entityName: Line.entityName)
        do {
            let count = try container.viewContext.fetch(fetchRequest).count
            return count
        } catch {
            return 0
        }
    }
    
    /**
     - Parameter inTrack: The track to query.
     - Returns: How many lines are in the track.
     */
    public func countLines(inTrack: Track) -> Int {
        if let lines = inTrack.lines {
            return lines.count
        } else {
            return 0
        }
    }
    
    /**
     - Returns: How many points are in the store.
     */
    public func countPoints() -> Int {
        let fetchRequest = NSFetchRequest<Point>(entityName: Point.entityName)
        do {
            let count = try container.viewContext.fetch(fetchRequest).count
            return count
        } catch {
            return 0
        }
    }
    
    /**
     - Parameter inLine: The line to query.
     - Returns: How many points are in the line.
     */
    public func countPoints(inLine: Line) -> Int {
        if let points = inLine.points {
            return points.count
        } else {
            return 0
        }
    }
    
}

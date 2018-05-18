//
//  GPSRecordingStore.swift
//  GPS Recording
//
//  Created by Aaron Blondeau on 5/18/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import Foundation
import CoreData

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
        
        // Since we have shouldAddStoreAsynchronously = false above, we could do this here:
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
    
    public func countTracks() -> Int {
        let fetchRequest = NSFetchRequest<Track>(entityName: Track.entityName)
        do {
            let count = try container.viewContext.fetch(fetchRequest).count
            return count
        } catch {
            return 0
        }
    }
    
}

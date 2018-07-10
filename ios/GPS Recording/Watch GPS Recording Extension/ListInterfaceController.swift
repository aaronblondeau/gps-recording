//
//  ListInterfaceController.swift
//  Watch GPS Recording Extension
//
//  Created by Aaron Blondeau on 7/10/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import WatchKit
import Foundation
import CoreData

class ListInterfaceController: WKInterfaceController, NSFetchedResultsControllerDelegate {
    
    var store: GPSRecordingStore?
    var service: GPSRecordingService?
    var fetchedResultsController: NSFetchedResultsController<Track>?
    
    @IBOutlet var table: WKInterfaceTable!
    
    override func awake(withContext context: Any?) {
        super.awake(withContext: context)
        self.setTitle("Tracks")
        // Configure interface objects here.
        
        if context != nil {
            let ctx = context as! GPSRecordingContext
            if let service = ctx.service {
                self.service = service
                print("~~ Got service from context")
            }
            if let store = ctx.store {
                self.store = store
                print("~~ Got store from context")
                configureFetchedResultsController()
            }
        }
    }
    
    func configureFetchedResultsController() {
        if let context = store?.container.viewContext {
            let trackFetchRequest = NSFetchRequest<Track>(entityName: Track.entityName)
            let dateSortDescriptor = NSSortDescriptor(key: #keyPath(Track.startedAt), ascending: false)
            trackFetchRequest.sortDescriptors = [dateSortDescriptor]
            self.fetchedResultsController = NSFetchedResultsController<Track>(fetchRequest: trackFetchRequest, managedObjectContext: context, sectionNameKeyPath: nil, cacheName: nil)  // #keyPath(Track.name)
            self.fetchedResultsController!.delegate = self
            
            do {
                try self.fetchedResultsController!.performFetch()
                if let tracks = fetchedResultsController?.fetchedObjects {
                    print("~~ fetched \(tracks.count) tracks")
                    self.table.setNumberOfRows(tracks.count, withRowType: "trackRow")
                    if ( tracks.count > 0 )
                    {
                        for i in 0...(tracks.count - 1)
                        {
                            configureTableRow(index: i)
                        }
                    }
                }
            } catch {
                let action1 = WKAlertAction(title: "Ok", style: .default) {}
                presentAlert(withTitle: "Error", message: "There was a problem loading the list of Tracks.", preferredStyle: .actionSheet, actions: [action1])
            }
        }
    }
    
    func configureTableRow(index: Int)
    {
        let row = table.rowController(at: index) as! ListRowController
        if let tracks = fetchedResultsController?.fetchedObjects, let service = service {
            let track: Track = tracks[index]
            row.setTrack(track, service: service)
        }
    }
    
    override func willActivate() {
        // This method is called when watch view controller is about to be visible to user
        super.willActivate()
    }
    
    override func didDeactivate() {
        // This method is called when watch view controller is no longer visible
        super.didDeactivate()
    }
    
    // MARK: NSFetchedResultsController Delegate Methods
    
    func controllerWillChangeContent(_ controller: NSFetchedResultsController<NSFetchRequestResult>) {
        // nothing
    }
    
    func controllerDidChangeContent(_ controller: NSFetchedResultsController<NSFetchRequestResult>) {
        // nothing
    }
    
    func controller(_ controller: NSFetchedResultsController<NSFetchRequestResult>, didChange anObject: Any, at indexPath: IndexPath?, for type: NSFetchedResultsChangeType, newIndexPath: IndexPath?) {
        switch type {
        case .insert:
            print(".insert")
            if let insertIndexPath = newIndexPath {
                table.insertRows(at: NSIndexSet(index: insertIndexPath.row) as IndexSet, withRowType: "tableRow")
                configureTableRow(index: newIndexPath!.row)
            }
        case .delete:
            print(".delete")
            if let deleteIndexPath = indexPath {
                table.removeRows(at: NSIndexSet(index: deleteIndexPath.row) as IndexSet)
            }
        case .update:
            print(".update")
            if let updateIndexPath = indexPath {
                configureTableRow(index: updateIndexPath.row)
            }
        case .move:
            print(".move")
            if let deleteIndexPath = indexPath {
                table.removeRows(at: NSIndexSet(index: deleteIndexPath.row) as IndexSet)
            }
            if let insertIndexPath = newIndexPath {
                table.insertRows(at: NSIndexSet(index: insertIndexPath.row) as IndexSet, withRowType: "tableRow")
                configureTableRow(index: newIndexPath!.row)
            }
        }
    }
    
    override func table(_ table: WKInterfaceTable, didSelectRowAt rowIndex: Int) {
        print("~~ User selected row \(rowIndex)")
    }
    
}

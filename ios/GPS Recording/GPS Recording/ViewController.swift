//
//  ViewController.swift
//  GPS Recording
//
//  Created by Aaron Blondeau on 5/15/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import UIKit
import CoreData

class ViewController: UIViewController, UITableViewDataSource, UITableViewDelegate, NSFetchedResultsControllerDelegate {
    
    var store: GPSRecordingStore?

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var activityIndicatorView: UIActivityIndicatorView!
    @IBOutlet weak var messageLabel: UILabel!
    
    var fetchedResultsController: NSFetchedResultsController<Track>?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
        title = "GPS Recording"
        
        if(store != nil) {
            configureFetchedResultsController()
        }
        
        activityIndicatorView.isHidden = false
        messageLabel.isHidden = true
        tableView.isHidden = true
        
        NotificationCenter.default.addObserver(self, selector: #selector(storeLoaded(notification:)), name: .gpsRecordingStoreReady, object: nil)
        
    }
    
    @objc func storeLoaded(notification: NSNotification) {
        self.store = notification.object as? GPSRecordingStore
        DispatchQueue.main.async {
            self.configureFetchedResultsController()
        }
    }
    
    func updateViews() {
        if let tracks = fetchedResultsController?.fetchedObjects {
            print("~~ fetched \(tracks.count) tracks")
            let hasTracks = tracks.count > 0
            messageLabel.isHidden = hasTracks
            tableView.isHidden = !hasTracks
        }
        activityIndicatorView.isHidden = true
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
                updateViews()
                
                // TODO - why is this needed?
                tableView.reloadData()
            } catch {
                let alertController = UIAlertController(title: "Loading Tracks Failed",
                                                        message: "There was a problem loading the list of Tracks.",
                                                        preferredStyle: .alert)
                let okAction = UIAlertAction(title: "OK", style: .default, handler: nil)
                alertController.addAction(okAction)
                self.present(alertController, animated: true, completion: nil)
            }
        }
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    // MARK: Navigation
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == "homeToRecord" {
            let destination = segue.destination as! RecordViewController
            destination.store = self.store
        }
    }
    
    // MARK: TableView Data Source Methods
    
    func numberOfSections(in tableView: UITableView) -> Int {
        if let sections = self.fetchedResultsController?.sections {
            return sections.count
        }
        return 0
    }
    
    func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        if let sections = self.fetchedResultsController?.sections {
            let currentSection = sections[section]
            return currentSection.name
        }
        return nil
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if let sections = self.fetchedResultsController?.sections {
            return sections[section].numberOfObjects
        }
        return 0
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: TrackTableViewCell.reuseIdentifier, for: indexPath) as! TrackTableViewCell
        if let track = self.fetchedResultsController?.object(at: indexPath) {
            populateCell(cell, track: track)
        }
        return cell
    }
    
    func tableView(_ tableView: UITableView, commit editingStyle: UITableViewCellEditingStyle, forRowAt indexPath: IndexPath) {
        if editingStyle == .delete {
            if let track = self.fetchedResultsController?.object(at: indexPath) {
                do {
                    try store?.delete(track: track)
                } catch {
                    print("Failed to delete track \(error.localizedDescription)")
                }
            }
        }
    }
    
    func populateCell(_ cell: TrackTableViewCell, track: Track) {
        if let name = track.name {
            cell.nameLabel!.text = name
        } else {
            let formatter = DateFormatter()
            formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
            cell.nameLabel!.text = formatter.string(from: track.startedAt)
        }
        
        if let note = track.note {
            cell.infoLabel!.text = note
        } else {
            let formatter = DateFormatter()
            formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
            cell.infoLabel!.text = formatter.string(from: track.startedAt)
        }
    }
    
    // MARK: TableView Delegate Methods
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        print("~~ A row was selected!")
        tableView.deselectRow(at: indexPath, animated: true)
    }
    
    // MARK: NSFetchedResultsController Delegate Methods
    
    func controllerWillChangeContent(_ controller: NSFetchedResultsController<NSFetchRequestResult>) {
        self.tableView.beginUpdates()
    }
    
    func controllerDidChangeContent(_ controller: NSFetchedResultsController<NSFetchRequestResult>) {
        self.tableView.endUpdates()
        updateViews()
    }
    
    func controller(_ controller: NSFetchedResultsController<NSFetchRequestResult>, didChange anObject: Any, at indexPath: IndexPath?, for type: NSFetchedResultsChangeType, newIndexPath: IndexPath?) {
        switch type {
        case .insert:
            if let insertIndexPath = newIndexPath {
                self.tableView.insertRows(at: [insertIndexPath], with: .fade)
            }
        case .delete:
            if let deleteIndexPath = indexPath {
                self.tableView.deleteRows(at: [deleteIndexPath], with: .fade)
            }
        case .update:
            if let updateIndexPath = indexPath {
                let cell = self.tableView.cellForRow(at: updateIndexPath) as! TrackTableViewCell
                if let updatedTrack = self.fetchedResultsController?.object(at: updateIndexPath) {
                    populateCell(cell, track: updatedTrack)
                }
            }
        case .move:
            if let deleteIndexPath = indexPath {
                self.tableView.deleteRows(at: [deleteIndexPath], with: .fade)
            }
            if let insertIndexPath = newIndexPath {
                self.tableView.insertRows(at: [insertIndexPath], with: .fade)
            }
        }
    }
    
    func controller(_ controller: NSFetchedResultsController<NSFetchRequestResult>, sectionIndexTitleForSectionName sectionName: String) -> String? {
        return sectionName
    }
    
    func controller(_ controller: NSFetchedResultsController<NSFetchRequestResult>, didChange sectionInfo: NSFetchedResultsSectionInfo, atSectionIndex sectionIndex: Int, for type: NSFetchedResultsChangeType) {
        let sectionIndexSet = NSIndexSet(index: sectionIndex) as IndexSet
        
        switch type {
        case .insert:
            self.tableView.insertSections(sectionIndexSet, with: .fade)
        case .delete:
            self.tableView.deleteSections(sectionIndexSet, with: .fade)
        // https://developer.apple.com/library/archive/documentation/Cocoa/Conceptual/CoreData/nsfetchedresultscontroller.html
        case .move:
            break
        case .update:
            break
        }
    }
}

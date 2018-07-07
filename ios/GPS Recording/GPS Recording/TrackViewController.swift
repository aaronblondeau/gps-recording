//
//  TrackViewController.swift
//  GPS Recording
//
//  Created by Aaron Blondeau on 6/8/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import UIKit

class TrackViewController: UIViewController, UIPickerViewDelegate, UIPickerViewDataSource {
    
    var store: GPSRecordingStore?
    var track: Track?
    @IBOutlet weak var nameTextField: UITextField!
    @IBOutlet weak var noteTextView: UITextView!
    @IBOutlet weak var activityPickerView: UIPickerView!
    @IBOutlet weak var buttonGpxExport: UIButton!
    @IBOutlet weak var buttonGeoJSONExport: UIButton!
    
    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
        self.title = track?.name
        nameTextField.text = track?.name
        noteTextView.text = track?.note
        
        activityPickerView.delegate = self
        activityPickerView.dataSource = self
        
        if let activity = track?.activity {
            let codes = [String](Track.activities.keys)
            if let selected = codes.index(of: activity) {
                activityPickerView.selectRow(selected, inComponent: 0, animated: true)
            }
        }
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    @IBAction func doneAction(_ sender: Any) {
        // navigationController?.popViewController(animated: true)
        navigationController?.popToRootViewController(animated: true)
    }
    
    @IBAction func exportGPXAction(_ sender: Any) {
        if let track = self.track {
            let trackFilename = "\(track.getFilenameBase()).gpx"
            if let dir = NSSearchPathForDirectoriesInDomains(FileManager.SearchPathDirectory.documentDirectory, FileManager.SearchPathDomainMask.allDomainsMask, true).first {
                let path = URL(fileURLWithPath: dir).appendingPathComponent(trackFilename)
                
                buttonGpxExport.isEnabled = false
                
                DispatchQueue.global(qos: .background).async {
                    do {
                        try track.toGpx(url: path)
                        
                        DispatchQueue.main.async {
                            self.buttonGpxExport.isEnabled = true
                            let activityViewController = UIActivityViewController(activityItems: [path, trackFilename], applicationActivities: nil)
                            activityViewController.excludedActivityTypes = [UIActivityType.addToReadingList, UIActivityType.assignToContact, UIActivityType.copyToPasteboard, UIActivityType.print]
                            self.present(activityViewController, animated: true, completion: nil)
                        }
                    } catch {
                        DispatchQueue.main.async {
                            self.buttonGpxExport.isEnabled = true
                            let alert = UIAlertController(title: "Unable To Export GPX File", message: "Error : \(error)", preferredStyle: UIAlertControllerStyle.alert)
                            alert.addAction(UIAlertAction(title: "Ok", style: UIAlertActionStyle.default, handler: nil))
                            self.present(alert, animated: true, completion: nil)
                        }
                    }
                }
            }
        }
    }
    
    @IBAction func exportGeoJSONAction(_ sender: Any) {
        if let track = self.track {
            let trackFilename = "\(track.getFilenameBase()).json"
            if let dir = NSSearchPathForDirectoriesInDomains(FileManager.SearchPathDirectory.documentDirectory, FileManager.SearchPathDomainMask.allDomainsMask, true).first {
                let path = URL(fileURLWithPath: dir).appendingPathComponent(trackFilename)
                
                buttonGeoJSONExport.isEnabled = false
                
                DispatchQueue.global(qos: .background).async {
                    do {
                        try track.toGeoJSON(url: path)
                        
                        DispatchQueue.main.async {
                            self.buttonGeoJSONExport.isEnabled = true
                            let activityViewController = UIActivityViewController(activityItems: [path, trackFilename], applicationActivities: nil)
                            activityViewController.excludedActivityTypes = [UIActivityType.addToReadingList, UIActivityType.assignToContact, UIActivityType.copyToPasteboard, UIActivityType.print]
                            self.present(activityViewController, animated: true, completion: nil)
                        }
                    } catch {
                        DispatchQueue.main.async {
                            self.buttonGeoJSONExport.isEnabled = true
                            let alert = UIAlertController(title: "Unable To Export GeoJSON File", message: "Error : \(error)", preferredStyle: UIAlertControllerStyle.alert)
                            alert.addAction(UIAlertAction(title: "Ok", style: UIAlertActionStyle.default, handler: nil))
                            self.present(alert, animated: true, completion: nil)
                        }
                    }
                }
            }
        }
    }
    
    
    @IBAction func saveAction(_ sender: Any) {
        if let store = self.store, let track = self.track {
            let codes = [String](Track.activities.keys)
            let code = codes[activityPickerView.selectedRow(inComponent: 0)]
            do {
                try store.update(track: track, name: nameTextField.text, note: noteTextView.text, activity: code)
            } catch {
                let alertController = UIAlertController(title: "Save Track Failed",
                                                        message: "There was a problem saving the Track.",
                                                        preferredStyle: .alert)
                let okAction = UIAlertAction(title: "OK", style: .default, handler: nil)
                alertController.addAction(okAction)
                self.present(alertController, animated: true, completion: nil)
            }
        }
    }
    
    // MARK: - Picker View
    
    func numberOfComponents(in pickerView: UIPickerView) -> Int {
        return 1
    }
    
    func pickerView(_ pickerView: UIPickerView, numberOfRowsInComponent component: Int) -> Int {
        return Track.activities.count
    }
    
    func pickerView(_ pickerView: UIPickerView, titleForRow row: Int, forComponent component: Int) -> String? {
        return [String](Track.activities.values)[row]
    }
    
    // MARK: Navigation
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == "showTrackMap" {
            let destination = segue.destination as! TrackMapViewController
            destination.store = self.store
            destination.track = self.track
        }
    }

}

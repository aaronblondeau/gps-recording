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
    
    @IBAction func saveAction(_ sender: Any) {
        if let store = self.store, let track = self.track {
            let codes = [String](Track.activities.keys)
            let code = codes[activityPickerView.selectedRow(inComponent: 0)]
            do {
                try store.update(track: track, name: nameTextField.text, note: noteTextView.text, activity: code)
                navigationController?.popViewController(animated: true)
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
    
    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destinationViewController.
        // Pass the selected object to the new view controller.
    }
    */

}

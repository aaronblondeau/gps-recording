//
//  RecordViewController.swift
//  GPS Recording
//
//  Created by Aaron Blondeau on 6/4/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import UIKit

class RecordViewController: UIViewController {

    @IBOutlet weak var textTrackName: UITextField!
    @IBOutlet weak var buttonSave: UIButton!
    
    var store: GPSRecordingStore?
    
    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
    
        NotificationCenter.default.addObserver(self, selector: #selector(storeLoaded(notification:)), name: .gpsRecordingStoreReady, object: nil)
        if store != nil {
            buttonSave.isEnabled = true
        }
        
    }
    
    @objc func storeLoaded(notification: NSNotification) {
        self.store = notification.object as? GPSRecordingStore
        buttonSave.isEnabled = true
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
        self.title = "Record"
    }
    
    @IBAction func buttonSaveTap(_ sender: Any) {
        if (self.store != nil) {
            print("~~ Store has \(store!.countTracks()) tracks")
            print("~~ Would create a new track with name \(textTrackName.text)")
        } else {
            print("~~ Store is nil!")
        }
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

//
//  ViewController.swift
//  GPS Recording
//
//  Created by Aaron Blondeau on 5/15/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import UIKit

class ViewController: UIViewController {
    
    var store: GPSRecordingStore?

    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
        title = "GPS Recording"
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == "homeToRecord" {
            let destination = segue.destination as! RecordViewController
            destination.store = self.store
        }
    }

}


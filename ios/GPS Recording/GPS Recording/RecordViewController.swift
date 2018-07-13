//
//  RecordViewController.swift
//  GPS Recording
//
//  Created by Aaron Blondeau on 6/4/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import UIKit
import CoreData

class RecordViewController: UIViewController {

    @IBOutlet weak var buttonStart: UIButton!
    @IBOutlet weak var buttonPause: UIButton!
    @IBOutlet weak var buttonResume: UIButton!
    @IBOutlet weak var buttonFinish: UIButton!
    @IBOutlet weak var labelDistance: UILabel!
    @IBOutlet weak var labelDuration: UILabel!
    
    var store: GPSRecordingStore?
    var serviceManager: GPSRecordingServiceManager?
    var finishedTrack: Track?
    var observingStore = false
    
    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
    
        NotificationCenter.default.addObserver(self, selector: #selector(storeLoaded(notification:)), name: .gpsRecordingStoreReady, object: nil)
        
        NotificationCenter.default.addObserver(self, selector: #selector(onRecordingStarted(notification:)), name: .gpsRecordingStarted, object: nil)
        
        NotificationCenter.default.addObserver(self, selector: #selector(onRecordingStopped(notification:)), name: .gpsRecordingStopped, object: nil)
        
        NotificationCenter.default.addObserver(self, selector: #selector(onGPSRecordingError(notification:)), name: .gpsRecordingError, object: nil)
        
        NotificationCenter.default.addObserver(self, selector: #selector(onGPSRecordingManualPermissionsNeeded(notification:)), name: .gpsRecordingManualPermissionsNeeded, object: nil)
        
        NotificationCenter.default.addObserver(self, selector: #selector(onGPSAwaitingPermissions(notification:)), name: .gpsRecordingAwaitingPermissions, object: nil)
        
        NotificationCenter.default.addObserver(self, selector: #selector(onGPSRecordingLocationServicesDisabled(notification:)), name: .gpsRecordingLocationServicesDisabled, object: nil)
        
        observeStore()
        updateButtons()
        updateStats()
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
        observingStore = false
    }
    
    @objc func storeLoaded(notification: NSNotification) {
        self.store = notification.object as? GPSRecordingStore
        observeStore()
    }
    
    func observeStore() {
        if let store = self.store {
            if (!observingStore) {
                NotificationCenter.default.addObserver(self, selector: #selector(managedObjectContextObjectsDidChange), name: NSNotification.Name.NSManagedObjectContextObjectsDidChange, object: store.container.viewContext)
                observingStore = true
            }
        }
    }
    
    @objc func managedObjectContextObjectsDidChange() {
        updateStats()
    }
    
    func updateStats() {
        if let track = serviceManager?.service.currentTrack {
            let formatter = DateComponentsFormatter()
            formatter.allowedUnits = [.hour, .minute, .second]
            formatter.unitsStyle = .full
            let formattedString = formatter.string(from: TimeInterval(track.totalDurationInMilliseconds / 1000))!
            labelDuration.text = "\(formattedString)"
            
            if Settings.useMetricUnits {
                let kilometers = Double(round(100*(track.totalDistanceInMeters / 1000))/100)
                labelDistance.text = "\(kilometers) km"
            } else {
                let miles = Double(round(100*(track.totalDistanceInMeters * 0.000621371))/100)
                labelDistance.text = "\(miles) miles"
            }
        } else {
            if Settings.useMetricUnits {
                labelDuration.text = "0 seconds"
                labelDistance.text = "? km"
            } else {
                labelDuration.text = "0 seconds"
                labelDistance.text = "? miles"
            }
        }
    }
    
    @objc func onRecordingStarted(notification: NSNotification) {
        updateButtons()
    }
    
    @objc func onRecordingStopped(notification: NSNotification) {
        updateButtons()
    }
    
    @objc func onGPSRecordingError(notification: NSNotification) {
        if let message = notification.object as? String {
            let alert = UIAlertController(title: "Unable to create track!", message: "\(message)", preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: "Ok", style: .default, handler: nil))
            self.present(alert, animated: true)
        }
    }
    
    @objc func onGPSRecordingManualPermissionsNeeded(notification: NSNotification) {
        
        let alert = UIAlertController(title: "GPS Permissions Needed!", message: "This app needs permission to 'Always' access GPS so that it can record even while you are using other apps.  The permission has previously been denied for this app.  You need to go to the device settings for this app and set Location to 'Always'.", preferredStyle: .alert)
        
        alert.addAction(UIAlertAction(title: "Take Me There", style: .default, handler: {
            action in
            print("Would try to take user to app settings.")
            if let url = NSURL(string: UIApplicationOpenSettingsURLString) as URL? {
                UIApplication.shared.open(url, options: [:], completionHandler: nil)
            }
        }))
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel, handler: {
            action in
        }))
        self.present(alert, animated: true)
        
    }
    
    @objc func onGPSRecordingLocationServicesDisabled(notification: NSNotification) {
        
        let alert = UIAlertController(title: "Location Services Disabled!", message: "Location services are disabled on this device.  Use the Privacy section in the Settings app to turn location services on before recording.", preferredStyle: .alert)
        
        alert.addAction(UIAlertAction(title: "Ok", style: .default, handler: {
            action in
        }))
        self.present(alert, animated: true)
        
    }
    
    @objc func onGPSAwaitingPermissions(notification: NSNotification) {
        // Nothing to do for phone app
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
        self.title = "Record"
    }
    
    @IBAction func buttonStartTap(_ sender: Any) {
        serviceManager?.service.start()
        updateButtons()
    }
    
    @IBAction func buttonPauseTap(_ sender: Any) {
        serviceManager?.service.pause()
        updateButtons()
    }
    
    @IBAction func buttonResumeTap(_ sender: Any) {
        serviceManager?.service.resume()
        updateButtons()
    }
    
    @IBAction func buttonFinishTap(_ sender: Any) {
        finishedTrack = serviceManager?.service.currentTrack
        serviceManager?.service.finish()
        updateButtons()
        performSegue(withIdentifier: "showFinishedTrack", sender: self)
    }
    
    func updateButtons() {
        if let service = serviceManager?.service {
            buttonStart.isEnabled = !service.recording
            buttonStart.isHidden = service.hasCurrentTrack || service.recording
            
            buttonPause.isEnabled = service.recording
            buttonPause.isHidden = !service.recording
            
            buttonResume.isEnabled = !service.recording && service.hasCurrentTrack
            buttonResume.isHidden = service.recording || !service.hasCurrentTrack
            
            buttonFinish.isEnabled = service.hasCurrentTrack
            buttonFinish.isHidden = !service.hasCurrentTrack
            
            labelDistance.isHidden = !(service.hasCurrentTrack || service.recording)
            labelDuration.isHidden = !(service.hasCurrentTrack || service.recording)
        } else {
            buttonStart.isEnabled = false
            buttonPause.isEnabled = false
            buttonResume.isEnabled = false
            buttonFinish.isEnabled = false
            
            buttonStart.isHidden = false
            buttonPause.isHidden = true
            buttonResume.isHidden = true
            buttonFinish.isHidden = true
            
            labelDistance.isHidden = true
            labelDistance.isHidden = true
        }
    }
    
    // MARK: - Navigation

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == "showFinishedTrack" {
            let destination = segue.destination as! TrackViewController
            destination.store = self.store
            destination.track = finishedTrack
        }
    }

}

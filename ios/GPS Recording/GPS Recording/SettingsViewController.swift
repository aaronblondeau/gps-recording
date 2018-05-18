//
//  SettingsViewController.swift
//  GPS Recording
//
//  Created by Aaron Blondeau on 5/15/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import UIKit

class SettingsViewController: UIViewController, UITableViewDataSource {
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return 2
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        
        switch indexPath.row {
        case 0:
            let cell = UITableViewCell()
            cell.textLabel?.text = "Use Metric Units"
            let metricUnitsSwitch = UISwitch()
            metricUnitsSwitch.setOn(Settings.useMetricUnits, animated: true)
            metricUnitsSwitch.addTarget(self, action: #selector(SettingsViewController.useMetricUnitsSwtichChange(_:)), for: .valueChanged)
            cell.accessoryView = metricUnitsSwitch
            return cell
        case 1:
            let cell = tableView.dequeueReusableCell(withIdentifier: "distanceFilter", for: indexPath)
            return cell
        default:
            let cell = UITableViewCell()
            cell.textLabel?.text = ""
            return cell
        }
    }
    
    @objc func useMetricUnitsSwtichChange(_ sender: UISwitch!) {
        Settings.useMetricUnits = sender.isOn
    }
    
    func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }
    
    func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        switch section {
        case 0:
            return "Recording"
        default:
            return ""
        }
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Do any additional setup after loading the view.
        title = "Settings"
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
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

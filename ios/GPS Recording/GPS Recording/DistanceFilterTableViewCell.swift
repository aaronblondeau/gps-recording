//
//  DistanceFilterTableViewCell.swift
//  GPS Recording
//
//  Created by Aaron Blondeau on 5/18/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import UIKit

class DistanceFilterTableViewCell: UITableViewCell {

    @IBOutlet weak var distanceFilterSlider: UISlider!
    @IBOutlet weak var detailLabel: UILabel!
    
    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
        distanceFilterSlider.value = Settings.distanceFilterInMeters
        updateDetailLabel()
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        // Configure the view for the selected state
    }
    
    @IBAction func distanceFilterSliderChange(_ sender: Any) {
        Settings.distanceFilterInMeters = round(distanceFilterSlider.value)
        updateDetailLabel()
    }
    
    func updateDetailLabel() {
        detailLabel.text = "\(Settings.distanceFilterInMeters) Meters"
    }
    
}

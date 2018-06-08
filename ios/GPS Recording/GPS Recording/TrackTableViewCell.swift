//
//  TrackTableViewCell.swift
//  GPS Recording
//
//  Created by Aaron Blondeau on 6/7/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import UIKit

class TrackTableViewCell: UITableViewCell {
    
    static let reuseIdentifier = "TableCell"

    @IBOutlet weak var nameLabel: UILabel!
    @IBOutlet weak var infoLabel: UILabel!
    
    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        // Configure the view for the selected state
    }

}

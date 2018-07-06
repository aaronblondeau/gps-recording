//
//  GPSRecordingService.swift
//  GPS Recording
//
//  Created by Aaron Blondeau on 6/22/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import Foundation
import UIKit

protocol GPSRecordingService {
    var recording: Bool {get set}
    var currentTrack: Track? {get set}
    var hasCurrentTrack: Bool {get}
    func start(_ viewController: UIViewController)
    func pause()
    func resume()
    func finish()
}

extension Notification.Name {
    static let gpsRecordingStarted = Notification.Name("gpsRecordingStarted")
    static let gpsRecordingStopped = Notification.Name("gpsRecordingStopped")
}

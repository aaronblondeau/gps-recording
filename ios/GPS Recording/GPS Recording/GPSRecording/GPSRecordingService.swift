//
//  GPSRecordingService.swift
//  GPS Recording
//
//  Created by Aaron Blondeau on 6/22/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import Foundation

protocol GPSRecordingService {
    var recording: Bool {get set}
    var currentTrack: Track? {get}
    var hasCurrentTrack: Bool {get}
    func start()
    func pause()
    func resume()
    func finish()
}

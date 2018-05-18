//
//  GPS_RecordingTests.swift
//  GPS RecordingTests
//
//  Created by Aaron Blondeau on 5/15/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import XCTest
import CoreData

@testable import GPS_Recording

class GPS_RecordingTests: XCTestCase {
    
    var store: GPSRecordingStore?
    
    override func setUp() {
        super.setUp()
        // Put setup code here. This method is called before the invocation of each test method in the class.
        
        let exp = expectation(description: "Initialize CoreData")
        
        // Get test bundle for the store to use when building in-memory container
        let bundle = Bundle(for: type(of: self))
        
        // Use the model to create a container
        GPSRecordingStore.buildTestContainer(bundle: bundle) {
            (container: NSPersistentContainer) in
            
            self.store = GPSRecordingStore(withContainer: container)
            
            // Our store is ready
            exp.fulfill()
        }
        
        // Wait for the async request to complete
        waitForExpectations(timeout: 100, handler: nil)
    }
    
    override func tearDown() {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
        super.tearDown()
    }
    
    func testCreateTrack() {
        // This is an example of a functional test case.
        // Use XCTAssert and related functions to verify your tests produce the correct results.
        
        // Count of tracks should be 0 to start with
        XCTAssertEqual(0, store!.countTracks())
        
        // Create a new track
        do {
            let track = try store!.createTrack(name: "Foo", note: "Bar", activity: nil)
            XCTAssertEqual(track.name, "Foo")
            XCTAssertEqual(track.totalDistanceInMeters, 0)
        } catch {
            XCTFail(error.localizedDescription)
        }
        
        // Count of tracks should now be 1
        XCTAssertEqual(1, store!.countTracks())
    }
    
    func testPerformanceExample() {
        // This is an example of a performance test case.
        self.measure {
            // Put the code you want to measure the time of here.
        }
    }
    
}

//
//  GPS_RecordingTests.swift
//  GPS RecordingTests
//
//  Created by Aaron Blondeau on 5/15/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import XCTest
import CoreData
import CoreLocation

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
    
    func testGetTrackById() {
        do {
            // Create a new track
            let track = try store!.createTrack(name: "Foo Get", note: "Bar By Id", activity: nil)
            XCTAssertEqual(track.name, "Foo Get")
            XCTAssertEqual(track.totalDistanceInMeters, 0)
            let id = track.objectID
            // Then fetch it by id
            if let found = store?.getTrack(withId: id) {
                XCTAssertEqual(found.name, "Foo Get")
            } else {
                XCTFail("No track with id \(id.uriRepresentation()) found")
            }
        } catch {
            XCTFail(error.localizedDescription)
        }
    }
    
    func testAddLine() {
        do {
            // Create a new track
            let track = try store!.createTrack(name: "Foo Line", note: "Bar Line", activity: nil)
            XCTAssertEqual(track.name, "Foo Line")
            XCTAssertEqual(store?.countLines(inTrack: track), 0)
            XCTAssertEqual(store?.countLines(), 0)
            
            // Add a line to the track
            let line = try store?.addLine(toTrack: track)

            XCTAssertEqual(store?.countLines(inTrack: track), 1)
            XCTAssertEqual(store?.countLines(), 1)
            
            XCTAssertEqual(line?.totalDistanceInMeters, 0)
        } catch {
            XCTFail(error.localizedDescription)
        }
    }
    
    func testAddSinglePoint() {
        do {
            // Create a new track
            let track = try store!.createTrack(name: "Foo Point", note: "Bar Point", activity: nil)
            
            // Add a line to the track
            let line = try store!.addLine(toTrack: track)
            
            XCTAssertEqual(store?.countPoints(inLine: line), 0)
            XCTAssertEqual(store?.countPoints(), 0)
            
            let pointTime = Date()
            let location = CLLocation(coordinate: CLLocationCoordinate2D(latitude: 38.5, longitude: -106.0), altitude: 2900, horizontalAccuracy: 15, verticalAccuracy: 20, course: 15.5, speed: 16.5, timestamp: pointTime)
            let point = try store!.addPoint(toLine: line, fromLocation: location)
            
            // Verify point fields
            XCTAssertEqual(point.latitude, 38.5)
            XCTAssertEqual(point.longitude, -106.0)
            XCTAssertEqual(point.altitude, 2900)
            XCTAssertEqual(point.horizontalAccuracy, 15)
            XCTAssertEqual(point.verticalAccuracy, 20)
            XCTAssertEqual(point.course, 15.5)
            XCTAssertEqual(point.speed, 16.5)
            XCTAssertEqual(point.timestamp, pointTime)
            
            // Upon first point insertion, startedAt and endedAt should match the point
            XCTAssertEqual(line.endedAt, pointTime)
            XCTAssertEqual(line.startedAt, pointTime)
            XCTAssertEqual(track.endedAt, pointTime)
            XCTAssertEqual(track.startedAt, pointTime)
            XCTAssertEqual(track.totalDurationInMilliseconds, 0)
            
            // Distances should be 0
            XCTAssertEqual(line.totalDistanceInMeters, 0)
            XCTAssertEqual(track.totalDistanceInMeters, 0)
            
            // Points count should have increased
            XCTAssertEqual(store?.countPoints(inLine: line), 1)
            XCTAssertEqual(store?.countPoints(), 1)
        } catch {
            XCTFail(error.localizedDescription)
        }
    }
    
    func testAddTwoPoints() {
        do {
            // Create a new track
            let track = try store!.createTrack(name: "Foo Two Points", note: "Bar Two Points", activity: nil)
            
            // Add a line to the track
            let line = try store!.addLine(toTrack: track)
            
            XCTAssertEqual(store?.countPoints(inLine: line), 0)
            XCTAssertEqual(store?.countPoints(), 0)
            
            let pointTime1 = Date(timeIntervalSince1970: 1527275521)
            let location1 = CLLocation(coordinate: CLLocationCoordinate2D(latitude: 38.5, longitude: -106.0), altitude: 2900, horizontalAccuracy: 15, verticalAccuracy: 20, course: 15.5, speed: 16.5, timestamp: pointTime1)
            let _ = try store!.addPoint(toLine: line, fromLocation: location1)
            
            let pointTime2 = Date(timeIntervalSince1970: 1527275522)
            let location2 = CLLocation(coordinate: CLLocationCoordinate2D(latitude: 38.5, longitude: -106.1), altitude: 2900, horizontalAccuracy: 15, verticalAccuracy: 20, course: 15.5, speed: 16.5, timestamp: pointTime2)
            let _ = try store!.addPoint(toLine: line, fromLocation: location2)
            
            // startedAt should match first point's time
            XCTAssertEqual(track.startedAt, pointTime1)
            XCTAssertEqual(line.startedAt, pointTime1)
            
            // endedAt should match second point's line
            XCTAssertEqual(track.endedAt, pointTime2)
            XCTAssertEqual(line.endedAt, pointTime2)
            XCTAssertEqual(track.totalDurationInMilliseconds, 1000)
            
            // Distances should have updated
            XCTAssertEqual(line.totalDistanceInMeters, 8727.23, accuracy: 0.1)
            XCTAssertEqual(track.totalDistanceInMeters, 8727.23, accuracy: 0.1)
            
            // Points count should have increased
            XCTAssertEqual(store?.countPoints(inLine: line), 2)
            XCTAssertEqual(store?.countPoints(), 2)
            
        } catch {
            XCTFail(error.localizedDescription)
        }
    }
    
    func testAddThreePoints() {
        do {
            // Create a new track
            let track = try store!.createTrack(name: "Foo Three Points", note: "Bar Three Points", activity: nil)
            
            // Add a line to the track
            let line = try store!.addLine(toTrack: track)
            
            XCTAssertEqual(store?.countPoints(inLine: line), 0)
            XCTAssertEqual(store?.countPoints(), 0)
            
            let pointTime1 = Date(timeIntervalSince1970: 1527275521)
            let location1 = CLLocation(coordinate: CLLocationCoordinate2D(latitude: 38.5, longitude: -106.0), altitude: 2900, horizontalAccuracy: 15, verticalAccuracy: 20, course: 15.5, speed: 16.5, timestamp: pointTime1)
            let _ = try store!.addPoint(toLine: line, fromLocation: location1)
            
            let pointTime2 = Date(timeIntervalSince1970: 1527275522)
            let location2 = CLLocation(coordinate: CLLocationCoordinate2D(latitude: 38.5, longitude: -106.1), altitude: 2900, horizontalAccuracy: 15, verticalAccuracy: 20, course: 15.5, speed: 16.5, timestamp: pointTime2)
            let _ = try store!.addPoint(toLine: line, fromLocation: location2)
            
            let pointTime3 = Date(timeIntervalSince1970: 1527275524)
            let location3 = CLLocation(coordinate: CLLocationCoordinate2D(latitude: 38.5, longitude: -106.3), altitude: 2900, horizontalAccuracy: 15, verticalAccuracy: 20, course: 15.5, speed: 16.5, timestamp: pointTime3)
            let _ = try store!.addPoint(toLine: line, fromLocation: location3)
            
            // startedAt should match first point's time
            XCTAssertEqual(track.startedAt, pointTime1)
            XCTAssertEqual(line.startedAt, pointTime1)
            
            // endedAt should match third point's line
            XCTAssertEqual(track.endedAt, pointTime3)
            XCTAssertEqual(line.endedAt, pointTime3)
            XCTAssertEqual(track.totalDurationInMilliseconds, 3000)
            
            // Distances should have updated
            XCTAssertEqual(line.totalDistanceInMeters, 26181.71, accuracy: 0.1)
            XCTAssertEqual(track.totalDistanceInMeters, 26181.71, accuracy: 0.1)
            
            // Points count should have increased
            XCTAssertEqual(store?.countPoints(inLine: line), 3)
            XCTAssertEqual(store?.countPoints(), 3)
            
        } catch {
            XCTFail(error.localizedDescription)
        }
    }
    
    func testAddPointsInMultipleLines() {
        do {
            // Create a new track
            let track = try store!.createTrack(name: "Test Multiple Lines", note: "", activity: nil)
            
            // Add a line to the track
            let line1 = try store!.addLine(toTrack: track)
            
            let pointTime1 = Date(timeIntervalSince1970: 1527275521)
            let location1 = CLLocation(coordinate: CLLocationCoordinate2D(latitude: 38.5, longitude: -106.0), altitude: 2900, horizontalAccuracy: 15, verticalAccuracy: 20, course: 15.5, speed: 16.5, timestamp: pointTime1)
            let _ = try store!.addPoint(toLine: line1, fromLocation: location1)
            
            let pointTime2 = Date(timeIntervalSince1970: 1527275522)
            let location2 = CLLocation(coordinate: CLLocationCoordinate2D(latitude: 38.5, longitude: -106.1), altitude: 2900, horizontalAccuracy: 15, verticalAccuracy: 20, course: 15.5, speed: 16.5, timestamp: pointTime2)
            let _ = try store!.addPoint(toLine: line1, fromLocation: location2)
            
            let pointTime3 = Date(timeIntervalSince1970: 1527275524)
            let location3 = CLLocation(coordinate: CLLocationCoordinate2D(latitude: 38.5, longitude: -106.3), altitude: 2900, horizontalAccuracy: 15, verticalAccuracy: 20, course: 15.5, speed: 16.5, timestamp: pointTime3)
            let _ = try store!.addPoint(toLine: line1, fromLocation: location3)
            
            XCTAssertEqual(store?.countPoints(inLine: line1), 3)
            XCTAssertEqual(store?.countPoints(), 3)
            
            // Add a second line to the track
            let line2 = try store!.addLine(toTrack: track)
            
            let pointTime4 = Date(timeIntervalSince1970: 1527275525)
            let location4 = CLLocation(coordinate: CLLocationCoordinate2D(latitude: 38.5, longitude: -106.0), altitude: 2900, horizontalAccuracy: 15, verticalAccuracy: 20, course: 15.5, speed: 16.5, timestamp: pointTime4)
            let _ = try store!.addPoint(toLine: line2, fromLocation: location4)
            
            let pointTime5 = Date(timeIntervalSince1970: 1527275526)
            let location5 = CLLocation(coordinate: CLLocationCoordinate2D(latitude: 38.6, longitude: -106.0), altitude: 2900, horizontalAccuracy: 15, verticalAccuracy: 20, course: 15.5, speed: 16.5, timestamp: pointTime5)
            let _ = try store!.addPoint(toLine: line2, fromLocation: location5)
            
            let pointTime6 = Date(timeIntervalSince1970: 1527275528)
            let location6 = CLLocation(coordinate: CLLocationCoordinate2D(latitude: 38.8, longitude: -106.0), altitude: 2900, horizontalAccuracy: 15, verticalAccuracy: 20, course: 15.5, speed: 16.5, timestamp: pointTime6)
            let _ = try store!.addPoint(toLine: line2, fromLocation: location6)
            
            XCTAssertEqual(store?.countPoints(inLine: line2), 3)
            XCTAssertEqual(store?.countPoints(), 6)
            
            // startedAt should match first point's time
            XCTAssertEqual(track.startedAt, pointTime1)
            XCTAssertEqual(line1.startedAt, pointTime1)
            XCTAssertEqual(line2.startedAt, pointTime4)
            
            // endedAt should match last point's line
            XCTAssertEqual(track.endedAt, pointTime6)
            XCTAssertEqual(line1.endedAt, pointTime3)
            XCTAssertEqual(line2.endedAt, pointTime6)
            XCTAssertEqual(track.totalDurationInMilliseconds, 6000)
            
            // Distances should be updated
            XCTAssertEqual(line1.totalDistanceInMeters, 26181.71, accuracy: 0.1)
            XCTAssertEqual(line2.totalDistanceInMeters, 33317.82, accuracy: 0.1)
            XCTAssertEqual(track.totalDistanceInMeters, 59499.54, accuracy: 0.1)
            
        } catch {
            XCTFail(error.localizedDescription)
        }
    }
    
    func testDeleteTrack() {
        do {
            // Create a new track
            let track = try store!.createTrack(name: "Foo Three Points", note: "Bar Three Points", activity: nil)
            
            // Add a line to the track
            let line = try store!.addLine(toTrack: track)

            let pointTime1 = Date(timeIntervalSince1970: 1527275521)
            let location1 = CLLocation(coordinate: CLLocationCoordinate2D(latitude: 38.5, longitude: -106.0), altitude: 2900, horizontalAccuracy: 15, verticalAccuracy: 20, course: 15.5, speed: 16.5, timestamp: pointTime1)
            let _ = try store!.addPoint(toLine: line, fromLocation: location1)
            
            let pointTime2 = Date(timeIntervalSince1970: 1527275522)
            let location2 = CLLocation(coordinate: CLLocationCoordinate2D(latitude: 38.5, longitude: -106.1), altitude: 2900, horizontalAccuracy: 15, verticalAccuracy: 20, course: 15.5, speed: 16.5, timestamp: pointTime2)
            let _ = try store!.addPoint(toLine: line, fromLocation: location2)
            
            let pointTime3 = Date(timeIntervalSince1970: 1527275524)
            let location3 = CLLocation(coordinate: CLLocationCoordinate2D(latitude: 38.5, longitude: -106.3), altitude: 2900, horizontalAccuracy: 15, verticalAccuracy: 20, course: 15.5, speed: 16.5, timestamp: pointTime3)
            let _ = try store!.addPoint(toLine: line, fromLocation: location3)
            
            // Points count should have increased
            XCTAssertEqual(store?.countPoints(inLine: line), 3)
            XCTAssertEqual(store?.countPoints(), 3)
            XCTAssertEqual(store?.countLines(), 1)
            
            // Delete the track
            try store?.delete(track: track)
            
            XCTAssertEqual(store?.countTracks(), 0)
            XCTAssertEqual(store?.countLines(), 0)
            XCTAssertEqual(store?.countPoints(), 0)
            
        } catch {
            XCTFail(error.localizedDescription)
        }
    }
    
    func testUpdateTrack() {
        do {
            // Create a new track
            let track = try store!.createTrack(name: "Name One", note: "Note One", activity: "run")
            
            try store!.update(track: track, name: "Name Two", note: "Note Two", activity: "bike")
            
            let id = track.objectID
            // Then fetch it by id
            if let found = store?.getTrack(withId: id) {
                XCTAssertEqual(found.name, "Name Two")
                XCTAssertEqual(found.note, "Note Two")
                XCTAssertEqual(found.activity, "bike")
            } else {
                XCTFail("No track with id \(id.uriRepresentation()) found")
            }
            
        } catch {
            XCTFail(error.localizedDescription)
        }
    }
    
    func testFindTrackWithURL() {
        do {
            let track = try store!.createTrack(name: "Please Find Me", note: "Ok", activity: "run")
            
            let id = track.objectID.uriRepresentation().absoluteString
            
            print("~~ Track's path is \(id)")
            
            let idURL = URL(string: id)
            
            // Then fetch it by id
            if let found = store?.getTrack(atURL: idURL!) {
                XCTAssertEqual(found.name, "Please Find Me")
                XCTAssertEqual(found.note, "Ok")
                XCTAssertEqual(found.activity, "run")
            } else {
                XCTFail("No track at url \(String(describing: idURL?.absoluteString))) found")
            }
            
        } catch {
            XCTFail(error.localizedDescription)
        }
    }
    
    func testFindTrackWithBadURL() {
        let idURL = URL(string: "x-coredata://THINGS-AND-STUFF/Track/p9999")
        
        // Then fetch it by id
        if let _ = store?.getTrack(atURL: idURL!) {
            XCTFail("No track should be found at URL \(String(describing: idURL?.absoluteString)))")
        } else {
            XCTAssert(true)
        }
    }
    
    func testAddPointDirectlyToTrack() {
        do {
            // Create a new track
            let track = try store!.createTrack(name: "Foo Three Points", note: "Bar Three Points", activity: nil)
            
            // DO NOT Add a line to the track
            XCTAssertEqual(store?.countPoints(), 0)
            XCTAssertEqual(store?.countLines(), 0)
            
            let pointTime1 = Date(timeIntervalSince1970: 1527275521)
            let location1 = CLLocation(coordinate: CLLocationCoordinate2D(latitude: 38.5, longitude: -106.0), altitude: 2900, horizontalAccuracy: 15, verticalAccuracy: 20, course: 15.5, speed: 16.5, timestamp: pointTime1)
            let _ = try store!.addPoint(toTrack: track, fromLocation: location1)
            
            // A line should have been automatically created
            XCTAssertEqual(store?.countPoints(), 1)
            XCTAssertEqual(store?.countLines(), 1)
            
            let pointTime2 = Date(timeIntervalSince1970: 1527275522)
            let location2 = CLLocation(coordinate: CLLocationCoordinate2D(latitude: 38.5, longitude: -106.1), altitude: 2900, horizontalAccuracy: 15, verticalAccuracy: 20, course: 15.5, speed: 16.5, timestamp: pointTime2)
            let _ = try store!.addPoint(toTrack: track, fromLocation: location2)
            
            // The default aline should have been re-used
            XCTAssertEqual(store?.countPoints(), 2)
            XCTAssertEqual(store?.countLines(), 1)
            
            let pointTime3 = Date(timeIntervalSince1970: 1527275524)
            let location3 = CLLocation(coordinate: CLLocationCoordinate2D(latitude: 38.5, longitude: -106.3), altitude: 2900, horizontalAccuracy: 15, verticalAccuracy: 20, course: 15.5, speed: 16.5, timestamp: pointTime3)
            let _ = try store!.addPoint(toTrack: track, fromLocation: location3)
            
            // startedAt should match first point's time
            XCTAssertEqual(track.startedAt, pointTime1)
            
            // endedAt should match third point's line
            XCTAssertEqual(track.endedAt, pointTime3)
            XCTAssertEqual(track.totalDurationInMilliseconds, 3000)
            
            // Distances should have updated
            XCTAssertEqual(track.totalDistanceInMeters, 26181.71, accuracy: 0.1)
            
            // Points count should have increased
            XCTAssertEqual(store?.countPoints(), 3)
            XCTAssertEqual(store?.countLines(), 1)
            
        } catch {
            XCTFail(error.localizedDescription)
        }
    }
    
    func testGetLine() {
        do {
            // Create a new track
            let track = try store!.createTrack(name: "Test Get Line", note: nil, activity: nil)
            
            // Add a line to the track
            let line = try store!.addLine(toTrack: track)
            
            let id = line.objectID
            
            // Then fetch it by id
            if let found = store?.getLine(withId: id) {
                XCTAssertEqual(id.uriRepresentation().absoluteString, found.objectID.uriRepresentation().absoluteString)
            } else {
                XCTFail("No line with id \(id.uriRepresentation()) found (id lookup)")
            }
            
            // Then fetch it by URL
            let idURL = URL(string: id.uriRepresentation().absoluteString)
            
            // Then fetch it by id
            if let found = store?.getLine(atURL: idURL!) {
                XCTAssertEqual(id.uriRepresentation().absoluteString, found.objectID.uriRepresentation().absoluteString)
            } else {
                XCTFail("No line with id \(id.uriRepresentation()) found (URL lookup)")
            }
            
            let badIdURL = URL(string: "x-coredata://THINGS-AND-STUFF/Line/p9999")
            
            // Then fetch it by id
            if let _ = store?.getLine(atURL: badIdURL!) {
                XCTFail("No line should be found at URL \(String(describing: idURL?.absoluteString)))")
            } else {
                XCTAssert(true)
            }
            
        } catch {
            XCTFail(error.localizedDescription)
        }
    }
    
    func testAddPointDirectlyToTrackWithMultipleLines() {
        do {
            // Create a new track
            let track = try store!.createTrack(name: "Test Unmanaged Lines", note: nil, activity: nil)
            
            // DO NOT Add a line to the track
            XCTAssertEqual(store?.countPoints(), 0)
            XCTAssertEqual(store?.countLines(), 0)
            
            let pointTime1 = Date(timeIntervalSince1970: 1527275521)
            let location1 = CLLocation(coordinate: CLLocationCoordinate2D(latitude: 38.5, longitude: -106.0), altitude: 2900, horizontalAccuracy: 15, verticalAccuracy: 20, course: 15.5, speed: 16.5, timestamp: pointTime1)
            let _ = try store!.addPoint(toTrack: track, fromLocation: location1)
            
            let pointTime2 = Date(timeIntervalSince1970: 1527275522)
            let location2 = CLLocation(coordinate: CLLocationCoordinate2D(latitude: 38.5, longitude: -106.1), altitude: 2900, horizontalAccuracy: 15, verticalAccuracy: 20, course: 15.5, speed: 16.5, timestamp: pointTime2)
            let _ = try store!.addPoint(toTrack: track, fromLocation: location2)
            
            let pointTime3 = Date(timeIntervalSince1970: 1527275524)
            let location3 = CLLocation(coordinate: CLLocationCoordinate2D(latitude: 38.5, longitude: -106.3), altitude: 2900, horizontalAccuracy: 15, verticalAccuracy: 20, course: 15.5, speed: 16.5, timestamp: pointTime3)
            let _ = try store!.addPoint(toTrack: track, fromLocation: location3)
            
            // Points count should have increased
            XCTAssertEqual(store?.countPoints(), 3)
            XCTAssertEqual(store?.countLines(), 1)
            
            let _ = try store?.addLine(toTrack: track)
            
            let pointTime4 = Date(timeIntervalSince1970: 1527275525)
            let location4 = CLLocation(coordinate: CLLocationCoordinate2D(latitude: 38.5, longitude: -106.0), altitude: 2900, horizontalAccuracy: 15, verticalAccuracy: 20, course: 15.5, speed: 16.5, timestamp: pointTime4)
            let _ = try store!.addPoint(toTrack: track, fromLocation: location4)
            
            let pointTime5 = Date(timeIntervalSince1970: 1527275526)
            let location5 = CLLocation(coordinate: CLLocationCoordinate2D(latitude: 38.6, longitude: -106.0), altitude: 2900, horizontalAccuracy: 15, verticalAccuracy: 20, course: 15.5, speed: 16.5, timestamp: pointTime5)
            let _ = try store!.addPoint(toTrack: track, fromLocation: location5)
            
            let pointTime6 = Date(timeIntervalSince1970: 1527275528)
            let location6 = CLLocation(coordinate: CLLocationCoordinate2D(latitude: 38.8, longitude: -106.0), altitude: 2900, horizontalAccuracy: 15, verticalAccuracy: 20, course: 15.5, speed: 16.5, timestamp: pointTime6)
            let _ = try store!.addPoint(toTrack: track, fromLocation: location6)
            
            XCTAssertEqual(store?.countLines(), 2)
            XCTAssertEqual(store?.countPoints(), 6)
            
            // startedAt should match first point's time
            XCTAssertEqual(track.startedAt, pointTime1)
            
            // endedAt should match last point's line
            XCTAssertEqual(track.endedAt, pointTime6)
            XCTAssertEqual(track.totalDurationInMilliseconds, 6000)
            
            // Distances should be updated
            XCTAssertEqual(track.totalDistanceInMeters, 59499.54, accuracy: 0.1)
            
        } catch {
            XCTFail(error.localizedDescription)
        }
    }

    func testPerformanceExample() {
        // This is an example of a performance test case.
        self.measure {
            // Put the code you want to measure the time of here.
        }
    }
    
}

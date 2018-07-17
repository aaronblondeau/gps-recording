//
//  Track.swift
//  GPS Recording
//
//  Created by Aaron Blondeau on 5/18/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import Foundation
import CoreData

class Track: NSManagedObject {
    static var entityName = "Track"
    
    // These are code:displayName
    static var activities = ["run":"Run","bike":"Bike","hike":"Hike","walk":"Walk","ski":"Ski"]
    
    @NSManaged var name: String?
    @NSManaged var note: String?
    @NSManaged var activity: String?
    @NSManaged var startedAt: Date
    @NSManaged var endedAt: Date
    @NSManaged var totalDistanceInMeters: Double
    @NSManaged var totalDurationInMilliseconds: Double
    @NSManaged var lines: NSSet?
    @NSManaged var upstreamId: String?
    @NSManaged var downstreamId: String?
    
    public func getFilenameBase() -> String {
        var trackName = ""
        if let name = self.name {
            trackName = name
        }
        
        if trackName == "" {
            let formatter = DateFormatter()
            formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
            trackName = formatter.string(from: self.startedAt)
        }
        
        let replaceChars = CharacterSet.alphanumerics.inverted
        trackName = trackName.components(separatedBy: replaceChars).joined(separator: "_");
        return trackName
    }
    
    public func getTransferFileUrl() -> URL? {
        if let dir = NSSearchPathForDirectoriesInDomains(FileManager.SearchPathDirectory.documentDirectory, FileManager.SearchPathDomainMask.allDomainsMask, true).first {
            var idText = self.objectID.uriRepresentation().absoluteString
            let replaceChars = CharacterSet.alphanumerics.inverted
            idText = idText.components(separatedBy: replaceChars).joined(separator: "_");
            let fileUrl = URL(fileURLWithPath: dir).appendingPathComponent("track_transfer_\(idText)_.dat")
            return fileUrl
        }
        return nil
    }
    
    public func toGpx(url: URL) throws {
        let out: OutputStream = OutputStream(url: url, append: false)!
        out.open()
        try self.toGPXStream(out: out)
        out.close()
    }
    
    public func toGeoJSON(url: URL) throws {
        let out: OutputStream = OutputStream(url: url, append: false)!
        out.open()
        try self.toGeoJSONStream(out: out)
        out.close()
    }
    
    private func fileStreamAppend(_ out: OutputStream, text: String) {
        out.write(text, maxLength: text.lengthOfBytes(using: String.Encoding.utf8))
    }
    
    private func gpxFormatDate(_ timestamp: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'"
        return formatter.string(from: timestamp)
    }
    
    public func toGPXStream(out: OutputStream) throws {
        let gpxHeader: String = "<?xml version=\"1.0\" standalone=\"yes\"?>"
        let gpxOpen: String = "<gpx"
            + " xmlns=\"http://www.topografix.com/GPX/1/1\""
            + " version=\"1.1\" creator=\"salidasoftware.com LocationTools 1.0\""
            + " xmlns:gpxtpx=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\""
            + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
            + " xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd http://www.topografix.com/GPX/gpx_style/0/2 http://www.topografix.com/GPX/gpx_style/0/2/gpx_style.xsd\">"
        
        self.fileStreamAppend(out, text: gpxHeader + "\n")
        self.fileStreamAppend(out, text: gpxOpen + "\n")
        
        self.fileStreamAppend(out, text: "\t<trk>\n")
        if let name = self.name {
            self.fileStreamAppend(out, text: "\t\t<name>" + name + "</name>\n")
        } else {
            let formatter = DateFormatter()
            formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
            self.fileStreamAppend(out, text: "\t\t<name>" + formatter.string(from: self.startedAt) + "</name>\n")
        }
        self.fileStreamAppend(out, text: "\t\t<src>GPSRecording iOS</src>\n")
        self.fileStreamAppend(out, text: "\t\t<extensions>\n")
        self.fileStreamAppend(out, text: "\t\t\t<line xmlns=\"http://www.topografix.com/GPX/gpx_style/0/2\">\n")
        self.fileStreamAppend(out, text: "\t\t\t\t<color>ff0000</color>\n")
        self.fileStreamAppend(out, text: "\t\t\t\t<opacity>0.78</opacity>\n")
        self.fileStreamAppend(out, text: "\t\t\t\t<width>0.6000</width>\n")
        self.fileStreamAppend(out, text: "\t\t\t\t<pattern>Solid</pattern>\n")
        self.fileStreamAppend(out, text: "\t\t\t</line>\n")
        self.fileStreamAppend(out, text: "\t\t</extensions>\n")
        
        let linesSortDescriptor = NSSortDescriptor(key: #keyPath(Line.startedAt), ascending: true)
        let pointsSortDescriptor = NSSortDescriptor(key: #keyPath(Point.timestamp), ascending: true)
        
        if let lines = self.lines {
            
            for line in lines.sortedArray(using: [linesSortDescriptor]) {
                let l = line as! Line
                if let points = l.points {
                    if points.count > 1 {
                        self.fileStreamAppend(out, text: "\t\t<trkseg>\n")
                        for point in points.sortedArray(using: [pointsSortDescriptor]) {
                            let p = point as! Point
                            self.fileStreamAppend(out, text: "\t\t\t" + "<trkpt lat=\"\(p.latitude)\" " + "lon=\"\(p.longitude)\">")
                            self.fileStreamAppend(out, text: "<ele>\(p.altitude)</ele>")
                            self.fileStreamAppend(out, text: "<time>\(self.gpxFormatDate(p.timestamp))</time>")
                            self.fileStreamAppend(out, text: "<speed>\(p.speed)</speed>")
                            self.fileStreamAppend(out, text: "<course>\(p.course)</course>")
                            self.fileStreamAppend(out, text: "</trkpt>\n")
                        }
                        self.fileStreamAppend(out, text: "\t\t</trkseg>\n")
                    }
                }
            }
            
        }
        self.fileStreamAppend(out, text: "\t</trk>\n")
        
        //        let waypoints = ...
        //        for waypoint in waypoints {
        //            self.fileStreamAppend(out, text: "\t<wpt lat=\"\(waypoint.latitude)\" lon=\"\(waypoint.longitude)\">")
        //            self.fileStreamAppend(out, text: "<ele>\(waypoint.altitude)</ele>")
        //            self.fileStreamAppend(out, text: "<time>\(self.gpxFormatDate(waypoint.timestamp))</time>")
        //            self.fileStreamAppend(out, text: "<name>\(waypoint.name)</name>")
        //            self.fileStreamAppend(out, text: "<desc>\(waypoint.desc)</desc>")
        //            self.fileStreamAppend(out, text: "</wpt>\n")
        //        }
        
        self.fileStreamAppend(out, text: "</gpx>")
    }
    
    public func toGeoJSONStream(out: OutputStream) throws {
        
        self.fileStreamAppend(out, text: "{")
        self.fileStreamAppend(out, text: "  \"type\": \"FeatureCollection\",\n")
        self.fileStreamAppend(out, text: "  \"features\": [\n")
        self.fileStreamAppend(out, text: "    {\n")
        self.fileStreamAppend(out, text: "      \"type\": \"Feature\",\n")
        self.fileStreamAppend(out, text: "      \"properties\": {},\n")
        self.fileStreamAppend(out, text: "      \"geometry\": {\n")
        self.fileStreamAppend(out, text: "        \"type\": \"MultiLineString\",\n")
        self.fileStreamAppend(out, text: "        \"coordinates\": [\n")
        
        let linesSortDescriptor = NSSortDescriptor(key: #keyPath(Line.startedAt), ascending: true)
        let pointsSortDescriptor = NSSortDescriptor(key: #keyPath(Point.timestamp), ascending: true)
        
        if let lines = self.lines?.sortedArray(using: [linesSortDescriptor]) {
            var index = 0
            for line in lines {
                let l = line as! Line
                
                if let points = l.points?.sortedArray(using: [pointsSortDescriptor]) {
                    
                    if (points.count > 1) {
                    
                        if (index > 0) {
                            self.fileStreamAppend(out, text: "          ,\n")
                        }
                        self.fileStreamAppend(out, text: "          [\n")
                        
                        var pointIndex = 0
                        for point in points {
                            let p = point as! Point
                            self.fileStreamAppend(out, text: "            [\n")
                            self.fileStreamAppend(out, text: "              \(p.longitude),\n")
                            self.fileStreamAppend(out, text: "              \(p.latitude)\n")
                            if (pointIndex >= points.count - 1) {
                                // No comma for last coord
                                self.fileStreamAppend(out, text: "            ]\n")
                            } else {
                                self.fileStreamAppend(out, text: "            ],\n")
                            }
                            pointIndex = pointIndex + 1
                        }
                        
                        self.fileStreamAppend(out, text: "          ]\n")
                        index = index + 1
                        
                    }
                    
                }
            }
        }
        
        self.fileStreamAppend(out, text: "        ]\n")
        self.fileStreamAppend(out, text: "      }\n")
        self.fileStreamAppend(out, text: "    }\n")
        self.fileStreamAppend(out, text: "  ]\n")
        self.fileStreamAppend(out, text: "}\n")
    }
        
    public func toDict() -> [String:Any] {
        var trackDict = [String:Any]()
        trackDict["id"] = self.objectID.uriRepresentation().absoluteString
        if let name = self.name {
            trackDict["name"] = name
        }
        if let note = self.note {
            trackDict["note"] = note
        }
        if let activity = self.activity {
            trackDict["activity"] = activity
        }
        trackDict["startedAt"] = self.startedAt
        trackDict["endedAt"] = self.startedAt
        trackDict["totalDistanceInMeters"] = self.totalDistanceInMeters
        trackDict["totalDurationInMilliseconds"] = self.totalDurationInMilliseconds
        
        var trackDictLines = [[String:Any]]()
        
        let linesSortDescriptor = NSSortDescriptor(key: #keyPath(Line.startedAt), ascending: true)
        let pointsSortDescriptor = NSSortDescriptor(key: #keyPath(Point.timestamp), ascending: true)
        
        if let lines = self.lines {
            for line in lines.sortedArray(using: [linesSortDescriptor]) {
                let l = line as! Line
                var lineDict = [String:Any]()
                lineDict["id"] = l.objectID.uriRepresentation().absoluteString
                lineDict["startedAt"] = l.startedAt
                lineDict["endedAt"] = l.startedAt
                lineDict["totalDistanceInMeters"] = l.totalDistanceInMeters
                
                var lineDictPoints = [[String:Any]]()
                
                if let points = l.points {
                    for point in points.sortedArray(using: [pointsSortDescriptor]) {
                        let p = point as! Point
                        var pointDict = [String:Any]()
                        
                        pointDict["id"] = p.objectID.uriRepresentation().absoluteString
                        pointDict["altitude"] = p.altitude
                        pointDict["course"] = p.course
                        pointDict["horizontalAccuracy"] = p.horizontalAccuracy
                        pointDict["verticalAccuracy"] = p.verticalAccuracy
                        pointDict["latitude"] = p.latitude
                        pointDict["longitude"] = p.longitude
                        pointDict["speed"] = p.speed
                        pointDict["timestamp"] = p.timestamp
                        
                        lineDictPoints.append(pointDict)
                    }
                }
                
                lineDict["points"] = lineDictPoints
                
                trackDictLines.append(lineDict)
            }
        }
        
        trackDict["lines"] = trackDictLines
        
        return trackDict
    }
    
}

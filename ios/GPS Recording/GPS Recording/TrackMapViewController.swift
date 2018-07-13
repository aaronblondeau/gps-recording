//
//  TrackMapViewController.swift
//  GPS Recording
//
//  Created by Aaron Blondeau on 7/6/18.
//  Copyright © 2018 Aaron Blondeau. All rights reserved.
//

import UIKit
import MapKit

class TrackMapViewController: UIViewController, MKMapViewDelegate {

    var store: GPSRecordingStore?
    var track: Track?
    
    @IBOutlet weak var mapView: MKMapView!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Do any additional setup after loading the view.
        mapView.delegate = self
        renderTrack()
    }
    
    private func renderTrack() {
        if let track = self.track {
            
            let linesSortDescriptor = NSSortDescriptor(key: #keyPath(Line.startedAt), ascending: true)
            let pointsSortDescriptor = NSSortDescriptor(key: #keyPath(Point.timestamp), ascending: true)
            
            if let lines = track.lines {
                var trackPoints = [CLLocationCoordinate2D]()
                for line in lines.sortedArray(using: [linesSortDescriptor]) {
                    let l = line as! Line
                    var linePoints = [CLLocationCoordinate2D]()
                    
                    if let points = l.points {
                        for point in points.sortedArray(using: [pointsSortDescriptor]) {
                            let p = point as! Point
                            linePoints.append(CLLocationCoordinate2D(latitude: p.latitude, longitude: p.longitude))
                            trackPoints.append(CLLocationCoordinate2D(latitude: p.latitude, longitude: p.longitude))
                        }
                    }
                    
                    // MKGeodesicPolyline?
                    let mapLine = MKPolyline(coordinates: linePoints, count: linePoints.count)
                    self.mapView.add(mapLine, level: MKOverlayLevel.aboveRoads)
                }
                
                if trackPoints.count > 0 {
                    //if let center = trackPoints.last {
                        //let region = MKCoordinateRegionMakeWithDistance(center, 5000, 5000)
                        
                        let trackBoundsLine = MKPolyline(coordinates: trackPoints, count: trackPoints.count)
                        var region = MKCoordinateRegionForMapRect(trackBoundsLine.boundingMapRect)
                        // Add some padding
                        region.span.latitudeDelta *= 1.1
                        region.span.longitudeDelta *= 1.1
                        mapView.setRegion(region, animated: true)
                    //}
                } else {
                    let center = CLLocationCoordinate2D(latitude: 38.51, longitude: -106.01)
                    let viewRegion = MKCoordinateRegionMakeWithDistance(center, 5000, 5000)
                    mapView.setRegion(viewRegion, animated: true)
                }
            }
        }
    }
    
    func mapView(_ mapView: MKMapView, rendererFor overlay: MKOverlay) -> MKOverlayRenderer {
        if overlay is MKPolyline {
            // draw the track
            let polyLine = overlay
            let polyLineRenderer = MKPolylineRenderer(overlay: polyLine)
            polyLineRenderer.strokeColor = UIColor.blue
            polyLineRenderer.lineWidth = 2.0
            return polyLineRenderer
        }

        return MKOverlayRenderer()
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

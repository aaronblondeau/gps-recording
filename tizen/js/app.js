(function () {
	
	// TODO - when showing tracks list, use list view stuff detailed here : https://developer.samsung.com/galaxy-watch/develop/getting-started/web/create-ui
	
	// TAU components list
	// https://developer.tizen.org/ko/development/api-references/web-application?redirect=/dev-guide/2.4.0/org.tizen.web.apireference/html/ui_fw_api/Mobile_UIComponents/mobile_Button.htm&langredirect=1
	
	// TODO - send track to device : https://developer.samsung.com/galaxy-watch/develop/creating-your-first-app/web-companion/use-sap
	
	var baseDirPath = 'documents' // 'wgt-private' -> does not work
	
	var app = new Vue({
		el: '#app',
		data: {
			message: 'Hello Vue!',
			isRecording: localStorage.getItem('isRecording') === 'yes',
			tracksDir: null,
			trackFileHandle: null,
			currentTrackId: localStorage.getItem('currentTrackId'),
			page: 'home',
			watchId: null,
			error: ''
		},
		computed: {
			appTitle: function() {
				if (this.page === 'record') {
					return 'Record';
				}
				return 'GPS Recording';
			},
			canRecord: function() {
				if (this.tracksDir != null) {
					return true
				}
				return false
			}
		},
		watch: {
			isRecording: function () {
				localStorage.setItem('isRecording', this.isRecording ? 'yes' : 'no')
			}
		},
		methods: {
			startTrack: function() {
				this.currentTrackId = new Date().getTime()
				localStorage.setItem('currentTrackId', this.currentTrackId)
				// TODO 
				// set localstorage currentTrackId_startTime = 
				this.openFile()
				this.startGettingLocations()
			},
			
			openFile: function() {
				this.trackFileHandle = tizen.filesystem.openFile(baseDirPath + '/tracks/' + this.currentTrackId + '.trk', 'a') // use append
				console.log('~~ Track file opened for appending');
			},
			
			closeFile: function() {
				this.trackFileHandle.close()
				this.trackFileHandle = null
				console.log('~~ Track file closed');
			},
			
			logPause: function(position) {
				this.trackFileHandle.writeString('--pause--\n')
				
				// TODO Clear last lat, lng, and timestamp
			},
			
			logPosition: function(position) {
				// TODO
				console.log('~~ Would log position : ' + position.coords.latitude + ',' + position.coords.longitude + ',' + position.coords.altitude)
				
				this.trackFileHandle.writeString(position.coords.latitude + ',' + position.coords.longitude + ',' + position.coords.altitude + '\n')
				
				console.log("~~ accuracy " + position.coords.accuracy)
				console.log("~~ altitudeAccuracy " + position.coords.altitudeAccuracy)
				console.log("~~ heading " + position.coords.heading)
				console.log("~~ speed " + position.coords.speed)
				
				// TODO Each of these in local storage currentTrackId_<field name>
				// Store last lat, lng, and timestamp
				// Update total time
				// Update total distance
			},
			
			resetAll: function() {
				// Delete all track files
				tizen.filesystem.resolve(baseDirPath, function(documentsDir) {
					console.log("~~ resolved documents directory!")
					tizen.filesystem.resolve(baseDirPath + '/tracks', function(tracksDir) {
						console.log("~~ " + baseDirPath + "/tracks resolved")
						tizen.filesystem.listDirectory(baseDirPath + '/tracks', function(files) {
							for (var i = 0; i < files.length; i++)
							{
								var fileName = files[i] + ''								
								console.log("~~ found file : " + i + " - " + fileName);
								if (fileName.endsWith(".trk")) {
									console.log("~~ Delete track file : " + i + " - " + fileName);
									
									tizen.filesystem.deleteFile(baseDirPath + '/tracks/' + fileName, function() {
										console.log("~~ file delete success")
									}, function(deleteError) {
										console.log("~~ file delete error : " + deleteError.message)
									})
									
									// TODO - stop if recording (above this loop!)
									// TODO - also unset all localStorage keys that go with the track id
									// TODO - reset tracks list
									// TODO - reset currentTrackId && trackFileHandle
									
								}
							}
							console.log("~~ done listing tracks directory")
						}, function(e2) {
							console.error("~~ tracks directory list files error " + e2.message);
						});
					}, function (e3) {
						console.log("~~ " + baseDirPath + "/tracks did not resolve " + e3.message)
					})
				}, function(e1) {
					console.error("~~ unable to resolve documents directory " + e1.message);
				}, "rw");
				
			},
			
			fetchTracks: function() {
				var _this = this
								
				tizen.filesystem.resolve(baseDirPath, function(documentsDir) {
					console.log("~~ resolved documents directory!")
					
					tizen.filesystem.resolve(baseDirPath + '/tracks', function(tracksDir) {
						console.log("~~ " + baseDirPath + "/tracks resolved")
						_this.tracksDir = tracksDir
						tizen.filesystem.listDirectory(baseDirPath + '/tracks', function(files) {
							for (var i = 0; i < files.length; i++)
							{
								var fileName = files[i] + ''

//									// TODO : remove me
//									var fileHandleRead = tizen.filesystem.openFile(baseDirPath + '/tracks/' + fileName, 'r');
//									var fileContents = fileHandleRead.readString();
//									console.log('Current track file contents: ' + fileContents);
//									fileHandleRead.close()
								
								console.log("~~ found file : " + i + " - " + fileName);
								if (fileName.endsWith(".trk")) {
									// TODO - add file to tracks list
									console.log("~~ found track file : " + i + " - " + fileName);
								}
							}
							console.log("~~ done listing tracks directory")
						}, function(e2) {
							console.error("~~ tracks directory list files error " + e2.message);
						});
					}, function (e3) {
						console.log("~~ " + baseDirPath + "/tracks did not resolve " + e3.message)
						try {
							tracksDir = documentsDir.createDirectory("tracks")
							_this.tracksDir = tracksDir
							console.log("~~ created tracks directory")
						} catch (e4) {
							console.error("~~ unable to create tracks directory " + e4.message);
						}
					})
				}, function(e1) {
					console.error("~~ unable to resolve documents directory " + e1.message);
				}, "rw");
				
				
			},
			
			gpsStart: function() {
				console.log("~~ gpsStart " + this.message)
				this.isRecording = true
				this.startTrack()
			},
			gpsResume: function() {
				console.log("~~ gpsResume")
				this.isRecording = true
				this.startGettingLocations()
				this.openFile()
			},
			gpsPause: function() {
				console.log("~~ gpsPause")
				this.logPause()
				this.closeFile()
				this.isRecording = false
				this.stopGettingLocations()
			},
			gpsFinish: function() {
				console.log("~~ gpsFinish")
				this.closeFile()
				this.isRecording = false
				this.stopGettingLocations()
				this.currentTrackId = null
				localStorage.removeItem('currentTrackId')
			},
			stopGettingLocations: function() {
				console.log('~~ stopGettingLocations')
				if (this.watchId != null) {
					navigator.geolocation.clearWatch(this.watchId)
					this.watchId = null
				}
			},
			startGettingLocations: function() {
				this.stopGettingLocations()
				var _this = this
				console.log('~~ startGettingLocations')
				if (navigator.geolocation) {
			        this.watchId = navigator.geolocation.watchPosition(function(position) {
			        		// ??? https://developer.tizen.org/dev-guide/2.4/org.tizen.devtools/html/common_tools/emulator.htm 
			        		_this.logPosition(position)
			        }, function(error) {
			        		// Error
				        	switch (error.code) {
				            case error.PERMISSION_DENIED:
				                _this.error = 'User denied the request for Geolocation.';
				                break;
				            case error.POSITION_UNAVAILABLE:
				            		_this.error = 'Location information is unavailable.';
				                break;
				            case error.TIMEOUT:
				            		_this.error = 'The request to get user location timed out.';
				                break;
				            case error.UNKNOWN_ERROR:
				            		_this.error = 'An unknown error occurred.';
				                break;
				        }
				        	console.error('~~ startGettingLocations Error : ' + this.error)
				        	_this.stopGettingLocations()
			        }, {
			        		enableHighAccuracy: true,
			        		maximumAge: 0
			        });
			    } else {
			        this.error = 'Geolocation is not supported.';
			    }
			}
		},
		mounted: function() {
			var _this = this;
			window.addEventListener("tizenhwkey", function (ev) {
				if (ev.keyName === "back") {
					if (_this.page === 'home') {
						console.log('~~ app exit')
						if (_this.isRecording) {
							_this.gpsPause()
						}
						tizen.application.getCurrentApplication().exit();
					} else {
						_this.page = 'home'
					}
				}
			})
			_this.fetchTracks()
		},
		beforeDestroy: function() {
			this.stopGettingLocations()
		}
	})
	
	// https://developer.tizen.org/ko/forums/web-application-development/web-app-running-background?langswitch=ko
	tizen.power.request("CPU", "CPU_AWAKE");
}());



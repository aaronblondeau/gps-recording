(function () {
//	window.addEventListener("tizenhwkey", function (ev) {
//		var activePopup = null,
//			page = null,
//			pageid = "";
//
//		if (ev.keyName === "back") {
//			activePopup = document.querySelector(".ui-popup-active");
//			page = document.getElementsByClassName("ui-page-active")[0];
//			pageid = page ? page.id : "";
//
//			if (pageid === "main" && !activePopup) {
//				try {
//					tizen.application.getCurrentApplication().exit();
//				} catch (ignore) {
//				}
//			} else {
//				window.history.back();
//			}
//		}
//	});
	
	// TODO - when showing tracks list, use list view stuff detailed here : https://developer.samsung.com/galaxy-watch/develop/getting-started/web/create-ui
	
	// TAU components list
	// https://developer.tizen.org/ko/development/api-references/web-application?redirect=/dev-guide/2.4.0/org.tizen.web.apireference/html/ui_fw_api/Mobile_UIComponents/mobile_Button.htm&langredirect=1
	
	var app = new Vue({
		el: '#app',
		data: {
			message: 'Hello Vue!',
			isRecording: localStorage.getItem('isRecording') === 'yes',
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
			}
		},
		watch: {
			isRecording: function () {
				localStorage.setItem('isRecording', this.isRecording ? 'yes' : 'no')
			}
		},
		methods: {
			gpsStart: function() {
				console.log("~~ gpsStart " + this.message)
				this.isRecording = true
				this.startGettingLocations()
			},
			gpsResume: function() {
				console.log("~~ gpsResume")
				this.isRecording = true
				this.startGettingLocations()
			},
			gpsPause: function() {
				console.log("~~ gpsPause")
				this.isRecording = false
				this.stopGettingLocations()
			},
			gpsFinish: function() {
				console.log("~~ gpsFinish")
				this.isRecording = false
				this.stopGettingLocations()
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
			        		console.log(position)
			        		// console.log(position.coords.latitude + ',' + position.coords.longitude)
			        		// TODO - update current recording stats
			        		// TODO - log position to current file
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
						tizen.application.getCurrentApplication().exit();
					} else {
						_this.page = 'home'
					}
				}
			})
		},
		beforeDestroy: function() {
			this.stopGettingLocations()
		}
	})
	
	// https://developer.tizen.org/ko/forums/web-application-development/web-app-running-background?langswitch=ko
	tizen.power.request("CPU", "CPU_AWAKE");
}());



package com.mapbox.rctmgl.modules;

import android.location.Location;
import android.support.annotation.NonNull;

import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.RCTNativeAppEventEmitter;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.rctmgl.events.EventEmitter;
import com.mapbox.rctmgl.events.IEvent;
import com.mapbox.rctmgl.events.LocationEvent;
import com.mapbox.rctmgl.location.LocationManager;

public class RCTMGLLocationModule extends ReactContextBaseJavaModule {
    public static final String REACT_CLASS = RCTMGLLocationModule.class.getSimpleName();
    public static final String LOCATION_UPDATE = "MapboxUserLocationUpdate";

    private boolean isEnabled;
    private boolean isPaused;

    private LocationManager locationManager;

    private LifecycleEventListener lifecycleEventListener = new LifecycleEventListener() {
        @Override
        public void onHostResume() {
            if (isEnabled) {
                startLocationManager();
            }
        }

        @Override
        public void onHostPause() {
            pauseLocationManager();
        }

        @Override
        public void onHostDestroy() {
            startLocationManager();
        }
    };

    private LocationManager.OnUserLocationChange onUserLocationChangeCallback = new LocationManager.OnUserLocationChange() {
        @Override
        public void onLocationChange(Location location) {
            LocationEvent locationEvent = new LocationEvent(location);

            RCTNativeAppEventEmitter emitter = EventEmitter.getModuleEmitter(getReactApplicationContext());
            if (emitter != null) {
                emitter.emit(LOCATION_UPDATE, locationEvent.getPayload());
            }
        }
    };

    public RCTMGLLocationModule(ReactApplicationContext reactContext) {
        super(reactContext);
        locationManager = LocationManager.getInstance(reactContext);
        reactContext.addLifecycleEventListener(lifecycleEventListener);
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }
    
    @ReactMethod
    public void addListener(String eventName) {
    }

    @ReactMethod
    public void removeListeners(Integer count) {
    }

    @ReactMethod
    public void start() {
        isEnabled = true;
        startLocationManager();
    }

    @ReactMethod
    public void stop() {
        stopLocationManager();
    }

    @ReactMethod
    public void pause() {
        pauseLocationManager();
    }

    @ReactMethod
    public void getLastKnownLocation(final Promise promise) {
        locationManager.getLastKnownLocation(
          new LocationEngineCallback<LocationEngineResult>() {
              public void onSuccess(LocationEngineResult result) {
                  Location location = result.getLastLocation();
                  if (result.getLastLocation() != null) {
                      LocationEvent locationEvent = new LocationEvent(location);
                      promise.resolve(locationEvent.getPayload());
                  } else {
                      promise.resolve(null);
                  }
              }
              public void onFailure(@NonNull Exception exception) {
                  promise.reject(exception);
              }
          }
        );
    }

    private void startLocationManager() {
        locationManager.addLocationListener(onUserLocationChangeCallback);
        locationManager.enable();
        isPaused = false;
    }

    private void pauseLocationManager() {
        if (isPaused) {
            return;
        }
        locationManager.disable();
        isPaused = true;
    }

    private void stopLocationManager() {
        if (!isEnabled) {
            return;
        }
        locationManager.removeLocationListener(onUserLocationChangeCallback);
        locationManager.dispose();
        isEnabled = false;
        isPaused = false;
    }
}

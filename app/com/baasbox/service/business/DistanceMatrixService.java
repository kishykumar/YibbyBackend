package com.baasbox.service.business;

import com.baasbox.dao.business.CaberDao;
import com.baasbox.dao.exception.InternalException;
import com.baasbox.databean.LocationBean;
import com.baasbox.push.databean.PickupDriversBean;
import com.baasbox.service.business.TrackingService.OnlineCaberState;
import com.baasbox.service.logging.BaasBoxLogger;
import com.baasbox.databean.GeoLocation;
import com.google.common.graph.ValueGraph;
import com.google.maps.DistanceMatrixApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.LatLng;
import com.google.maps.model.TravelMode;
import com.google.maps.model.Unit;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


// TODO:
// Problem: 
// - Before the bid, show the min ETA of nearest drivers to the rider.
//     Pick the drivers based on static distance and call Google API to get ETA.
//     Mobile app calls the webserver to get ETAs of 5 drivers nearby *DONE*
//
// - During bid, figure out which drivers are nearby to a rider location?
//     Static is OK for now!  *DONE*
//
// - Once the ride is in DRIVER_EN_ROUTE, show the ETA of the driver to the rider. 
//     As we are already sending the driver's location to the rider. The mobile app can make a Google call to get the ETA. 
//
// - Once the ride starts, show the ETA to the destination to the rider.
//     The mobile app can make a Google call to get the ETA.
//
// - Show the distance and duration of the ride in the History of rides on mobile apps. 
//     The driver does a start and end ride. Record miles using Google API at end of ride. Calculate duration using start and end timestamp. 

/**
 * This is code based on https://dzone.com/articles/distance-calculation-using-3
 * @author jason
 *
 */
public class DistanceMatrixService {
    
    private static GeoApiContext context = new GeoApiContext().setApiKey("AIzaSyDJ4MgpeQ33SQ9Bv9_wKFzbwK9Jpkivo3I");
    
    private static class UserNameAndLocation {
        
        public UserNameAndLocation(String un, LatLng location) {
            this.username = un;
            this.loc = location;
        }
        
        public String username;
        public LatLng loc;
    }
    
    public static List<PickupDriversBean> getNearestDriverEta(LatLng loc) throws InternalException {
        
        // get 10 nearest drivers to this pickup location
        List<UserNameAndLocation> nearbyDrivers = getNearbyDriversList(loc);
        
        // Call Google Distance Matrix API to find eta and distance
        if (nearbyDrivers != null && !nearbyDrivers.isEmpty()) {
        
            LatLng[] origins = new LatLng[nearbyDrivers.size()];
            for (int idx = 0; idx < nearbyDrivers.size(); idx++) {
                origins[idx] = nearbyDrivers.get(idx).loc;
            }
            
            return getNearestDriverFromRider(origins, loc);
        }
        
        return null;
    }
    
    public static void updatePickupHint(LatLng loc) throws InternalException {
                
        // get 10 nearest drivers to this pickup location
        List<UserNameAndLocation> nearbyDrivers = getNearbyDriversList(loc);
        
        // Call Google Distance Matrix API to calculate the edges of the graph
        if (nearbyDrivers != null && !nearbyDrivers.isEmpty()) {
        
            LatLng[] origins = new LatLng[nearbyDrivers.size()];
            for (int idx = 0; idx < nearbyDrivers.size(); idx++) {
                origins[idx] = nearbyDrivers.get(idx).loc;
            }
            
            getNearestDriverFromRider(origins, loc);
            
            // https://github.com/google/guava/wiki/GraphsExplained#graph
            
            // update the graph with drivers and rider info
//            ValueGraph<User, Time>
            // User -> userType, timestamp
            // Time -> ETA, road distance
            
            // Create graph class that subclasses MutableValueGraph and override methods
            // need to override equals and hashcode method
            
            // Create an instance of graph 
            // MutableValueGraph<Integer, Double> weightedGraph = ValueGraphBuilder.directed().build();
            
            // Insertion is simple. This also updates existing nodes.
            // weightedGraph.putEdgeValue(2, 3, 1.5);  // also adds nodes 2 and 3 if not already present
            
            // Searching a node
            // graph.nodes().contains(node);
            
            // find neighbors 
            // Set<Integer> successorsOfTwo = graph.successors(2); // returns {3}

            // deletion may be removeEdge, but have to check.??
        }
    }
    
    private static List<PickupDriversBean> getNearestDriverFromRider(LatLng[] origins, LatLng destination) throws InternalException {
        
        DistanceMatrix matrix = DistanceMatrixApi.newRequest(context)
            .origins(origins)
            .units(Unit.IMPERIAL)
            .destinations(destination)
            .mode(TravelMode.DRIVING)
            .awaitIgnoreError();
        
        // confirm if the rows received are the same as origins
        if (origins.length != matrix.rows.length) {
            throw new InternalException("Invalid data from Google");
        }

        List<PickupDriversBean> driversDist = new ArrayList<PickupDriversBean> (matrix.rows.length);
        
        for (int i = 0; i < matrix.rows.length; i++) {
            long distance = matrix.rows[i].elements[0].distance.inMeters;
            long duration = matrix.rows[i].elements[0].duration.inSeconds;
            
            driversDist.add(new PickupDriversBean(new LocationBean(origins[i].lat, origins[i].lng, null), duration, distance));
        }
        
        return driversDist;
    }
    
    public static long getTripDistance(LatLng origin, LatLng destination) {
        
        DistanceMatrix matrix = DistanceMatrixApi.newRequest(context)
            .origins(origin)
            .units(Unit.IMPERIAL)
            .destinations(destination)
            .mode(TravelMode.DRIVING)
            .awaitIgnoreError();

        if (matrix.rows[0].elements[0].distance == null) {
            return 0;
        }
        
        return matrix.rows[0].elements[0].distance.inMeters;
    }
    
    /**
     * Given a range limit, decides whether a point is nearby.
     * @param start
     * @return
     */
    public static boolean isDriverNearby(LatLng loc, LatLng driverLoc) {
        
        double driverLati = driverLoc.lat;
        double driverLongi = driverLoc.lng;
        
        double distance = DistanceCalculationService.distance(loc.lat, loc.lng, driverLati, driverLongi, 'M');
        
        BaasBoxLogger.debug("DistMatSvc::isDriverNearby Calculated one distance: " + distance);
        
        if (distance > BiddingService.VALID_DRIVER_RANGE_LIMIT_IN_MILE) {
            return false;
        }
        else {  
            return true;
        }
    }
    
    /**
     * Finds nearby drivers based on the pickup location.
     * @param lati
     * @param longi
     * @return
     */
    private static List<UserNameAndLocation> getNearbyDriversList (LatLng loc) {
        
        Map<String, OnlineCaberState> onlineCabers = TrackingService.onlineCabers;
        if (onlineCabers != null) {
            List<UserNameAndLocation> nearbyDrivers = new ArrayList<UserNameAndLocation>();
            
            for(String driverUserName: onlineCabers.keySet()) {
                
                GeoLocation geoLocation = TrackingService.getTrackingService().getDriverLocation(driverUserName);
                
                    if (geoLocation != null) {
                    LatLng driverLoc = new LatLng(geoLocation.getLatitude(), geoLocation.getLongitude());
                    
                    // TODO: Sort by distance and keep top 10 drivers
                    if (geoLocation != null && isDriverNearby(loc, driverLoc)) {
                        nearbyDrivers.add(new UserNameAndLocation(driverUserName, driverLoc));
                        BaasBoxLogger.debug("DistMatSvc::getNearbyDriversList Found one driver nearby: " + driverUserName);   
                    }
                }
            }
            
            BaasBoxLogger.debug("DistMatSvc::getNearbyDriversList List of drivers nearby: " + nearbyDrivers.toString()); 
            return nearbyDrivers;
        }
        else {
            return null;
        }
    }
}

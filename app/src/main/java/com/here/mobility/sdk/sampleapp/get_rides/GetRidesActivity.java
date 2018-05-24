package com.here.mobility.sdk.sampleapp.get_rides;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.Lists;
import com.here.mobility.sdk.common.util.PermissionUtils;
import com.here.mobility.sdk.core.auth.UserAuthenticationException;
import com.here.mobility.sdk.core.geo.LatLng;
import com.here.mobility.sdk.core.net.ResponseException;
import com.here.mobility.sdk.core.net.ResponseFuture;
import com.here.mobility.sdk.core.net.ResponseListener;
import com.here.mobility.sdk.demand.BookingConstraints;
import com.here.mobility.sdk.demand.DemandClient;
import com.here.mobility.sdk.demand.PassengerDetails;
import com.here.mobility.sdk.demand.PublicTransportRideOffer;
import com.here.mobility.sdk.demand.Ride;
import com.here.mobility.sdk.demand.RideOffer;
import com.here.mobility.sdk.demand.RideOffersRequest;
import com.here.mobility.sdk.demand.RideQuery;
import com.here.mobility.sdk.demand.RideQueryResponse;
import com.here.mobility.sdk.demand.RideStatusLog;
import com.here.mobility.sdk.demand.RideWaypoints;
import com.here.mobility.sdk.demand.TaxiRideOffer;
import com.here.mobility.sdk.map.FusedUserLocationSource;
import com.here.mobility.sdk.map.MapController;
import com.here.mobility.sdk.map.MapFragment;
import com.here.mobility.sdk.map.MapView;
import com.here.mobility.sdk.map.Marker;
import com.here.mobility.sdk.map.PolylineOverlay;
import com.here.mobility.sdk.map.geocoding.GeocodingResult;
import com.here.mobility.sdk.map.route.Route;
import com.here.mobility.sdk.map.route.RouteRequest;
import com.here.mobility.sdk.map.route.RouteResponse;
import com.here.mobility.sdk.map.route.RoutingClient;
import com.here.mobility.sdk.sampleapp.R;
import com.here.mobility.sdk.sampleapp.geocoding.AutoCompleteActivity;
import com.here.mobility.sdk.sampleapp.registration.RegistrationDialog;
import com.here.mobility.sdk.sampleapp.ride_offers.RideOffersActivity;
import com.here.mobility.sdk.sampleapp.rides.ActiveRidesActivity;
import com.here.mobility.sdk.sampleapp.util.AuthUtils;
import com.here.mobility.sdk.sampleapp.util.Constant;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**********************************************************
 * Copyright © 2018 HERE Global B.V. All rights reserved. *
 **********************************************************/
public class GetRidesActivity extends AppCompatActivity implements MapView.MapReadyListener , RideDetailsFragment.RideDetailsFragmentCallback {


    /**
     * Activity log tag.
     */
    @NonNull
    private static final String LOG_TAG = GetRidesActivity.class.getSimpleName();


    /**
     * Start activity for result pickup id.
     */
    @NonNull
    private static final int PICKUP_GEOCODING_REQUEST = 1;


    /**
     * Start activity for result destination id.
     */
    @NonNull
    private static final int DESTINATION_GEOCODING_REQUEST = 2;


    /**
     * RideDetailsFragment tag.
     */
    @NonNull
    private static final String RIDE_DETAILS_FRAGMENT = "RIDE_DETAILS_FRAGMENT";


    /**
     * Location permission code.
     */
    private static final int LOCATION_PERMISSIONS_CODE = 42;


    /**
     * MapController zoom level.
     */
    private static final float MAP_ZOOM = 14.5f;


    /**
     * Used to interact with the map.
     */
    private MapController mapController;


    /**
     * Use for route calculation.
     */
    @Nullable
    private RoutingClient routingClient;


    /**
     * The ride pickup.
     */
    @Nullable
    private GeocodingResult pickup;


    /**
     * The ride destination.
     */
    @Nullable
    private GeocodingResult destination;


    /**
     * The ride pickup.
     */
    @Nullable
    private Marker pickupMarker;


    /**
     * The ride destination.
     */
    @Nullable
    private Marker destinationMarker;


    /**
     * Save an id which the polyline can later be removed.
     */
    private long routePolylineId = 0;


    /**
     * Use DemandClient to request ride offers.
     */
    private DemandClient demandClient;


    /**
     * Ride Passenger Details.
     */
    @Nullable
    private PassengerDetails passengerDetails;


    /**
     * List of future rides.
     */
    @Nullable
    private List<Ride> activeRides;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_rides);

        //Initialize DemandClient.
        demandClient = DemandClient.newInstance(this);

        //MapFragment initialization.
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.loadMapAsync(this);
        }
        updateUI();
    }


    @Override
    protected void onStart() {
        super.onStart();

        //Get Active rides.
        getActiveRides();
    }


    /**
     * Update UI.
     */
    private void updateUI(){
        findViewById(R.id.destAddressView).setOnClickListener(view -> {
            String query = ((EditText)view).getText().toString();
            startActivityForResult(AutoCompleteActivity.createIntent(GetRidesActivity.this,query),DESTINATION_GEOCODING_REQUEST);

        });
        findViewById(R.id.pickupAddressView).setOnClickListener(view -> {
            String query = ((EditText)view).getText().toString();
            startActivityForResult(AutoCompleteActivity.createIntent(GetRidesActivity.this,query),PICKUP_GEOCODING_REQUEST);
        });
        findViewById(R.id.show_rides_button).setOnClickListener(this::onShowRidesButtonClicked);
        findViewById(R.id.show_future_rides_button).setOnClickListener(v -> {
            if (activeRides != null) {
                startActivity(ActiveRidesActivity.createIntent(this, activeRides));
            }
        });
    }


    /**
     * this callback is called when the map is set-up, before we render any tiles to the screen - so this is the place to set those values
     * @param mapController map controller to interact with the map.
     */
    @Override
    public void onMapReady(@NonNull MapController mapController) {
        this.mapController = mapController;
        //Set the map center position.
        mapController.setPosition(Constant.CENTER_OF_LONDON);
        //Set map zoom.
        mapController.setZoom(MAP_ZOOM);

        if (!PermissionUtils.hasAnyLocationPermissions(this)) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSIONS_CODE);
        }else{
            startLocationUpdates();
        }
    }


    @Override
    public void onMapFailure(@NonNull Exception e) {
        Log.e(LOG_TAG, "onMapFailure: ", e);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK){
            if (requestCode == PICKUP_GEOCODING_REQUEST){

                GeocodingResult pickup = data.getParcelableExtra(AutoCompleteActivity.GEOCODING_RESULT);
                this.pickup = pickup;
                mapController.setPosition(pickup.getLocation());

                //add pickup marker
                showPickupMarkerAt(pickup.getLocation());
                String address = String.format(Locale.getDefault(),
                        "%s, %s",pickup.getTitle(),pickup.getAddressText());
                ((TextView)findViewById(R.id.pickupAddressView))
                        .setText(address);

            }else if (requestCode == DESTINATION_GEOCODING_REQUEST){
                GeocodingResult destination = data.getParcelableExtra(AutoCompleteActivity.GEOCODING_RESULT);
                this.destination = destination;

                //add destination marker
                showDestinationMarkerAt(destination.getLocation());
                String address = String.format(Locale.getDefault(),
                        "%s, %s",destination.getTitle(),destination.getAddressText());
                ((TextView)findViewById(R.id.destAddressView))
                        .setText(address);
            }
        }
        notifyRideDetailsChanged();
    }


    /**
     * Notify that ride details pickup or destination has changed.
     */
    private void notifyRideDetailsChanged() {
        if ( pickup != null && destination != null ) {

            //lazy initialization for RoutingClient.
            if (routingClient == null) {
                routingClient = new RoutingClient(this);
            }else{
                routingClient.cancelAllActiveRequests();
            }
            //Request route calculation between pickup to destination.
            RouteRequest routeRequest = RouteRequest.create(pickup.getLocation(),
                    destination.getLocation());

            //Route Request, register to updates listener.
            routingClient.
                    requestRoute(routeRequest).
                    registerListener(routeListener);
        }
    }


    /**
     * Route response listener.
     */
    private ResponseListener<RouteResponse> routeListener = new ResponseListener<RouteResponse>() {
        @Override
        public void onResponse(RouteResponse routeResponse) {
            if(routeResponse.getRoute() != null){
                drawRoute(routeResponse.getRoute());
            }
        }

        @Override
        public void onError(@NonNull ResponseException e) {
            Toast.makeText(GetRidesActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    };


    /**
     * Draw route on map by adding route polyline to the map.
     * @param route calculated route that received from {@link RouteResponse}
     */
    private void drawRoute(@NonNull Route route){

        if (routePolylineId != 0){
            mapController.removePolyline(routePolylineId);
        }
        routePolylineId = mapController.addPolyline(new PolylineOverlay(route.getGeometry()));

        //Map center. After adding route polylines to map we need center the map around the route.
        //The best practice to do so is use route bounding box and padding in needed.
        mapController.showBoundingBox(route.getGeometry().getBoundingBox(),new Rect(20, 180, 20, 70));


    }


    /**
     * Call when show rides action button clicked.
     */
    public void onShowRidesButtonClicked(@NonNull View view){
        if (pickup != null && destination != null){
            Fragment rideDetailsFragment = getSupportFragmentManager().findFragmentByTag(RIDE_DETAILS_FRAGMENT);
            if (rideDetailsFragment == null) {
                rideDetailsFragment = RideDetailsFragment.newInstance();
            }
            if (!rideDetailsFragment.isAdded()){
                getSupportFragmentManager().beginTransaction().
                        add(R.id.ride_details_container,rideDetailsFragment,RIDE_DETAILS_FRAGMENT)
                        .addToBackStack(null).commit();
            }
        }else{
            Toast.makeText(this, R.string.fill_mandatory_fields, Toast.LENGTH_SHORT).show();
        }
    }


    /**
     *
     * @param passengerDetails passenger details
     * @param constraints the booking constraint of ride.
     * @param note user ride note.
     * @param preBookTime leave after, pre-book time in timestamp, null if leave time is now.
     */
    @Override
    public void onRideDetailsFill(@NonNull PassengerDetails passengerDetails,
                                  @NonNull BookingConstraints constraints,
                                  @Nullable String note,
                                  @Nullable Long preBookTime) {
        this.passengerDetails = passengerDetails;
        RideWaypoints rideWaypoints = RideWaypoints.create(pickup.getLocation(),destination.getLocation());
        requestRideOffers(rideWaypoints, constraints, note, preBookTime);
    }


    /**
     * Ride offers request.
     */
    private void requestRideOffers(@NonNull RideWaypoints rideWaypoints,
                                   @NonNull BookingConstraints constraints,
                                   @Nullable String passengerNote,
                                   @Nullable Long preBookTime) {
        RideOffersRequest.Builder rideOfferBuilder = RideOffersRequest.builder()
                .setConstraints(constraints)
                .setRideWaypoints(rideWaypoints);

        //set pre-book time, default is now.
        if (preBookTime != null) {
            rideOfferBuilder.setPrebookPickupTime(preBookTime);
        }

        //set passenger note
        if (passengerNote != null){
            rideOfferBuilder.setPassengerNote(passengerNote);
        }

        RideOffersRequest rideOffersRequest = rideOfferBuilder.build();

        //Request ride offers.
        ResponseFuture<List<RideOffer>> offersFuture = demandClient.getRideOffers(rideOffersRequest);

        //Register offers future listener.
        offersFuture.registerListener(rideOffersFutureListener);

    }


    /**
     * A callback method that received ride offers after {@link DemandClient#getRideOffers(RideOffersRequest)} request.
     */
    private ResponseListener<List<RideOffer>> rideOffersFutureListener = new ResponseListener<List<RideOffer>>() {
        @Override
        public void onResponse(@NonNull List<RideOffer> rideOffers) {
            showRideOffersActivity(rideOffers);
        }

        @Override
        public void onError(@NonNull ResponseException e) {
            Toast.makeText(GetRidesActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    };


    /**
     * Start Ride offers activity.
     * @param rideOffers list of ride offers.
     */
    private void showRideOffersActivity(List<RideOffer> rideOffers){
        if (passengerDetails != null) {

            if(rideOffers.size() > 0){

                ArrayList<TaxiRideOffer> taxiRideOffers = Lists.newArrayList();
                ArrayList<PublicTransportRideOffer> ptRideOffers = Lists.newArrayList();

                for(RideOffer offer : rideOffers){
                    offer.accept(new RideOffer.Visitor<Void>() {
                        @Override
                        public Void visit(@NonNull TaxiRideOffer taxiRideOffer) {
                            taxiRideOffers.add((TaxiRideOffer) offer);
                            return null;
                        }

                        @Override
                        public Void visit(@NonNull PublicTransportRideOffer publicTransportRideOffer) {
                            ptRideOffers.add((PublicTransportRideOffer) offer);
                            return null;
                        }
                    });
                }

                startActivity(RideOffersActivity
                        .createIntent(this, taxiRideOffers, ptRideOffers, passengerDetails));
            }else{
                Toast.makeText(this, R.string.error_no_ride_options_results, Toast.LENGTH_LONG).show();
            }
        }
    }


    /**
     * Request ride attach to user.
     */
    private void getActiveRides() {

        //Build a ride query.
        RideQuery rideQuery =  RideQuery.builder()
                .setStatusFilter(RideQuery.StatusFilter.ALL)
                .build();

        //get the rides and register a listener.
        demandClient.getRides(rideQuery)
                .registerListener(getRideListener);
    }


    /**
     * Get rides response listener.
     */
    @NonNull
    private ResponseListener<RideQueryResponse> getRideListener = new ResponseListener<RideQueryResponse>() {

        @Override
        public void onResponse(@NonNull RideQueryResponse rideQueryResponse) {
            setActiveRides(rideQueryResponse.getRides());
        }

        @Override
        public void onError(@NonNull ResponseException e) {
            // If the user authentication token that was provided by HereMobilitySDK.setUserAuthInfo() is expired,
            // UserAuthenticationException will be returned. To handle this, call HereMobilitySDK.setUserAuthInfo()
            // again with a valid token, and initiate the SDK API call again.
            // Note that this exception can be returned from any API call, so this error handling should
            // be implemented on every onError call.
            if(e.getRootCause() instanceof UserAuthenticationException){
                showRegistrationDialog();
            }else{
                Toast.makeText(GetRidesActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    };


    /**
     * Show registration dialog.
     */
    private void showRegistrationDialog() {
        RegistrationDialog dialog = new RegistrationDialog(GetRidesActivity.this);
        dialog.setPositiveButton(R.string.register, (d, which) -> {
            String userName = dialog.getUserName();
            if (!userName.isEmpty()){

                // The user registration should be done with your app's backend (see the documentation for more info).
                // This is a snippet to generate the token in the app, for testing purposes.
                AuthUtils.registerUser(userName,
                        getString(R.string.here_sdk_app_id),
                        getString(R.string.here_sdk_app_secret));
                getActiveRides();
            }else{
                Toast.makeText(GetRidesActivity.this,R.string.register_not_valid_user_name,Toast.LENGTH_LONG).show();
            }
        });
        dialog.show();
    }


    /**
     * Set future rides and update UI.
     * @param rides list of rides.
     */
    private void setActiveRides(@NonNull List<Ride> rides){

        //show rides with type ongoing or future.
        activeRides = Lists.newArrayList();

        for (Ride ride : rides) {
            boolean isActive = ride.getStatusLog().getCurrentStatus() != RideStatusLog.Status.REJECTED &&
                    ride.getStatusLog().getCurrentStatus().ordinal() < RideStatusLog.Status.COMPLETED.ordinal();
            if (isActive){
                activeRides.add(ride);
            }
        }

        //show of dismiss button according to rides exist.
        boolean hideRidesButton = activeRides.size() == 0;
        findViewById(R.id.show_future_rides_button)
                .setVisibility(hideRidesButton ? View.GONE : View.VISIBLE);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        //It's important to call shutdownNow function when the client is no longer needed.
        if (demandClient != null) {
            demandClient.shutdownNow();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        if (requestCode == LOCATION_PERMISSIONS_CODE){
            if ((grantResults.length > 0) && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                startLocationUpdates();
            }
        }
    }


    /**
     * Start user location updates.
     */
    @SuppressLint("MissingPermission")
    private void startLocationUpdates(){

        mapController.getUserLocationMarkerManager().setLocationSource(new FusedUserLocationSource(this));

    }


    /**
     * Creates a marker at the given location.
     * @param location the marker location
     * @param imageRes image res of marker icon.
     * @return Marker
     */
    @NonNull
    private Marker createMarker(@NonNull LatLng location, @DrawableRes int imageRes){

        //Create map marker.
        Marker marker = mapController.addMarker();
        Resources resources = getResources();
        Drawable drawable = ResourcesCompat.getDrawable(resources, imageRes, null);
        if (drawable == null){
            throw new Resources.NotFoundException();
        }

        //Set marker style.
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        float density = resources.getDisplayMetrics().density;
        String size = "[" + Math.round(width/density) + "px, " + Math.round(height/density) + "px]";
        String styleString = "{ style: 'points', color: 'white', size: " + size + ", order: 10000, collide: false, anchor: top }";
        marker.setStylingFromString(styleString);

        //Set marker icon
        marker.setDrawable(drawable);

        //Set marker location
        marker.setPoint(location);

        return marker;
    }


    /**
     * Show pickup marker at point.
     * @param point the point.
     */
    public void showPickupMarkerAt(@NonNull LatLng point){
        if (pickupMarker == null){
            // Create marker lazily.
            pickupMarker = createMarker(point, R.drawable.ic_location_on_black_24dp);
        }else{
            // Otherwise just set marker location.
            pickupMarker.setPoint(point);
        }
    }


    /**
     * Show pickup marker at point.
     * @param point the point.
     */
    public void showDestinationMarkerAt(@NonNull LatLng point){
        if (destinationMarker == null) {
            // Create marker lazily.
            destinationMarker = createMarker(point, R.drawable.ic_pin_drop_black_24dp);
        }else{
            // Otherwise just set marker location.
            destinationMarker.setPoint(point);
        }
    }
}
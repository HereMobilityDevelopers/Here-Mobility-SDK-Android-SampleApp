package com.here.mobility.sdk.sampleapp.get_rides;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Process;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;
import com.google.common.collect.Lists;
import com.here.mobility.sdk.core.MobilitySdk;
import com.here.mobility.sdk.core.auth.UserAuthenticationException;
import com.here.mobility.sdk.core.geo.Address;
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
import com.here.mobility.sdk.demand.RidePreferences;
import com.here.mobility.sdk.demand.RideQuery;
import com.here.mobility.sdk.demand.RideQueryResponse;
import com.here.mobility.sdk.demand.RideStatusLog;
import com.here.mobility.sdk.demand.RideWaypoints;
import com.here.mobility.sdk.demand.TaxiRideOffer;
import com.here.mobility.sdk.demand.Waypoint;
import com.here.mobility.sdk.map.FusedUserLocationSource;
import com.here.mobility.sdk.map.MapController;
import com.here.mobility.sdk.map.MapFragment;
import com.here.mobility.sdk.map.MapImageStyle;
import com.here.mobility.sdk.map.MapView;
import com.here.mobility.sdk.map.Marker;
import com.here.mobility.sdk.map.Polyline;
import com.here.mobility.sdk.map.PolylineStyle;
import com.here.mobility.sdk.map.geocoding.GeocodingResult;
import com.here.mobility.sdk.map.route.Route;
import com.here.mobility.sdk.map.route.RouteRequest;
import com.here.mobility.sdk.map.route.RouteResponse;
import com.here.mobility.sdk.map.route.RoutingClient;
import com.here.mobility.sdk.sampleapp.R;
import com.here.mobility.sdk.sampleapp.geocoding.AutoCompleteActivity;
import com.here.mobility.sdk.sampleapp.registration.LoginActivity;
import com.here.mobility.sdk.sampleapp.ride_offers.RideOffersActivity;
import com.here.mobility.sdk.sampleapp.rides.ActiveRidesActivity;
import com.here.mobility.sdk.sampleapp.util.Constant;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**********************************************************
 * Copyright © 2018 HERE Global B.V. All rights reserved. *
 **********************************************************/
public class GetRidesActivity extends AppCompatActivity implements MapView.MapControllerListener, RideDetailsFragment.RideDetailsFragmentCallback {


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
	 * Start activity for user id authorization.
	 */
	@NonNull
	private static final int LOGIN_REQUEST = 3;


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
	 * The ride pickup location.
	 */
	@Nullable
	private GeocodingResult pickup;


	/**
	 * The ride pickup address.
	 */
	@Nullable
	private Address pickupAddress;

	/**
	 * The ride destination location.
	 */
	@Nullable
	private GeocodingResult destination;


	/**
	 * The ride destination address.
	 */
	@Nullable
	private Address destinationAddress;


	/**
	 * The ride pickup marker.
	 */
	@Nullable
	private Marker pickupMarker;


	/**
	 * The ride destination marker.
	 */
	@Nullable
	private Marker destinationMarker;


	/**
	 * Save a polyline which can later be removed.
	 */
	private Polyline routePolyline;


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


	/**
	 * Ride preferences.
	 */
	@Nullable
	private RidePreferences ridePreferences;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		installProvider();

		setContentView(R.layout.activity_get_rides);

		//Initialize DemandClient.
		demandClient = DemandClient.newInstance();

		//MapFragment initialization.
		MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map_fragment);
		if (mapFragment != null) {
			mapFragment.loadMapAsync(this);
		}
		updateUI();
	}


	/**
	 * Install the security provider to protect against SSL exploits.
	 * <p>
	 * We are using TLS version 1.2 which is not supported in API <= 19.
	 * So we need to install the gms security provider, otherwise, all network requests done by HereSDK will fail.
	 * <p>
	 * See <a href="https://developer.android.com/training/articles/security-gms-provider</a> for more details
	 */
	private void installProvider() {
		try {
			ProviderInstaller.installIfNeeded(this);
		} catch (GooglePlayServicesRepairableException e) {
			// Indicates that Google Play services is out of date, disabled, etc.
			// Prompt the user to install/update/enable Google Play services.
			Log.e(LOG_TAG, "installProvider: GooglePlayServicesRepairableException: ", e);
		} catch (GooglePlayServicesNotAvailableException e) {
			// Indicates a non-recoverable error; the ProviderInstaller is not able to install an up-to-date Provider.
			Log.e(LOG_TAG, "installProvider: GooglePlayServicesNotAvailableException: ", e);
		}
	}


	@Override
	protected void onStart() {
		super.onStart();

		if (MobilitySdk.getInstance().getUserAuthInfo() == null) {
			showLoginActivity();
		}
		// User that not verified can only use mapServices, getVerticalCoverage, getOffers. To receive your active rides - your phone need to be verified.
		if (MobilitySdk.getInstance().isVerified()) {
			getActiveRides();
		}
	}


	/**
	 * Update UI.
	 */
	private void updateUI() {
		findViewById(R.id.destAddressView).setOnClickListener(view -> {
			String query = ((EditText) view).getText().toString();
			startActivityForResult(AutoCompleteActivity.createIntent(GetRidesActivity.this, query), DESTINATION_GEOCODING_REQUEST);

		});
		findViewById(R.id.pickupAddressView).setOnClickListener(view -> {
			String query = ((EditText) view).getText().toString();
			startActivityForResult(AutoCompleteActivity.createIntent(GetRidesActivity.this, query), PICKUP_GEOCODING_REQUEST);
		});
		findViewById(R.id.show_rides_button).setOnClickListener(this::onShowRidesButtonClicked);
		findViewById(R.id.show_future_rides_button).setOnClickListener(v -> {
			if (activeRides != null) {
				startActivity(ActiveRidesActivity.createIntent(this, activeRides));
			}
		});
	}


	/**
	 * This callback is called when the map is set-up, before we render any tiles to the screen - so this is the place to set those values.
	 *
	 * @param mapController Map controller to interact with the map.
	 */
	@Override
	public void onMapReady(@NonNull MapController mapController) {
		this.mapController = mapController;
		//Set the map center position.
		mapController.setPosition(Constant.CENTER_OF_LONDON);
		//Set map zoom.
		mapController.setZoom(MAP_ZOOM);

		if (!hasAnyLocationPermissions()) {
			ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSIONS_CODE);
		} else {
			startLocationUpdates();
		}
	}


	/**
	 * Returns whether we have either fine <strong>or</strong> coarse location permissions.
	 */
	private boolean hasAnyLocationPermissions(){
		return hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
				hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
	}


	/**
	 * Returns whether we have been granted the given permission.
	 */
	private boolean hasPermission(@NonNull String permission) {
		return checkPermission(permission, android.os.Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED;
	}


	@Override
	public void onMapInitializationFailure(@NonNull Exception e) {
		Log.e(LOG_TAG, "onMapInitializationFailure: ", e);
	}


	@Override
	public void onAuthenticationFailure(@NonNull UserAuthenticationException e) {
		Log.e(LOG_TAG, "onMapAuthFailure: ", e);
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode == RESULT_OK) {
			if (requestCode == PICKUP_GEOCODING_REQUEST) {

				GeocodingResult pickup = data.getParcelableExtra(AutoCompleteActivity.GEOCODING_RESULT);
				Address pickupAddress = data.getParcelableExtra(AutoCompleteActivity.ADDRESS_DATA);
				this.pickup = pickup;
				this.pickupAddress = pickupAddress;
				mapController.setPosition(pickup.getLocation());

				//add pickup marker
				showPickupMarkerAt(pickup.getLocation());
				String address = String.format(Locale.getDefault(),
						"%s, %s", pickup.getTitle(), pickup.getAddressText());
				((TextView) findViewById(R.id.pickupAddressView))
						.setText(address);

			} else if (requestCode == DESTINATION_GEOCODING_REQUEST) {
				GeocodingResult destination = data.getParcelableExtra(AutoCompleteActivity.GEOCODING_RESULT);
				Address destinationAddress = data.getParcelableExtra(AutoCompleteActivity.ADDRESS_DATA);
				this.destination = destination;
				this.destinationAddress = destinationAddress;

				//add destination marker
				showDestinationMarkerAt(destination.getLocation());
				String address = String.format(Locale.getDefault(),
						"%s, %s", destination.getTitle(), destination.getAddressText());
				((TextView) findViewById(R.id.destAddressView))
						.setText(address);
			} else if (requestCode == LOGIN_REQUEST) {
				if (mapController != null) {
					// The loginActivity is on top of GetRidesActivity, and the latter tried to show a map before registering.
					// This means that the map tiles failed since the user was not yet registered.
					// So we need to refresh the loading tiles for the map to be presented correctly.
					mapController.refreshLoadingTiles();
				}
			}
		}
		notifyRideDetailsChanged();
	}


	/**
	 * Called when ride details (pickup or destination) have changed.
	 */
	private void notifyRideDetailsChanged() {
		if (pickup != null && destination != null) {

			//lazy initialization for RoutingClient.
			if (routingClient == null) {
				routingClient = RoutingClient.newInstance();
			} else {
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
			if (routeResponse.getRoute() != null) {
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
	 *
	 * @param route Calculated route received from {@link RouteResponse}
	 */
	private void drawRoute(@NonNull Route route) {

		if (routePolyline != null) {
			mapController.removePolyline(routePolyline);
		}
		routePolyline = mapController.addPolyline(route.getGeometry(), PolylineStyle.builder().build());

		//Map center. After adding route polylines to map we need center the map around the route.
		//The best practice to do so is use route bounding box and padding in needed.
		mapController.showBoundingBox(route.getGeometry().getBoundingBox(), new Rect(20, 180, 20, 70));


	}


	/**
	 * Called when "Show Rides" action button is clicked.
	 */
	public void onShowRidesButtonClicked(@NonNull View view) {
		if (pickup != null && destination != null) {
			Fragment rideDetailsFragment = getSupportFragmentManager().findFragmentByTag(RIDE_DETAILS_FRAGMENT);
			if (rideDetailsFragment == null) {
				rideDetailsFragment = RideDetailsFragment.newInstance();
			}
			if (!rideDetailsFragment.isAdded()) {
				getSupportFragmentManager().beginTransaction().
						add(R.id.ride_details_container, rideDetailsFragment, RIDE_DETAILS_FRAGMENT)
						.addToBackStack(null).commit();
			}
		} else {
			Toast.makeText(this, R.string.fill_mandatory_fields, Toast.LENGTH_SHORT).show();
		}
	}


	/**
	 * Called when the ride details are filled.
	 *
	 * @param passengerDetails Passenger details
	 * @param constraints      The booking constraint of the ride.
	 * @param note             Optional user note.
	 * @param preBookTime      The pre-booked pickup time for a future ride. Null for an immediate ride (leave now).
	 */
	@Override
	public void onRideDetailsFill(@NonNull PassengerDetails passengerDetails,
								  @NonNull BookingConstraints constraints,
								  @Nullable String note,
								  @Nullable Long preBookTime,
								  boolean subscribeToMessages) {
		this.passengerDetails = passengerDetails;
		this.ridePreferences = RidePreferences.create(subscribeToMessages);
		Waypoint pickupWaypoint = Waypoint.builder(pickup.getLocation())
				.setAddress(pickupAddress)
				.build();
		Waypoint destinationWaypoint = Waypoint.builder(destination.getLocation())
				.setAddress(destinationAddress)
				.build();
		RideWaypoints rideWaypoints = RideWaypoints.create(pickupWaypoint, destinationWaypoint);
		requestRideOffers(rideWaypoints, constraints, note, preBookTime);
	}

	/**
	 * Request ride offers with the given details.
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
		if (passengerNote != null) {
			rideOfferBuilder.setPassengerNote(passengerNote);
		}

		RideOffersRequest rideOffersRequest = rideOfferBuilder.build();

		//Request ride offers.
		ResponseFuture<List<RideOffer>> offersFuture = demandClient.getRideOffers(rideOffersRequest);

		//Register offers future listener.
		offersFuture.registerListener(rideOffersFutureListener);

	}


	/**
	 * A callback method that receives ride offers after a {@link DemandClient#getRideOffers(RideOffersRequest)} request.
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
	 *
	 * @param rideOffers The list of ride offers.
	 */
	private void showRideOffersActivity(List<RideOffer> rideOffers) {
		if (passengerDetails != null) {

			if (rideOffers.size() > 0) {

				ArrayList<TaxiRideOffer> taxiRideOffers = Lists.newArrayList();
				ArrayList<PublicTransportRideOffer> ptRideOffers = Lists.newArrayList();

				for (RideOffer offer : rideOffers) {
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
						.createIntent(this, taxiRideOffers, ptRideOffers, passengerDetails, ridePreferences));
			} else {
				Toast.makeText(this, R.string.error_no_ride_options_results, Toast.LENGTH_LONG).show();
			}
		}
	}


	/**
	 * Request rides for the current user.
	 */
	private void getActiveRides() {

		//Build a ride query.
		RideQuery rideQuery = RideQuery.builder()
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
			if (e.getRootCause() instanceof UserAuthenticationException) {
				showLoginActivity();
			} else {
				Toast.makeText(GetRidesActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
			}
		}
	};


	/**
	 * Show registration dialog.
	 */
	private void showLoginActivity() {
		Intent loginActivity = LoginActivity.createIntent(this, true, false);
	    startActivityForResult(loginActivity, LOGIN_REQUEST);
	}


	/**
	 * Set ongoing and future rides and update the UI.
	 *
	 * @param rides list of rides.
	 */
	private void setActiveRides(@NonNull List<Ride> rides) {

		//show rides with type ongoing or future.
		activeRides = Lists.newArrayList();

		for (Ride ride : rides) {
			boolean isActive = ride.getStatusLog().getCurrentStatus() != RideStatusLog.Status.REJECTED &&
					ride.getStatusLog().getCurrentStatus().ordinal() < RideStatusLog.Status.COMPLETED.ordinal();
			if (isActive) {
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
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == LOCATION_PERMISSIONS_CODE) {
			if ((grantResults.length > 0) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				startLocationUpdates();
			}
		}
	}


	/**
	 * Start user location updates.
	 */
	@SuppressLint("MissingPermission")
	private void startLocationUpdates() {

		mapController.getUserLocationMarkerManager().setLocationSource(new FusedUserLocationSource(this));

	}


	/**
	 * Creates a marker at the given location.
	 *
	 * @param location The marker location
	 * @param imageRes The image resolution of the marker icon.
	 * @return Marker
	 */
	@NonNull
	private Marker createMarker(@NonNull LatLng location, @DrawableRes int imageRes) {

		// Styling object of the marker. Can include the image to show, set it's size, etc.
		MapImageStyle style = MapImageStyle.builder(this, imageRes).build();

		return mapController.addMarker(location, style);
	}


	/**
	 * Show the pickup marker at the given point.
	 *
	 * @param point The point where the marker should be shown.
	 */
	public void showPickupMarkerAt(@NonNull LatLng point) {
		if (pickupMarker == null) {
			// Create marker lazily.
			pickupMarker = createMarker(point, R.drawable.ic_location_on_black_24dp);
		} else {
			// Otherwise just set marker location.
			pickupMarker.setPoint(point);
		}
	}


	/**
	 * Show the destination marker at the given point.
	 *
	 * @param point The point where the marker should be shown.
	 */
	public void showDestinationMarkerAt(@NonNull LatLng point) {
		if (destinationMarker == null) {
			// Create marker lazily.
			destinationMarker = createMarker(point, R.drawable.ic_pin_drop_black_24dp);
		} else {
			// Otherwise just set marker location.
			destinationMarker.setPoint(point);
		}
	}
}
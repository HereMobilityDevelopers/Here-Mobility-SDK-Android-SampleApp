package com.here.mobility.sdk.sampleapp.ride_status;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.here.mobility.sdk.demand.DemandClient;
import com.here.mobility.sdk.demand.DriverDetails;
import com.here.mobility.sdk.demand.PriceEstimate;
import com.here.mobility.sdk.demand.Ride;
import com.here.mobility.sdk.demand.RideLocation;
import com.here.mobility.sdk.demand.RideStatusLog;
import com.here.mobility.sdk.demand.Supplier;
import com.here.mobility.sdk.sampleapp.R;
import com.here.mobility.sdk.sampleapp.util.Utils;


import java.util.Locale;

/**********************************************************
 * Copyright © 2018 HERE Global B.V. All rights reserved. *
 **********************************************************/
public class RideStatusActivity extends AppCompatActivity {


    /**
     * Ride, Intent.extra key.
     */
    private static final String EXTRA_RIDE_KEY = "EXTRA_RIDE";


    /**
     * Use DemandClient to register for ride updates.
     */
    private DemandClient demandClient;


    /**
     * The shown ride.
     */
    @NonNull
    private Ride ride;


    /**
     * Ride Record list.
     */
    @NonNull
    private RideStatusAdapter adapter = new RideStatusAdapter();


    /**
     * HandlerThread provides looper for driver images fetch.
     */
    HandlerThread handlerThread;


    /**
     * Store current shown driver image url.
     */
    @Nullable
    private String currentImageURL;


    /**
     * TextView that hold the ride ETA.
     */
    private TextView etaTextView;


    /**
     * TextView that hold the ride supplier name.
     */
    private TextView supplierName;


    /**
     * TextView that hold the ride driver name.
     */
    private TextView driverName;


    /**
     * TextView that hold the ride driver plate vehicle.
     */
    private TextView driverPlateVehicle;


    /**
     * TextView that hold the ride price.
     */
    private TextView priceTextView;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_status);

        //Get the ride that is passed to this activity as parameter.
        ride = getExtraRide();

        //Initialize DemandClient.
        demandClient = DemandClient.newInstance(this);
        initUI();
        setRideInfo(ride);

    }


    @Override
    protected void onStart() {

        super.onStart();
        registerRideStatusUpdates();
    }


    private void initUI(){
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.ride_status_title);
        }

        RecyclerView rideRecordList = findViewById(R.id.ride_records_list);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        rideRecordList.setLayoutManager(layoutManager);
        rideRecordList.setItemAnimator(new DefaultItemAnimator());
        rideRecordList.setAdapter(adapter);
        findViewById(R.id.ride_status_cancel_ride_btn).setOnClickListener( v -> {
            cancelRide(this.ride);
            finish();
        });

        supplierName = findViewById(R.id.supplierNameView);
        driverName = findViewById(R.id.driverNameView);
        driverPlateVehicle = findViewById(R.id.driverPlateVehicle);
        priceTextView = findViewById(R.id.estimatedPriceView);
    }


    /**
     * Register for rides updates.
     * Notify when ride status is updated.
     */
    private void registerRideStatusUpdates() {

        handlerThread = new HandlerThread("DriverImageFetch");
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());

        //Register for route updates. the listener will be called on handler looper.
        //Because we need to background fetch the driver image when we get the response.
        //By default the response will called on the request looper. By sending handler we can
        //get the response updates on handler looper.
        demandClient.registerToRideUpdates(ride.getRideId(),listener,handler);

    }


    private void cancelRide(@NonNull Ride ride){
        demandClient.cancelRide(ride.getRideId(), "user cancel");
    }


    /**
     * Rides updates listener.
     */
    @NonNull
    private DemandClient.RideUpdateListener listener = new DemandClient.RideUpdateListener() {
        @Override
        public void onRideStatusChanged(@NonNull Ride changedRide, @NonNull RideStatusLog rideStatusLog) {

                runOnUiThread(() -> {
                    adapter.updateDataSource(rideStatusLog.getPreviousStatuses(),rideStatusLog.getCurrentStatus());
                    setRideInfo(ride);
                });
                if (ride.getDriver() != null){
                    fetchDriverImage(ride.getDriver());
                }
        }

        @Override
        public void onRideLocationChanged(@NonNull Ride ride, @NonNull RideLocation rideLocation) {

                runOnUiThread(() -> {
                    setRideETA(rideLocation);
                    setRideInfo(ride);
                });
                if (ride.getDriver() != null){
                    fetchDriverImage(ride.getDriver());
                }
        }

        @Override
        public void onErrorOccurred(@NonNull Throwable throwable) {
            runOnUiThread(() -> Toast.makeText(RideStatusActivity.this, throwable.getLocalizedMessage(), Toast.LENGTH_SHORT).show());
        }
    };


    /**
     * Fetch driver image.
     * @param driver the driver.
     */
    @AnyThread
    private void fetchDriverImage(@NonNull DriverDetails driver) {
        String imageURL = driver.getPhotoUrl();

        //Driver image url can change during the lifecycle of the ride.
        //(the market-place can assign a new driver if the driver cancelled the ride)
        //So need to compare the new url to the previous one.
        if (imageURL != null){
            if (currentImageURL == null || !currentImageURL.equals(imageURL)){
                currentImageURL = imageURL;
                Drawable drawable = Utils.loadImageFromURL(imageURL);
                if (drawable != null){
                    runOnUiThread(() -> ((ImageView)findViewById(R.id.driver_photo_view))
                            .setImageDrawable(drawable));
                }
            }
        }else if(currentImageURL != null){
            currentImageURL = null;
            runOnUiThread(() -> ((ImageView)findViewById(R.id.driver_photo_view))
                    .setImageResource(R.drawable.ic_driver_placeholder));
        }
    }


    /**
     * Set ride eta.
     * @param rideLocation ride location.
     */
    @UiThread
    private void setRideETA(@NonNull RideLocation rideLocation){

        //Set ride eta.
        Long etaTimestamp = rideLocation.getEstimatedDropOffTime();
        etaTextView = findViewById(R.id.etaView);
        if (etaTimestamp != null) {
            String time = DateUtils.formatDateTime(this,etaTimestamp , DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_24HOUR);
            etaTextView.setText(time);
        }else{
            etaTextView.setText(R.string.not_available_initial);
        }
    }


    /**
     * Update layout UI status.
     * @param ride updated ride.
     */
    @UiThread
    private void setRideInfo(@NonNull Ride ride){

        //Set supplier
        Supplier supplier = ride.getSupplier();
        if (supplier != null) {
            supplierName.setText(supplier.getLocalName());
        }

        //update driver
        DriverDetails driver = ride.getDriver();
        if (driver != null) {
            driverName.setText(driver.getName());
            driverPlateVehicle.setText(driver.getDrivingLicenseId());
        }

        //update ride price
        //price can be fixed or range of prices.
        PriceEstimate price = ride.getBookingEstimatedPrice();

        //The best practice to show price is by calling toPlainString()
        if(price.isFixedPrice()){
            priceTextView.setText(
                    String.format(Locale.getDefault(),"%s %s"
                            ,price.getFixedPrice().getAmount().toPlainString()
                            ,price.getFixedPrice().getCurrencyCode()));
        }else if (price.isRange()){
            priceTextView.setText(
                    String.format(Locale.getDefault(),"%s - %s %s"
                            ,price.getPriceRange().getLowerBound().toPlainString()
                            ,price.getPriceRange().getUpperBound().toPlainString()
                            ,price.getPriceRange().getCurrencyCode()));
        }
    }


    /**
     * Start {@link RideStatusActivity} to display status updates of the given {@link Ride}
     * @param context the context of the sender.
     * @param ride The ride.
     * @return An intent with extra params the pass Ride to this activity.
     */
    @NonNull
    public static Intent createIntent(@NonNull Context context,@NonNull Ride ride){
        Intent intent = new Intent(context,RideStatusActivity.class);
        intent.putExtra(EXTRA_RIDE_KEY,ride);
        return intent;
    }


    /**
     * Getter get ride from Intent.extra.
     * @return ride if exist otherwise null.
     * @throws RuntimeException for cases that ride not provided.
     */
    @NonNull
    private Ride getExtraRide(){
        Intent intent = getIntent();
        if(intent.hasExtra(EXTRA_RIDE_KEY)){
            return intent.getParcelableExtra(EXTRA_RIDE_KEY);
        }else{
            throw new RuntimeException("Ride is mandatory to start RideStatusActivity.");
        }
    }


    @Override
    protected void onStop() {

        super.onStop();

        //unregister demand client.
        demandClient.unregisterFromRideUpdates(listener);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        //It's important to call shutdownNow function when the client is no longer needed.
        demandClient.shutdownNow();

        if (handlerThread != null) {
            handlerThread.quit();
        }
    }
}


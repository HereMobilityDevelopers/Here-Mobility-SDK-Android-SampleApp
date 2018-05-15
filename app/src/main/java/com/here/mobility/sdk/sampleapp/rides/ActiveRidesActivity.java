package com.here.mobility.sdk.sampleapp.rides;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.here.mobility.sdk.demand.Ride;
import com.here.mobility.sdk.sampleapp.R;
import com.here.mobility.sdk.sampleapp.ride_status.RideStatusActivity;

import java.util.ArrayList;
import java.util.List;

/**********************************************************
 * Copyright © 2018 HERE Global B.V. All rights reserved. *
 **********************************************************/


/**
 * Display user current active rides.
 */
public class ActiveRidesActivity extends AppCompatActivity implements RidesAdapter.RidesListener {


    /**
     * list of Rides Intent.extra key.
     */
    private static final String EXTRA_RIDE_LIST = "RIDE_LIST";

    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rides_activity);
        updateUI();
    }


    /**
     * Update ui
     */
    private void updateUI() {

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.active_rides);
        }

        List<Ride> rides = getRides();

        RecyclerView rideOffersList = findViewById(R.id.ride_list);
        RidesAdapter adapter = new RidesAdapter(this);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        rideOffersList.setLayoutManager(layoutManager);
        rideOffersList.setItemAnimator(new DefaultItemAnimator());
        rideOffersList.setAdapter(adapter);
        adapter.updateDataSource(rides);
    }


    /**
     * A callback method, called when ride item clicked.
     * @param ride selected {@link Ride}.
     */
    @Override
    public void rideItemSelected(@NonNull Ride ride) {
        startActivity(RideStatusActivity.createIntent(this,ride));
        finish();
    }


    /**
     * A Helper method, The main task of ActiveRidesActivity is to present list of Ride.
     * To do so list of Ride must be passed by use this function.
     * @param context the sender context.
     * @param rides list of rides.
     * @return An Intent to ActiveRidesActivity with safe pass params.
     */
    @NonNull
    public static Intent createIntent(@NonNull Context context, @NonNull List<Ride> rides){
        Intent intent = new Intent(context,ActiveRidesActivity.class);
        intent.putParcelableArrayListExtra(EXTRA_RIDE_LIST, (ArrayList<? extends Parcelable>) rides);
        return intent;
    }


    /**
     * Getter, get rides the pass as parameter by using ActiveRidesActivity.createIntent function.
     * note that list of ride is mandatory to start ActiveRidesActivity.
     * In case rides not found RuntimeException will be thrown.
     * @return List of ride.
     */
    @NonNull
    private List<Ride> getRides(){
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_RIDE_LIST)){
            return intent.getParcelableArrayListExtra(EXTRA_RIDE_LIST);
        }else{
             throw new RuntimeException("List of ride is mandatory to start ActiveRidesActivity");
        }
    }
}
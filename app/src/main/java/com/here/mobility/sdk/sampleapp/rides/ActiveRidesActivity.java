package com.here.mobility.sdk.sampleapp.rides;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.here.mobility.sdk.demand.Ride;
import com.here.mobility.sdk.sampleapp.R;
import com.here.mobility.sdk.sampleapp.ride_status.RideStatusActivity;

import java.util.ArrayList;
import java.util.List;

/**********************************************************
 * Copyright © 2018 HERE Global B.V. All rights reserved. *
 **********************************************************/


/**
 * Display the user's currently active rides.
 */
public class ActiveRidesActivity extends AppCompatActivity implements RidesAdapter.RidesListener {


    /**
     * List of Rides Intent.extra key.
     */
    private static final String EXTRA_RIDE_LIST = "RIDE_LIST";

    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rides_activity);
        updateUI();
    }


    /**
     * Update UI
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
     * A callback method, called when a ride item is clicked.
     * @param ride selected {@link Ride}.
     */
    @Override
    public void rideItemSelected(@NonNull Ride ride) {
        startActivity(RideStatusActivity.createIntent(this,ride));
        finish();
    }


    /**
     * A Helper method. The main task of ActiveRidesActivity is to present a list of Rides.
     * To do so, a list of Rides must be passed to this function.
     * @param context The sender context.
     * @param rides The list of rides.
     * @return An Intent to ActiveRidesActivity with safe pass parameters.
     */
    @NonNull
    public static Intent createIntent(@NonNull Context context, @NonNull List<Ride> rides){
        Intent intent = new Intent(context,ActiveRidesActivity.class);
        intent.putParcelableArrayListExtra(EXTRA_RIDE_LIST, (ArrayList<? extends Parcelable>) rides);
        return intent;
    }


    /**
     * Gets rides passed as a parameter by using ActiveRidesActivity.createIntent function.
     * Note that the list of rides is mandatory to start ActiveRidesActivity.
     * If rides were not found, throws RuntimeException.
     * @return List of rides.
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
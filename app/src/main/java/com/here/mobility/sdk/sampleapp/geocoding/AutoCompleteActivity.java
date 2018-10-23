package com.here.mobility.sdk.sampleapp.geocoding;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.here.mobility.sdk.common.util.Cancelable;
import com.here.mobility.sdk.common.util.PermissionUtils;
import com.here.mobility.sdk.core.geo.Address;
import com.here.mobility.sdk.core.geo.LatLng;
import com.here.mobility.sdk.core.net.ResponseException;
import com.here.mobility.sdk.core.net.ResponseFuture;
import com.here.mobility.sdk.core.net.ResponseListener;
import com.here.mobility.sdk.map.geocoding.GeocodingClient;
import com.here.mobility.sdk.map.geocoding.GeocodingRequest;
import com.here.mobility.sdk.map.geocoding.GeocodingResponse;
import com.here.mobility.sdk.map.geocoding.GeocodingResult;
import com.here.mobility.sdk.sampleapp.R;
import com.here.mobility.sdk.sampleapp.util.Constant;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CancellationException;


/**********************************************************
 * Copyright © 2018 HERE Global B.V. All rights reserved. *
 **********************************************************/
public class AutoCompleteActivity extends AppCompatActivity {


    /**
     * Location permission code.
     */
    private static final int LOCATION_PERMISSIONS_CODE = 41;


    /**
     * Geocoding Result key for Intent.extra result.
     */
    public static final String GEOCODING_RESULT = AutoCompleteActivity.class.getSimpleName();


    /**
     * Address Result key for Intent.extra result.
     */
    public static final String ADDRESS_DATA = "ADDRESS";


    /**
     * Geocoding Result Intent.extra. Use to pass {@link GeocodingResult} as a parameter to this activity.
     */
    private static final String EXTRA_GEOCODING_QUERY_KEY = "EXTRA_GEOCODING_QUERY";


    /**
     *  Use GeocodingClient for forward (address/place to latitude/longitude) and reverse (latitude/longitude to address/place) geocoding requests.
     *  Note that this client must be shut down by calling {@link GeocodingClient#shutdown()} after the client is no longer needed.
     */
    private GeocodingClient autocompleteClient;


    /**
     * Current geocoding request. Store it so we can cancel it when a new request is sent.
     */
    @Nullable
    private Cancelable autocompleteResponseFuture = null;


    /**
     * the selected Geocoding result.
     */
    private GeocodingResult selectedGeocodingResult;


    /**
     * Auto-complete adapter.
     */
    private AutocompleteAdapter adapter;


	/**
	 * Provides our user's last known location.
	 */
    private FusedLocationProviderClient fusedLocationClient;


    /**
     * User's last known location.
     */
    @Nullable
    private Location lastKnownLocation;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geocoding);
        autocompleteClient = GeocodingClient.newInstance(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        updateUI();
        updateLastKnownLocation();
    }


    /**
     * Update UI.
     */
    private void updateUI() {

        EditText searchAddressEdit = findViewById(R.id.search_address_edit_text);
        searchAddressEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }


            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                onSearchAddressTextChanged(s.toString());
            }


            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        RecyclerView autoCompleteRecyclerView = findViewById(R.id.auto_complete_recycler_view);
        adapter = new AutocompleteAdapter(adapterListener);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        autoCompleteRecyclerView.setLayoutManager(layoutManager);
        autoCompleteRecyclerView.setItemAnimator(new DefaultItemAnimator());
        autoCompleteRecyclerView.setAdapter(adapter);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.autocomplete_title);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        //inject address, provide support to start AutocompleteActivity with Geocoding query as parameter.
        //The best practice to start AutocompleteActivity with Geocoding query is calling to createIntent with String query.
        //This query can be the last address the user selected, and we re-initiate server call here to get more options.
        String injectQuery = getExtraGeocodingQuery();
        if (injectQuery != null) {
            searchAddressEdit.setText(injectQuery);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            setResult(RESULT_CANCELED);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Callback method called when an address item was clicked.
     */
    @NonNull
    private AutocompleteAdapter.AutoCompleteItemClicked adapterListener = (int position, @NonNull GeocodingResult selected) -> {
        this.selectedGeocodingResult = selected;
        fetchAddressData(selected);
    };


    /**
     * Prepare and sets the Intent to forward the selected Geocoding result and the Address data.
     */
    private void postSelectedAddress(@NonNull GeocodingResult selected, @NonNull Address address) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(GEOCODING_RESULT, selected);
        resultIntent.putExtra(ADDRESS_DATA, address);
        setResult(RESULT_OK, resultIntent);
        finish();
    }


    /**
     * Fetching the AddressData for the User's selected Geocoding result.
	 * We want the AutoComplete to be fast and reactive so we'll Fetch the AddressData only after the user's selecting the Result,
     * since the AutoComplete result contains only free text and we'll need the full Address Object.
     */
    private void fetchAddressData(@NonNull GeocodingResult selected) {
        ResponseFuture<Address> addressDataResponse = autocompleteClient.getAddressDetails(selected, Locale.getDefault().getISO3Language());
        addressDataResponse.registerListener(addressResponseListener);
    }


    /**
     * Callback method called when the search address text field has changed.
     * Send geocoding request only when the text length is greater than one character.
     * @param text the query for forward geocoding
     */
    private void onSearchAddressTextChanged(@NonNull String text) {

        //Cancel recent geocoding request.
        if (autocompleteResponseFuture != null) {
            autocompleteResponseFuture.cancel();
            autocompleteResponseFuture = null;
        }

        int GEOCODING_REQUEST_MIN_CHAR = 2;
        if (text.length() >= GEOCODING_REQUEST_MIN_CHAR) {
            updateLastKnownLocation();
            geocodingRequest(text);
        } else {

            //clean autocomplete list.
            if (adapter != null) {
                adapter.setDataSource(Collections.emptyList());
                adapter.notifyDataSetChanged();
            }
        }
    }


    /**
     * Forward geocoding by given a textual query (address or place name).
     * @param query The query for forward geocoding.
     */
    private void geocodingRequest(@NonNull String query) {

        //User current location.
        LatLng location;
        if (lastKnownLocation != null) {
            location = LatLng.fromLocation(lastKnownLocation);
        }else{
            location = Constant.CENTER_OF_LONDON;
        }

        String languageCode = Locale.getDefault().getISO3Language();

        //The result type can be - ADDRESS:     and or PLACE:
        Set<GeocodingResult.Type> resultTypes = EnumSet.of(GeocodingResult.Type.ADDRESS);

        //Create forward geocoding request.
        GeocodingRequest geocodingRequest = GeocodingRequest.newForwardRequest(
                query,
                location, //The location around which to search for results.
                //put here countryCode if you want suggestions only from a specific country.
                null, //ISO 3166 alpha 3 country to code used filter results.
                languageCode, //ISO 639-1 language code for the preferred language of the results
                resultTypes); // The result types to obtain.

        //send the request.
        ResponseFuture<GeocodingResponse> autocompleteResponse =
                autocompleteClient.geocode(geocodingRequest);

        //register listener for updates.
        autocompleteResponse.registerListener(geocodingResponseResponseListener);

        //Store it so we can cancel it when new request is sent.
        this.autocompleteResponseFuture = autocompleteResponse;
    }


    /**
     * Address response listener.
     */
    @NonNull
    private ResponseListener<Address> addressResponseListener = new ResponseListener<Address>() {
        @Override
        public void onResponse(@NonNull  Address address) {
            postSelectedAddress(selectedGeocodingResult, address);
        }

        @Override
        public void onError(@NonNull ResponseException exception) {
        	Toast.makeText(AutoCompleteActivity.this, exception.getMessage(), Toast.LENGTH_LONG).show();
        }
    };


    /**
     * Geocoding response listener.
     */
    @NonNull
    private ResponseListener<GeocodingResponse> geocodingResponseResponseListener = new ResponseListener<GeocodingResponse>() {
        @Override
        public void onResponse(@NonNull GeocodingResponse geocodingResponse) {

            List<GeocodingResult> results = geocodingResponse.getResults();
            adapter.setDataSource(results);
            adapter.notifyDataSetChanged();
        }


        @Override
        public void onError(@NonNull ResponseException exception) {
            // Since we are cancelling the the request if there are new requests, do not report
            // cancellation errors
            if (!(exception.getRootCause() instanceof CancellationException)) {
                Toast.makeText(AutoCompleteActivity.this, exception.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    };


    @Override
    protected void onDestroy() {

        super.onDestroy();

        //It's important to call shutdown function when the client is no longer needed.
        if (autocompleteClient != null) {
            autocompleteClient.shutdownNow();
        }
    }


    /**
     * A Helper method. Use to create intent with address injection to start this activity with an address.
     * @param context The context of the activity sender.
     * @param query An address or place name to inject to the auto-complete edit text.
     * @return An intent to {@link AutoCompleteActivity} with inject query as extra.
     */
    @NonNull
    public static Intent createIntent(@NonNull Context context, @Nullable String query) {

        Intent intent = new Intent(context, AutoCompleteActivity.class);
        if (query != null) {
            intent.putExtra(EXTRA_GEOCODING_QUERY_KEY, query);
        }
        return intent;
    }


    /**
     * Get inject geocoding query if it exists.
     * @return geocoding query if it exist.
     */
    @Nullable
    public String getExtraGeocodingQuery() {

        String extra = null;
        if (getIntent().hasExtra(EXTRA_GEOCODING_QUERY_KEY)) {
            extra = getIntent().getStringExtra(EXTRA_GEOCODING_QUERY_KEY);
        }
        return extra;
    }


    @SuppressLint("MissingPermission")
    private void updateLastKnownLocation() {

        if (!PermissionUtils.hasAnyLocationPermissions(this)) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSIONS_CODE);
        }else{
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            lastKnownLocation = location;
                        }
                    });
        }
    }
}

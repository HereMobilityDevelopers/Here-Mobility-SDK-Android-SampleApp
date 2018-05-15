package com.here.mobility.sdk.sampleapp.get_rides;


import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.here.mobility.sdk.demand.BookingConstraints;
import com.here.mobility.sdk.demand.PassengerDetails;
import com.here.mobility.sdk.sampleapp.R;
import com.here.mobility.sdk.sampleapp.widget.CounterView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**********************************************************
 * Copyright © 2018 HERE Global B.V. All rights reserved. *
 **********************************************************/
public class RideDetailsFragment extends Fragment {


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity.
     */
    public interface RideDetailsFragmentCallback{


        /**
         * Notify when the user finish to fill the form with all mandatory fields.
         * @param passengerDetails passenger details
         * @param constraints the booking constraint of ride.
         * @param note user ride note.
         * @param preBookTime leave after, pre-book time in timestamp, null if leave time is now.
         */
        void onRideDetailsFill(@NonNull PassengerDetails passengerDetails,
                               @NonNull BookingConstraints constraints,
                               @Nullable String note,
                               @Nullable Long preBookTime);
    }


    /**
     * User name
     */
    @NonNull
    private EditText name;


    /**
     * User phone
     */
    @NonNull
    private EditText phone;


    /**
     * User note
     */
    @NonNull
    private EditText note;


    /**
     * Passenger counter view
     */
    @NonNull
    private CounterView passenger;


    /**
     * Booking leave date.
     */
    @NonNull
    TextView leaveTime;


    /**
     * Pre book timestamp, null means to now.
     */
    @Nullable
    Long preBookTime;


    /**
     * Suitcases counter view
     */
    @NonNull
    private CounterView suitcases;


    /**
     * Fragment callback listener.
     */
    @Nullable
    private RideDetailsFragmentCallback listener;


    /**
     * Use this factory method to create a new instance.
     * @return A new instance of fragment RideDetailsFragment.
     */
    public static RideDetailsFragment newInstance() {
        return new RideDetailsFragment();
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ride_details, container, false);

        name = view.findViewById(R.id.nameEditText);
        phone = view.findViewById(R.id.phoneEditText);
        note = view.findViewById(R.id.notesEditText);
        passenger = view.findViewById(R.id.ride_details_passenger_counter);
        suitcases = view.findViewById(R.id.ride_details_suitcase_counter);
        leaveTime = view.findViewById(R.id.ride_details_leave_time);
        view.findViewById(R.id.ride_details_action_button)
                .setOnClickListener( v -> getRidesClicked() );
        view.findViewById(R.id.book_now_layout).setOnClickListener(this::bookNowItemClicked);
        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Toolbar toolbar = view.findViewById(R.id.ride_details_toolbar);
        AppCompatActivity compatActivity = (AppCompatActivity) getActivity();
        if (compatActivity != null) {
            compatActivity.setSupportActionBar(toolbar);
            ActionBar actionBat = compatActivity.getSupportActionBar();
            if (actionBat != null) {
                toolbar.setTitle(R.string.ride_options);
                actionBat.setDisplayHomeAsUpEnabled(true);
                toolbar.setNavigationOnClickListener( v -> {
                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.onBackPressed();
                    }
                });
            }
        }
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof RideDetailsFragmentCallback) {
            listener = (RideDetailsFragmentCallback) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }


    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    
    /**
     * Called when ride offers action button clicked.
     */
    public void getRidesClicked(){

        PassengerDetails passengerDetails = getPassengerDetails();
        String note = this.note.getText().toString();

        if (passengerDetails != null && listener != null) {
            listener.onRideDetailsFill(passengerDetails, getBookingConstraints(), note, preBookTime);
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity != null) {
                activity.getSupportFragmentManager().popBackStack();
            }
        }else{
            Toast.makeText(getContext(), R.string.fill_mandatory_fields, Toast.LENGTH_SHORT).show();
        }

    }


    /**
     * Booking item clicked.
     * @param clickedItem anchor view to show PopupMenu.
     */
    public void bookNowItemClicked(@NonNull View clickedItem){
        PopupMenu menu = new PopupMenu(getContext(), clickedItem);
        menu.inflate(R.menu.booking_menu);
        menu.setOnMenuItemClickListener(item -> {

            switch (item.getItemId()){
                case R.id.booking_leave_now:
                    setLeaveTimeToNow();
                    break;

                case R.id.booking_leave_later:
                    showTimePickerDialog();
                    break;
            }

            return false;
        });
        menu.show();
    }


    /**
     * Set the leave time to leave now.
     */
    private void setLeaveTimeToNow(){
        preBookTime = null;
        leaveTime.setText(R.string.leave_now);
    }


    /**
     * Set leave time to later.
     * @param calendar a valid calendar.
     */
    private void setLeaveTime(@NonNull Calendar calendar){
        preBookTime = calendar.getTimeInMillis();
        SimpleDateFormat simple = new SimpleDateFormat("HH:mm", Locale.getDefault());
        simple.setTimeZone(calendar.getTimeZone());
        String dateStr = simple.format(calendar.getTime());
        leaveTime.setText(dateStr);
    }


    /**
     * Show TimePickerDialog.
     */
    private void showTimePickerDialog(){
        Calendar calendar = Calendar.getInstance();
        //The minimum time for pre-book offers is NOW() + 30 minutes.
        calendar.add(Calendar.MINUTE,30);

        TimePickerDialog picker = new TimePickerDialog(getContext(), R.style.BookingDatePicker,(view, hourOfDay, minute) -> {
            Calendar minimumTimeForPreBook = Calendar.getInstance();
            minimumTimeForPreBook.add(Calendar.MINUTE,30);
            Calendar pickerTime = Calendar.getInstance();
            pickerTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            pickerTime.set(Calendar.MINUTE, minute);
            //check if pickerTime is valid.
            if (pickerTime.after(minimumTimeForPreBook)){
                setLeaveTime(pickerTime);
            }else{
                Toast.makeText(getContext(),R.string.invalid_prebooking_time,Toast.LENGTH_LONG).show();
            }

        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
        picker.setTitle(R.string.prebook_time_picker_title);
        picker.show();
    }


    /**
     * Getter- get passenger details
     * @return If all mandatory field are valid return {@link PassengerDetails} otherwise null will be returned.
     */
    @Nullable
    private PassengerDetails getPassengerDetails(){
        PassengerDetails passengerDetails = null;
        String name = this.name.getText().toString();
        String phone = this.phone.getText().toString();

        if (!name.isEmpty() && !phone.isEmpty()){
            passengerDetails = PassengerDetails.builder()
                    .setName(name)
                    .setPhoneNumber(phone).build();
        }
        return passengerDetails;
    }


    /**
     * Getter- get booking constraint
     * @return {@link BookingConstraints}
     */
    @NonNull
    private BookingConstraints getBookingConstraints(){
        return BookingConstraints.create(passenger.getCounterValue(), suitcases.getCounterValue());
    }
}

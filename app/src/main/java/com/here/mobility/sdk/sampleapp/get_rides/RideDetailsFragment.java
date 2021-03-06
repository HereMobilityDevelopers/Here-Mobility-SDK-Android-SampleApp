package com.here.mobility.sdk.sampleapp.get_rides;


import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.here.mobility.sdk.core.services.timezone.TimeZone;
import com.here.mobility.sdk.demand.BookingConstraints;
import com.here.mobility.sdk.demand.DemandDateTime;
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
    public interface RideDetailsFragmentCallback {


        /**
         * Called when the user finishes filling in the form with all mandatory fields.
         * @param passengerDetails The passenger details
         * @param constraints The booking constraints of ride.
         * @param note Optional user note.
         * @param preBookTime The pre-booked pickup time for a future ride. Null for an immediate ride (leave now).
         */
        void onRideDetailsFill(@NonNull PassengerDetails passengerDetails,
                               @NonNull BookingConstraints constraints,
                               @Nullable String note,
                               @Nullable DemandDateTime preBookTime,
                               boolean subscribeToMessages);
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
     * Booking leave date/time.
     */
    @NonNull
    TextView leaveTime;


    /**
     * Pre-booked time null means the ride should leave now.
     */
    @Nullable
    DemandDateTime preBookTime;


    /**
     * Suitcase counter view
     */
    @NonNull
    private CounterView suitcases;


    /**
     * Fragment callback listener.
     */
    @Nullable
    private RideDetailsFragmentCallback listener;


    /**
     * Subscribe to messages switcher.
     */
    @NonNull
    private Switch subscribeToMessagesSwitcher;


    /**
     * Use this factory method to create a new instance.
     * @return A new instance of RideDetailsFragment.
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
        subscribeToMessagesSwitcher = view.findViewById(R.id.subscribe_to_messages_switcher);
        view.findViewById(R.id.ride_details_action_button)
                .setOnClickListener(v -> getRidesClicked());
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
                toolbar.setNavigationOnClickListener(v -> {
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
     * Called when the "Ride Offers" action button is clicked.
     */
    public void getRidesClicked() {

        PassengerDetails passengerDetails = getPassengerDetails();
        String note = this.note.getText().toString();

        if (passengerDetails != null && listener != null) {
            listener.onRideDetailsFill(
                    passengerDetails,
                    getBookingConstraints(),
                    note,
                    preBookTime,
                    subscribeToMessagesSwitcher.isChecked());
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity != null) {
                activity.getSupportFragmentManager().popBackStack();
            }
        } else {
            Toast.makeText(getContext(), R.string.fill_mandatory_fields, Toast.LENGTH_SHORT).show();
        }

    }


    /**
     * Called when a Booking item is clicked.
     * @param clickedItem The anchor view to show PopupMenu.
     */
    public void bookNowItemClicked(@NonNull View clickedItem) {
        PopupMenu menu = new PopupMenu(getContext(), clickedItem);
        menu.inflate(R.menu.booking_menu);
        menu.setOnMenuItemClickListener(item -> {

            switch (item.getItemId()) {
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
     * Set the leave time to "now".
     */
    private void setLeaveTimeToNow() {
        preBookTime = null;
        leaveTime.setText(R.string.leave_now);
    }


    /**
     * Set leave time to a future time.
     * @param calendar A valid Calendar object.
     */
    private void setLeaveTime(@NonNull Calendar calendar) {
        String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(calendar.getTime());
        leaveTime.setText(dateStr);

        // Init DemandDateTime from calendar.
        preBookTime = DemandDateTime.create(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE));
    }


    /**
     * Show TimePickerDialog.
     */
    private void showTimePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        //The minimum time for pre-book offers is NOW() + 30 minutes.
        calendar.add(Calendar.MINUTE, 30);

        // Open DatePickerDialog to get the date.
        new DatePickerDialog(getContext(), R.style.BookingDatePicker,
                (datePicker, year, month, day) -> {
            Calendar pickerTime = Calendar.getInstance();
            pickerTime.set(Calendar.YEAR, year);
            pickerTime.set(Calendar.MONTH, month);
            pickerTime.set(Calendar.DAY_OF_MONTH, day);

            // Open TimePickerDialog to get the time.
            TimePickerDialog picker = new TimePickerDialog(getContext(), R.style.BookingDatePicker, (view, hourOfDay, minute) -> {
                Calendar minimumTimeForPreBook = Calendar.getInstance();
                minimumTimeForPreBook.add(Calendar.MINUTE, 30);

                pickerTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                pickerTime.set(Calendar.MINUTE, minute);
                //check if pickerTime is valid.
                if (pickerTime.after(minimumTimeForPreBook)) {
                    setLeaveTime(pickerTime);
                } else {
                    Toast.makeText(getContext(), R.string.invalid_prebooking_time, Toast.LENGTH_LONG).show();
                }

            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
            picker.setTitle(R.string.prebook_time_picker_title);
            picker.show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }


    /**
     * Gets passenger details.
     * @return If all mandatory fields are valid, returns {@link PassengerDetails}, otherwise returns null.
     */
    @Nullable
    private PassengerDetails getPassengerDetails() {
        PassengerDetails passengerDetails = null;
        String name = this.name.getText().toString();
        String phone = this.phone.getText().toString();

        if (!name.isEmpty() && !phone.isEmpty()) {
            passengerDetails = PassengerDetails.builder()
                    .setName(name)
                    .setPhoneNumber(phone).build();
        }
        return passengerDetails;
    }


    /**
     * Gets booking constraints.
     * @return {@link BookingConstraints}
     */
    @NonNull
    private BookingConstraints getBookingConstraints() {
        return BookingConstraints.create(passenger.getCounterValue(), suitcases.getCounterValue());
    }
}

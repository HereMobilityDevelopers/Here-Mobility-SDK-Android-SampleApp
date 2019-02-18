package com.here.mobility.sdk.sampleapp.ride_status;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.collect.Lists;
import com.here.mobility.sdk.demand.RideStatusLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**********************************************************
 * Copyright © 2018 HERE Global B.V. All rights reserved. *
 **********************************************************/
public class RideStatusAdapter extends RecyclerView.Adapter<RideStatusAdapter.RideStatusItem> {


    /**
     * Ride status list data source.
     */
    @NonNull
    private List<RideStatus> dataSource = Collections.emptyList();


    @NonNull
    @Override
    public RideStatusItem onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new RideStatusItem(itemView);
    }


    @Override
    public void onBindViewHolder(@NonNull RideStatusItem holder, int position) {
        RideStatus record = dataSource.get(position);
        holder.status.setText(record.name);
        holder.time.setText(record.time);
    }


    @Override
    public int getItemCount() {
        return dataSource.size();

    }


    /**
     * Update records list.
     * @param records The records to show.
     * @param currentStatus The current status of the ride.
     */
    @UiThread
    public void updateDataSource(@NonNull List<RideStatusLog.Record> records,@NonNull RideStatusLog.Status currentStatus){

        ArrayList<RideStatus> rideStatuses = Lists.newArrayList();
        for (RideStatusLog.Record record : records){
            rideStatuses.add(new RideStatus(record.getStatus().name(),record.getTimestamp()));
        }

        rideStatuses.add(new RideStatus(currentStatus.name()));

        this.dataSource = rideStatuses;
        this.notifyDataSetChanged();
    }


    class RideStatusItem extends RecyclerView.ViewHolder {


        /**
         * Record Status.
         */
        @NonNull
        TextView status;


        /**
         * Record time.
         */
        @NonNull
        TextView time;


        RideStatusItem(@NonNull View itemView) {
            super(itemView);
            status = itemView.findViewById(android.R.id.text1);
            time = itemView.findViewById(android.R.id.text2);
        }
    }


    class RideStatus{


        /**
         * Status name
         */
        String name;


        /**
         * Status time.
         */
        String time = "";


        RideStatus(String name,long timestamp){
            this.name = name;
            this.time = new Date(timestamp * 1000L).toString();
        }


        RideStatus(String name){
            this.name = name;
        }
    }
}

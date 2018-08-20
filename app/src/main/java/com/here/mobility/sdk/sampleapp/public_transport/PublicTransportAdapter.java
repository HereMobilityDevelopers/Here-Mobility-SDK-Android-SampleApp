package com.here.mobility.sdk.sampleapp.public_transport;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.here.mobility.sdk.demand.PublicTransportRouteLeg;
import com.here.mobility.sdk.sampleapp.R;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**********************************************************
 * Copyright © 2018 HERE Global B.V. All rights reserved. *
 **********************************************************/
public class PublicTransportAdapter extends RecyclerView.Adapter<PublicTransportAdapter.PublicTransportItem> {


    /**
     * Ride status list data source.
     */
    @NonNull
    private List<PublicTransportRouteLeg> dataSource = Collections.emptyList();


    @NonNull
    @Override
    public PublicTransportItem onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new PublicTransportItem(itemView);
    }


    @Override
    public void onBindViewHolder(@NonNull PublicTransportItem holder, int position) {
        PublicTransportRouteLeg leg = dataSource.get(position);

        holder.type.setText(leg.getTransportMode().name());

        StringBuilder infoBuilder = new StringBuilder();

        if (leg.getEstimatedDurationMs() != null){
            infoBuilder.append("Duration ")
                    .append(Long.toString(TimeUnit.MILLISECONDS.toMinutes(leg.getEstimatedDurationMs())))
                    .append(" min");
        }

        if (leg.getLineName() != null){
            infoBuilder.append("\nLine ")
                    .append(leg.getLineName());
        }

        if (leg.getOperatorName() != null){
            infoBuilder.append("\nOperator Name ")
                    .append(leg.getOperatorName());
        }

        holder.info.setText(infoBuilder.toString());
    }


    /**
     * Data source setter.
     * @param dataSource List of PublicTransportRouteLeg objects.
     */
    public void setDataSource(@NonNull List<PublicTransportRouteLeg> dataSource) {
        this.dataSource = dataSource;
    }


    @Override
    public int getItemCount() {
        return dataSource.size();
    }


    class PublicTransportItem extends RecyclerView.ViewHolder {


        /**
         * type of leg.
         */
        @NonNull
        TextView type;


        /**
         * leg info.
         */
        @NonNull
        TextView info;


        PublicTransportItem(@NonNull View itemView) {
            super(itemView);
            type = itemView.findViewById(android.R.id.text1);
            info = itemView.findViewById(android.R.id.text2);
        }
    }

}

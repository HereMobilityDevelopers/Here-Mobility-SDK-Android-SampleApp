package com.here.mobility.sdk.sampleapp.rides;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.recyclerview.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.here.mobility.sdk.demand.PriceEstimate;
import com.here.mobility.sdk.demand.Ride;
import com.here.mobility.sdk.sampleapp.R;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**********************************************************
 * Copyright © 2018 HERE Global B.V. All rights reserved. *
 **********************************************************/
public class RidesAdapter extends RecyclerView.Adapter<RidesAdapter.RideItem> {


    /**
     * Ride offer list item listener.
     */
    public interface RidesListener{


        /**
         * Callback method, called when a ride item is selected.
         * @param ride selected {@link Ride}.
         */
        void rideItemSelected(@NonNull Ride ride);
    }


    /**
     * Ride list data source.
     */
    @NonNull
    private List<Ride> dataSource = Collections.emptyList();


    /**
     * Ride list item listener.
     */
    @NonNull
    private RidesListener listener;


    RidesAdapter(@NonNull RidesListener listener){
        this.listener = listener;
    }


    @NonNull
    @Override
    public RideItem onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.ride_list_item, parent, false);
        return new RideItem(view);
    }


    @Override
    public void onBindViewHolder(@NonNull RideItem holder, int position) {

        Ride ride = dataSource.get(position);

        if (ride.getSupplier() != null){
            holder.supplierName.setText(ride.getSupplier().getEnglishName());
        }
        PriceEstimate price = ride.getBookingEstimatedPrice();

        //Price can be fixed or range of prices.
        //The best practice to show price is by calling toPlainString()
        if(price.isFixedPrice()){
            holder.estimatedPrice.setText(
                    String.format(Locale.getDefault(),"%s %s",
                            price.getFixedPrice().getAmount().toPlainString(),
                            price.getFixedPrice().getCurrencyCode()));
        }else if (price.isRange()){
            holder.estimatedPrice.setText(
                    String.format(Locale.getDefault(),"%s - %s %s",
                            price.getPriceRange().getLowerBound().toPlainString(),
                            price.getPriceRange().getUpperBound().toPlainString(),
                            price.getPriceRange().getCurrencyCode()));
        }

        Long etaTimestamp = ride.getPrebookPickupTime();
        if (etaTimestamp != null) {
            String time = DateUtils.formatDateTime(holder.itemView.getContext(), etaTimestamp, DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_24HOUR);
            holder.eta.setText(time);
        }
    }


    @Override
    public int getItemCount() {
        return dataSource.size();
    }


    /**
     * Update ride list items.
     * @param rides Ride list.
     */
    @UiThread
    public void updateDataSource(@NonNull List<Ride> rides) {

        this.dataSource = rides;
        this.notifyDataSetChanged();
    }


    class RideItem extends RecyclerView.ViewHolder{


        /**
         * Supplier name.
         */
        @NonNull
        TextView supplierName;


        /**
         * Estimated price of ride.
         */
        @NonNull
        TextView estimatedPrice;


        /**
         * Estimate time of arrival.
         */
        @NonNull
        TextView eta;


        RideItem(@NonNull View itemView) {
            super(itemView);
            supplierName = itemView.findViewById(R.id.ride_item_supplier_name_view);
            estimatedPrice = itemView.findViewById(R.id.ride_item_estimated_price_view);
            eta = itemView.findViewById(R.id.ride_item_eta_view);

            itemView.setOnClickListener(view -> {
                int position = getLayoutPosition();
                listener.rideItemSelected(dataSource.get(position));
            });
        }
    }
}

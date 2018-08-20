package com.here.mobility.sdk.sampleapp.ride_offers;

import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.here.mobility.sdk.demand.PriceEstimate;
import com.here.mobility.sdk.demand.PublicTransportRideOffer;
import com.here.mobility.sdk.demand.RideOffer;
import com.here.mobility.sdk.demand.TaxiRideOffer;
import com.here.mobility.sdk.sampleapp.R;

import java.util.Collections;
import java.util.List;
import java.util.Locale;


/**********************************************************
 * Copyright © 2018 HERE Global B.V. All rights reserved. *
 **********************************************************/
public class RideOffersAdapter extends RecyclerView.Adapter<RideOffersAdapter.RideOfferItem> {


    /**
     * Ride offer list item listener.
     */
    public interface RideOffersListener{


        /**
         * Callback method, called when a ride offer item is selected.
         * @param offer selected {@link RideOffer}.
         */
        void offerItemSelected(@NonNull RideOffer offer);
    }


    /**
     * RideOffer list data source.
     */
    @NonNull
    private List<RideOffer> dataSource = Collections.emptyList();


    /**
     * RideOffer list item listener.
     */
    @NonNull
    private RideOffersListener listener;


    RideOffersAdapter(@NonNull RideOffersListener listener) {
        this.listener = listener;
    }


    @NonNull
    @Override
    public RideOfferItem onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.ride_offer_list_item, parent, false);
        return new RideOffersAdapter.RideOfferItem(itemView);
    }


    @Override
    public void onBindViewHolder(@NonNull RideOfferItem holder, int position) {
        RideOffer offer = dataSource.get(position);

        offer.accept(new RideOffer.Visitor<Void>() {
            @Override
            public Void visit(@NonNull TaxiRideOffer taxiRideOffer) {
                bindTaxiOffer(taxiRideOffer, holder);
                return null;
            }

            @Override
            public Void visit(@NonNull PublicTransportRideOffer publicTransportRideOffer) {
                bindPublicTransportOffer(publicTransportRideOffer, holder);
                return null;
            }
        });
    }


    /**
     * Bind taxi offer to cell.
     * @param offer The taxi offer to bind
     */
    private void bindTaxiOffer(@NonNull TaxiRideOffer offer, @NonNull RideOfferItem holder){
        holder.supplierName.setText(offer.getSupplier().getEnglishName());
        PriceEstimate price = offer.getEstimatedPrice();

        //Price can be fixed or range of prices.
        if (price != null) {
            //The best practice to show price is by calling toPlainString()
            if(price.isFixedPrice()){
                holder.estimatedPrice.setText(
                        String.format(Locale.getDefault(),"%s %s"
                                ,price.getFixedPrice().getAmount().toPlainString()
                                ,price.getFixedPrice().getCurrencyCode()));
            }else if (price.isRange()){
                holder.estimatedPrice.setText(
                        String.format(Locale.getDefault(),"%s - %s %s"
                                ,price.getPriceRange().getLowerBound().toPlainString()
                                ,price.getPriceRange().getUpperBound().toPlainString()
                                ,price.getPriceRange().getCurrencyCode()));
            }
        }

        Long etaTimestamp = offer.getEstimatedPickupTime();
        if (etaTimestamp != null) {
            String time = DateUtils.formatDateTime(holder.itemView.getContext(), etaTimestamp, DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_24HOUR);
            holder.eta.setText(time);
        }else{
            holder.eta.setText(R.string.not_available_initial);
        }
    }


    /**
     * Bind public transport offer to cell.
     * @param offer The public transport offer to bind
     */
    public void bindPublicTransportOffer(@NonNull PublicTransportRideOffer offer, @NonNull RideOfferItem holder){

        holder.actionButton.setText(R.string.public_transport_details);
        holder.supplierName.setText(R.string.public_transport);

        PriceEstimate price = offer.getEstimatedPrice();

        //Price can be fixed or range of prices.
        if (price != null) {
            //The best practice to show price is by calling toPlainString()
            if(price.isFixedPrice()){
                holder.estimatedPrice.setText(
                        String.format(Locale.getDefault(),"%s %s"
                                ,price.getFixedPrice().getAmount().toPlainString()
                                ,price.getFixedPrice().getCurrencyCode()));
            }else if (price.isRange()){
                holder.estimatedPrice.setText(
                        String.format(Locale.getDefault(),"%s - %s %s"
                                ,price.getPriceRange().getLowerBound().toPlainString()
                                ,price.getPriceRange().getUpperBound().toPlainString()
                                ,price.getPriceRange().getCurrencyCode()));
            }
        }

        Long etaTimestamp = offer.getEstimatedPickupTime();
        if (etaTimestamp != null) {
            String time = DateUtils.formatDateTime(holder.itemView.getContext(), etaTimestamp, DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_24HOUR);
            holder.eta.setText(time);
        }else{
            holder.eta.setText(R.string.not_available_initial);
        }

    }


    @Override
    public int getItemCount() {
        return dataSource.size();
    }


    /**
     * Update ride offer list items.
     * @param dataSource RideOffer array.
     */
    @UiThread
    public void updateDataSource(@NonNull List<RideOffer> dataSource) {
        this.dataSource = dataSource;
        this.notifyDataSetChanged();
    }


    class RideOfferItem extends RecyclerView.ViewHolder{


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


        /**
         * Action Button.
         */
        @NonNull
        Button actionButton;


        RideOfferItem(@NonNull View itemView) {
            super(itemView);
            supplierName = itemView.findViewById(R.id.supplierNameView);
            estimatedPrice = itemView.findViewById(R.id.estimatedPriceView);
            eta = itemView.findViewById(R.id.etaView);
            actionButton = itemView.findViewById(R.id.book_button);
            actionButton.setOnClickListener(view -> {
                int position = getLayoutPosition();
                listener.offerItemSelected(dataSource.get(position));
            });
        }
    }
}

package com.here.mobility.sdk.sampleapp.geocoding;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.here.mobility.sdk.map.geocoding.GeocodingResult;
import com.here.mobility.sdk.sampleapp.R;

import java.util.Collections;
import java.util.List;

/**********************************************************
 * Copyright © 2018 HERE Global B.V. All rights reserved. *
 * Autocomplete Adapter.
 **********************************************************/
public class AutocompleteAdapter extends  RecyclerView.Adapter<AutocompleteAdapter.AddressResultHolderView> {

    /**
     * Auto-complete list item listener.
     */
    public interface AutoCompleteItemClicked {
        /**
         * Callback method, notified when an auto-complete item is clicked.
         * @param position The position of the clicked item.
         * @param selected The {@link GeocodingResult} selected by the user.
         */
        void onItemClicked(int position,@NonNull GeocodingResult selected);
    }


    /**
     * AutocompleteAdapter data source.
     */
    @NonNull
    private List<GeocodingResult> dataSource = Collections.emptyList();


    /**
     * AutocompleteAdapter item listener.
     */
    @NonNull
    private AutoCompleteItemClicked listener;


    AutocompleteAdapter(@NonNull AutoCompleteItemClicked adapterListener) {
        this.listener = adapterListener;
    }


    @NonNull
    @Override
    public AddressResultHolderView onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.address_list_item, parent, false);
        return new AddressResultHolderView(itemView);

    }


    @Override
    public void onBindViewHolder(@NonNull AddressResultHolderView holder, int position) {

        GeocodingResult address = dataSource.get(position);
        String title = address.getTitle();
        if (title != null) {
            holder.address.setText(title);
        }
        String details = address.getAddressText();
        if (details != null) {
            holder.details.setText(details);
        }

    }


    @Override
    public int getItemCount() {
        return dataSource.size();
    }

    /**
     * Data source setter
     * @param dataSource The list of geocoding result data sources.
     */
    public void setDataSource(@NonNull List<GeocodingResult> dataSource) {
        this.dataSource = dataSource;
    }


    class AddressResultHolderView extends RecyclerView.ViewHolder {


        /**
         * The distance between the current position to the address position
         */
        @NonNull
        TextView distance;


        /**
         * Address text display.
         */
        @NonNull
        TextView address;


        /**
         * Address more details.
         */
        @NonNull
        TextView details;


        AddressResultHolderView(@NonNull View view) {
            super(view);
            distance = view.findViewById(R.id.address_list_item_distance);
            address = view.findViewById(R.id.address_list_item_address);
            details = view.findViewById(R.id.address_list_item_details);
            view.setOnClickListener(item -> {
                    int position = getLayoutPosition();
                    listener.onItemClicked(position, dataSource.get(position));
            });
        }
    }

}

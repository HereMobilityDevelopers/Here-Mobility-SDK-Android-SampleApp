package com.here.mobility.sdk.sampleapp.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.here.mobility.sdk.sampleapp.R;


/**********************************************************
 * Copyright © 2018 HERE Global B.V. All rights reserved. *
 **********************************************************/
public class CounterView extends FrameLayout{


    /**
     * Decrement button
     */
    private ImageView decrementButton;


    /**
     * Increment button
     */
    private ImageView incrementButton;


    /**
     * Counter Value TextView.
     */
    private TextView counterValueTextView;


    /**
     * Count value.
     */
    private int counterValue;


    /**
     * Minimum value.
     */
    private int minCounterValue;


    /**
     * Maximum value
     */
    private int maxCounterValue;



    public CounterView(@NonNull Context context) {
        super(context);
        init(context,null,0);
    }

    public CounterView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context,attrs,0);
    }

    public CounterView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context,attrs,defStyleAttr);
    }


    /**
     * Init CounterView.
     */
    private void init(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {

        TypedArray attrSet = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CounterView, defStyleAttr, 0);
        counterValue = attrSet.getInt(R.styleable.CounterView_initial_value,1);
        minCounterValue = attrSet.getInt(R.styleable.CounterView_min_value,0);
        maxCounterValue = attrSet.getInt(R.styleable.CounterView_max_value,Integer.MAX_VALUE);

        View.inflate(context, R.layout.counter_view,this);
        decrementButton = findViewById(R.id.counter_view_decrement_button);
        decrementButton.setOnClickListener(v -> decrement());
        incrementButton = findViewById(R.id.counter_view_increment_button);
        incrementButton.setOnClickListener(v -> increment());
        counterValueTextView = findViewById(R.id.counter_view_value_text_view);

        updateUI();
    }


    /**
     * @return true if decrement is allow otherwise false
     */
    private boolean canDecrement(){
        return counterValue > minCounterValue;
    }


    /**
     * @return true if increment is allow otherwise false
     */
    private boolean canIncrement(){
        return counterValue < maxCounterValue;
    }


    /**
     * Action, decrement.
     */
    private void decrement() {
        if (canDecrement()) {
            counterValue--;
            updateUI();
        }
    }


    /**
     * Action, increment.
     */
    private void increment() {
        if (canIncrement()) {
            counterValue++;
            updateUI();
        }
    }


    /**
     * Update CounterView UI.
     */
    private void updateUI() {
        counterValueTextView.setText(String.format("%d",counterValue));

        if (!canIncrement()) {
            disableIncrement();
        } else {
            enableIncrement();
        }

        if (!canDecrement()) {
            disableDecrement();
        } else {
            enableDecrement();
        }
    }


    /**
     * Disable Increment.
     */
    private void disableIncrement() {
        incrementButton.setImageResource(R.drawable.plus_disable);
        incrementButton.setEnabled(false);
    }


    /**
     * Disable Decrement.
     */
    private void disableDecrement() {
        decrementButton.setImageResource(R.drawable.minus_disable);
        decrementButton.setEnabled(false);
    }


    /**
     * Enable Increment.
     */
    private void enableIncrement() {
        incrementButton.setImageResource(R.drawable.plus);
        incrementButton.setEnabled(true);
    }


    /**
     * Enable Decrement.
     */
    private void enableDecrement() {
        decrementButton.setImageResource(R.drawable.minus);
        decrementButton.setEnabled(true);
    }


    /**
     * Getter counter value.
     * @return the value of the counter.
     */
    public int getCounterValue() {
        return counterValue;
    }


    /**
     * set the minimum value for counter.
     * @param minValue the minimum value.
     */
    public void setMinCounterValue(int minValue) {
        this.minCounterValue = minValue;
        updateUI();
    }


    /**
     * set maximum value for counter.
     * @param maxValue the maximum value.
     */
    private void setMaxCounterValue(int maxValue) {
        this.maxCounterValue = maxValue;
        updateUI();
    }
}

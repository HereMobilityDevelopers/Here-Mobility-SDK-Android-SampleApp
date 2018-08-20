package com.here.mobility.sdk.sampleapp.registration;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.widget.EditText;

import com.here.mobility.sdk.sampleapp.R;


/**********************************************************
 * Copyright © 2018 HERE Global B.V. All rights reserved. *
 **********************************************************/
public class RegistrationDialog extends AlertDialog.Builder {


    /**
     * User name
     */
    @NonNull
    private EditText userName;


    public RegistrationDialog(@NonNull Context context) {
        super(context,R.style.RegistrationDialog);
        setTitle(R.string.register);
        setMessage(R.string.enter_user_name);
        userName = new EditText(context);
        setView(userName);
        setNegativeButton(android.R.string.cancel,
                (dialog, which) -> dialog.cancel());
    }


    /**
     * Get user name.
     * @return input user name.
     */
    public String getUserName(){
        return userName.getText().toString();
    }
}

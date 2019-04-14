package com.here.mobility.sdk.sampleapp.registration;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.here.mobility.sdk.core.MobilitySdk;
import com.here.mobility.sdk.core.net.ResponseException;
import com.here.mobility.sdk.core.net.ResponseFuture;
import com.here.mobility.sdk.core.net.ResponseListener;
import com.here.mobility.sdk.sampleapp.R;


/**********************************************************
 * Copyright © 2018 HERE Global B.V. All rights reserved. *
 **********************************************************/
public class LoginActivity extends AppCompatActivity {


	/**
	 * The phone edit text.
	 */
	private EditText phoneEditText;


	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		phoneEditText = findViewById(R.id.phoneNumberEditText);
	}


	/**
	 * Send SMS button click listener
	 */
	public void sendSMSButtonClicked(@NonNull View v) {
		String userPhoneNumber = phoneEditText.getText().toString();
		if(!userPhoneNumber.isEmpty()) {
			// User phone number verification steps:
			// 1. send phone number verification request, this request will send SMS with pin code to the user
			// 2. receive pin code from user input and send the phone number and pin code for final verification.
			sendPhoneVerification(userPhoneNumber);
		} else {
			Toast.makeText(this, R.string.login_not_valid_phone_number, Toast.LENGTH_LONG).show();
		}
	}


	/**
	 * Send a request for SMS verification.
	 * @param userPhoneNumber the user phone number.
	 */
	private void sendPhoneVerification(@NonNull String userPhoneNumber) {
		ResponseFuture<Void> futureVerification = MobilitySdk.getInstance().sendVerificationSms(userPhoneNumber);
		futureVerification.registerListener(phoneVerificationResponse);
	}


	/**
	 * SMS verification class response.
	 */
	private ResponseListener<Void> phoneVerificationResponse = new LoginResponseListener(R.string.login_sms_sent_successfully, false);


	/**
	 * Verify phone number button click listener
	 */
	public void verifyPhoneNumberButtonClicked(@NonNull View v) {
		String pinCode = ((EditText)findViewById(R.id.picCodeEditText)).getText().toString();
		if (!pinCode.isEmpty()) {

			// Send the phone number and the pin code to verify the pin is correct.
			ResponseFuture<Void> verifyPhoneFuture =
					MobilitySdk.getInstance().verifyUserPhoneNumber(phoneEditText.getText().toString(), pinCode);
			verifyPhoneFuture.registerListener(verifyPhoneFutureResponse);
		} else {
			Toast.makeText(this, R.string.login_verification_not_valid_pic_code, Toast.LENGTH_LONG).show();
		}
	}


	/**
	 * verify phone response listener.
	 */
	private ResponseListener<Void> verifyPhoneFutureResponse = new LoginResponseListener(R.string.login_pin_verified_successfully, true);


	/**
	 * A generic login listener.
	 */
	private class LoginResponseListener implements ResponseListener<Void> {


		private final int successMessage;


		private final boolean shouldFinishOnSuccess;


		LoginResponseListener(int successfulMessage, boolean shouldFinishOnSuccess) {
			this.successMessage = successfulMessage;
			this.shouldFinishOnSuccess = shouldFinishOnSuccess;
		}


		@Override
		public void onResponse(Void aVoid) {
			Toast.makeText(LoginActivity.this, successMessage, Toast.LENGTH_LONG).show();
			if (shouldFinishOnSuccess) {
				setResult(RESULT_OK);
				finish();
			}
		}


		@Override
		public void onError(@NonNull ResponseException e) {
			Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * A Helper method. Use to create intent with visibility parameters.
	 * @param context The context of the activity sender.
	 * @return An intent to {@link LoginActivity} with inject showLoginFunctionality and showPhoneVerificationFunctionality as extra.
	 */
	@NonNull
	public static Intent createIntent(@NonNull Context context) {
		return new Intent(context, LoginActivity.class);
	}
}

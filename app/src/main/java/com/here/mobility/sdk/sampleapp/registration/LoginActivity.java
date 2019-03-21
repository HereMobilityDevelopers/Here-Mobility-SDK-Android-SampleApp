package com.here.mobility.sdk.sampleapp.registration;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.here.mobility.sdk.core.MobilitySdk;
import com.here.mobility.sdk.core.net.ResponseException;
import com.here.mobility.sdk.core.net.ResponseFuture;
import com.here.mobility.sdk.core.net.ResponseListener;
import com.here.mobility.sdk.sampleapp.R;
import com.here.mobility.sdk.sampleapp.util.AuthUtils;


/**********************************************************
 * Copyright © 2018 HERE Global B.V. All rights reserved. *
 **********************************************************/
public class LoginActivity extends AppCompatActivity {


	/**
	 * The phone edit text.
	 */
	private EditText phoneEditText;


	/**
	 * User info text view
	 */
	private TextView userInfoTextView;


	/**
	 * Logout button
	 */
	private Button logoutButton;


	/**
	 * Intent.extra key. Extracts bool value for userId login visibility.
	 */
	private static final String LOGIN_VIEW_VISIBILITY = "showUserIdLogin";


	/**
	 * Intent.extra key. Extracts bool value for phone verification visibility.
	 */
	private static final String PHONE_VERIFICATION_VISIBILITY = "showPhoneVerification";


	/**
	 * True if should show the login functionality
	 */
	private boolean showLogin;


	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		phoneEditText = findViewById(R.id.phoneNumberEditText);
		userInfoTextView = findViewById(R.id.userInfoTextView);
		logoutButton = findViewById(R.id.logoutButton);
		updateViews();
	}


	private void updateViews() {
		Bundle b = getIntent().getExtras();
		showLogin = (b != null && b.getBoolean(LOGIN_VIEW_VISIBILITY));
		boolean showPhoneVerification = (b != null && b.getBoolean(PHONE_VERIFICATION_VISIBILITY));

		findViewById(R.id.userIdLoginView).setVisibility(showLogin ? View.VISIBLE : View.GONE);
		findViewById(R.id.accountDetailView).setVisibility(showLogin ? View.VISIBLE : View.GONE);

		findViewById(R.id.phoneEnterLoginView).setVisibility(showPhoneVerification ? View.VISIBLE : View.GONE);
		findViewById(R.id.phoneVerificationLoginView).setVisibility(showPhoneVerification ? View.VISIBLE : View.GONE);

		updateUserInfoUI();
	}


	/**
	 * Update the user info that is shown in the UI.
	 */
	private void updateUserInfoUI() {
		boolean hasUser = MobilitySdk.getInstance().getUserAuthInfo() != null;
		String userInfo =
				hasUser ?
						MobilitySdk.getInstance().getUserAuthInfo().getUserId() : getString(R.string.login_not_logged_id);
		String userVerification = getString(MobilitySdk.getInstance().isVerified() ? R.string.login_verified : R.string.login_not_verified);
		userInfoTextView.setText(String.format(getString(R.string.login_user_info_format), userInfo , userVerification));
		logoutButton.setVisibility(hasUser ? View.VISIBLE : View.GONE);
	}


	/**
	 * Logout the user
	 */
	public void onLogoutButtonClicked(@NonNull View v) {
		MobilitySdk.getInstance().setUserAuthInfo(null);
		updateUserInfoUI();
	}


	/**
	 * Register button click listener
	 */
	public void onRegisterButtonClicked(@NonNull View v) {
		String userName = ((EditText)findViewById(R.id.userNameEditText)).getText().toString();
		if (!userName.isEmpty()) {

			// The user registration should be done with your app's backend (see the documentation for more info).
			// This is a snippet to generate the token in the app, for testing purposes.
			AuthUtils.registerUser(userName,
					getString(R.string.here_sdk_app_id),
					getString(R.string.here_sdk_app_secret));
		} else {
			Toast.makeText(this, R.string.login_not_valid_user_name, Toast.LENGTH_LONG).show();
		}
		updateUserInfoUI();
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
					MobilitySdk.getInstance().verifyPhoneNumber(phoneEditText.getText().toString(), pinCode);
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
			} else {
				updateUserInfoUI();
			}
		}


		@Override
		public void onError(@NonNull ResponseException e) {
			Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
			updateUserInfoUI();
		}
	}

	/**
	 * A Helper method. Use to create intent with visibility parameters.
	 * @param context The context of the activity sender.
	 * @param showLoginFunctionality int value that indicate visibility of login userID functionality
	 * @param showPhoneVerificationFunctionality int value that indicate visibility of phone verifcation functionality
	 * @return An intent to {@link LoginActivity} with inject showLoginFunctionality and showPhoneVerificationFunctionality as extra.
	 */
	@NonNull
	public static Intent createIntent(@NonNull Context context, boolean showLoginFunctionality, boolean showPhoneVerificationFunctionality) {

		Intent intent = new Intent(context, LoginActivity.class);

		intent.putExtra(LOGIN_VIEW_VISIBILITY, showLoginFunctionality);
		intent.putExtra(PHONE_VERIFICATION_VISIBILITY, showPhoneVerificationFunctionality);

		return intent;
	}


	/**
	 * When pressing back, after finished setting the AuthInfo successfully - return an OK result
	 */
	@Override
	public void onBackPressed() {
		if (showLogin && MobilitySdk.getInstance().getUserAuthInfo() != null) {
			setResult(RESULT_OK);
		}
		super.onBackPressed();
	}
}

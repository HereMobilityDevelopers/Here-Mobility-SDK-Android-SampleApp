package com.here.mobility.sdk.sampleapp;

import androidx.multidex.MultiDexApplication;

import com.here.mobility.sdk.core.HereSdkInitializationException;
import com.here.mobility.sdk.core.MobilitySdk;


/**********************************************************
 * Copyright © 2018 HERE Global B.V. All rights reserved. *
 **********************************************************/
public class SampleApplication extends MultiDexApplication {


	@Override
	public void onCreate() {
		super.onCreate();

		//MobilitySdk initialization must be called in your app's onCreate() method to initialize the SDK.
		MobilitySdk.init(this);

		//Returns whether the current process is the SDK agent process.
		if (MobilitySdk.getInstance().isHereAgentProcess()) {
			return;
		}

		//Checks that you've replaced the default app Id and secret with your own.
		//The App ID is initialized in the here_mobility_key.xml file.
		//To get  your own creedantials please visit https://developers.mobility.here.com/ and sign up for a developer account.
		String DEFAULT_APP_ID = "{YOUR_SDK_APP_ID}";
		if (getString(R.string.here_sdk_app_id).equals(DEFAULT_APP_ID)) {
			throw new HereSdkInitializationException("Missing App ID, Please replace the default App ID in the here_mobility_key.xml with your own");
		}

		// Put the rest of your app's initialization here
	}
}
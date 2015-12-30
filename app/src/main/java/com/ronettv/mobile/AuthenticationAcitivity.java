package com.ronettv.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.ronettv.data.ActivePlanDatum;
import com.ronettv.data.DeviceDatum;
import com.ronettv.retrofit.OBSClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class AuthenticationAcitivity extends Activity {
    public static String TAG = com.ronettv.mobile.AuthenticationAcitivity.class.getName();
    MyApplication mApplication = null;
    OBSClient mOBSClient;
    boolean mIsReqCanceled = false;
    boolean mIsFailed = false;
    private ProgressBar mProgressBar;
    private Button mBtnRefresh;
    final Callback<List<ActivePlanDatum>> activePlansCallBack = new Callback<List<ActivePlanDatum>>() {

        @Override
        public void success(List<ActivePlanDatum> list, Response arg1) {
            if (!mIsReqCanceled) {
                /** on success if client has active plans redirect to home page */
                if (list != null && list.size() > 0) {
                    Intent intent = new Intent(com.ronettv.mobile.AuthenticationAcitivity.this,
                            MainActivity.class);
                    com.ronettv.mobile.AuthenticationAcitivity.this.finish();
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(com.ronettv.mobile.AuthenticationAcitivity.this,
                            PlanActivity.class);
                    com.ronettv.mobile.AuthenticationAcitivity.this.finish();
                    startActivity(intent);
                }
            } else
                mIsReqCanceled = false;
        }

        @Override
        public void failure(RetrofitError retrofitError) {
            if (!mIsReqCanceled) {
                mIsFailed = true;
                mBtnRefresh.setVisibility(View.VISIBLE);
                Toast.makeText(
                        com.ronettv.mobile.AuthenticationAcitivity.this,
                        "Server Error : "
                                + retrofitError.getResponse().getStatus(),
                        Toast.LENGTH_LONG).show();
            } else
                mIsReqCanceled = false;
        }
    };
    final Callback<DeviceDatum> deviceCallBack = new Callback<DeviceDatum>() {

        @Override
        public void success(DeviceDatum device, Response arg1) {
            if (!mIsReqCanceled) {
                if (device != null) {
                    try {
                        /** on success save client id and check for active plans */
                        mApplication.resetPrefs();
                        mApplication.setClientId(Long.toString(device
                                .getClientId()));
                        mApplication.setBalance(device.getBalanceAmount());
                        mApplication.setBalanceCheck(device.isBalanceCheck());
                        mApplication.setCurrency(device.getCurrency());
                        boolean isPayPalReq = false;
                        if (device.getPaypalConfigData() != null)
                            isPayPalReq = device.getPaypalConfigData()
                                    .getEnabled();
                        mApplication.setPayPalCheck(isPayPalReq);
                        if (isPayPalReq) {
                            String value = device.getPaypalConfigData()
                                    .getValue();
                            if (value != null && value.length() > 0) {
                                JSONObject json = new JSONObject(value);
                                try {
                                    if (json != null) {
                                        mApplication.setPayPalClientID(json
                                                .get("clientId").toString());
                                    }
                                } catch (NullPointerException npe) {
                                    Log.e("AuthenticationAcitivity",
                                            (npe.getMessage() == null) ? "NPE Exception"
                                                    : npe.getMessage());
                                    Toast.makeText(
                                            com.ronettv.mobile.AuthenticationAcitivity.this,
                                            "Invalid Data for PayPal details",
                                            Toast.LENGTH_LONG).show();
                                }
                            } else
                                Toast.makeText(com.ronettv.mobile.AuthenticationAcitivity.this,
                                        "Invalid Data for PayPal details",
                                        Toast.LENGTH_LONG).show();
                        }
                        mOBSClient.getActivePlans(mApplication.getClientId(),
                                activePlansCallBack);
                    } catch (NullPointerException npe) {
                        Log.e("AuthenticationAcitivity",
                                (npe.getMessage() == null) ? "NPE Exception"
                                        : npe.getMessage());
                        Toast.makeText(com.ronettv.mobile.AuthenticationAcitivity.this,
                                "Invalid Data-NPE Exception", Toast.LENGTH_LONG)
                                .show();
                    } catch (JSONException e) {
                        Log.e("AuthenticationAcitivity",
                                (e.getMessage() == null) ? "Json Exception" : e
                                        .getMessage());
                        Toast.makeText(com.ronettv.mobile.AuthenticationAcitivity.this,
                                "Invalid Data-Json Exception",
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(com.ronettv.mobile.AuthenticationAcitivity.this,
                            "Server Error  :Device details not exists",
                            Toast.LENGTH_LONG).show();
                }
            } else
                mIsReqCanceled = false;
        }

        @Override
        public void failure(RetrofitError retrofitError) {
            if (!mIsReqCanceled) {
                mIsFailed = true;
                if (mProgressBar.isShown()) {
                    mProgressBar.setVisibility(View.INVISIBLE);
                }
                mBtnRefresh.setVisibility(View.VISIBLE);
                if (retrofitError.isNetworkError()) {
                    Toast.makeText(
                            com.ronettv.mobile.AuthenticationAcitivity.this,
                            getApplicationContext().getString(
                                    R.string.error_network), Toast.LENGTH_LONG)
                            .show();
                } else if (retrofitError.getResponse().getStatus() == 403) {
                    Intent intent = new Intent(com.ronettv.mobile.AuthenticationAcitivity.this,
                            RegisterActivity.class);
                    com.ronettv.mobile.AuthenticationAcitivity.this.finish();
                    startActivity(intent);
                } else if (retrofitError.getResponse().getStatus() == 401) {
                    Toast.makeText(com.ronettv.mobile.AuthenticationAcitivity.this,
                            "Authorization Failed", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(
                            com.ronettv.mobile.AuthenticationAcitivity.this,
                            "Server Error : "
                                    + retrofitError.getResponse().getStatus(),
                            Toast.LENGTH_LONG).show();
                }
            } else
                mIsReqCanceled = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);
        setTitle("");
        mApplication = ((MyApplication) getApplicationContext());
        mOBSClient = mApplication.getOBSClient();
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mBtnRefresh = (Button) findViewById(R.id.btn_refresh);
        validateDevice();
    }

    private void validateDevice() {
        if (!mProgressBar.isShown()) {
            mProgressBar.setVisibility(View.VISIBLE);
        }
        String androidId = Settings.Secure.getString(getApplicationContext()
                .getContentResolver(), Settings.Secure.ANDROID_ID);
        mOBSClient.getMediaDevice(androidId, deviceCallBack);
    }

    public void Refresh_OnClick(View v) {
        mBtnRefresh.setVisibility(View.INVISIBLE);
        validateDevice();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mIsFailed) {
                com.ronettv.mobile.AuthenticationAcitivity.this.finish();
            } else {
                AlertDialog mConfirmDialog = mApplication
                        .getConfirmDialog(this);
                mConfirmDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                if (mProgressBar.isShown()) {
                                    mProgressBar.setVisibility(View.INVISIBLE);
                                }
                                mIsReqCanceled = true;
                                com.ronettv.mobile.AuthenticationAcitivity.this.finish();
                            }
                        });
                mConfirmDialog.show();
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}

package com.ronettv.mobile;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.gson.Gson;
import com.ronettv.data.ActivePlanDatum;
import com.ronettv.data.ClientDatum;
import com.ronettv.data.ConfigurationProperty;
import com.ronettv.data.RegClientRespDatum;
import com.ronettv.data.ResForgetPwd;
import com.ronettv.data.ResponseObj;
import com.ronettv.data.SenderMailId;
import com.ronettv.data.TemplateDatum;
import com.ronettv.retrofit.CustomUrlConnectionClient;
import com.ronettv.retrofit.OBSClient;
import com.ronettv.utils.Utilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

public class RegisterActivity extends Activity implements
        ForgetPwdDialogFragment.PwdSubmitClickListener {

    private final static String NETWORK_ERROR = "Network error.";
    private final static int LOGIN_LAYOUT = 0;
    private final static int REGISTRATION_LAYOUT = 1;
    private final static int LINK_USER_LAYOUT = 2;
    // public static String TAG = RegisterActivity.class.getName();
    public static String TAG = com.ronettv.mobile.RegisterActivity.class.getName();
    static String CLIENT_DATA;
    // login
    EditText et_login_EmailId;
    EditText et_Password;
    // register
    EditText et_MobileNumber;
    EditText et_FullName;
    Spinner sp_City;
    EditText et_EmailId;
    String mCountry;
    String mState;
    String mCity;
    int mCurrentLayout;
    com.ronettv.mobile.MyApplication mApplication = null;
    OBSClient mOBSClient;
    boolean mIsReqCanceled = false;
    final Callback<List<ActivePlanDatum>> activePlansCallBack = new Callback<List<ActivePlanDatum>>() {

        @Override
        public void success(List<ActivePlanDatum> list, Response arg1) {
            if (!mIsReqCanceled) {
                /** on success if client has active plans redirect to home page */
                if (list != null && list.size() > 0) {
                    Intent intent = new Intent(com.ronettv.mobile.RegisterActivity.this,
                            MainActivity.class);
                    com.ronettv.mobile.RegisterActivity.this.finish();
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(com.ronettv.mobile.RegisterActivity.this,
                            PlanActivity.class);
                    com.ronettv.mobile.RegisterActivity.this.finish();
                    startActivity(intent);
                }
            } else
                mIsReqCanceled = false;
        }

        @Override
        public void failure(RetrofitError retrofitError) {
            if (!mIsReqCanceled) {
                Toast.makeText(
                        com.ronettv.mobile.RegisterActivity.this,
                        "Server Error : "
                                + retrofitError.getResponse().getStatus(),
                        Toast.LENGTH_LONG).show();
            } else
                mIsReqCanceled = false;
        }
    };
    private ProgressDialog mProgressDialog;
    final Callback<TemplateDatum> templateCallBack = new Callback<TemplateDatum>() {
        @Override
        public void failure(RetrofitError retrofitError) {

            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
                mProgressDialog = null;
            }
            if (retrofitError.isNetworkError()) {
                Toast.makeText(
                        com.ronettv.mobile.RegisterActivity.this,
                        getApplicationContext().getString(
                                R.string.error_network), Toast.LENGTH_LONG)
                        .show();
            } else {
                Toast.makeText(
                        com.ronettv.mobile.RegisterActivity.this,
                        "Server Error : "
                                + retrofitError.getResponse().getStatus(),
                        Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void success(TemplateDatum template, Response response) {

            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
                mProgressDialog = null;
            }
            try {
                mCountry = template.getAddressTemplateData().getCountryData()
                        .get(0);
                mState = template.getAddressTemplateData().getStateData()
                        .get(0);
                ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(
                        com.ronettv.mobile.RegisterActivity.this,
                        R.layout.a_reg_city_spinner_item, template
                        .getAddressTemplateData().getCityData());
                dataAdapter
                        .setDropDownViewResource(R.layout.a_reg_city_spinner_dropdown_item);
                sp_City.setAdapter(dataAdapter);
                // mCity =
                // template.getAddressTemplateData().getCityData().get(0);
            } catch (Exception e) {
                Log.e("templateCB-success", e.getMessage());
                Toast.makeText(com.ronettv.mobile.RegisterActivity.this,
                        "Server Error : Country/City/State not Specified",
                        Toast.LENGTH_LONG).show();
            }
        }
    };
    final Callback<ClientDatum> getClientDetailsCallBack = new Callback<ClientDatum>() {
        @Override
        public void failure(RetrofitError retrofitError) {
            if (!mIsReqCanceled) {
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }
                if (retrofitError.isNetworkError()) {
                    Toast.makeText(com.ronettv.mobile.RegisterActivity.this,
                            mApplication.getString(R.string.error_network),
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(
                            com.ronettv.mobile.RegisterActivity.this,
                            "Server Error : "
                                    + retrofitError.getResponse().getStatus(),
                            Toast.LENGTH_LONG).show();
                }
            } else
                mIsReqCanceled = false;
        }

        @Override
        public void success(ClientDatum client, Response response) {
            if (!mIsReqCanceled) {
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }
                if (client == null) {
                    Toast.makeText(com.ronettv.mobile.RegisterActivity.this, "Server Error.",
                            Toast.LENGTH_LONG).show();
                } else {
                    mApplication.resetPrefs();
                    mApplication.setClientId(client.getAccountNo());
                    mApplication.getEditor().putString(mApplication.getResources().getString(
                            R.string.client_data), new Gson().toJson(client)).commit();
                    mApplication.setBalance(client.getBalanceAmount());
                    mApplication.setCurrency(client.getCurrency());
                    mApplication.setBalanceCheck(client.isBalanceCheck());
                    boolean isPayPalReq = false;
                    if (client.getConfigurationProperty() != null)
                        isPayPalReq = client.getConfigurationProperty()
                                .getEnabled();
                    mApplication.setPayPalCheck(isPayPalReq);
                    if (isPayPalReq) {
                        String value = client.getConfigurationProperty()
                                .getValue();
                        if (value != null && value.length() > 0) {
                            try {
                                JSONObject json = new JSONObject(value);
                                if (json != null) {
                                    mApplication.setPayPalClientID(json.get(
                                            "clientId").toString());
                                    // mApplication.setPayPalSecret(json.get(
                                    // "secretCode").toString());
                                }
                            } catch (JSONException e) {
                                Log.e("RegisterActivity",
                                        (e.getMessage() == null) ? "Json Exception"
                                                : e.getMessage());
                                Toast.makeText(com.ronettv.mobile.RegisterActivity.this,
                                        "Invalid Data-Json Exception",
                                        Toast.LENGTH_LONG).show();
                            }
                        } else
                            Toast.makeText(com.ronettv.mobile.RegisterActivity.this,
                                    "Invalid Data for PayPal details",
                                    Toast.LENGTH_LONG).show();
                    }
                    Intent intent = new Intent(com.ronettv.mobile.RegisterActivity.this,
                            PlanActivity.class);
                    com.ronettv.mobile.RegisterActivity.this.finish();
                    startActivity(intent);
                }
            } else
                mIsReqCanceled = false;

        }
    };

    public static ClientDatum parseJsonToClient(String json) {
        ClientDatum client = null;
        try {
            client = new ClientDatum();

            JSONObject jsonObj = new JSONObject(json);
            client.setAccountNo(jsonObj.getString("accountNo"));
            try {
                JSONArray arrDate = jsonObj.getJSONArray("activationDate");
                Date date = com.ronettv.mobile.MyApplication.df.parse(arrDate.getString(0) + "-"
                        + arrDate.getString(1) + "-" + arrDate.getString(2));
                client.setActivationDate(MyApplication.df.format(date));
            } catch (JSONException e) {
                client.setActivationDate(jsonObj.getString("activationDate"));
            }

            client.setFirstname(jsonObj.getString("firstname"));
            client.setLastname(jsonObj.getString("lastname"));
            client.setFullname(jsonObj.getString("firstname") + " "
                    + jsonObj.getString("lastname"));
            client.setEmail(jsonObj.getString("email"));
            client.setPhone(jsonObj.getString("phone"));
            client.setCountry(jsonObj.getString("country"));
            client.setBalanceAmount((float) jsonObj.getDouble("balanceAmount"));
            client.setCurrency(jsonObj.getString("currency"));
            client.setBalanceCheck(jsonObj.getBoolean("balanceCheck"));
            // client.setHwSerialNumber(MyApplication.androidId);

            // paypal config data
            JSONObject configJson = jsonObj
                    .getJSONObject("configurationProperty");
            if (configJson != null) {
                ConfigurationProperty configProperty = new ConfigurationProperty();
                configProperty.setEnabled(configJson.getBoolean("enabled"));
                configProperty.setValue(configJson.getString("value"));
                client.setConfigurationProperty(configProperty);
            }

        } catch (JSONException e) {
            Log.i(TAG, e.getMessage());
        } catch (ParseException e) {
            Log.i(TAG, e.getMessage());
        }

        return client;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        mApplication = ((com.ronettv.mobile.MyApplication) getApplicationContext());
        mOBSClient = mApplication.getOBSClient();
        mCurrentLayout = LOGIN_LAYOUT;
    }

    public void textForgetPwd_onClick(View v) {
        showDialog();
    }

    public void textRegister_onClick(View v) {
        mCurrentLayout = REGISTRATION_LAYOUT;
        setLayout(mCurrentLayout);
        et_FullName = (EditText) findViewById(R.id.a_reg_et_full_name);
        sp_City = (Spinner) findViewById(R.id.a_reg_sp_city);
        et_MobileNumber = (EditText) findViewById(R.id.a_reg_et_mobile_no);
        et_EmailId = (EditText) findViewById(R.id.a_reg_et_email_id);
        et_Password = (EditText) findViewById(R.id.a_reg_et_pwd);
        getCountries();
    }

    public void textLinkUser_onClick(View v) {
        mCurrentLayout = LINK_USER_LAYOUT;
        setLayout(mCurrentLayout);
    }

    private void setLayout(int Layout) {
        LinearLayout container = (LinearLayout) findViewById(R.id.a_reg_ll_container);
        LayoutTransition transition = new LayoutTransition();
        container.setLayoutTransition(transition);
        LayoutInflater inflater = this.getLayoutInflater();
        LinearLayout layout;
        if (mCurrentLayout == LINK_USER_LAYOUT) {
            layout = (LinearLayout) inflater.inflate(
                    R.layout.a_reg_link_user_layout, null);
        } else if (mCurrentLayout == REGISTRATION_LAYOUT) {
            layout = (LinearLayout) inflater.inflate(
                    R.layout.a_reg_registration_layout, null);
        } else {
            layout = (LinearLayout) inflater.inflate(
                    R.layout.a_reg_login_layout, null);
        }
        layout.setLayoutParams(new LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        container.removeAllViews();
        container.addView(layout);
    }

    public void textLogin_onClick(View v) {
        mCurrentLayout = LOGIN_LAYOUT;
        setLayout(mCurrentLayout);
    }

    public void btnLogin_onClick(View v) {

        // et_LastName = (EditText) findViewById(R.id.a_reg_et_last_name);
        String sEmailId = ((EditText) findViewById(R.id.a_reg_et_login_email_id))
                .getText().toString().trim();
        String sPassword = ((EditText) findViewById(R.id.a_reg_et_password))
                .getText().toString();

        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        if (sPassword.length() <= 0) {
            Toast.makeText(com.ronettv.mobile.RegisterActivity.this, "Please enter Password",
                    Toast.LENGTH_LONG).show();
        } else if (sEmailId.matches(emailPattern)) {
            SelfCareUserDatum data = new SelfCareUserDatum();
            data.email_id = sEmailId;
            data.password = sPassword;
            DoSelfCareLoginAsyncTask task2 = new DoSelfCareLoginAsyncTask();
            task2.execute(data);
        } else {
            Toast.makeText(com.ronettv.mobile.RegisterActivity.this,
                    "Please enter valid Email Id", Toast.LENGTH_LONG).show();
        }
    }

    private void getCountries() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
        mProgressDialog = new ProgressDialog(com.ronettv.mobile.RegisterActivity.this,
                ProgressDialog.THEME_HOLO_DARK);
        mProgressDialog.setMessage("Connecting Server...");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setOnCancelListener(new OnCancelListener() {
            public void onCancel(DialogInterface arg0) {
                if (mProgressDialog != null && mProgressDialog.isShowing())
                    mProgressDialog.dismiss();
                mProgressDialog = null;
            }
        });
        mProgressDialog.show();
        mOBSClient.getTemplate(templateCallBack);

    }

    public void btnRegister_onClick(View v) {

        String email = et_EmailId.getText().toString().trim();
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        if (et_MobileNumber.getText().toString().length() <= 0) {
            Toast.makeText(com.ronettv.mobile.RegisterActivity.this, "Please enter Mobile Number",
                    Toast.LENGTH_LONG).show();
        } else if (et_FullName.getText().toString().length() <= 0) {
            Toast.makeText(com.ronettv.mobile.RegisterActivity.this, "Please enter Full Name",
                    Toast.LENGTH_LONG).show();
        }
        if (sp_City.getSelectedItem() == null) {
            Toast.makeText(com.ronettv.mobile.RegisterActivity.this, "Please select City",
                    Toast.LENGTH_LONG).show();
        }
        if (et_Password.getText().toString().length() <= 0) {
            Toast.makeText(com.ronettv.mobile.RegisterActivity.this, "Please enter Password",
                    Toast.LENGTH_LONG).show();
        } else if (email.matches(emailPattern)) {
            mCity = sp_City.getSelectedItem().toString();
            ClientDatum client = new ClientDatum();
            client.setPhone(et_MobileNumber.getText().toString());
            client.setFullname(et_FullName.getText().toString());
            client.setCountry(mCountry);
            client.setState(mState);
            client.setCity(mCity);
            client.setEmail(et_EmailId.getText().toString());
            client.setPassword(et_Password.getText().toString());
            DoRegistrationAsyncTask task = new DoRegistrationAsyncTask();
            task.execute(client);
        } else {
            Toast.makeText(com.ronettv.mobile.RegisterActivity.this,
                    "Please enter valid Email Id", Toast.LENGTH_LONG).show();
        }
    }

    public void btnLinkUser_onClick(View v) {
        mCurrentLayout = LINK_USER_LAYOUT;
        et_EmailId = (EditText) findViewById(R.id.a_reg_et_link_user_email_id);
        String email = et_EmailId.getText().toString().trim();
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        if (email.matches(emailPattern)) {
            DoLinkUserAsyncTask task = new DoLinkUserAsyncTask();
            task.execute(email);
        } else {
            Toast.makeText(com.ronettv.mobile.RegisterActivity.this,
                    "Please enter valid Email Id", Toast.LENGTH_LONG).show();
        }
    }

    public void btnCancel_onClick(View v) {

        if (mCurrentLayout == LOGIN_LAYOUT)
            closeApp();
        else if (mCurrentLayout == REGISTRATION_LAYOUT) {
            mCurrentLayout = LOGIN_LAYOUT;
            setLayout(mCurrentLayout);
        } else if (mCurrentLayout == LINK_USER_LAYOUT) {
            mCurrentLayout = LOGIN_LAYOUT;
            setLayout(mCurrentLayout);
        }
    }

    private void closeApp() {
        AlertDialog mConfirmDialog = mApplication.getConfirmDialog(this);
        mConfirmDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mProgressDialog != null) {
                            mProgressDialog.dismiss();
                            mProgressDialog = null;
                        }
                        mIsReqCanceled = true;
                        com.ronettv.mobile.RegisterActivity.this.finish();
                    }
                });
        mConfirmDialog.show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (mCurrentLayout == LOGIN_LAYOUT)
                closeApp();
            else if (mCurrentLayout == REGISTRATION_LAYOUT) {
                mCurrentLayout = LOGIN_LAYOUT;
                setLayout(mCurrentLayout);
            } else if (mCurrentLayout == LINK_USER_LAYOUT) {
                mCurrentLayout = LOGIN_LAYOUT;
                setLayout(mCurrentLayout);
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void getClientDetails_n_RedirectPlanActivity() {


        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(com.ronettv.mobile.MyApplication.API_URL)
                .setLogLevel(RestAdapter.LogLevel.NONE)
                .setConverter(new ClientDetailsJSONConverter())
                .setClient(
                        new CustomUrlConnectionClient(com.ronettv.mobile.MyApplication.tenentId,
                                com.ronettv.mobile.MyApplication.basicAuth,
                                com.ronettv.mobile.MyApplication.contentType)).build();
        mOBSClient = restAdapter.create(OBSClient.class);
        mOBSClient.getClinetDetails(mApplication.getClientId(),
                getClientDetailsCallBack);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    private RegClientRespDatum readJsonUser(String jsonText) {
        Gson gson = new Gson();
        RegClientRespDatum response = gson.fromJson(jsonText,
                RegClientRespDatum.class);
        return response;
    }

    @Override
    public void onPwdSubmitClickListener(String mailId) {
        new PwdSenderTask().execute(mailId);
    }

    void showDialog() {
        // Create the fragment and show it as a dialog.
        ForgetPwdDialogFragment newFragment = new ForgetPwdDialogFragment();
        newFragment.show(getFragmentManager(), "dialog");
    }

    static class ClientDetailsJSONConverter implements Converter {

        @Override
        public ClientDatum fromBody(TypedInput typedInput, Type type)
                throws ConversionException {
            ClientDatum client = null;

            try {
                String json = com.ronettv.mobile.MyApplication.getJSONfromInputStream(typedInput
                        .in());
                client = parseJsonToClient(json);
            } catch (IOException e) {
                Log.i(TAG, e.getMessage());
            }
            return client;
        }

        @Override
        public TypedOutput toBody(Object o) {
            return null;
        }

    }

    private class DoRegistrationAsyncTask extends
            AsyncTask<ClientDatum, Void, ResponseObj> {
        ClientDatum clientData;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
                mProgressDialog = null;
            }
            mProgressDialog = new ProgressDialog(com.ronettv.mobile.RegisterActivity.this,
                    ProgressDialog.THEME_HOLO_DARK);
            mProgressDialog.setMessage("Registering Details...");
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setOnCancelListener(new OnCancelListener() {

                public void onCancel(DialogInterface arg0) {
                    if (mProgressDialog.isShowing())
                        mProgressDialog.dismiss();
                    String msg = "Client Registration Failed.";
                    Toast.makeText(com.ronettv.mobile.RegisterActivity.this, msg,
                            Toast.LENGTH_LONG).show();
                    cancel(true);
                }
            });
            mProgressDialog.show();
        }

        @Override
        protected ResponseObj doInBackground(ClientDatum... arg0) {
            ResponseObj resObj = new ResponseObj();
            clientData = (ClientDatum) arg0[0];

            String firstName = "";
            String fullName = "";
            String name[] = clientData.getFullname().split(" ", 2);
            if (name[0] != null && name[0].length() != 0) {
                fullName = firstName = name[0];
            }
            if (name.length > 1) {
                fullName = name[1];
            }

            if (mApplication.isNetworkAvailable()) {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("TagURL", "/activationprocess/selfregistration");// map.put("TagURL","/clients");
                String androidId = Settings.Secure.getString(
                        getApplicationContext().getContentResolver(),
                        Settings.Secure.ANDROID_ID);
                map.put("device", androidId);
                map.put("firstname", firstName);// clientData.getFullname());
                map.put("address", "none");
                map.put("isMailCheck", "N");
                // map.put("nationalId", "junk");
                map.put("fullname", fullName);// clientData.getFullname());
                map.put("deviceAgreementType", "OWN");
                map.put("city", clientData.getCity());
                map.put("password", clientData.getPassword());
                map.put("zipCode", "436346"); // junk
                map.put("phone", clientData.getPhone());
                map.put("email", clientData.getEmail());
                resObj = Utilities.callExternalApiPostMethod(
                        getApplicationContext(), map);
            } else {
                resObj.setFailResponse(100, NETWORK_ERROR);
            }
            return resObj;

        }

        @Override
        protected void onPostExecute(ResponseObj resObj) {

            super.onPostExecute(resObj);
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
                mProgressDialog = null;
            }

            if (resObj.getStatusCode() == 200) {
                RegClientRespDatum clientResData = readJsonUser(resObj
                        .getsResponse());
                mApplication.resetPrefs();
                if(clientResData.getClientId()== 0) {
                    mApplication.setClientId(Long.toString(clientResData
                    .getResourceId()));
                }else
                {
                    mApplication.setClientId(Long.toString(clientResData
                            .getClientId()));
                }
                getClientDetails_n_RedirectPlanActivity();
            } else {
                Toast.makeText(com.ronettv.mobile.RegisterActivity.this,
                        "Server Error : " + resObj.getsErrorMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private class DoLinkUserAsyncTask extends
            AsyncTask<String, Void, ResponseObj> {
        String clientEmailId;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
                mProgressDialog = null;
            }
            mProgressDialog = new ProgressDialog(com.ronettv.mobile.RegisterActivity.this,
                    ProgressDialog.THEME_HOLO_DARK);
            mProgressDialog.setMessage("Registering Details...");
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setOnCancelListener(new OnCancelListener() {

                public void onCancel(DialogInterface arg0) {
                    if (mProgressDialog.isShowing())
                        mProgressDialog.dismiss();
                    String msg = "Client Registration Failed.";
                    Toast.makeText(com.ronettv.mobile.RegisterActivity.this, msg,
                            Toast.LENGTH_LONG).show();
                    cancel(true);
                }
            });
            mProgressDialog.show();
        }

        @Override
        protected ResponseObj doInBackground(String... arg0) {
            ResponseObj resObj = new ResponseObj();
            clientEmailId = (String) arg0[0];
            if (mApplication.isNetworkAvailable()) {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("TagURL", "/linkupaccount");// map.put("TagURL","/clients");
                map.put("userName", clientEmailId);
                resObj = Utilities.callExternalApiPostMethod(
                        getApplicationContext(), map);
            } else {
                resObj.setFailResponse(100, NETWORK_ERROR);
            }
            return resObj;
        }

        @Override
        protected void onPostExecute(ResponseObj resObj) {

            super.onPostExecute(resObj);
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
                mProgressDialog = null;
            }
            if (resObj.getStatusCode() == 200) {
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        com.ronettv.mobile.RegisterActivity.this, AlertDialog.THEME_HOLO_LIGHT);
                builder.setIcon(R.drawable.ic_logo_confirm_dialog);
                builder.setTitle("Information");
                builder.setMessage("Login details sent to your mail.Please check your mail.");
                builder.setCancelable(false);
                AlertDialog dialog = builder.create();
                dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Ok",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {

                                // do nothing
                            }
                        });
                dialog.show();
                mCurrentLayout = LOGIN_LAYOUT;
                setLayout(mCurrentLayout);
            } else {
                Toast.makeText(com.ronettv.mobile.RegisterActivity.this,
                        "Server Error : " + resObj.getsErrorMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Async Task For Handling selfcare validation
     */

    private class DoSelfCareLoginAsyncTask extends
            AsyncTask<SelfCareUserDatum, Void, ResponseObj> {
        SelfCareUserDatum userData;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
                mProgressDialog = null;
            }
            mProgressDialog = new ProgressDialog(com.ronettv.mobile.RegisterActivity.this,
                    ProgressDialog.THEME_HOLO_DARK);
            mProgressDialog.setMessage("Connecting to Server...");
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setOnCancelListener(new OnCancelListener() {

                public void onCancel(DialogInterface arg0) {
                    if (mProgressDialog.isShowing())
                        mProgressDialog.dismiss();
                    cancel(true);
                }
            });
            mProgressDialog.show();
        }

        @Override
        protected ResponseObj doInBackground(SelfCareUserDatum... arg0) {
            ResponseObj resObj = new ResponseObj();
            userData = (SelfCareUserDatum) arg0[0];
            if (mApplication.isNetworkAvailable()) {
                HashMap<String, String> map = new HashMap<String, String>();
                // https://192.168.1.104:7070/obsplatform/api/v1/selfcare/login?username="10@gmail.com"&password="wnrodihw"
                map.put("TagURL", "/selfcare/login?username="
                        + userData.email_id + "&password=" + userData.password);
                resObj = Utilities.callExternalApiPostMethod(
                        getApplicationContext(), map);
            } else {
                resObj.setFailResponse(100, NETWORK_ERROR);
            }
            if (resObj.getStatusCode() == 200) {

               boolean isPayPalReq = false;
                JSONObject jResObj;
                try {
                    jResObj = new JSONObject(resObj.getsResponse());

                    JSONObject jClientData = jResObj
                            .getJSONObject("clientData");
                    mApplication.resetPrefs();
                    mApplication.setClientId(jClientData.getString("id"));
                    mApplication.setBalance(Float.parseFloat(jClientData
                            .getString("balanceAmount")));
                    mApplication.setBalanceCheck(jClientData
                            .getBoolean("balanceCheck"));
                    mApplication.setCurrency(jClientData.getString("currency"));

                    JSONObject jPayPalData = jResObj
                            .getJSONObject("paypalConfigData");
                    isPayPalReq = jPayPalData.getBoolean("enabled");
                    mApplication.setPayPalCheck(isPayPalReq);
                    if (isPayPalReq) {
                        String value = jPayPalData.getString("value");
                        if (value != null && value.length() > 0) {
                            JSONObject json = new JSONObject(value);
                            if (json != null) {
                                mApplication.setPayPalClientID(json.get(
                                        "clientId").toString());
                            }
                        } else
                            Toast.makeText(com.ronettv.mobile.RegisterActivity.this,
                                    "Invalid Data for PayPal details",
                                    Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    if (resObj.getStatusCode() != 200) {
                        resObj.setFailResponse(100, "Json Error");
                        return resObj;
                    }
                }
                if (mApplication.isNetworkAvailable()) {
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put("TagURL",
                            "/ownedhardware/" + mApplication.getClientId());
                    map.put("itemType", "1");
                    map.put("dateFormat", "dd MMMM yyyy");
                    String androidId = Settings.Secure.getString(
                            getApplicationContext().getContentResolver(),
                            Settings.Secure.ANDROID_ID);
                    map.put("serialNumber", androidId);
                    map.put("provisioningSerialNumber", androidId);
                    Date date = new Date();
                    SimpleDateFormat df = new SimpleDateFormat("dd MMMM yyyy",
                            new Locale("en"));
                    String formattedDate = df.format(date);
                    map.put("allocationDate", formattedDate);
                    map.put("locale", "en");
                    map.put("status", "");
                    resObj = Utilities.callExternalApiPostMethod(
                            getApplicationContext(), map);
                } else {
                    resObj.setFailResponse(100, NETWORK_ERROR);
                }
            }
            return resObj;
        }

        @Override
        protected void onPostExecute(ResponseObj resObj) {

            super.onPostExecute(resObj);
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
                mProgressDialog = null;
            }

            if (resObj.getStatusCode() == 200) {

                mOBSClient.getActivePlans(mApplication.getClientId(),
                        activePlansCallBack);


				/*
				 * Intent intent = new Intent(RegisterActivity.this,
				 * MainActivity.class); RegisterActivity.this.finish();
				 * startActivity(intent);
				 */
            } else {
                Toast.makeText(com.ronettv.mobile.RegisterActivity.this,
                        "Server Error : " + resObj.getsErrorMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private class SelfCareUserDatum {

        String email_id;
        String password;
    }

    private class PwdSenderTask extends AsyncTask<String, Void, ResForgetPwd> {

        RetrofitError error = null;
        int status = -1;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
                mProgressDialog = null;
            }
            mProgressDialog = new ProgressDialog(com.ronettv.mobile.RegisterActivity.this,
                    ProgressDialog.THEME_HOLO_DARK);
            mProgressDialog.setMessage("Please wait...");
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setOnCancelListener(new OnCancelListener() {

                public void onCancel(DialogInterface arg0) {
                    if (mProgressDialog.isShowing())
                        mProgressDialog.dismiss();
                    cancel(true);
                }
            });
            mProgressDialog.show();
        }

        @Override
        protected ResForgetPwd doInBackground(String... arg0) {

            String mailId = arg0[0];
            ResForgetPwd result = null;
            if (mApplication.isNetworkAvailable()) {
                OBSClient mOBSClient = mApplication.getOBSClient();

                if (mailId != null && mailId.length() != 0) {
                    try {
                        result = mOBSClient
                                .sendPasswordToMail(new SenderMailId(mailId));
                    } catch (Exception e) {
                        error = ((RetrofitError) e);
                        status = error.getResponse().getStatus();
                    }
                }
            } else {
                Toast.makeText(com.ronettv.mobile.RegisterActivity.this, "Communication Error.",
                        Toast.LENGTH_LONG).show();
            }

            return result;
        }

        @Override
        protected void onPostExecute(ResForgetPwd result) {
            if (mProgressDialog != null) {
                if (mProgressDialog.isShowing())
                    mProgressDialog.dismiss();
                mProgressDialog = null;
            }
            if (result != null || status == -1) {

                Toast.makeText(com.ronettv.mobile.RegisterActivity.this,
                        getResources().getString(R.string.password_mail),
                        Toast.LENGTH_LONG).show();
            } else {
                final String toastMsg = (status == 403 ? mApplication
                        .getDeveloperMessage(error)
                        : "Server Communication Error");// errMsg;
                Toast.makeText(com.ronettv.mobile.RegisterActivity.this, toastMsg,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

}

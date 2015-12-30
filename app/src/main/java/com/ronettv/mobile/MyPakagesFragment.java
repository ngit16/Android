package com.ronettv.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;
import com.ronettv.adapter.PackageAdapter;
import com.ronettv.adapter.PaytermAdapter;
import com.ronettv.data.ClientDatum;
import com.ronettv.data.ConfigurationProperty;
import com.ronettv.data.OrderDatum;
import com.ronettv.data.PaytermPaymentdatum;
import com.ronettv.data.Paytermdatum;
import com.ronettv.data.PlanDatum;
import com.ronettv.data.ResponseObj;
import com.ronettv.data.Subscriptiondatum;
import com.ronettv.mobile.MyApplication.DoBGTasks;
import com.ronettv.parser.JSONPaytermConverter;
import com.ronettv.paypal.PaypalHelper;
import com.ronettv.retrofit.CustomUrlConnectionClient;
import com.ronettv.retrofit.OBSClient;
import com.ronettv.service.DoBGTasksService;
import com.ronettv.utils.Utilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

public class MyPakagesFragment extends Fragment {
    private final static String NETWORK_ERROR = "Network error.";
    private final static String PREPAID_PLANS = "Prepaid plans";
    private final static String MY_PLANS = "My plans";
    public static String TAG = com.ronettv.mobile.MyPakagesFragment.class.getName();
    public static int selGroupId = -1;
    public static int selPaytermId = -1;
    public static int orderId = -1;
    private static String NEW_PLANS_DATA;
    private static String MY_PLANS_DATA;
    com.ronettv.mobile.MyApplication mApplication = null;
    boolean mIsReqCanceled = false;
    Activity mActivity;
    View mRootView;
    List<PlanDatum> mPrepaidPlans;
    List<Paytermdatum> mPayterms;
    List<PlanDatum> mMyPlans;
    List<PlanDatum> mNewPlans;
    List<OrderDatum> mMyOrders;
    List<Subscriptiondatum> mSubscription;
    /*public void fetchAndbulidContracts() {

        OBSClient.getSubscription(getContractCallback);
    }*/
    public final Callback<List<Subscriptiondatum>> getContractCallback = new Callback<List<Subscriptiondatum>>() {
        @Override
        public void failure(RetrofitError retrofitError) {
            System.out.println("Error");

        }

        @Override
        public void success(List<Subscriptiondatum> Contractdata, Response response) {
            mSubscription = Contractdata;
        }
    };
    PackageAdapter listAdapter;
    ExpandableListView mExpListView;
    PaytermAdapter mListAdapter;
    ListView mPaytermLv;
    AlertDialog mConfirmDialog;
    PaypalHelper mPaypalHelper;
    Integer subId;
    private ProgressDialog mProgressDialog;
    final Callback<ClientDatum> getClientDetailsCallBack = new Callback<ClientDatum>() {
        @Override
        public void failure(RetrofitError retrofitError) {
            if (!mIsReqCanceled) {
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }
                if (retrofitError.isNetworkError()) {
                    Toast.makeText(mActivity,
                            mApplication.getString(R.string.error_network),
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(
                            mActivity,
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
                    Toast.makeText(mActivity, "Server Error.",
                            Toast.LENGTH_LONG).show();
                } else {
                    SharedPreferences.Editor editor = mApplication.getEditor();
                    editor.putString(mApplication.getResources().getString(
                            R.string.client_data), new Gson().toJson(client));
                    editor.commit();
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
                                Log.e("AuthenticationAcitivity",
                                        (e.getMessage() == null) ? "Json Exception"
                                                : e.getMessage());
                                Toast.makeText(getActivity(),
                                        "Invalid Data-Json Exception",
                                        Toast.LENGTH_LONG).show();
                            }
                        } else
                            Toast.makeText(getActivity(),
                                    "Invalid Data for PayPal details",
                                    Toast.LENGTH_LONG).show();
                    }
                }
            } else
                mIsReqCanceled = false;

        }
    };
    Callback<List<OrderDatum>> getMyPlansCallBack = new Callback<List<OrderDatum>>() {

        @Override
        public void success(List<OrderDatum> orderList, Response response) {
            if (!mIsReqCanceled) {
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }
                if (orderList == null || orderList.size() == 0) {

                    BufferedReader reader;
                    String line;
                    StringBuilder builder = new StringBuilder();
                    try {
                        reader = new BufferedReader(
                                new InputStreamReader(response.getBody().in()));

                        while ((line = reader.readLine()) != null) {
                            builder.append(line);
                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }


                    (mRootView.findViewById(R.id.f_my_pkg_exlv_my_plans)).setVisibility(View.GONE);
                    (mRootView.findViewById(R.id.f_my_pkg_payterm_lv)).setVisibility(View.GONE);

                    Toast.makeText(mActivity, "No Active Plans.",
                            Toast.LENGTH_LONG).show();


                } else {
                    mMyOrders = orderList;
                    CheckPlansnUpdate();
                }

            } else
                mIsReqCanceled = false;

        }

        @Override
        public void failure(RetrofitError retrofitError) {
            if (!mIsReqCanceled) {
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }
                if (retrofitError.isNetworkError()) {
                    Toast.makeText(mActivity,
                            mApplication.getString(R.string.error_network),
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(
                            mActivity,
                            "Server Error : "
                                    + retrofitError.getResponse().getStatus(),
                            Toast.LENGTH_LONG).show();
                }
            } else
                mIsReqCanceled = false;

        }
    };
    final Callback<List<PlanDatum>> getPrepaidPlansCallBack = new Callback<List<PlanDatum>>() {
        @Override
        public void failure(RetrofitError retrofitError) {
            if (!mIsReqCanceled) {
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }
                if (retrofitError.isNetworkError()) {
                    Toast.makeText(mActivity,
                            mApplication.getString(R.string.error_network),
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(
                            mActivity,
                            "Server Error : "
                                    + retrofitError.getResponse().getStatus(),
                            Toast.LENGTH_LONG).show();
                }
            } else
                mIsReqCanceled = true;
        }

        @Override
        public void success(List<PlanDatum> planList, Response response) {
            if (!mIsReqCanceled) {
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }
                if (planList != null) {
                    mPrepaidPlans = planList;
                    getMyPlansFromServer();
                }
            } else
                mIsReqCanceled = true;
        }
    };
    final Callback<PaytermPaymentdatum> getPayAmountCallBack = new Callback<PaytermPaymentdatum>() {
        @Override
        public void failure(RetrofitError retrofitError) {
            if (!mIsReqCanceled) {
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }
                if (retrofitError.isNetworkError()) {
                    Toast.makeText(
                            getActivity(),
                            mApplication.getApplicationContext().getString(
                                    R.string.error_network), Toast.LENGTH_LONG)
                            .show();
                } else {
                    Toast.makeText(
                            getActivity(),
                            "Server Error : "
                                    + retrofitError.getResponse().getStatus(),
                            Toast.LENGTH_LONG).show();
                }
            } else
                mIsReqCanceled = false;
        }

        @Override
        public void success(final PaytermPaymentdatum paytermPaymentdatum, Response response) {


            final Double minBalanceReq1 = paytermPaymentdatum.getFinalAmount();
            AlertDialog.Builder builder1 = new AlertDialog.Builder(
                    (getActivity()),
                    AlertDialog.THEME_HOLO_LIGHT);
            builder1.setIcon(R.drawable.ic_logo_confirm_dialog);
            builder1.setTitle("Confirmation");
            String msg1 = "plan price is."
                    + (minBalanceReq1);
            builder1.setMessage(msg1);
            builder1.setCancelable(true);
            mConfirmDialog = builder1.create();
            mConfirmDialog.setButton(AlertDialog.BUTTON_NEGATIVE,
                    (minBalanceReq1 != null ? "cancel" : ""),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                            int buttonId) {
                        }
                    });
            mConfirmDialog.setButton(AlertDialog.BUTTON_POSITIVE,
                    (minBalanceReq1 != null ? "ok" : "Ok"),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                            int which) {
                            if (minBalanceReq1 != null) {
                                paytermPaymentdatum(paytermPaymentdatum);
                            }

                        }

                    });
            mConfirmDialog.show();
        }

        public void paytermPaymentdatum(final PaytermPaymentdatum paytermPaymentdatum) {


            if (!mIsReqCanceled) {
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }
                if (paytermPaymentdatum != null) {
                    Double minBalanceReq = paytermPaymentdatum.getFinalAmount() + mApplication.getBalance();
                    if (mApplication.balanceCheck == true
                            && (minBalanceReq >= 0)) {

                        final boolean isPayPalChk = mApplication
                                .isPayPalCheck();
                        AlertDialog.Builder builder = new AlertDialog.Builder(
                                getActivity(),
                                AlertDialog.THEME_HOLO_LIGHT);
                        builder.setIcon(R.drawable.ic_logo_confirm_dialog);
                        builder.setTitle("Confirmation");
                        String msg = "Insufficient Balance."
                                + (isPayPalChk == true ? "Go to PayPal ??"
                                : "Please do Payment.");
                        builder.setMessage(msg);
                        builder.setCancelable(true);
                        mConfirmDialog = builder.create();
                        mConfirmDialog.setButton(AlertDialog.BUTTON_NEGATIVE,
                                (isPayPalChk == true ? "No" : ""),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int buttonId) {
                                    }
                                });
                        mConfirmDialog.setButton(AlertDialog.BUTTON_POSITIVE,
                                (isPayPalChk == true ? "Yes" : "Ok"),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        if (isPayPalChk == true) {
                                            redirectToPaypal(paytermPaymentdatum);
                                        }
                                    }
                                });
                        mConfirmDialog.show();
                    } else {
                        //Change paln in OBS
                        changePlan(mNewPlans.get(selGroupId).toString());
                    }
                } else {
                    Toast.makeText(getActivity(), "PaytermAmount detials not Available", Toast.LENGTH_LONG).show();
                }
            } else
                mIsReqCanceled = false;
        }
    };
    final Callback<List<Paytermdatum>> getPlanPayterms = new Callback<List<Paytermdatum>>() {
        @Override
        public void failure(RetrofitError retrofitError) {
            if (!mIsReqCanceled) {
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }
                if (retrofitError.isNetworkError()) {
                    Toast.makeText(
                            getActivity(),
                            getActivity().getApplicationContext().getString(
                                    R.string.error_network), Toast.LENGTH_LONG)
                            .show();
                } else {
                    Toast.makeText(
                            getActivity(),
                            "No Payterms for this plan.", Toast.LENGTH_LONG)
                            .show();
                }
            } else
                mIsReqCanceled = false;
            Button b = (Button) mRootView.findViewById(R.id.a_plan_btn_submit);
            b.setEnabled(true);
            selGroupId = -1;
        }

        @Override
        public void success(List<Paytermdatum> payterms, Response response) {
            if (!mIsReqCanceled) {
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }
                if (payterms != null) {
                    mPayterms = payterms;
                    buildPaytermList();
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
                client.setActivationDate(com.ronettv.mobile.MyApplication.df.format(date));
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

    public static List<OrderDatum> getOrdersFromJson(String json) {
        List<OrderDatum> ordersList = new ArrayList<OrderDatum>();
        try {

            JSONArray arrOrders = new JSONArray(json);
            for (int i = 0; i < arrOrders.length(); i++) {

                JSONObject obj = arrOrders.getJSONObject(i);
                if ("ACTIVE".equalsIgnoreCase(obj.getString("status"))) {
                    OrderDatum order = new OrderDatum();
                    order.setOrderId(obj.getString("id"));
                    order.setPlanCode(obj.getString("planCode"));
                    order.setPdid(obj.getInt("pdid"));
                    order.setPrice(obj.getString("price"));
                    order.setStatus(obj.getString("status"));
                    try {
                        JSONArray arrDate = obj.getJSONArray("activeDate");
                        Date date = com.ronettv.mobile.MyApplication.df.parse(arrDate.getString(0)
                                + "-" + arrDate.getString(1) + "-"
                                + arrDate.getString(2));
                        order.setActiveDate(com.ronettv.mobile.MyApplication.df.format(date));
                    } catch (JSONException e) {
                        order.setActiveDate(obj.getString("activeDate"));
                    }
                    try {
                        JSONArray arrDate = obj.getJSONArray("invoiceTilldate");
                        Date date = com.ronettv.mobile.MyApplication.df.parse(arrDate.getString(0)
                                + "-" + arrDate.getString(1) + "-"
                                + arrDate.getString(2));
                        order.setInvoiceTilldate(com.ronettv.mobile.MyApplication.df.format(date));
                    } catch (JSONException e) {
                        try {
                            order.setInvoiceTilldate(obj
                                    .getString("invoiceTilldate"));
                        } catch (JSONException ex) {
                            // no invoice till date.
                        }
                    }
                    ordersList.add(order);
                }
            }
        } catch (Exception e) {
            Log.i(TAG, e.getMessage());
        }
        return ordersList;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        mActivity = getActivity();
        mApplication = ((com.ronettv.mobile.MyApplication) mActivity.getApplicationContext());
        mPaypalHelper = new PaypalHelper(getActivity(), mApplication);
        // mOBSClient = mApplication.getOBSClient(mActivity);
        setHasOptionsMenu(true);
        selGroupId = -1;
        orderId = -1;
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_my_packages, container,
                false);

        TextView tv_title = (TextView) mRootView
                .findViewById(R.id.a_plan_tv_selpkg);
        tv_title.setText(R.string.choose_plan_change);

        Button btnNext = (Button) mRootView
                .findViewById(R.id.a_plan_btn_submit);
        btnNext.setText(R.string.next);

        NEW_PLANS_DATA = mApplication.getResources().getString(
                R.string.new_plans_data);
        MY_PLANS_DATA = mApplication.getResources().getString(
                R.string.my_plans_data);
        String newPlansJson = mApplication.getPrefs().getString(NEW_PLANS_DATA,
                "");
        String myPlansJson = mApplication.getPrefs().getString(MY_PLANS_DATA,
                "");
        //Update client details
        GetnUpdateFromServer();
        if (newPlansJson != null && newPlansJson.length() != 0) {
            mNewPlans = getPlanListFromJSON(newPlansJson);
        }
        if (myPlansJson != null && myPlansJson.length() != 0) {
            mMyPlans = getPlanListFromJSON(myPlansJson);
            buildPlansList();
        } else {
            getPlansFromServer();
        }
        return mRootView;
    }

    private void buildPlansList() {

        (mRootView.findViewById(R.id.f_my_pkg_exlv_my_plans)).setVisibility(View.VISIBLE);
        (mRootView.findViewById(R.id.f_my_pkg_payterm_lv)).setVisibility(View.GONE);

        TextView tv_title = (TextView) mRootView.findViewById(R.id.a_plan_tv_selpkg);
        tv_title.setText(R.string.sel_pkg);

        Button btnNext = (Button) mRootView.findViewById(R.id.a_plan_btn_submit);
        btnNext.setText(R.string.next);

        if (mMyPlans != null && mMyPlans.size() > 0) {
            mExpListView = (ExpandableListView) mRootView
                    .findViewById(R.id.f_my_pkg_exlv_my_plans);
            listAdapter = new PackageAdapter(mActivity, mMyPlans);
            mExpListView.setAdapter(listAdapter);
            mExpListView.setOnGroupClickListener(new OnGroupClickListener() {
                @Override
                public boolean onGroupClick(ExpandableListView parent, View v,
                                            int groupPosition, long id) {

                    RadioButton rb1 = (RadioButton) v
                            .findViewById(R.id.plan_list_plan_rb);
                    if (null != rb1 && (!rb1.isChecked())) {
                        selGroupId = groupPosition;
                    } else {
                        selGroupId = -1;
                    }
                    return false;
                }
            });
        }
    }

    private void GetnUpdateFromServer() {

        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
        mProgressDialog = new ProgressDialog(mActivity,
                ProgressDialog.THEME_HOLO_DARK);
        mProgressDialog.setMessage("Connecting Server");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setOnCancelListener(new OnCancelListener() {

            public void onCancel(DialogInterface arg0) {
                if (mProgressDialog.isShowing())
                    mProgressDialog.dismiss();
                mIsReqCanceled = true;
            }
        });
        mProgressDialog.show();
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(com.ronettv.mobile.MyApplication.API_URL)
                .setLogLevel(RestAdapter.LogLevel.NONE)
                .setConverter(new ClientDetailsJSONConverter())
                .setClient(
                        new CustomUrlConnectionClient(com.ronettv.mobile.MyApplication.tenentId,
                                com.ronettv.mobile.MyApplication.basicAuth,
                                com.ronettv.mobile.MyApplication.contentType)).build();
        OBSClient obsClient = restAdapter.create(OBSClient.class);
        obsClient.getClinetDetails(mApplication.getClientId(),
                getClientDetailsCallBack);
    }

    private void getPlansFromServer() {
        getPlans(PREPAID_PLANS);
    }

    private void getMyPlansFromServer() {
        getPlans(MY_PLANS);
    }

    public void getPlans(String planType) {
        boolean showProgressDialog = false;
        if (mProgressDialog != null)
            if (mProgressDialog.isShowing()) {
                // do nothing
            } else {
                showProgressDialog = true;
            }
        else {
            showProgressDialog = true;
        }
        if (showProgressDialog) {
            mProgressDialog = null;
            mProgressDialog = new ProgressDialog(mActivity,
                    ProgressDialog.THEME_HOLO_DARK);
            mProgressDialog.setMessage("Connecting Server");
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setOnCancelListener(new OnCancelListener() {

                public void onCancel(DialogInterface arg0) {
                    if (mProgressDialog.isShowing())
                        mProgressDialog.dismiss();
                    mIsReqCanceled = true;
                }
            });
            mProgressDialog.show();
        }

        if (PREPAID_PLANS.equalsIgnoreCase(planType)) {
            OBSClient mOBSClient = mApplication.getOBSClient();
            mOBSClient.getPrepaidPlans(getPrepaidPlansCallBack);
            mOBSClient.getSubscription(getContractCallback);
        } else if (MY_PLANS.equalsIgnoreCase(planType)) {
            OBSClient mOBSClient = null;
            RestAdapter restAdapter = new RestAdapter.Builder()
                    .setEndpoint(com.ronettv.mobile.MyApplication.API_URL)
                    .setLogLevel(RestAdapter.LogLevel.NONE)
                            /** Need to change Log level to NONe */
                    .setConverter(new JSONConverter())
                    .setClient(
                            new CustomUrlConnectionClient(
                                    com.ronettv.mobile.MyApplication.tenentId,
                                    com.ronettv.mobile.MyApplication.basicAuth,
                                    com.ronettv.mobile.MyApplication.contentType)).build();
            mOBSClient = restAdapter.create(OBSClient.class);
            mOBSClient.getClinetPackageDetails(mApplication.getClientId(),
                    getMyPlansCallBack);
        }
    }

    private void CheckPlansnUpdate() {
        mNewPlans = new ArrayList<PlanDatum>();
        mMyPlans = new ArrayList<PlanDatum>();
        if (null != mPrepaidPlans && null != mMyOrders
                && mPrepaidPlans.size() > 0 && mMyOrders.size() > 0) {
            for (PlanDatum plan : mPrepaidPlans) {
                int planId = plan.getId();
                boolean isNew = true;
                String sOrderId = null;
                for (int i = 0; i < mMyOrders.size(); i++) {
                    if (mMyOrders.get(i).getPdid() == planId
                            && mMyOrders.get(i).status
                            .equalsIgnoreCase("ACTIVE")) {
                        isNew = false;
                        sOrderId = mMyOrders.get(i).orderId;
                    }
                }
                if (isNew) {
                    mNewPlans.add(plan);
                } else {
                    plan.orderId = sOrderId;
                    mMyPlans.add(plan);
                }
            }
        }

        boolean savePlans = false;
        if (null != mNewPlans && mNewPlans.size() != 0) {
            mApplication.getEditor().putString(NEW_PLANS_DATA,
                    new Gson().toJson(mNewPlans));
            savePlans = true;
        }
        if (null != mMyPlans && mMyPlans.size() != 0) {
            mApplication.getEditor().putString(MY_PLANS_DATA,
                    new Gson().toJson(mMyPlans));
            savePlans = true;
        }
        if (savePlans)
            mApplication.getEditor().commit();
        buildPlansList();

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.nav_menu, menu);
        MenuItem refreshItem = menu.findItem(R.id.action_refresh);
        refreshItem.setVisible(true);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_home:
                startActivity(new Intent(getActivity(), MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                break;
            case R.id.action_refresh:
                Button btn = (Button) mRootView.findViewById(R.id.a_plan_btn_submit);
                if (btn.getText().toString()
                        .equalsIgnoreCase(getString(R.string.subscribe))) {
                    selGroupId = -1;
                    TextView tv_title = (TextView) mRootView
                            .findViewById(R.id.a_plan_tv_selpkg);
                    tv_title.setText(R.string.choose_plan_change);

                    btn.setText(R.string.next);
                }
                getPlansFromServer();
                break;
            case R.id.action_logout:
                logout();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void btnSubmit_onClick(View v) {
        if (((Button) v).getText().toString()
                .equalsIgnoreCase(getString(R.string.next))) {
            if (selGroupId == -1) {
                Toast.makeText(getActivity(), "Select a Plan to Change",
                        Toast.LENGTH_LONG).show();
            } else {
                ((Button) v).setEnabled(false);
                orderId = Integer
                        .parseInt(mMyPlans.get(selGroupId).orderId);
                selGroupId = -1;
                TextView tv_title = (TextView) mRootView
                        .findViewById(R.id.a_plan_tv_selpkg);
                tv_title.setText(R.string.choose_plan_sub);

                ((Button) v).setText(R.string.next2);
                mExpListView = (ExpandableListView) mRootView
                        .findViewById(R.id.f_my_pkg_exlv_my_plans);
                listAdapter = null;
                if (mNewPlans != null && mNewPlans.size() > 0) {
                    listAdapter = new PackageAdapter(mActivity, mNewPlans);
                    mExpListView.setAdapter(listAdapter);
                    mExpListView
                            .setOnGroupClickListener(new OnGroupClickListener() {
                                @Override
                                public boolean onGroupClick(
                                        ExpandableListView parent, View v,
                                        int groupPosition, long id) {

                                    RadioButton rb1 = (RadioButton) v
                                            .findViewById(R.id.plan_list_plan_rb);
                                    if (null != rb1 && (!rb1.isChecked())) {
                                        selGroupId = groupPosition;
                                    } else {
                                        selGroupId = -1;
                                    }
                                    return false;
                                }
                            });
                    ((Button) v).setEnabled(true);
                } else {
                    mExpListView.setAdapter(listAdapter);
                    Toast.makeText(getActivity(), "No new Plans",
                            Toast.LENGTH_LONG).show();
                }
            }
        } else if (((Button) v).getText().toString()
                .equalsIgnoreCase(getString(R.string.next2))) {
            if (selGroupId != -1) {
                ((Button) v).setEnabled(false);
                getPaytermsforSelPlan();
            } else {
                Toast.makeText(getActivity(), "Choose a Plan to Subscribe", Toast.LENGTH_LONG).show();
            }
        } else if (((Button) v).getText().toString()
                .equalsIgnoreCase(getString(R.string.subscribe))) {

            if (selGroupId != -1 && selPaytermId != -1) {
                OBSClient obsClient = mApplication.getOBSClient();
                obsClient.getPayAmountforPayterm(mPayterms.get(selPaytermId).getId().toString(), mApplication.getClientId(), getPayAmountCallBack);


            } else {
                Toast.makeText(getActivity().getApplicationContext(),
                        "Select a Plan", Toast.LENGTH_SHORT).show();
            }

        }
    }

    private void redirectToPaypal(PaytermPaymentdatum paytermPaymentdatum) {
        mPaypalHelper.startPaypalActivity(paytermPaymentdatum.getFinalAmount().floatValue());
    }

    private void getPaytermsforSelPlan() {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(com.ronettv.mobile.MyApplication.API_URL)
                .setLogLevel(RestAdapter.LogLevel.NONE)
                .setConverter(new JSONPaytermConverter())
                .setClient(
                        new CustomUrlConnectionClient(com.ronettv.mobile.MyApplication.tenentId,
                                com.ronettv.mobile.MyApplication.basicAuth,
                                com.ronettv.mobile.MyApplication.contentType)).build();
        OBSClient client = restAdapter.create(OBSClient.class);
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
        mProgressDialog = new ProgressDialog(getActivity(),
                ProgressDialog.THEME_HOLO_DARK);
        mProgressDialog.setMessage("Connecting Server");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setOnCancelListener(new OnCancelListener() {

            public void onCancel(DialogInterface arg0) {
                if (mProgressDialog.isShowing())
                    mProgressDialog.dismiss();
                mIsReqCanceled = true;
            }
        });
        mProgressDialog.show();
        client.getPlanPayterms((mNewPlans.get(selGroupId).getId()) + "", getPlanPayterms);
    }

    public void onCameResult(int requestCode, int resultCode, Intent data) {
        //@Override
        //public void onActivityResult(int requestCode, int resultCode, Intent data) {
        /** Stop PayPalIntent Service... */
        getActivity().stopService(new Intent(mActivity, PayPalService.class));
        if (mConfirmDialog != null && mConfirmDialog.isShowing()) {
            mConfirmDialog.dismiss();
        }
        if (resultCode == Activity.RESULT_OK) {
            PaymentConfirmation confirm = data
                    .getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
            if (confirm != null) {
                try {
                    Log.i("OBSPayment", confirm.toJSONObject().toString(4));
                    /** Call OBS API for verification and payment record. */
                    OBSPaymentAsyncTask task = new OBSPaymentAsyncTask();
                    task.execute(confirm.toJSONObject().toString(4));
                } catch (JSONException e) {
                    Log.e("OBSPayment",
                            "an extremely unlikely failure occurred: ", e);
                }
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Log.i("OBSPayment", "The user canceled.");
            Toast.makeText(getActivity(), "The user canceled.", Toast.LENGTH_LONG)
                    .show();
        } else if (resultCode == PaymentActivity.RESULT_EXTRAS_INVALID) {
            Log.i("OBSPayment",
                    "An invalid Payment or PayPalConfiguration was submitted. Please see the docs.");
            Toast.makeText(getActivity(),
                    "An invalid Payment or PayPalConfiguration was submitted",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void buildPaytermList() {

        (mRootView.findViewById(R.id.f_my_pkg_exlv_my_plans)).setVisibility(View.GONE);
        (mRootView.findViewById(R.id.f_my_pkg_payterm_lv)).setVisibility(View.VISIBLE);

        TextView tv_title = (TextView) mRootView.findViewById(R.id.a_plan_tv_selpkg);
        tv_title.setText(R.string.choose_payterm);

        mPaytermLv = (ListView) mRootView.findViewById(R.id.f_my_pkg_payterm_lv);
        mListAdapter = null;
        if (mPayterms != null && mPayterms.size() > 0) {
            boolean isNewPlan = false;
            mListAdapter = new PaytermAdapter(getActivity(), mPayterms, isNewPlan);
            mPaytermLv.setAdapter(mListAdapter);
            mPaytermLv.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    // TODO Auto-generated method stub
                    RadioButton rb1 = (RadioButton) view
                            .findViewById(R.id.a_plan_payterm_row_rb);
                    if (null != rb1 && (!rb1.isChecked())) {
                        selPaytermId = mPayterms.get(position).getId();
                    } else {
                        selPaytermId = -1;
                    }
                }
            });
            Button b = (Button) mRootView.findViewById(R.id.a_plan_btn_submit);
            b.setText(R.string.subscribe);
            b.setEnabled(true);
        } else {
            mPaytermLv.setAdapter(null);
            Toast.makeText(getActivity(), "No Payterms for this Plan",
                    Toast.LENGTH_LONG).show();
        }
    }

    public void changePlan(String planid) {
        new ChangePlansAsyncTask().execute();
    }

    public void onBackPressed() {
        Button btn = (Button) mRootView.findViewById(R.id.a_plan_btn_submit);
        if (btn.getText().toString()
                .equalsIgnoreCase(getString(R.string.subscribe))) {
            //selGroupId = -1;
            selPaytermId = -1;
            (mRootView.findViewById(R.id.f_my_pkg_exlv_my_plans)).setVisibility(View.VISIBLE);
            (mRootView.findViewById(R.id.f_my_pkg_payterm_lv)).setVisibility(View.GONE);

            TextView tv_title = (TextView) mRootView
                    .findViewById(R.id.a_plan_tv_selpkg);
            tv_title.setText(R.string.choose_plan_change);

            btn.setText(R.string.next2);
            mExpListView = (ExpandableListView) mRootView
                    .findViewById(R.id.f_my_pkg_exlv_my_plans);

            if (mNewPlans != null && mNewPlans.size() > 0) {
                mExpListView = (ExpandableListView) mRootView
                        .findViewById(R.id.f_my_pkg_exlv_my_plans);
                listAdapter = new PackageAdapter(mActivity, mNewPlans);
                mExpListView.setAdapter(listAdapter);
                mExpListView
                        .setOnGroupClickListener(new OnGroupClickListener() {
                            @Override
                            public boolean onGroupClick(
                                    ExpandableListView parent, View v,
                                    int groupPosition, long id) {

                                RadioButton rb1 = (RadioButton) v
                                        .findViewById(R.id.plan_list_plan_rb);
                                if (null != rb1 && (!rb1.isChecked())) {
                                    selGroupId = groupPosition;
                                } else {
                                    selGroupId = -1;
                                }
                                return false;
                            }
                        });
            }
        } else if (btn.getText().toString()
                .equalsIgnoreCase(getString(R.string.next2))) {
            selGroupId = -1;
            TextView tv_title = (TextView) mRootView
                    .findViewById(R.id.a_plan_tv_selpkg);
            tv_title.setText(R.string.choose_plan_change);

            btn.setText(R.string.next);

            mExpListView = (ExpandableListView) mRootView
                    .findViewById(R.id.f_my_pkg_exlv_my_plans);

            if (mMyPlans != null && mMyPlans.size() > 0) {
                mExpListView = (ExpandableListView) mRootView
                        .findViewById(R.id.f_my_pkg_exlv_my_plans);
                listAdapter = new PackageAdapter(mActivity, mMyPlans);
                mExpListView.setAdapter(listAdapter);
                mExpListView
                        .setOnGroupClickListener(new OnGroupClickListener() {
                            @Override
                            public boolean onGroupClick(
                                    ExpandableListView parent, View v,
                                    int groupPosition, long id) {

                                RadioButton rb1 = (RadioButton) v
                                        .findViewById(R.id.plan_list_plan_rb);
                                if (null != rb1 && (!rb1.isChecked())) {
                                    selGroupId = groupPosition;
                                } else {
                                    selGroupId = -1;
                                }
                                return false;
                            }
                        });
            }
        } else
            getActivity().finish();
    }

    protected void UpdateUI() {
        selGroupId = -1;
        TextView tv_title = (TextView) mRootView
                .findViewById(R.id.a_plan_tv_selpkg);
        tv_title.setText(R.string.choose_plan_change);
        Button btn = (Button) mRootView.findViewById(R.id.a_plan_btn_submit);
        btn.setText(R.string.next);
        getPlansFromServer();
    }

    private List<PlanDatum> getPlanListFromJSON(String json) {
        Type t = new TypeToken<List<PlanDatum>>() {
        }.getType();
        return new Gson().fromJson(json, t);
    }

    public void logout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),
                AlertDialog.THEME_HOLO_LIGHT);
        builder.setIcon(R.drawable.ic_logo_confirm_dialog);
        builder.setTitle("Confirmation");
        builder.setMessage("Are you sure to Logout?");
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int buttonId) {
                    }
                });
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        // Clear shared preferences..
                        ((MyApplication) getActivity().getApplicationContext()).clearAll();
                        // close all activities..
                        Intent Closeintent = new Intent(getActivity(),
                                MainActivity.class);
                        Closeintent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        Closeintent.putExtra("LOGOUT", true);
                        startActivity(Closeintent);
                        getActivity().finish();
                    }
                });
        dialog.show();

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

    static class JSONConverter implements Converter {

        @Override
        public List<OrderDatum> fromBody(TypedInput typedInput, Type type)
                throws ConversionException {
            List<OrderDatum> ordersList = null;

            try {
                String json = com.ronettv.mobile.MyApplication.getJSONfromInputStream(typedInput
                        .in());

                JSONObject jsonObj;
                jsonObj = new JSONObject(json);
                JSONArray arrOrders = jsonObj.getJSONArray("clientOrders");
                ordersList = getOrdersFromJson(arrOrders.toString());

            } catch (IOException e) {
                Log.i(TAG, e.getMessage());
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return ordersList;
        }

        @Override
        public TypedOutput toBody(Object o) {
            return null;
        }

    }

    private class OBSPaymentAsyncTask extends
            AsyncTask<String, Void, ResponseObj> {
        JSONObject reqJson = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
                mProgressDialog = null;
            }
            mProgressDialog = new ProgressDialog(getActivity(),
                    ProgressDialog.THEME_HOLO_DARK);
            mProgressDialog.setMessage("Connecting Server...");
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setOnCancelListener(new OnCancelListener() {

                public void onCancel(DialogInterface arg0) {
                    if (mProgressDialog.isShowing())
                        mProgressDialog.dismiss();

                    Toast.makeText(getActivity(),
                            "Payment verification Failed.", Toast.LENGTH_LONG)
                            .show();
                    cancel(true);
                }
            });
            mProgressDialog.show();
        }

        @Override
        protected ResponseObj doInBackground(String... arg) {
            ResponseObj resObj = new ResponseObj();
            try {
                reqJson = new JSONObject(arg[0]);

                if (mApplication.isNetworkAvailable()) {
                    resObj = Utilities.callExternalApiPostMethod(
                            getActivity().getApplicationContext(),
                            "/payments/paypalEnquirey/"
                                    + mApplication.getClientId(), reqJson);
                } else {
                    resObj.setFailResponse(100, "Network error.");
                }
            } catch (JSONException e) {
                Log.e("Plans :PaymentCheck",
                        (e.getMessage() == null) ? "Json Exception" : e
                                .getMessage());
                e.printStackTrace();
                Toast.makeText(getActivity(),
                        "Invalid data: On PayPal Payment ", Toast.LENGTH_LONG)
                        .show();
            }
            if (mConfirmDialog != null && mConfirmDialog.isShowing()) {
                mConfirmDialog.dismiss();
            }
            return resObj;
        }

        @Override
        protected void onPostExecute(ResponseObj resObj) {

            super.onPostExecute(resObj);
            if (mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }

            if (resObj.getStatusCode() == 200) {
                if (resObj.getsResponse().length() > 0) {
                    JSONObject json;
                    try {
                        json = new JSONObject(resObj.getsResponse());
                        json = json.getJSONObject("changes");
                        if (json != null) {
                            String mPaymentStatus = json
                                    .getString("paymentStatus");
                            if (mPaymentStatus.equalsIgnoreCase("Success")) {
                                mApplication.setBalance((float) json
                                        .getLong("totalBalance"));
                                Toast.makeText(getActivity(),
                                        "Payment Verification Success",
                                        Toast.LENGTH_LONG).show();
                                //Book paln in OBS
                                changePlan(mNewPlans.get(selGroupId).toString());


                            } else if (mPaymentStatus.equalsIgnoreCase("Fail")) {
                                Toast.makeText(getActivity(),
                                        "Payment Verification Failed",
                                        Toast.LENGTH_LONG).show();
                            }
                        }

                    } catch (JSONException e) {
                        Toast.makeText(getActivity(),
                                "Server Error", Toast.LENGTH_LONG).show();
                        Log.i("VodMovieDetailsActivity",
                                "JsonEXception at payment verification");
                    } catch (NullPointerException e) {
                        Toast.makeText(getActivity(),
                                "Server Error  ", Toast.LENGTH_LONG).show();
                        Log.i("VodMovieDetailsActivity",
                                "Null PointerEXception at payment verification");
                    }
                }
            } else {
                Toast.makeText(getActivity(), "Server Error",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private class ChangePlansAsyncTask extends
            AsyncTask<Void, Void, ResponseObj> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            boolean showProgressDialog = false;
            if (mProgressDialog != null)
                if (mProgressDialog.isShowing()) {
                    // do nothing
                } else {
                    showProgressDialog = true;
                }
            else {
                showProgressDialog = true;
            }
            if (showProgressDialog) {
                mProgressDialog = new ProgressDialog(getActivity(),
                        ProgressDialog.THEME_HOLO_DARK);
                mProgressDialog.setMessage("Processing Order...");
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
        }

        @Override
        protected ResponseObj doInBackground(Void... params) {
            PlanDatum plan = mNewPlans.get(selGroupId);
            ResponseObj resObj = new ResponseObj();
            if (Utilities.isNetworkAvailable(getActivity()
                    .getApplicationContext())) {
                String value = mPayterms.get(selPaytermId).getDuration();
                for (Subscriptiondatum data : mSubscription) {
                    String value1 = data.getSubscriptionPeriod();
                    System.out.println("subscriptiontype:" + value1);
                    if (value.equalsIgnoreCase(value1)) {
                        subId = data.getId();
                    }
                }
                HashMap<String, String> map = new HashMap<String, String>();
                Date discDate = new Date();
                SimpleDateFormat df = new SimpleDateFormat("dd MMMM yyyy",
                        new Locale("en"));
                String strStartDate = df.format(discDate);
                String strDiscDate = df.format(discDate);
                map.put("TagURL", "/orders/changePlan/" + orderId);
                map.put("planCode", plan.getId().toString());
                map.put("dateFormat", "dd MMMM yyyy");
                map.put("locale", "en");
                map.put("contractPeriod", String.valueOf(subId));
                map.put("isNewplan", "false");
                map.put("start_date", strStartDate);
                map.put("disconnectionDate", strDiscDate);
                map.put("disconnectReason", "Not Interested");
                map.put("billAlign", "false");
                map.put("paytermCode", mPayterms.get(selPaytermId).getPaytermtype());
                resObj = Utilities.callExternalApiPutMethod(getActivity()
                        .getApplicationContext(), map);
            } else {
                resObj.setFailResponse(100, NETWORK_ERROR);
            }
            return resObj;
        }

        @Override
        protected void onPostExecute(ResponseObj resObj) {
            super.onPostExecute(resObj);
            if (mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            if (resObj.getStatusCode() == 200) {
                // update balance config n Values
                Toast.makeText(mActivity, "Plan Change Success",
                        Toast.LENGTH_LONG).show();
                Intent intent = new Intent(mActivity, DoBGTasksService.class);
                intent.putExtra(DoBGTasksService.TASK_ID,
                        DoBGTasks.UPDATESERVICES_CONFIGS.ordinal());
                mActivity.startService(intent);
                UpdateUI();
                //CheckBalancenGetData();
            } else {
                Toast.makeText(getActivity(), resObj.getsErrorMessage(),
                        Toast.LENGTH_LONG).show();
            }

            Button b = (Button) mRootView.findViewById(R.id.a_plan_btn_submit);
            b.setEnabled(true);
        }
    }
}

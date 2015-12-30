package com.ronettv.retrofit;

import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;

public interface OBSClient {

    //https://41.76.90.173:8181/obsplatform/api/v1

    /**
     * getClientConfigDataSync get method used to get clientData n configData
     * Synchronously
     */
    @GET("/mediadevices/client/{clientId}")
    com.ronettv.data.ClientnConfigDatum getClientnConfigDataSync(
            @Path("clientId") String clientId);

    /**
     * getMediaDevice get method used to get client details based on device id
     * Async'ly
     */
    @GET("/mediadevices/{device}")
    void getMediaDevice(@Path("device") String device, Callback<com.ronettv.data.DeviceDatum> cb);

    @GET("/orders/{clientId}/activeplans")
    void getActivePlans(@Path("clientId") String clientId,
                        Callback<List<com.ronettv.data.ActivePlanDatum>> cb);

    @GET("/subscriptions")
    void getSubscription(Callback<List<com.ronettv.data.Subscriptiondatum>> getContractCallback);

    @GET("/clients/template")
    void getTemplate(Callback<com.ronettv.data.TemplateDatum> cb);

    @GET("/plans?planType=prepaid")
    void getPrepaidPlans(Callback<List<com.ronettv.data.PlanDatum>> cb);

    @GET("/orders/{planid}/template?template=true")
    void getPlanPayterms(@Path("planid") String planid, Callback<List<com.ronettv.data.Paytermdatum>> cb);

    @GET("/chargecode/{priceId}/{clientId}")
    void getPayAmountforPayterm(@Path("priceId") String priceId, @Path("clientId") String clientId, Callback<com.ronettv.data.PaytermPaymentdatum> cb);

    @GET("/planservices/{clientId}?serviceType=TV")
    ArrayList<com.ronettv.data.ServiceDatum> getPlanServicesSync(
            @Path("clientId") String clientId);

    @GET("/planservices/{clientId}?serviceType=TV")
    void getPlanServices(@Path("clientId") String clientId,
                         Callback<List<com.ronettv.data.ServiceDatum>> cb);

    @GET("/epgprogramguide/{channelName}/{reqDate}")
    void getEPGDetails(@Path("channelName") String channelName,
                       @Path("reqDate") String reqDate, Callback<com.ronettv.data.EPGData> cb);

    @GET("/assets")
    void getPageCountAndMediaDetails(@Query("filterType") String category,
                                     @Query("pageNo") String pageNo, @Query("deviceId") String deviceId, /*@Query("clientType") String clientType,*/
                                     Callback<com.ronettv.data.MediaDetailRes> cb);

    @GET("/assetdetails/{mediaId}")
    void getMediaDetails(@Path("mediaId") String mediaId,
                         @Query("eventId") String eventId,
                         @Query("deviceId") String deviceId,
                         Callback<com.ronettv.data.MediaDetailsResDatum> cb);

    @GET("/clients/{clientId}")
    void getClinetDetails(@Path("clientId") String clientId,
                          Callback<com.ronettv.data.ClientDatum> cb);

    @GET("/orders/{clientId}/orders")
    void getClinetPackageDetails(@Path("clientId") String clientId,
                                 Callback<List<com.ronettv.data.OrderDatum>> cb);

    /**
     * getMediaDevice put method used to update the device status for the client
     * Async'ly
     *//*
    @PUT("/mediadevices/{device}")
	ResourceIdentifier updateAppStatus(@Path("device") String device,
			@Body StatusReqDatum request);
*/

    /**
     * sendPasswordToMail post method used to initiate the server process of
     * sending mail to specified MailId Sync'ly. Usage: DoBGTasksService
     */
    @POST("/selfcare/forgotpassword")
    com.ronettv.data.ResForgetPwd sendPasswordToMail(@Body com.ronettv.data.SenderMailId senderMailId);

    /**
     * changePassword put method used to reset the password the server process
     * of sending mail to specified MailId Sync'ly. Usage: DoBGTasksService
     */
    @PUT("/selfcare/resetpassword")
    com.ronettv.data.ResForgetPwd resetPassword(@Body com.ronettv.data.ResetPwdDatum restPwdData);

}

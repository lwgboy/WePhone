package tools.unrealbot.aj.wephone;

/**
 * Created by ashokjoshi on 4/10/2017.
 */

import android.app.Activity;
//import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.avaya.clientservices.call.Call;
import com.avaya.clientservices.call.CallEndReason;
import com.avaya.clientservices.call.CallException;
import com.avaya.clientservices.call.CallListener;
import com.avaya.clientservices.call.CallService;
import com.avaya.clientservices.call.VideoChannel;
import com.avaya.clientservices.client.Client;
import com.avaya.clientservices.client.ClientConfiguration;
import com.avaya.clientservices.client.ClientListener;
import com.avaya.clientservices.client.CreateUserCompletionHandler;
import com.avaya.clientservices.client.UserCreatedException;
import com.avaya.clientservices.common.ConnectionPolicy;
import com.avaya.clientservices.common.SignalingServer;
import com.avaya.clientservices.credentials.Challenge;
import com.avaya.clientservices.credentials.CredentialCompletionHandler;
import com.avaya.clientservices.credentials.CredentialProvider;
import com.avaya.clientservices.credentials.UserCredential;
import com.avaya.clientservices.user.User;
import com.avaya.clientservices.user.UserConfiguration;
import com.avaya.clientservices.provider.sip.SIPUserConfiguration;
import com.avaya.clientservices.user.UserRegistrationListener;

import java.util.List;
import java.util.UUID;

public class MySDKManager implements ClientListener, CredentialProvider, UserRegistrationListener, CallListener{
    private String myTag ="AJ";
    private Activity activity;
    public static final String PREFS_NAME= "Wephone_Client";
    private String userInstanceId;
    private Client myClient;
    private User myUser;
    private UserConfiguration userConfig;
    private String userId = null;
    private String password =null;
    private String domain =null;

    //Define Broadcast Strings, They wil b eused to Broadcast events, all may not be used
    private String AUTH_CHALLENGE_RECEIVED = "onAuthenticationChallenge";
    private String CRED_ACCEPTED="onCredentialAccepted";
    private String AUTH_CHALLENGE_CANCELLED="nAuthenticationChallengeCancelled";
    private String CLIENT_SHUTDOWN="onClientShutdown";
    private String CLIENT_USER_CREATED="onClientUserCreated";
    private String CLIENT_USER_REMOVED ="onClientUserRemoved";
    private String REGISTER_INPROGRESS ="onUserRegistrationInProgress";
    private String REGISTER_SUCCESS="onUserRegistrationSuccessful";
    private String REGISTER_FAIL="onUserRegistrationFailed";
    private String REGISTER_ALL_SUCCESS ="onUserAllRegistrationsSuccessful";
    private String REGISTER_ALL_FAIL= "onUserAllRegistrationsFailed";
    private String UNREGISTER_INPROGRESS ="onUserUnregistrationInProgress";
    private String UNREGISTER_SUCCESS = "onUserUnregistrationSuccessful";
    private String UNREGISTER_FAIL ="onUserUnregistrationFailed";
    private String UNREGISTER_COMPLETE= "onUserUnregistrationComplete";
    private String CALL_START = "onCallStarted";
    private String REMOTE_CALL_ALERTING = "onCallRemoteAlerting";
    private String CALL_REDIRECTED ="onCallRedirected";
    private String CALL_QUEUED = "onCallQueued";
    private String CALL_ESTABLISHED = "onCallEstablished";
    private String REMOTE_ADDRESS_CHANGE ="onCallRemoteAddressChanged";
    private String CALL_HELD ="onCallHeld";
    private String CALL_UNHOLD ="onCallUnheld";
    private String CALL_HELD_REMOTELY ="onCallHeldRemotely";
    private String CALL_UNHELD_REMOTELY ="onCallUnheldRemotely";
    private String CALL_JOINED="onCallJoined";
    private String CALL_END ="onCallEnded";
    private String CALL_FAILED ="onCallFailed";
    private String CALL_DENIED = "onCallDenied";
    private String CALL_IGNORED = "onCallIgnored";
    private String CALL_AUDIOMUTE_CHANGED="onCallAudioMuteStatusChanged";
    private String CALL_CONF_STATUS_CHANGED="onCallConferenceStatusChanged";
    private String CALL_CAPABILITIES_CHANGED = "onCallCapabilitiesChanged";
    private String CALL_SERVICE_AVAIL="onCallServiceAvailable";
    private String CALL_SERVICE_UNAVAIL= "onCallServiceUnavailable";













    public MySDKManager(Activity activity){
        Log.i(myTag, "Creating a new MySDKManager object");
        this.activity=activity;

    }



    public void CreateClientConfig() {

        String productName = activity.getResources().getString(R.string.productName);
        String productVersion = activity.getResources().getString(R.string.productVersion);
        String buildNumber = activity.getResources().getString(R.string.buildNumber);
        String vendorName = activity.getResources().getString(R.string.vendorName);
        String dataDirectory = activity.getResources().getString(R.string.dataDirectory);
        Log.i(myTag, "Read the Resources, it will be used to create a ClientConfiguration object with parameters");
        Log.i(myTag, "Product Name is: "+productName);
        Log.i(myTag," Product Version is: "+productVersion);
        Log.i(myTag," Build Number: "+buildNumber+ " Vendor Name: "+vendorName+" Data Directory is: "+dataDirectory);
        ClientConfiguration MyClientConfig= new ClientConfiguration(dataDirectory,productName,productVersion, Build.MODEL,Build.VERSION.RELEASE,buildNumber,vendorName);
        Log.i(myTag," Trying to generate a UUID for this client");
        SharedPreferences sharedPref = activity.getApplication().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        userInstanceId = sharedPref.getString("UUID",null);
        if(userInstanceId!=null) {
            Log.i(myTag, "UUID is already available with a value of: " + userInstanceId);
        }
        else {
            userInstanceId=PersistUUID();
            Log.i(myTag, this.getClass().getCanonicalName()+"Created a new UUID as its not present with a value of: "+userInstanceId);
        }
        MyClientConfig.setUserAgentName(userInstanceId);
        myClient = new Client(MyClientConfig, activity.getApplication(),this );

       }

    public void createUserConfiguration(String userId, String password, String domain, boolean useTls, String serverName, int serverPort){
        this.userId=userId;
        this.password=password;
        this.domain=domain;
        Log.i(myTag, this.getClass().getCanonicalName()+"Trying to create the UserConfiguration object");
        userConfig = new UserConfiguration();
        Log.i(myTag, this.getClass().getCanonicalName()+"Retrieving the SIPUserConfiguration object form the UserConfiguration object");
        SIPUserConfiguration sipUserConfig = userConfig.getSIPUserConfiguration();
        sipUserConfig.setEnabled(true);
        sipUserConfig.setUserId(userId);
        sipUserConfig.setDomain(domain);
        Log.i(myTag,this.getClass().getCanonicalName()+"Setting a Signaling server with TLS/TCP");
        SignalingServer.TransportType tType = useTls ? SignalingServer.TransportType.TLS : SignalingServer.TransportType.TCP;
        SignalingServer.FailbackPolicy myPolicy = SignalingServer.FailbackPolicy.AUTOMATIC;
        SignalingServer mySigServer =  new SignalingServer(tType, serverName, serverPort, myPolicy);
        sipUserConfig.setConnectionPolicy(new ConnectionPolicy(mySigServer));
        Log.i(myTag, this.getClass().getCanonicalName()+"Successfully set the ConnectionPolicy object");
        Log.i(myTag, this.getClass().getCanonicalName()+"Setting the Credentials using the CredentialProvider method");
        sipUserConfig.setCredentialProvider(this);
        Log.i(myTag, this.getClass().getCanonicalName()+"Finished the SIP USer configuration, saving it to the UserConfiguration object");
        userConfig.setSIPUserConfiguration(sipUserConfig);
        Log.i(myTag, this.getClass().getCanonicalName()+" User configuration completed and saved in the UserConfiguration object ");
        Log.i(myTag, this.getClass().getCanonicalName()+"Going to create the User Object now");
    }

    public void registerUser(){
        myClient.createUser(userConfig, new CreateUserCompletionHandler() {
            @Override
            public void onSuccess(User user) {
                myUser=user;
                myUser.addRegistrationListener(MySDKManager.this);
                myUser.start();
                getCallReady();
            }

            @Override
            public void onError(UserCreatedException e) {
                Log.i(myTag, this.getClass().getCanonicalName()+"Failed to create the user, exception follows");
                e.printStackTrace();

            }
        });
    }

    private void getCallReady(){
        CallService clsrv = myUser.getCallService();
        Call myCall=clsrv.createCall();
        myCall.addListener(this);
        myCall.start();

    }

    // Method to create a UUID and store it in a persistence object
    private String PersistUUID() {
        String userInstanceId =null;
        userInstanceId= UUID.randomUUID().toString();
        Log.i(myTag, this.getClass().getCanonicalName()+"Generated a UUID for this client, with a value of : "+userInstanceId);
        SharedPreferences sharedPref = activity.getApplication().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor= sharedPref.edit();
        editor.putString("UUID", userInstanceId );
        editor.commit();
        return userInstanceId;

    }

    // These methods are implemented for the CredentialProvider interface
    @Override
    public void onAuthenticationChallenge(Challenge challenge, CredentialCompletionHandler credentialCompletionHandler) {
        Log.i(myTag, this.getClass().getCanonicalName()+"Authentication Challenge received for credentials");
        broadcast(AUTH_CHALLENGE_RECEIVED);
        Log.i(myTag, challenge.toString());
        UserCredential usCred = new UserCredential(userId,password,domain);
        credentialCompletionHandler.onCredentialProvided(usCred);



    }

    @Override
    public void onCredentialAccepted(Challenge challenge) {
        Log.i(myTag, this.getClass().getCanonicalName()+"Credentials have been accepted that were passed as authentication challenge");

    }

    @Override
    public void onAuthenticationChallengeCancelled(Challenge challenge) {
        Log.i(myTag, this.getClass().getCanonicalName()+"Authentication Challenge in cancelled");

    }
    // Finished implemented  methods for the CredentialProvider interface

    //These methods are implemented for the ClientListener Interface

    @Override
    public void onClientShutdown(Client client) {
        Log.i(myTag,this.getClass().getCanonicalName()+"onClientShutdown -->The ClientConfiguration object has been shutdown");

    }

    @Override
    public void onClientUserCreated(Client client, User user) {
        Log.i(myTag, this.getClass().getCanonicalName()+"onClientUserCreated -->The ClientConfiguration object has been created");

    }

    @Override
    public void onClientUserRemoved(Client client, User user) {

        Log.i(myTag, this.getClass().getCanonicalName()+"onClientUserRemoved -->The ClientConfiguration object has been removed");

    }
    //Finished implementing methods for the ClientListener Interface

    //Implementing the methods for UserRegistrationListener Interface
    @Override
    public void onUserRegistrationInProgress(User user, SignalingServer signalingServer) {
        Log.i(myTag, this.getClass().getCanonicalName()+"User Registration in Progress");

    }

    @Override
    public void onUserRegistrationSuccessful(User user, SignalingServer signalingServer) {
        Log.i(myTag, this.getClass().getCanonicalName()+"User Registration in successful");
        broadcast(REGISTER_SUCCESS);
    }

    @Override
    public void onUserRegistrationFailed(User user, SignalingServer signalingServer, Exception e) {
        broadcast(REGISTER_FAIL);
        Log.i(myTag, this.getClass().getCanonicalName()+"User Registration has failed");
        e.printStackTrace();
    }

    @Override
    public void onUserAllRegistrationsSuccessful(User user) {
        Log.i(myTag, this.getClass().getCanonicalName()+"All the User Registrations are successful");
    }

    @Override
    public void onUserAllRegistrationsFailed(User user, boolean b) {
        Log.i(myTag, this.getClass().getCanonicalName()+"All User Registrations have failed");
    }

    @Override
    public void onUserUnregistrationInProgress(User user, SignalingServer signalingServer) {
        Log.i(myTag, this.getClass().getCanonicalName()+"UNREGISTER in Progress");
    }

    @Override
    public void onUserUnregistrationSuccessful(User user, SignalingServer signalingServer) {
        Log.i(myTag, "UNREGISTER is successful");
    }

    @Override
    public void onUserUnregistrationFailed(User user, SignalingServer signalingServer, Exception e) {
        Log.i(myTag, "UNREGISTER has failed");
        e.printStackTrace();
    }

    @Override
    public void onUserUnregistrationComplete(User user) {
        Log.i(myTag, "UNREGISTER is completed");
    }
    //Finished implementing the methods for UserRegistrationListener Interface

    //Implementing the methods for CallListener Interface

    @Override
    public void onCallStarted(Call call) {
        Log.i(myTag," Call started");
    }

    @Override
    public void onCallRemoteAlerting(Call call, boolean b) {
        Log.i(myTag, "Remote Call is Alerting now");

    }

    @Override
    public void onCallRedirected(Call call) {
        Log.i(myTag, "The call was redirected");

    }

    @Override
    public void onCallQueued(Call call) {
        Log.i(myTag," Call is queued");

    }

    @Override
    public void onCallEstablished(Call call) {
        Log.i(myTag,"Call has been established");
    }

    @Override
    public void onCallRemoteAddressChanged(Call call, String s, String s1) {
        Log.i(myTag,"The call's remote address has changed");
    }

    @Override
    public void onCallHeld(Call call) {
        Log.i(myTag, "Call is put in a Hold state");
    }

    @Override
    public void onCallUnheld(Call call) {
        Log.i(myTag," Call is now on unhold");
    }

    @Override
    public void onCallHeldRemotely(Call call) {

    }

    @Override
    public void onCallUnheldRemotely(Call call) {

    }

    @Override
    public void onCallJoined(Call call) {

    }

    @Override
    public void onCallEnded(Call call, CallEndReason callEndReason) {

    }

    @Override
    public void onCallFailed(Call call, CallException e) {

    }

    @Override
    public void onCallDenied(Call call) {

    }

    @Override
    public void onCallIgnored(Call call) {

    }

    @Override
    public void onCallAudioMuteStatusChanged(Call call, boolean b) {

    }

    @Override
    public void onCallVideoChannelsUpdated(Call call, List<VideoChannel> list) {

    }

    @Override
    public void onCallIncomingVideoAddRequestReceived(Call call) {

    }

    @Override
    public void onCallIncomingVideoAddRequestAccepted(Call call, VideoChannel videoChannel) {

    }

    @Override
    public void onCallIncomingVideoAddRequestDenied(Call call) {

    }

    @Override
    public void onCallIncomingVideoAddRequestTimedOut(Call call) {

    }

    @Override
    public void onCallConferenceStatusChanged(Call call, boolean b) {

    }

    @Override
    public void onCallCapabilitiesChanged(Call call) {

    }

    @Override
    public void onCallServiceAvailable(Call call) {

    }

    @Override
    public void onCallServiceUnavailable(Call call) {

    }

    @Override
    public void onCallParticipantMatchedContactsChanged(Call call) {

    }

    @Override
    public void onCallDigitCollectionPlayDialTone(Call call) {

    }

    @Override
    public void onCallDigitCollectionCompleted(Call call) {

    }


    //BROADCAST METHODS

    public void broadcast (String message){
    Log.i(myTag, getClass().getCanonicalName()+"Sending a Broadcast message: "+message);
        Intent it = new Intent();
        it.setAction("fromSDKManager");
        it.putExtra("message" ,message);
        LocalBroadcastManager.getInstance(activity).sendBroadcast(it);



    }




}

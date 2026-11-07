package com.kays.coderelay;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.widget.*;
import java.util.List;

public class SecurityActivity extends Activity {
    private LinearLayout results;
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private TextView add(LinearLayout root,String val,int size){
        TextView t=new TextView(this);t.setText(val);t.setTextSize(size);
        t.setTextColor(Color.rgb(28,40,65));t.setPadding(0,dp(9),0,dp(9));root.addView(t);return t;
    }
    private Button button(LinearLayout root,String title,Runnable action){
        Button b=new Button(this);b.setText(title);b.setAllCaps(false);root.addView(b);b.setOnClickListener(v->action.run());return b;
    }
    private void open(String action){
        try{startActivity(new Intent(action));}
        catch(RuntimeException e){try{startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(RuntimeException ignored){}}
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        ScrollView sc=new ScrollView(this);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(20),dp(18),dp(24));
        sc.addView(root);setContentView(sc);
        add(root,"Kay's Signal Shield",26);
        add(root,"Read-only Android security review. This DOES NOT detect wiretaps, hacking in general, or government monitoring. Normal settings can be legitimate; an all-clear result cannot prove a phone is safe.",14);
        button(root,"SCAN PHONE SECURITY",()->scan());
        results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);root.addView(results);
        add(root,"For a deeper review, run Google Play Protect, install OS updates, check signed-in Google devices, and ask your carrier about SIM swaps or changes to your account.",14);
        button(root,"Open Android Security Settings",()->open(Settings.ACTION_SECURITY_SETTINGS));
    }
    private void finding(String title,String status,String guidance,String settings){
        add(results,title+"\n"+status+"\n"+guidance,15);
        if(settings!=null)button(results,"Review "+title+" in Settings",()->open(settings));
    }
    private void scan(){
        results.removeAllViews();
        boolean on=getSharedPreferences(MainActivity.PREFS,MODE_PRIVATE).getBoolean(MainActivity.ON,false);
        finding("This app's SMS forwarding",on?"ENABLED":"DISABLED",
            on?"The Code Relay forwarding switch is on. Disable it on the main screen if you do not want codes relayed.":"Code Relay is not currently forwarding messages.",null);
        try{
            ConnectivityManager cm=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            Network net=cm==null?null:cm.getActiveNetwork();
            NetworkCapabilities caps=net==null?null:cm.getNetworkCapabilities(net);
            finding("Active default-network VPN",
                caps==null?"UNAVAILABLE":caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)?"VPN PRESENT":"NO VPN DETECTED",
                "A VPN can be normal and is not proof of surveillance; only the current default network is checked.",Settings.ACTION_VPN_SETTINGS);
        }catch(RuntimeException e){finding("VPN","UNAVAILABLE","Review VPN settings manually.",Settings.ACTION_VPN_SETTINGS);}
        try{
            AccessibilityManager am=(AccessibilityManager)getSystemService(Context.ACCESSIBILITY_SERVICE);
            List<AccessibilityServiceInfo> enabled=am==null?null:am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
            finding("Accessibility services",enabled==null?"UNAVAILABLE":enabled.size()+" ENABLED",
                "Review enabled services you don't recognize. Assistive services are often legitimate.",Settings.ACTION_ACCESSIBILITY_SETTINGS);
        }catch(RuntimeException e){finding("Accessibility services","UNAVAILABLE","Review manually.",Settings.ACTION_ACCESSIBILITY_SETTINGS);}
        try{
            DevicePolicyManager dm=(DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
            int n=dm==null||dm.getActiveAdmins()==null?0:dm.getActiveAdmins().size();
            finding("Device administrator apps",n+" REPORTED","Review device admin apps. Work and security apps may legitimately use this.",Settings.ACTION_SECURITY_SETTINGS);
        }catch(RuntimeException e){finding("Device administrators","UNAVAILABLE","Review manually.",Settings.ACTION_SECURITY_SETTINGS);}
        try{
            String raw=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");
            finding("Notification reading apps",raw==null?"UNAVAILABLE":raw.isEmpty()?"NONE REPORTED":"ACCESS PRESENT",
                "Review which apps may read notifications, including sign-in codes.",Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        }catch(RuntimeException e){finding("Notification listeners","UNAVAILABLE","Review manually.",Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);}
        finding("Android security patch",Build.VERSION.SECURITY_PATCH,
            "Compare the patch date with your phone manufacturer's latest update.",Settings.ACTION_SECURITY_SETTINGS);
        finding("Carrier-level wiretapping","NOT TESTABLE",
            "An ordinary Android app cannot inspect carrier interception, identify investigators, or determine whether someone is following you.",null);
    }
}

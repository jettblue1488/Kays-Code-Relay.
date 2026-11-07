package com.kays.coderelay;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    public static final String PREFS = "relay";
    public static final String SOURCE = "source";
    public static final String DEST = "destination";
    public static final String SIM = "subscription";
    public static final String ON = "forward_enabled";
    public static final String HISTORY = "history";
    private EditText sourceInput, destInput;
    private Spinner simSpinner;
    private Switch forwardSwitch;
    private TextView status, history;
    private final List<Integer> simIds = new ArrayList<>();
    private SharedPreferences prefs;
    private int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
    private TextView text(String value, int size) {
        TextView t=new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(Color.rgb(28,40,65));
        t.setPadding(0,dp(7),0,dp(7)); return t;
    }
    private Button button(String value) {
        Button b=new Button(this); b.setText(value); b.setAllCaps(false); b.setMinHeight(dp(48)); return b;
    }
    private EditText field(String hint, String value) {
        EditText e=new EditText(this); e.setHint(hint); e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_PHONE); e.setText(value); return e;
    }
    @Override public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        prefs=getSharedPreferences(PREFS,MODE_PRIVATE);
        ScrollView scroll=new ScrollView(this);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(24),dp(18),dp(32));
        root.setBackgroundColor(Color.rgb(247,249,252));
        scroll.addView(root); setContentView(scroll);
        root.addView(text("Kay's Code Relay",28));
        root.addView(text("Private SMS history and optional forwarding",14));
        root.addView(text("Only texts delivered to a SIM/eSIM inside THIS Android device can be seen or forwarded. Typing an old number does not connect to it.",14));
        root.addView(text("Receiving number (label only)",16));
        sourceInput=field("(area code) phone number",prefs.getString(SOURCE,""));root.addView(sourceInput);
        root.addView(text("Forward new SMS to",16));
        destInput=field("(area code) destination number",prefs.getString(DEST,""));root.addView(destInput);
        root.addView(text("Receiving SIM or eSIM",16));
        simSpinner=new Spinner(this); root.addView(simSpinner);
        Button perms=button("1. Grant required permissions");root.addView(perms);
        perms.setOnClickListener(v->askPermissions());
        Button refreshSims=button("2. Refresh SIM list");root.addView(refreshSims);
        refreshSims.setOnClickListener(v->loadSims());
        Button save=button("3. Save number and SIM settings");root.addView(save);
        save.setOnClickListener(v->saveSettings());
        forwardSwitch=new Switch(this);
        forwardSwitch.setText("Automatically forward new SMS (OFF by default)");
        forwardSwitch.setTextSize(16);
        forwardSwitch.setPadding(0,dp(15),0,dp(15));
        root.addView(forwardSwitch);
        forwardSwitch.setChecked(prefs.getBoolean(ON,false));
        forwardSwitch.setOnCheckedChangeListener((b,checked)->{
            if(checked){
                if(!ready()){
                    forwardSwitch.setChecked(false);
                    Toast.makeText(this,"Grant permissions, save numbers and choose a receiving SIM first",Toast.LENGTH_LONG).show();
                    return;
                }
                new AlertDialog.Builder(this)
                    .setTitle("Enable SMS forwarding?")
                    .setMessage("New texts received on the selected SIM, including sign-in codes, will be sent as SMS to the destination number. This may cost money and grant account access to whoever receives them. Forwarding is visible and can be disabled at any time.")
                    .setNegativeButton("Cancel",(d,w)->forwardSwitch.setChecked(false))
                    .setPositiveButton("Enable",(d,w)->{prefs.edit().putBoolean(ON,true).apply();updateStatus();})
                    .show();
            } else {prefs.edit().putBoolean(ON,false).apply();updateStatus();}
        });
        status=text("",14);root.addView(status);
        Button security=button("SCAN PHONE SECURITY — Kay's Signal Shield");root.addView(security);
        security.setOnClickListener(v->startActivity(new Intent(this,SecurityActivity.class)));
        root.addView(text("Recent received messages (stored on this device)",17));
        history=text("",14);root.addView(history);
        Button historyRefresh=button("Refresh message history");root.addView(historyRefresh);
        historyRefresh.setOnClickListener(v->showHistory());
        Button clear=button("Clear local message history");root.addView(clear);
        clear.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("Delete local message history?")
                .setNegativeButton("Cancel",null)
                .setPositiveButton("Delete",(d,w)->{prefs.edit().remove(HISTORY).apply();showHistory();}).show());
        root.addView(text("Security note: This app cannot receive SMS from disconnected numbers or remotely access another phone. The security scan cannot detect carrier wiretaps or identify government surveillance.",13));
        loadSims();updateStatus();showHistory();
    }
    private static String normalize(String n){
        String digits=n.replaceAll("[^0-9]","");
        if(digits.length()==11 && digits.startsWith("1")) digits=digits.substring(1);
        return digits.length()==10 ? digits : null;
    }
    private void askPermissions() {
        List<String> want=new ArrayList<>();
        for(String p:new String[]{Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE}){
            if(checkSelfPermission(p)!=PackageManager.PERMISSION_GRANTED)want.add(p);
        }
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            want.add(Manifest.permission.POST_NOTIFICATIONS);
        if(want.isEmpty()){Toast.makeText(this,"Permissions already granted",Toast.LENGTH_SHORT).show();loadSims();}
        else requestPermissions(want.toArray(new String[0]),101);
    }
    @Override public void onRequestPermissionsResult(int code,String[] names,int[] results){
        super.onRequestPermissionsResult(code,names,results);
        loadSims(); updateStatus();
    }
    @SuppressWarnings("MissingPermission")
    private void loadSims(){
        simIds.clear(); List<String> labels=new ArrayList<>();
        labels.add("Select an active SIM");simIds.add(-1);
        if(checkSelfPermission(Manifest.permission.READ_PHONE_STATE)==PackageManager.PERMISSION_GRANTED){
            try{
                SubscriptionManager m=(SubscriptionManager)getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                List<SubscriptionInfo> sims=m==null?null:m.getActiveSubscriptionInfoList();
                if(sims!=null)for(SubscriptionInfo s:sims){
                    simIds.add(s.getSubscriptionId());
                    labels.add("SIM "+s.getSimSlotIndex()+": "+s.getDisplayName()+" (ID "+s.getSubscriptionId()+")");
                }
            }catch(SecurityException ignored){}
        }
        simSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));
        int saved=prefs.getInt(SIM,-1);
        for(int i=0;i<simIds.size();i++)if(simIds.get(i)==saved)simSpinner.setSelection(i);
        if(simIds.size()==1)Toast.makeText(this,"No SIM visible. Grant Phone permission and refresh.",Toast.LENGTH_LONG).show();
    }
    private void saveSettings(){
        String src=normalize(sourceInput.getText().toString());
        String dest=normalize(destInput.getText().toString());
        int sim=simIds.get(simSpinner.getSelectedItemPosition());
        if(src==null||dest==null||src.equals(dest)||sim<0){
            Toast.makeText(this,"Enter two different 10-digit numbers and select an active SIM",Toast.LENGTH_LONG).show();return;
        }
        prefs.edit().putString(SOURCE,src).putString(DEST,dest).putInt(SIM,sim).putBoolean(ON,false).apply();
        forwardSwitch.setChecked(false);
        Toast.makeText(this,"Saved. Forwarding is off until you enable it.",Toast.LENGTH_LONG).show();
        updateStatus();
    }
    private boolean ready(){
        return normalize(prefs.getString(SOURCE,""))!=null
            && normalize(prefs.getString(DEST,""))!=null
            && prefs.getInt(SIM,-1)>=0
            && checkSelfPermission(Manifest.permission.RECEIVE_SMS)==PackageManager.PERMISSION_GRANTED
            && checkSelfPermission(Manifest.permission.SEND_SMS)==PackageManager.PERMISSION_GRANTED
            && (Build.VERSION.SDK_INT<33||checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED);
    }
    private void updateStatus(){
        status.setText("SMS forwarding: "+(prefs.getBoolean(ON,false)?"ON":"OFF")
            +"\nReceiving SIM ID: "+prefs.getInt(SIM,-1)
            +"\nSMS permissions required. Text history is stored locally; no cloud account.");
    }
    private void showHistory(){
        String val=prefs.getString(HISTORY,"");
        history.setText(val.isEmpty()?"No messages have been received by this app yet.":val);
    }
    @Override protected void onResume(){super.onResume();if(prefs!=null){updateStatus();showHistory();}}
}

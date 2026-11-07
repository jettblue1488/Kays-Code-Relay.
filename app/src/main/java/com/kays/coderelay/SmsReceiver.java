package com.kays.coderelay;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import java.text.DateFormat;
import java.util.Date;

/** Local SMS processing for a selected active receiving SIM only. Not remote interception. */
public class SmsReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent intent){
        if(!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction()))return;
        SharedPreferences p=c.getSharedPreferences(MainActivity.PREFS,Context.MODE_PRIVATE);
        int selected=p.getInt(MainActivity.SIM,-1);
        if(selected<0)return;
        // If Android cannot report the originating SIM, do not relay texts from an unknown line.
        int received=intent.getIntExtra("subscription",-1);
        if(received<0)received=intent.getIntExtra("android.telephony.extra.SUBSCRIPTION_INDEX",-1);
        if(received!=selected)return;
        SmsMessage[] parts=Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if(parts==null||parts.length==0)return;
        String from=parts[0].getOriginatingAddress();
        if(from==null)from="Unknown sender";
        StringBuilder body=new StringBuilder();
        for(SmsMessage m:parts)if(m!=null && m.getMessageBody()!=null)body.append(m.getMessageBody());
        if(body.length()==0)return;
        String contents=body.toString();
        String fingerprint=from+"|"+parts[0].getTimestampMillis()+"|"+contents;
        if(fingerprint.equals(p.getString("last_message","")))return;
        p.edit().putString("last_message",fingerprint).apply();
        String when=DateFormat.getDateTimeInstance().format(new Date());
        String prev=p.getString(MainActivity.HISTORY,"");
        String incoming=when+"\nFrom: "+from+"\n"+contents+"\n\n";
        p.edit().putString(MainActivity.HISTORY,(incoming+prev).substring(0,Math.min(16000,(incoming+prev).length()))).apply();
        if(!p.getBoolean(MainActivity.ON,false))return;
        if(c.checkSelfPermission(Manifest.permission.SEND_SMS)!=PackageManager.PERMISSION_GRANTED)return;
        if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        String to=p.getString(MainActivity.DEST,"");
        if(to.length()!=10)return;
        showNotice(c,"Forwarding incoming SMS to configured number");
        try{
            SmsManager sms=SmsManager.getSmsManagerForSubscriptionId(selected);
            String outgoing="Kay's Code Relay received from "+from+": "+contents;
            for(String segment:sms.divideMessage(outgoing))sms.sendTextMessage(to,null,segment,null,null);
        }catch(RuntimeException ex){showNotice(c,"SMS forwarding failed. Check your SIM and permissions.");}
    }
    private void showNotice(Context c,String msg){
        NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(nm==null)return;
        if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel("relay","SMS relay activity",NotificationManager.IMPORTANCE_DEFAULT));
        Intent i=new Intent(c,MainActivity.class);
        PendingIntent pi=PendingIntent.getActivity(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification n=new Notification.Builder(c,"relay")
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("Kay's Code Relay is active")
            .setContentText(msg)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build();
        nm.notify(410,n);
    }
}

package com.security.ssms;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.telephony.SmsManager;
import android.util.Base64;
import android.widget.Toast;

import java.security.InvalidKeyException;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

//Class mostly used for getting information and sending sms messages
public class Function
{
    static final String _ID = "_id";
    static final String KEY_THREAD_ID = "thread_id";
    static final String KEY_NAME = "name";
    static final String KEY_PHONE = "phone";
    static final String KEY_MSG = "msg";
    static final String KEY_TYPE = "type";
    static final String KEY_TIMESTAMP = "timestamp";
    static final String KEY_TIME = "time";

    //Method for making sure the app has permission to read contacts and messages
    public static  boolean hasPermissions(Context context, String... permissions)
    {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null)
        {
            for (String permission : permissions)
            {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
                {
                    return false;
                }
            }
        }
        return true;
    }

    //Method that gets time of message
    public static String converToTime(String timestamp)
    {
        long datetime = Long.parseLong(timestamp);
        Date date = new Date(datetime);
        DateFormat formatter = new SimpleDateFormat("dd/MM HH:mm");
        return formatter.format(date);
    }

    //Method that stores the contents needed for the inbox in a HashMap
    public static HashMap<String, String> mappingInbox(String _id, String thread_id, String name, String phone, String msg, String type, String timestamp, String time)
    {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(_ID, _id);
        map.put(KEY_THREAD_ID, thread_id);
        map.put(KEY_NAME, name);
        map.put(KEY_PHONE, phone);
        map.put(KEY_MSG, msg);
        map.put(KEY_TYPE, type);
        map.put(KEY_TIMESTAMP, timestamp);
        map.put(KEY_TIME, time);
        return map;
    }

    //Method that will make sure messages from the same person doesn't show up in the inbox
    public static  ArrayList<HashMap<String, String>> removeDuplicates( ArrayList<HashMap<String, String>> smsList)
    {
        ArrayList<HashMap<String, String>> gpList = new ArrayList<HashMap<String, String>>();
        for (int i = 0; i<smsList.size(); i++)
        {
            boolean available = false;
            for (int j = 0; j<gpList.size(); j++)
            {
                if( Integer.parseInt(gpList.get(j).get(KEY_THREAD_ID)) == Integer.parseInt(smsList.get(i).get(KEY_THREAD_ID)))
                {
                    available = true;
                    break;
                }
            }

            if(!available)
            {
                gpList.add(mappingInbox(smsList.get(i).get(_ID), smsList.get(i).get(KEY_THREAD_ID), smsList.get(i).get(KEY_NAME), smsList.get(i).get(KEY_PHONE),
                        smsList.get(i).get(KEY_MSG), smsList.get(i).get(KEY_TYPE), smsList.get(i).get(KEY_TIMESTAMP), smsList.get(i).get(KEY_TIME)));
            }
        }
        return gpList;
    }

    //Method that tries to send an sms message using the default sms manager
    public static boolean sendSMS(String key, String toPhoneNumber, String smsMessage)
    {
        int Flags = android.util.Base64.NO_WRAP | android.util.Base64.NO_PADDING;

        // If there is no key we are going to send message the default way
        if(key == null)
        {
            return false;
        }

        byte [] myKey = android.util.Base64.encode(key.getBytes(), Flags);
        ArrayList<String> encrypted = EncMessage(smsMessage, myKey);

        try
        {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendMultipartTextMessage(toPhoneNumber, null, encrypted, null, null);

            return true;
        }
        catch(IllegalArgumentException e){
            System.out.print(encrypted);
            e.printStackTrace();
            return false;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    //This method gets a contacts name given their phone number
    public static String getContactbyPhoneNumber(Context c, String phoneNumber)
    {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        String[] projection = {ContactsContract.PhoneLookup.DISPLAY_NAME};
        Cursor cursor = c.getContentResolver().query(uri, projection, null, null, null);

        if (cursor == null)
        {
            return phoneNumber;
        }
        else{
            String name = phoneNumber;
            try
            {
                if (cursor.moveToFirst())
                {
                    name = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
                }

            }
            finally
            {
                cursor.close();
            }

            return name;
        }
    }

    //Method for creating a cached file that will hold or information
    public static void createCachedFile (Context context, String key, ArrayList<HashMap<String, String>> dataList) throws IOException
    {
            FileOutputStream fos = context.openFileOutput (key, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject (dataList);
            oos.close ();
            fos.close ();
    }

    //Method for getting data from a cached list
    public static Object readCachedFile (Context context, String key) throws IOException, ClassNotFoundException
    {
        FileInputStream fis = context.openFileInput (key);
        ObjectInputStream ois = new ObjectInputStream(fis);
        Object object = ois.readObject ();
        return object;
    }


    //encrypt method
    private static String Encrypt(String msgA, byte[] key){
        String pad = new String();
        byte[] msg;
        //String keyA = Base64.getEncoder().encodeToString(key.getBytes());
        int Flags = Base64.NO_WRAP | Base64.NO_PADDING;

        msg = msgA.getBytes();
        //System.out.println((new String(Base64.getDecoder().decode(msg))) + " " + msg.length %16); // still heretodo << make sure we get a %16 =0  before ENC happens !!1


        byte[] encrypted;
        try {



            SecretKeySpec aesKey = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");

            cipher.init(Cipher.ENCRYPT_MODE, aesKey);


            encrypted = cipher.doFinal(msg);

            return new String(android.util.Base64.encode(encrypted, Flags));


        }
        catch(InvalidKeyException e){
            e.printStackTrace();
            return msgA;
        }
        catch(Exception e) {
            e.printStackTrace();
            return null;
        }


    }
    public static String Decrypt(byte[] msg, byte[] key) {
        byte [] encrypted;
        String decrypted;
        String keyA;
        int Flags = Base64.NO_WRAP | Base64.NO_PADDING;

        try {

           try {
               SecretKeySpec aesKey = new SecretKeySpec(key, "AES");
               Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
               //System.out.print(key);
               cipher.init(Cipher.DECRYPT_MODE, aesKey );

               decrypted = new String(cipher.doFinal(android.util.Base64.decode(msg, Flags)));

               return decrypted; //return string

           }
           catch(InvalidKeyException e){
               return null;
           }



        } catch (Exception e) {

            e.printStackTrace();
            return null;
        }

    }
    public static ArrayList<String> EncMessage(String msg, byte [] key){  //port Body to byte[], enc, set to Body
        int index = 0;

        ArrayList<String>bodyToSend = new ArrayList<String>();
        //System.out.println("after enc: "+ msg.length() % 16);
        while (index < msg.length()) {                       //split into 140 char for sms
            bodyToSend.add((Encrypt(msg.substring(index, Math.min(index + 127, msg.length())), key)) );
            index += 127;
        }
        return bodyToSend;
    }
    public static String DecMessage(String msg, byte [] key){
        String decrypted = new String();
        //convert to single string
        decrypted = Decrypt((msg.getBytes()), key); //adds 8 /n chars?



        return decrypted;
    }

}


package com.security.ssms;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

//This is class is to get the all the sms messages
public class Chat extends AppCompatActivity
{
    ListView listView;
    ChatAdapter adapter;
    LoadSms loadsmsTask;
    String name;
    String address;
    EditText new_message;
    ImageButton send_message;
    int thread_id_main;
    private Handler handler = new Handler();
    Thread t;

    //Array list of hap maps to hold all conversations
    ArrayList<HashMap<String, String>> smsList = new ArrayList<>();
    ArrayList<HashMap<String, String>> customList = new ArrayList<>();
    ArrayList<HashMap<String, String>> tempList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //Get the intent that was sent to this activity
        Intent intent = getIntent();

        //Get the name, address and the id
        name = intent.getStringExtra("name");
        address = intent.getStringExtra("address");
        thread_id_main = Integer.parseInt(intent.getStringExtra("thread_id"));

        //Initialize the components we need
        listView = (ListView) findViewById(R.id.listView);
        new_message = (EditText) findViewById(R.id.new_message);
        send_message = (ImageButton) findViewById(R.id.send_message);

        //Call the method to load all the sms messages
        startLoadingSms();

        //Inner clack for when the user clicks on the send button
        send_message.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v) {
                //Get the message the user entered
                String text = new_message.getText().toString();

                //As long as the user actually enters in a message send it
                if(text.length() > 0)
                {
                    String tmp_msg = text;
                    new_message.setText("Sending....");
                    new_message.setEnabled(false);
                    String myKey = new String();
                    int Flags = android.util.Base64.NO_WRAP | android.util.Base64.NO_PADDING;
                    NewContact myContact = new NewContact();
                    try {
                        String key = myContact.readFromFile(getApplicationContext(), name);
                        myKey = new String(android.util.Base64.encode(key.getBytes(), Flags));
                    }
                    catch(NullPointerException e){
                        myKey = new String(android.util.Base64.encode("ab".getBytes(), Flags)); //send unencrypted

                    }
                    //If the message sends first clear out where the user enters in messages
                    if(Function.sendSMS(myKey, address, tmp_msg))
                    {
                        new_message.setText("");
                        new_message.setEnabled(true);

                        // Creating a custom list for newly added sms
                        customList.clear();
                        customList.addAll(smsList);
                        customList.add(Function.mappingInbox(null, null, null, null, tmp_msg, "2", null, "Sending..."));
                        adapter = new ChatAdapter(Chat.this, customList);
                        listView.setAdapter(adapter);
                    }
                    else {
                        new_message.setText(tmp_msg);
                        new_message.setEnabled(true);
                    }
                }
            }
        });
    }

    //Inner class that loads the sms messages of the user
    class LoadSms extends AsyncTask<String, Void, String>
    {
        //Method to call the super class and clear out the temporary list
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            tempList.clear();
        }

        protected String doInBackground(String... args)
        {
            //Junk value to be returned
            String junk = "";

            //Try catch to get the users inbox and sent messages and put them into to tempList
            try
            {
                Uri uriInbox = Uri.parse("content://sms/inbox");
                Cursor inbox = getContentResolver().query(uriInbox, null, "thread_id=" + thread_id_main, null, null);
                Uri uriSent = Uri.parse("content://sms/sent");
                Cursor sent = getContentResolver().query(uriSent, null, "thread_id=" + thread_id_main, null, null);
                Cursor c = new MergeCursor(new Cursor[]{inbox,sent}); // Attaching inbox and sent sms
                String myKeyString = new String();

                if (c.moveToFirst())
                {
                    for (int i = 0; i < c.getCount(); i++)
                    {
                        String phone = "";
                        String _id = c.getString(c.getColumnIndexOrThrow("_id"));
                        String thread_id = c.getString(c.getColumnIndexOrThrow("thread_id"));
                        //Toast.makeText(getApplicationContext(), "here", Toast.LENGTH_SHORT).show();
                        if(i == (c.getCount()-1)){
                            try{ myKeyString = new String(NewContact.readFromFile(getApplicationContext(),name));} //get key
                            catch(Exception e){myKeyString = new String("abcdefghijk ");}// no key, use "null"key
                            int Flags = android.util.Base64.NO_WRAP | android.util.Base64.NO_PADDING;
                            // get key from file and pass VV
                            byte[] myKey = android.util.Base64.encode(myKeyString.getBytes(), Flags);
                            String Decrypted = Function.DecMessage(c.getString(c.getColumnIndexOrThrow("body")), myKey);
                        }
                        String msg = c.getString(c.getColumnIndexOrThrow("body"));//decrypt here, need key
                        String type = c.getString(c.getColumnIndexOrThrow("type"));
                        String timestamp = c.getString(c.getColumnIndexOrThrow("date"));
                        phone = c.getString(c.getColumnIndexOrThrow("address"));

                        tempList.add(Function.mappingInbox(_id, thread_id, name, phone, msg, type, timestamp, Function.converToTime(timestamp)));
                        c.moveToNext();
                    }

                }
                c.close();
            }
            catch (IllegalArgumentException e)
            {
                e.printStackTrace();
            }

            //Sort the tempList by time of the last message sent
            Collections.sort(tempList, new MapComparator(Function.KEY_TIMESTAMP, "asc"));

            return junk;
        }

        //Method to make sure the smsList is the same as the temp list
        @Override
        protected void onPostExecute(String junk)
        {
            if(!tempList.equals(smsList))
            {
                smsList.clear();
                smsList.addAll(tempList);
                adapter = new ChatAdapter(Chat.this, smsList);
                listView.setAdapter(adapter);
            }
        }
    }

    //Method to start loading the sms messages
    public void startLoadingSms()
    {
        final Runnable r = new Runnable()
            {
                public void run()
                {
                loadsmsTask = new LoadSms();
                loadsmsTask.execute();

                handler.postDelayed(this, 5000);
            }
        };
        handler.postDelayed(r, 0);
    }
}

//Inner class for ChatAdapter
class ChatAdapter extends BaseAdapter
{
    private Activity activity;
    private ArrayList<HashMap< String, String >> data;
    public ChatAdapter(Activity a, ArrayList < HashMap < String, String >> d)
    {
        activity = a;
        data = d;
    }
    public int getCount()
    {
        return data.size();
    }

    public Object getItem(int position)
    {
        return position;
    }
    public long getItemId(int position)
    {
        return position;
    }

    //Method that gets all the attributes of the chat
    public View getView(int position, View convertView, ViewGroup parent)
    {
        ChatViewHolder holder = null;
        if (convertView == null) {
            holder = new ChatViewHolder();
            convertView = LayoutInflater.from(activity).inflate(
                    R.layout.chat_item, parent, false);

            holder.txtMsgYou = (TextView)convertView.findViewById(R.id.txtMsgYou);
            holder.lblMsgYou = (TextView)convertView.findViewById(R.id.lblMsgYou);
            holder.timeMsgYou = (TextView)convertView.findViewById(R.id.timeMsgYou);
            holder.lblMsgFrom = (TextView)convertView.findViewById(R.id.lblMsgFrom);
            holder.timeMsgFrom = (TextView)convertView.findViewById(R.id.timeMsgFrom);
            holder.txtMsgFrom = (TextView)convertView.findViewById(R.id.txtMsgFrom);
            holder.msgFrom = (LinearLayout)convertView.findViewById(R.id.msgFrom);
            holder.msgYou = (LinearLayout)convertView.findViewById(R.id.msgYou);

            convertView.setTag(holder);
        }
        else {
            holder = (ChatViewHolder) convertView.getTag();
        }
        holder.txtMsgYou.setId(position);
        holder.lblMsgYou.setId(position);
        holder.timeMsgYou.setId(position);
        holder.lblMsgFrom.setId(position);
        holder.timeMsgFrom.setId(position);
        holder.txtMsgFrom.setId(position);
        holder.msgFrom.setId(position);
        holder.msgYou.setId(position);

        HashMap < String, String > song = new HashMap < String, String > ();
        song = data.get(position);
        try
        {
            if(song.get(Function.KEY_TYPE).contentEquals("1"))
            {
                holder.lblMsgFrom.setText(song.get(Function.KEY_NAME));
                holder.txtMsgFrom.setText(song.get(Function.KEY_MSG));
                holder.timeMsgFrom.setText(song.get(Function.KEY_TIME));
                holder.msgFrom.setVisibility(View.VISIBLE);
                holder.msgYou.setVisibility(View.GONE);
            }else{
                holder.lblMsgYou.setText("You");
                holder.txtMsgYou.setText(song.get(Function.KEY_MSG));
                holder.timeMsgYou.setText(song.get(Function.KEY_TIME));
                holder.msgFrom.setVisibility(View.GONE);
                holder.msgYou.setVisibility(View.VISIBLE);
            }

        } catch (Exception e) {}
        return convertView;
    }
}

//Inner class for all the attributes of the chat
class ChatViewHolder {
    LinearLayout msgFrom, msgYou;
    TextView txtMsgYou, lblMsgYou, timeMsgYou, lblMsgFrom, txtMsgFrom, timeMsgFrom;
}
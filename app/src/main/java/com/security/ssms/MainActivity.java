package com.security.ssms;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.lifeofcoding.cacheutlislibrary.CacheUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_PERMISSION_KEY = 1;
    ArrayList<HashMap<String, String>> smsList = new ArrayList<HashMap<String, String>>();
    ArrayList<HashMap<String, String>> tmpList = new ArrayList<HashMap<String, String>>();
    static MainActivity inst;
    LoadSms loadsmsTask;
    InboxAdapter adapter, tmpadapter;;
    ListView listView;
    FloatingActionButton fab_new;
    FloatingActionButton new_contact;
    ProgressBar loader;
    int i;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CacheUtils.configureCache(this);

        listView = (ListView) findViewById(R.id.listView);
        loader = (ProgressBar) findViewById(R.id.loader);
        fab_new = (FloatingActionButton) findViewById(R.id.fab_new);
        new_contact = (FloatingActionButton) findViewById(R.id.newContact);
        listView.setEmptyView(loader);

        //Code for when the user clicks on the new contact button
        new_contact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NewContact.class);
                startActivity(intent);
            }
        });

        //Code for when the user wants to send a new sms message
        fab_new.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                //When the user clicks on the new message button the default contact app will open
                //And they will need to pick a contact
                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(intent, 1);
            }
        });
    }

    //Method for when the user click on the contact they want to send a message to
    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data)
    {
        super.onActivityResult(reqCode, resultCode, data);

        //Variable to hold the number of the contact the user clicks
        String number = "";

        //If statement to make sure the user chose a contact
        if (resultCode == RESULT_OK)
        {
            //Code to get the info needed to see if the specific contact chosen has a number
            Uri contactData = data.getData();
            Cursor cursor = getContentResolver().query(contactData, null, null, null, null);
            cursor.moveToFirst();
            String hasPhone = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER));
            String contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));

            //If the contact has a phone number retrieve it
            if (hasPhone.equals("1"))
            {
                //Gets a cursor to for going through numbers
                Cursor phones = getContentResolver().query
                        (ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                                        + " = " + contactId, null, null);

                //While loop that gets the desired phone number
                while (phones.moveToNext())
                {
                    number = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)).replaceAll("[-() ]", "");
                }
                phones.close();
            }
            //If the contact does the not a phone number show the user a notification saying the contact doesnt have a phone number
            else {
                Toast.makeText(getApplicationContext(), "This contact has no phone number", Toast.LENGTH_LONG).show();
            }
            cursor.close();
        }

        //Code that calls the newsms activity sending to the number desired
        Intent intent = new Intent(MainActivity.this, NewSms.class);
        intent.putExtra("Number", number);
        startActivity(intent);
    }

    //Method to initialize all the components of the MainActivity class
    public void init()
    {
        smsList.clear();

        //Try catch for initializing the components
        try
        {
            tmpList = (ArrayList<HashMap<String, String>>)Function.readCachedFile  (MainActivity.this, "smsapp");
            tmpadapter = new InboxAdapter(MainActivity.this, tmpList);
            listView.setAdapter(tmpadapter);

            //Code for when the user clicks on a chat that brings up the messages of that chat
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                public void onItemClick(AdapterView<?> parent, View view, final int position, long id)
                {
                    loadsmsTask.cancel(true);
                    Intent intent = new Intent(MainActivity.this, Chat.class);
                    intent.putExtra("name", tmpList.get(+position).get(Function.KEY_NAME));
                    intent.putExtra("address", tmpList.get(+position).get(Function.KEY_PHONE));
                    intent.putExtra("thread_id", tmpList.get(+position).get(Function.KEY_THREAD_ID));
                    startActivity(intent);
                }
            });
        }
        catch(Exception e)
        {

        }
    }

    //Inter class to load the sms messages
    class LoadSms extends AsyncTask<String, Void, String>
    {
        //Method to call the super class and clear out the sms list
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            smsList.clear();
        }

        protected String doInBackground(String... args)
        {
            String junk = "";

            //Try catch to get the users inbox and sent messages and put them into to smsList
            try
            {
                Uri uriInbox = Uri.parse("content://sms/inbox");

                Cursor inbox = getContentResolver().query(uriInbox, null, "address IS NOT NULL) GROUP BY (thread_id", null, null); // 2nd null = "address IS NOT NULL) GROUP BY (address"
                Uri uriSent = Uri.parse("content://sms/sent");
                Cursor sent = getContentResolver().query(uriSent, null, "address IS NOT NULL) GROUP BY (thread_id", null, null); // 2nd null = "address IS NOT NULL) GROUP BY (address"
                Cursor c = new MergeCursor(new Cursor[]{inbox,sent}); // Attaching inbox and sent sms

                   if (c.moveToFirst())
                {
                    for (int i = 0; i < c.getCount(); i++)
                    {
                        String name = null;
                        String phone = "";
                        String _id = c.getString(c.getColumnIndexOrThrow("_id"));
                        String thread_id = c.getString(c.getColumnIndexOrThrow("thread_id"));
                        String msg = c.getString(c.getColumnIndexOrThrow("body"));
                        String type = c.getString(c.getColumnIndexOrThrow("type"));
                        String timestamp = c.getString(c.getColumnIndexOrThrow("date"));
                        phone = c.getString(c.getColumnIndexOrThrow("address"));

                        name = CacheUtils.readFile(thread_id);

                        if(name == null)
                        {
                            name = Function.getContactbyPhoneNumber(getApplicationContext(), c.getString(c.getColumnIndexOrThrow("address")));
                            CacheUtils.writeFile(thread_id, name);
                        }
                        if (i == c.getCount() - 1 ){
                            try {
                                String myKey = new String(NewContact.readFromFile(getApplicationContext(), name));
                                byte[] key = android.util.Base64.encode(myKey.getBytes(), Base64.NO_WRAP | Base64.NO_PADDING);
                                msg = Function.DecMessage(c.getString(c.getColumnIndexOrThrow("body")), myKey.getBytes()); // decrypt if this is the most recent message
                            }
                            catch (Exception e){

                            }
                        }

                        smsList.add(Function.mappingInbox(_id, thread_id, name, phone, msg, type, timestamp, Function.converToTime(timestamp)));
                        c.moveToNext();
                    }
                }
                c.close();
            }
            catch (IllegalArgumentException e)
            {
                e.printStackTrace();
            }

            Collections.sort(smsList, new MapComparator(Function.KEY_TIMESTAMP, "dsc")); // Arranging sms by timestamp decending
            ArrayList<HashMap<String, String>> purified = Function.removeDuplicates(smsList); // Removing duplicates from inbox & sent
            smsList.clear();
            smsList.addAll(purified);

            // Updating cache data
            try
            {
                Function.createCachedFile (MainActivity.this,"smsapp", smsList);
            }
            catch (Exception e) {}

            return junk;
        }

        @Override
        protected void onPostExecute(String xml)
        {
            if(!tmpList.equals(smsList))
            {
                adapter = new InboxAdapter(MainActivity.this, smsList);
                listView.setAdapter(adapter);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
                {
                    public void onItemClick(AdapterView<?> parent, View view, final int position, long id)
                    {
                        Intent intent = new Intent(MainActivity.this, Chat.class);
                        intent.putExtra("name", smsList.get(+position).get(Function.KEY_NAME));
                        intent.putExtra("address", tmpList.get(+position).get(Function.KEY_PHONE));
                        intent.putExtra("thread_id", smsList.get(+position).get(Function.KEY_THREAD_ID));
                        startActivity(intent);
                    }
                });
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case REQUEST_PERMISSION_KEY:
                {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    init();
                    loadsmsTask = new LoadSms();
                    loadsmsTask.execute();
                }
                else
                {
                    Toast.makeText(MainActivity.this, "You must accept permissions.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }


    //Method that is used mostly for getting permissions from the user then if
    //they are granted initializing every thing needed
    @Override
    protected void onResume()
    {
        super.onResume();

        String[] PERMISSIONS = {Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS};
        if(!Function.hasPermissions(this, PERMISSIONS))
        {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSION_KEY);
        }
        else {
            init();
            loadsmsTask = new LoadSms();
            loadsmsTask.execute();
        }

    }

    //Method for when the activity starts
    @Override
    public void onStart() {
        super.onStart();
    }
}

//Inner class that gets all the details of the inbox
class InboxAdapter extends BaseAdapter
{
    private Activity activity;
    private ArrayList<HashMap< String, String >> data;
    public InboxAdapter(Activity a, ArrayList < HashMap < String, String >> d)
    {
        activity = a;
        data = d;
    }

    public int getCount() {
        return data.size();
    }
    public Object getItem(int position) {
        return position;
    }
    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent)
    {
        InboxViewHolder holder = null;
        if (convertView == null)
        {
            holder = new InboxViewHolder();
            convertView = LayoutInflater.from(activity).inflate(
                    R.layout.conversation_list_item, parent, false);

            holder.inbox_thumb = (ImageView) convertView.findViewById(R.id.inbox_thumb);
            holder.inbox_user = (TextView) convertView.findViewById(R.id.inbox_user);
            holder.inbox_msg = (TextView) convertView.findViewById(R.id.inbox_msg);
            holder.inbox_date = (TextView) convertView.findViewById(R.id.inbox_date);

            convertView.setTag(holder);
        }
        else {
            holder = (InboxViewHolder) convertView.getTag();
        }
        holder.inbox_thumb.setId(position);
        holder.inbox_user.setId(position);
        holder.inbox_msg.setId(position);
        holder.inbox_date.setId(position);

        HashMap < String, String > song = new HashMap < String, String > ();
        song = data.get(position);
        try
        {
            holder.inbox_user.setText(song.get(Function.KEY_NAME));
            holder.inbox_msg.setText(song.get(Function.KEY_MSG));
            holder.inbox_date.setText(song.get(Function.KEY_TIME));

            String firstLetter = String.valueOf(song.get(Function.KEY_NAME).charAt(0));
            ColorGenerator generator = ColorGenerator.MATERIAL;
            int color = generator.getColor(getItem(position));
            TextDrawable drawable = TextDrawable.builder()
                    .buildRound(firstLetter, color);
            holder.inbox_thumb.setImageDrawable(drawable);
        }
        catch (Exception e) {}

        return convertView;
    }
}

//Inner class that holds all the inbox info so it is visible
class InboxViewHolder
{
    ImageView inbox_thumb;
    TextView inbox_user, inbox_msg, inbox_date;
}
package com.security.ssms;



import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

//This is for creating a new sms message
public class NewSms extends AppCompatActivity
{
    EditText address, message;
    Button send_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_new);

        //This gets the phone number of the contact the user chose in the MainActivity
        final String num = getIntent().getStringExtra("Number");
        final String name = getIntent().getStringExtra("Name");
        //need to get key from file also maybe need our own json file and do in send

        //This holds the phone number on our xml file
        address = (EditText) findViewById(R.id.address);

        //This holds the message typed in by the user
        message = (EditText) findViewById(R.id.message);

        //This sends the message to the phone number in address
        send_btn = (Button) findViewById(R.id.send_btn);

        //Set the contents of the address EditText to the phone number
        address.setText(num);
        send_btn.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                //This gets the message the user typed in
                String str_message = message.getText().toString();
                String key = new String();

                //If statement to make sure the user entered in a phone number and message
                if (num.length() > 0 && str_message.length() > 0)
                {
                    try {
                        key = NewContact.readFromFile(getApplicationContext(), name);
                    }
                    catch(NullPointerException e){

                    }
                    //If statement to see if the message sent okay
                    if(Function.sendSMS(key, num, str_message) == true)
                    {
                        //Notifies the user that the message was indeed sent
                        Toast.makeText(getApplicationContext(), "Message Sent", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(NewSms.this, MainActivity.class);
                        startActivity(intent);
                    }
                    else
                    {
                        SmsManager sms = SmsManager.getDefault();
                        sms.sendTextMessage(num, null, str_message, null, null);
                        Toast.makeText(getApplicationContext(), "Message Sent", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(NewSms.this, MainActivity.class);
                        startActivity(intent);
                    }
                }
            }

        });
    }
}

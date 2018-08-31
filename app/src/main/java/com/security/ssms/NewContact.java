package com.security.ssms;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class NewContact extends AppCompatActivity {
    EditText contact_name;
    EditText contact_number;
    EditText contact_key;
    Button addContact;
    String name;
    String number;
    String secKey;
    String filename = "Keys.xml";

    public NewContact()
    {
        name = " ";
        number = " ";
        secKey = " ";
    }

    public NewContact(String name, String number, String secKey)
    {
        this.name = name;
        this.number = number;
        this.secKey = secKey;
    }

    public String getName()
    {
        return name;
    }

    public String getSecKey()
    {
        return secKey;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout. activity_new_contact);
        addContact = (Button) findViewById(R.id.btnAddContact);
        contact_name = (EditText) findViewById(R.id.contact_name);
        contact_number = (EditText) findViewById(R.id.contact_number);
        contact_key = (EditText) findViewById(R.id.contact_key);

        addContact.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                name = contact_name.getText().toString();
                number = contact_number.getText().toString();
                secKey = contact_key.getText().toString();
                writeToFile(name, number, secKey);
                String newKey = readFromFile(getApplicationContext(),name);
                //Toast.makeText(getApplicationContext(), newKey, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(NewContact.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    public void writeToFile(String name, String number, String secKey)
    {
        try
        {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(getApplicationContext().openFileOutput(filename, Context.MODE_PRIVATE));
            outputStreamWriter.write(name);
            outputStreamWriter.write(secKey);
            outputStreamWriter.close();
            //Toast.makeText(getApplicationContext(), "Contact Saved To File", Toast.LENGTH_SHORT).show();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            //Toast.makeText(getApplicationContext(), "Error Trying To Write To File", Toast.LENGTH_SHORT).show();
        }

    }

    public static String readFromFile(Context c,String name)
    {
        String nameFromFile = null;
        String keyFromFile = null;
        String myfilename = "keys.xml";
        try
        {
            InputStream inputStream = c.openFileInput(myfilename);

            if(inputStream != null)
            {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                keyFromFile = stringBuilder.substring(name.length());
                //Toast.makeText(c, keyFromFile, Toast.LENGTH_SHORT).show();
                return keyFromFile;
            }
            else
            {
                //Toast.makeText(c, "Nothin On File", Toast.LENGTH_SHORT).show();
            }
        }
        catch (FileNotFoundException e)
        {
            //Toast.makeText(c, "File Not Found", Toast.LENGTH_SHORT).show();
        }
        catch (IOException e)
        {
            //Toast.makeText(c, "Error", Toast.LENGTH_SHORT).show();
        }

        return keyFromFile;
    }
}

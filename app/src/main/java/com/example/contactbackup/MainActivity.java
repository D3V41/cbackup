package com.example.contactbackup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.os.StrictMode;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button btnBackup,btnRestore,btnContacts;
    Animation animebtn;
    ProgressBar progressBar,progressBar2;
    TextView noofcontacts,txtvname,txtvphone,txtshare;
    ImageView person;
    String vfile = null;
    Cursor cursor;
    public static FileOutputStream mFileOutputStream = null;
    ArrayList<String> vCard;
    File f;
    String storage_path;
    ProgressDialog progress;
    PopupWindow mpopup;
    public int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.WRITE_CONTACTS,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnBackup = findViewById(R.id.button2);
        btnRestore = findViewById(R.id.button3);
        animebtn = AnimationUtils.loadAnimation(this,R.anim.bounce);
        noofcontacts = findViewById(R.id.textView);
        person = findViewById(R.id.imageView);
        progressBar = findViewById(R.id.progressBar);
        progressBar2 = findViewById(R.id.progressBar2);
        txtvname = findViewById(R.id.textView2);
        btnContacts = findViewById(R.id.button4);
        txtshare = findViewById(R.id.textView4);

        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics realDisplayMetrics = new DisplayMetrics();
        display.getMetrics(realDisplayMetrics);
        final int h = realDisplayMetrics.heightPixels;
        final int w = realDisplayMetrics.widthPixels;
        btnRestore.setWidth(w/2);
        btnBackup.setWidth(w/4);
        btnBackup.setHeight(h/8);
        btnRestore.setHeight(h/2);
        person.setImageResource(R.drawable.ic_action_name);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        getPermissions();

        btnBackup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnBackup.startAnimation(animebtn);
                vfile = "Contacts" + "_" + System.currentTimeMillis()+".vcf";
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        int x = getcontacts();
                        noofcontacts.setText("Total Contacts : " + x);
                        new LongOperation().execute("");
                    }
                    else {
                        AlertDialog alertDialog1 = new AlertDialog.Builder(MainActivity.this).create();
                        alertDialog1.setTitle("Permission Access");
                        alertDialog1.setMessage("Please give Contacts and Storage permission. For that go to app>permissions");
                        alertDialog1.setButton("OK", new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            }
                        });
                        alertDialog1.show();

                    }
                } else {
                    AlertDialog alertDialog1 = new AlertDialog.Builder(MainActivity.this).create();
                    alertDialog1.setTitle("Permission Access");
                    alertDialog1.setMessage("Please give Contacts and Storage permission. For that go to app>permissions");
                    alertDialog1.setButton("OK", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        }
                    });
                    alertDialog1.show();
                }
            }
        });

        btnContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
                builder1.setTitle(vfile);
                builder1.setCancelable(false);

                builder1.setPositiveButton(
                        "Share",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent sharingIntent = new Intent();
                                sharingIntent.setAction(Intent.ACTION_SEND);
                                String shareSub = "Contacts BackUp";
                                sharingIntent.setPackage("com.google.android.gm");
                                sharingIntent.setType(ContactsContract.Contacts.CONTENT_VCARD_TYPE);
                                sharingIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(Environment.getExternalStorageDirectory().toString() + File.separator + vfile)));
                                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, shareSub);
                                startActivity(Intent.createChooser(sharingIntent, "Share Contacts"));
                            }



                        });

                builder1.setNegativeButton(
                        "No",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent mIntent = new Intent();

                            }
                        });
                AlertDialog alert11 = builder1.create();
                alert11.show();
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.threedot, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.info){
            AlertDialog alertDialog1 = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog1.setTitle("CBackup");
            alertDialog1.setMessage("CBackup is fastest contacts backup app.");
            alertDialog1.setButton("OK", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                }
            });
            alertDialog1.show();
        }
        if(id == R.id.slocation){
            AlertDialog alertDialog1 = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog1.setTitle("Storage Location");
            alertDialog1.setMessage("Your contact backup file is stored at \n"+Environment.getExternalStorageDirectory().toString()+"\nYou can access this file using your compute or in phone using File Explorer app");
            alertDialog1.setButton("OK", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                }
            });
            alertDialog1.show();
        }
        if(id == R.id.share)
        {
            if(vfile==null)
                Toast.makeText(MainActivity.this,  "First create backup file", Toast.LENGTH_LONG).show();
            else
            {
                Intent sharingIntent = new Intent();
                sharingIntent.setAction(Intent.ACTION_SEND);
                String shareSub = "Contacts BackUp";
                sharingIntent.setPackage("com.google.android.gm");
                sharingIntent.setType(ContactsContract.Contacts.CONTENT_VCARD_TYPE);
                sharingIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(Environment.getExternalStorageDirectory().toString() + File.separator + vfile)));
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, shareSub);
                startActivity(Intent.createChooser(sharingIntent, "Share Contacts"));
            }

        }
        return super.onOptionsItemSelected(item);
    }

    private void getPermissions() {
        if (!hasPermissions(MainActivity.this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, PERMISSION_ALL);
        }


    }

    private class LongOperation extends AsyncTask<String, Void, String> {

        ProgressDialog progress;

        @Override
        protected void onPreExecute() {
            progressBar2.setIndeterminate(true);
            progressBar2.setProgress(0);
            progressBar2.setVisibility(ProgressBar.VISIBLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        }

        @Override
        protected String doInBackground(String... params) {
            getVCF();
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            progressBar2.setVisibility(ProgressBar.INVISIBLE);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            btnContacts.setVisibility(View.VISIBLE);
            txtshare.setVisibility(View.VISIBLE);
            btnContacts.setText(vfile);
            final AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
            builder1.setTitle("Backup created successfully");
            builder1.setMessage("Do you want to share your backup?");
            builder1.setCancelable(false);

            builder1.setPositiveButton(
                    "Yes",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent sharingIntent = new Intent();
                            sharingIntent.setAction(Intent.ACTION_SEND);
                            String shareSub = "Contacts BackUp";
                            sharingIntent.setPackage("com.google.android.gm");
                            sharingIntent.setType(ContactsContract.Contacts.CONTENT_VCARD_TYPE);
                            sharingIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(Environment.getExternalStorageDirectory().toString() + File.separator + vfile)));
                            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, shareSub);
                            startActivity(Intent.createChooser(sharingIntent, "Share Contacts"));
                        }



                    });

            builder1.setNegativeButton(
                    "No",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Toast.makeText(MainActivity.this,  "Backup is Stored in internal storage", Toast.LENGTH_SHORT).show();
                            dialog.cancel();
                        }
                    });
            AlertDialog alert11 = builder1.create();
            alert11.show();

        }

    }

    public void getVCF()
    {
        progressBar = findViewById(R.id.progressBar);
        txtvname = findViewById(R.id.textView2);

        final Cursor phones = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null,null, null, null);

        phones.moveToFirst();
        for(int i =0;i<phones.getCount();i++)
        {
            String lookupKey =  phones.getString(phones.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
            Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);
            AssetFileDescriptor fd;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    txtvname.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(ProgressBar.VISIBLE);
                    progressBar.setMax(phones.getCount());
                }
            });
            try
            {
                fd = getContentResolver().openAssetFileDescriptor(uri, "r");
                FileInputStream fis = fd.createInputStream();
                byte[] buf = readBytes(fis);
                fis.read(buf);
                String VCard = new String(buf);
                storage_path = Environment.getExternalStorageDirectory().toString()+ File.separator + vfile;
                FileOutputStream mFileOutputStream = new FileOutputStream(storage_path, true);
                mFileOutputStream.write(VCard.toString().getBytes());
                final String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                final int pint = i;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txtvname.setText(name);
                        progressBar.setProgress(pint);
                    }
                });
                phones.moveToNext();
                Log.d("Vcard"+i,  VCard);
                Log.d("Name :",phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)));
            }
            catch (Exception e1)
            {
                e1.printStackTrace();
            }
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtvname.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.INVISIBLE);
            }
        });

    }
    public static byte[] readBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public int getcontacts()
    {
        Cursor phones = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null,null, null, null);
        return phones.getCount();
    }

    public void showPopup(View v) {

    }

}

package com.adarwawan.itbtreasurehunt;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {

    private EditText nim = null;
    public static String token;
    public static String nim_user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nim = (EditText) findViewById(R.id.nim_editText);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void playTreasureHandler(View view) {
        String _nim = nim.getText().toString();
        nim_user = nim.getText().toString();
        if (_nim.equalsIgnoreCase("") || _nim.length()!= 8)
            Toast.makeText(MainActivity.this, "ID is Invalid", Toast.LENGTH_SHORT).show();
        else {
            ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = connMgr.getActiveNetworkInfo();
            if (mNetworkInfo != null && mNetworkInfo.isConnected()) {
                /*Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                intent.putExtra("nim", nim_user);
                startActivity(intent);
                */
                new playTreasureComm().execute();
            }
            else
                Toast.makeText(MainActivity.this, "Your device is disconnecting", Toast.LENGTH_SHORT).show();
        }
    }

    // Activate Socket
    private class playTreasureComm extends AsyncTask <Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            StringBuilder sb = new StringBuilder();
            Socket socket = null;

            //String ip = "167.205.34.132";
            //int port = 3111;
            String ip = "api.nitho.me";
            int port = 8080;

            String result = "";

            try {
                InetAddress srvAddr = InetAddress.getByName(ip);
                socket = new Socket(srvAddr, port);

                JSONObject obj = new JSONObject();
                obj.put("nim", nim_user);
                obj.put("com", "req_loc");

                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(obj.toString());
                out.flush();

                Log.i("Sending", obj.toString());


                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line + "\n");

                }

                br.close();
                result = sb.toString();

            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            Log.i("Respond", result);

            return result;
        }

        @Override
        protected void onPostExecute(String out) {
            if (out.length() > 0) {
                Toast.makeText(MainActivity.this, out, Toast.LENGTH_SHORT).show();
                try {
                    JSONObject result = new JSONObject(out);

                    String status = result.getString("status");
                    if (status.equalsIgnoreCase("ok")) {
                        double longitude = result.getDouble("latitude");
                        double latitude = result.getDouble("longitude");
                        token = result.getString("token");

                        // start the maps activity
                        startMapsActivity(latitude, longitude);
                    }
                } catch(JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(MainActivity.this, "Your device is disconnecting.", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void startMapsActivity(double latitude, double longitude) {
        // resume maps activity intent
        Intent intent = new Intent(MainActivity.this, MapsActivity.class);
        intent.putExtra("latitude", latitude);
        intent.putExtra("longitude", longitude);
        intent.putExtra("nim", nim_user);
        startActivity(intent);
    }
}
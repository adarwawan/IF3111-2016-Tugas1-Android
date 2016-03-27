package com.adarwawan.itbtreasurehunt;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
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

public class SubmissionActivity extends AppCompatActivity {

    private Spinner spinnerAnswers;
    public static String nim_user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submission);

        Intent gettingIntent = getIntent();
        nim_user = gettingIntent.getStringExtra("nim");

        Toolbar toolbar = (Toolbar) findViewById(R.id.view);
        setSupportActionBar(toolbar);

        spinnerAnswers = (Spinner) findViewById(R.id.answer_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.answer, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAnswers.setAdapter(adapter);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void submitAnswer(View view) {
        String answer = spinnerAnswers.getSelectedItem().toString();

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mNetworkInfo = connMgr.getActiveNetworkInfo();
        if (mNetworkInfo != null && mNetworkInfo.isConnected()) {
            new SubmitAnswerTask().execute(answer);
            /*Intent intent = new Intent(SubmissionActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);*/
        }
        else {
            Toast.makeText(SubmissionActivity.this, "Your device is disconnecting", Toast.LENGTH_SHORT).show();
        }
    }

    private class SubmitAnswerTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... answers) {
            StringBuilder sb = new StringBuilder();
            String answer = answers[0];
            Socket socket = null;

            String ip = "167.205.34.132";
            int port = 3111;
            /*String ip = "api.nitho.me";
            int port = 8080;*/

            String result = "";


            try {
                InetAddress srvAddr = InetAddress.getByName(ip);
                socket = new Socket(srvAddr, port);

                JSONObject obj = new JSONObject();
                obj.put("com", "answer");
                obj.put("nim", nim_user);
                obj.put("answer", answer);
                obj.put("latitude", MapsActivity.curPos.latitude);
                obj.put("longitude", MapsActivity.curPos.longitude);
                obj.put("token", MainActivity.token);

                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(obj.toString());
                out.flush();

                Log.i("Sending", obj.toString());

                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf-8"));
                String line;
                while ((line = br.readLine()) != null)
                    sb.append(line + "\n");

                br.close();
                result = sb.toString();

            } catch (Exception e) {
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

            Log.i("Response", result);

            return result;
        }

        @Override
        protected void onPostExecute(String out) {
            if (out.length() > 0) {
                Toast.makeText(SubmissionActivity.this, out, Toast.LENGTH_SHORT).show();
                try {
                    JSONObject result = new JSONObject(out);
                    String status = result.getString("status");
                    if (status.equalsIgnoreCase("ok")) {
                        Toast.makeText(SubmissionActivity.this, "Your answer is Right!", Toast.LENGTH_LONG).show();
                        MapsActivity.longitude = result.getDouble("latitude");
                        MapsActivity.latitude = result.getDouble("longitude");
                        MainActivity.token = result.getString("token");

                        Intent intent = new Intent(SubmissionActivity.this, MapsActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);

                    } else if (status.equalsIgnoreCase("wrong_answer")) {
                        Toast.makeText(SubmissionActivity.this, "Your answer is Wrong!", Toast.LENGTH_LONG).show();
                    } else if (status.equalsIgnoreCase("finish")) {
                        Toast.makeText(SubmissionActivity.this, "Congratulations. Game Finish!", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(SubmissionActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    } else if (status.equalsIgnoreCase("err")) {
                        Toast.makeText(SubmissionActivity.this, "The Network is Error!", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(SubmissionActivity.this, "Your device is disconnecting.", Toast.LENGTH_LONG).show();
            }
        }
    }
}

package com.vmihalachi.turboeditor;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.vmihalachi.turboeditor.event.ErrorOpeningFileEvent;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;


public class Output extends Activity {

    private static final String TAG = "A0A";
    private static final String INPUT = "com.vmihalachi.turboeditor.fragment.userInput";
    private static final String FILE_PATH = "com.vmihalachi.turboeditor.fragment.filePath";
    private static final String CLIENT_SECRET = "d23a402188400212daacaaaae365d24394759c1d";
    private static final String CLIENT_ID = "8d5d1167884187a68f5e878e7514c2292e1d96c7086a.api.hackerearth.com";
    private static final String RUN_URL = "http://api.hackerearth.com/code/run/";
    private static final String COMPILE_URL = "http://api.hackerearth.com/code/compile/";
    private static String input;
    private static String lang;
    private static String file_path;
    private static String source;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_output);
        if (savedInstanceState == null) {
            PlaceholderFragment x = new PlaceholderFragment();
            getFragmentManager().beginTransaction()
                    .add(R.id.container, x)
                    .commit();
            x.setArgument(this);
        }
        Intent intent = getIntent();
        input = intent.getStringExtra(INPUT);
        file_path = intent.getStringExtra(FILE_PATH);
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

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        String text;
        String message,time_used,memory_used;
        Activity theActivity;
        public PlaceholderFragment() {
        }

        public void setArgument(Activity yourActivity){
            theActivity = yourActivity;
        }
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_output, container, false);
            return rootView;
        }

        @Override
        public void onStart(){
            super.onStart();
            compile_setup();
            RunCodeTask runcode = new RunCodeTask();
            runcode.execute();

        }

        public void compile_setup(){
            //now here I compile the source code
            try{
                final FileInputStream inputStream =
                        new FileInputStream(
                                new File(file_path));
                source = IOUtils.toString(inputStream);
                inputStream.close();
                Log.v(TAG, source);
                Log.v(TAG, input);
            }catch(Exception e){
                Log.e(TAG, e.getMessage(), e);
                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
                EventBus.getDefault().post(new ErrorOpeningFileEvent());
            }

            //to find the extension of the file(language of the code)
            int dot = file_path.lastIndexOf('.');
            String extension = (dot == -1) ? "" : file_path.substring(dot+1);
            switch(extension){
                case "c":
                    lang="C";
                    break;
                case "cpp":
                    lang="CPP";
                    break;
                case "java":
                    lang="JAVA";
                    break;
                case "py":
                    lang="PYTHON";
                    break;
                case "rb":
                    lang="RUBY";
                    break;
                default:
                    getActivity().finish();
            }
        }

        private class RunCodeTask extends AsyncTask<Void, Void, JSONObject>{
            private final String LOG_TAG = RunCodeTask.class.getSimpleName();

            @Override
            protected JSONObject doInBackground(Void... params){
                HttpClient httpclient = new DefaultHttpClient();
                HttpPost httppost = new HttpPost(RUN_URL);
                JSONObject result = new JSONObject();
                try{
                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(5);
                    nameValuePairs.add(new BasicNameValuePair("client_secret", CLIENT_SECRET));
                    nameValuePairs.add(new BasicNameValuePair("async", "0"));
                    nameValuePairs.add(new BasicNameValuePair("input", input));
                    nameValuePairs.add(new BasicNameValuePair("source", source));
                    nameValuePairs.add(new BasicNameValuePair("lang", lang));
                    httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                    // Execute HTTP Post Request
                    HttpResponse response = httpclient.execute(httppost);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                    StringBuilder builder = new StringBuilder();
                    for (String line = null; (line = reader.readLine()) != null;) {
                        builder.append(line).append("\n");
                    }
                    JSONTokener token = new JSONTokener(builder.toString());
                    result = new JSONObject(token);
                }catch (ClientProtocolException e) {
                    Log.e(LOG_TAG, e.getMessage(), e);
                }catch (IOException e) {
                    Log.e(LOG_TAG, e.getMessage(), e);
                }catch(JSONException e){
                    Log.e(LOG_TAG, e.getMessage(), e);
                }
                Log.v(LOG_TAG, result.toString());
                return result;
            }

            @Override
            protected void onPostExecute(JSONObject result){
                try {
                    if (result != null) {
                        if (result.getJSONObject("run_status").getString("status").equals("AC")&&result.getString("compile_status").equals("OK")) {

                            message = result.getString("message");
                            text = result.getJSONObject("run_status").getString("output");
                            time_used = "\nTime:"+result.getJSONObject("run_status").getString("time_used")+" sec";
                            memory_used = "\nMemory:"+result.getJSONObject("run_status").getString("memory_used");
                        } else {

                            message = "\nNot successful";
                            text = result.getString("compile_status");
                            time_used = "\nTime:0";
                            memory_used = "\nMemory:0";
                        }
                    }
                    else{
                        text = "No valid JSON Object received";
                        time_used = "\nTime:0";
                        memory_used = "\nMemory:0";
                        message = "\nNot successful";
                    }

                }catch(JSONException e){
                    Log.e(LOG_TAG, e.getMessage(), e);
                }
                /**
                 * NOW TO PRINT THE OUTPUT
                 */
                TextView final_output = (TextView) theActivity.findViewById(R.id.output_result);
                final_output.setText(text);

                TextView final_message = (TextView) theActivity.findViewById(R.id.output_message);
                final_message.setText(message);
                final_message.append(time_used);
                final_message.append(memory_used);
            }
        }
    }
}

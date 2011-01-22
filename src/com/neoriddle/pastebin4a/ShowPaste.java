package com.neoriddle.pastebin4a;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.neoriddle.pastebin4a.utils.AndroidUtils;

public class ShowPaste extends Activity {

    /**
     * The text to be pasted.
     */
    private static final String PASTE_DATA = "paste_data";

    /**
     * The development language used.
     */
    private static final String PASTE_LANGUAGE = "paste_lang";

    /**
     * Set this parameter to true.
     */
    private static final String PASTE_API_SUBMIT = "api_submit";

    /**
     * Pass xml or json to this parameter.
     */
    private static final String PASTE_MODE = "mode";

    /**
     * An alphanumeric username of the paste author.
     */
    private static final String PASTE_USER = "paste_user";

    /**
     * A password string to protect the paste.
     */
    private static final String PASTE_PASSWORD  = "paste_password";

    /**
     * Private post flag, having the values: yes or no
     */
    private static final String PASTE_PRIVATE  = "paste_private";

    /**
     * About dialog identifier.
     */
    private static final int ABOUT_DIALOG_ID = 0x01;

    /**
     * Paste response dialog identifier.
     */
    private static final int RESPONSE_DIALOG_ID = 0x02;

    private EditText pasteData;
    private Spinner pasteLanguage;
    private EditText pasteUser;
    private EditText pastePassword;
    private CheckBox private_flag;

    private int postedId;
    private String postedHash;

    private SharedPreferences preferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_paste);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        pasteData = (EditText)findViewById(R.id.paste_text);
        pasteUser = (EditText)findViewById(R.id.paste_user);
        pastePassword = (EditText)findViewById(R.id.paste_password);
        private_flag = (CheckBox)findViewById(R.id.paste_private);
        pasteLanguage = (Spinner)findViewById(R.id.paste_language);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.languages_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pasteLanguage.setAdapter(adapter);
    }

    public JSONObject sendPasteByGet()
        throws ClientProtocolException, IOException, JSONException {
        HttpClient httpclient = new DefaultHttpClient();

        String data = pasteData.getText().toString();
        String languaje = getLanguageCode(pasteLanguage.getSelectedItemPosition());
        String request = "http://paste.kde.org/?" +
            PASTE_DATA + "=" + data + "&" +
            PASTE_LANGUAGE + "=" + languaje  + "&" +
            PASTE_API_SUBMIT + "=true&" +
            PASTE_MODE + "=" + "json";
        HttpGet httpget = new HttpGet(request);

        String response = httpclient.execute(httpget, new BasicResponseHandler());
        return new JSONObject(response).getJSONObject("result");
    }

    public JSONObject sendPasteByPost()
        throws ClientProtocolException, IOException, JSONException {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://paste.kde.org/");

        httppost.setEntity(new UrlEncodedFormEntity(prepareHttpParameters(), HTTP.UTF_8));
        String response = httpclient.execute(httppost, new BasicResponseHandler());
        return new JSONObject(response).getJSONObject("result");
    }

    public List<NameValuePair> prepareHttpParameters() {
        List<NameValuePair> parameters = new ArrayList<NameValuePair>(2);

        parameters.add(new BasicNameValuePair(PASTE_DATA, pasteData.getText().toString()));
        parameters.add(new BasicNameValuePair(PASTE_LANGUAGE, getLanguageCode(pasteLanguage.getSelectedItemPosition())));
        parameters.add(new BasicNameValuePair(PASTE_API_SUBMIT, "true"));
        parameters.add(new BasicNameValuePair(PASTE_MODE, "json"));
        parameters.add(new BasicNameValuePair(PASTE_PRIVATE, private_flag.isSelected() ? "yes" : "no"));
        String pasteUserValue = pasteUser.getText().toString().trim();
        if(!"".equals(pasteUserValue.trim()))
            parameters.add(new BasicNameValuePair(PASTE_USER, pasteUserValue));
        String pastePasswordValue = pastePassword.getText().toString().trim();
        if(!"".equals(pastePasswordValue.trim()))
            parameters.add(new BasicNameValuePair(PASTE_PASSWORD, pastePasswordValue));

        return parameters;
    }

    public String getLanguageCode(int position) {
        String[] languageCodes = getResources().getStringArray(R.array.languages_codes_array);
        return languageCodes[position];

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.show_paste_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case R.id.sendMenu:
            // Avoid empty paste data
            if("".equals(pasteData.getText().toString().trim())) {
                Toast.makeText(this, R.string.paste_text_empty_warning, Toast.LENGTH_LONG).show();
                return true;
            }

            try {
                // Send paste
                JSONObject result =
                    "post".equals(preferences.getString("http_send_method", "post")) ?
                            sendPasteByPost() : sendPasteByGet();

                postedId = result.getInt("id");
                postedHash = result.getString("hash");
                showDialog(RESPONSE_DIALOG_ID);
            } catch(JSONException e) {
                // TODO: manage JSONException
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            } catch (ClientProtocolException e) {
                // TODO: manage ClientProtocolException
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                // TODO: manage IOException
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            }

            return true;
        case R.id.preferencesMenu:
            startActivity(new Intent(this, Preferences.class));
            return true;
        case R.id.aboutMenu:
            showDialog(ABOUT_DIALOG_ID);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        final LayoutInflater factory = LayoutInflater.from(this);

        switch(id) {
        case ABOUT_DIALOG_ID:
            final View aboutView = factory.inflate(R.layout.about_dialog, null);
            final TextView version = (TextView)aboutView.findViewById(R.id.version_label);
            version.setText(getString(R.string.version_msg, AndroidUtils.getAppVersionName(getApplicationContext())));

            return new AlertDialog.Builder(this).
                setIcon(R.drawable.icon).
                setTitle(R.string.app_name).
                setView(aboutView).
                setPositiveButton(R.string.close_button, null).
                create();
        case RESPONSE_DIALOG_ID:
            return new AlertDialog.Builder(this).
                setIcon(R.drawable.icon).
                setTitle(R.string.paste_created_dialog_title).
                setView(factory.inflate(R.layout.paste_created_dialog, null)).
                setNeutralButton(R.string.close_button, null).
                setPositiveButton(R.string.copy_button, new CopUrl()).
                create();
        default:
            return super.onCreateDialog(id);
        }
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch(id) {
        case RESPONSE_DIALOG_ID:
            final TextView pasteId = (TextView)dialog.findViewById(R.id.pasted_id);
            final TextView pasteHash = (TextView)dialog.findViewById(R.id.pasted_hash);
            final TextView pasteUrl = (TextView)dialog.findViewById(R.id.pasted_url);
            pasteId.setText(Integer.toString(postedId));
            pasteHash.setText(postedHash);
            pasteUrl.setText("http://paste.kde.org/" + postedId + "/" +
                    ("".equals(postedHash.trim()) ? "" : postedHash + "/"));
            break;
        default:
            break;
        }
        super.onPrepareDialog(id, dialog);
    }

    class CopUrl implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
            clipboard.setText("http://paste.kde.org/" + postedId + "/" +
                    ("".equals(postedHash.trim()) ? "" : postedHash + "/"));
            // TODO: show a toast notifying url pasted to clipboard
        }
    }
}
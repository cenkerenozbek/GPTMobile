package com.example.gptmobile;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;

import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    TextView welcometextView;
    EditText messageEditText;
    ImageButton sendButton;
    List<Message> messageList;
    MessageAdapter messageAdapter;
    TextView textView;
    public static final MediaType JSON
            = MediaType.get("application/json");
    OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        messageList = new ArrayList<>();

        recyclerView = findViewById(R.id.recycler_view);
        welcometextView = findViewById(R.id.welcome_text);
        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_btn);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        messageList = new ArrayList<>();

        recyclerView = findViewById(R.id.recycler_view);
        welcometextView = findViewById(R.id.welcome_text);
        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_btn);

        //setup recycler view
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        recyclerView.setLayoutManager(llm);
        if (!isNetworkAvailable()) {
            showNetworkAlertDialog();
        }

        // Initially set send button as unavailable
        sendButton.setImageResource(R.drawable.send_button_unavailable);
        sendButton.setEnabled(false);

        // TextWatcher to monitor EditText changes
        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // Check if EditText is empty
                if (TextUtils.isEmpty(charSequence)) {
                    // Set send button color to unavailable
                    sendButton.setImageResource(R.drawable.send_button_unavailable);
                    sendButton.setEnabled(false);
                } else {
                    // Set send button color to available
                    sendButton.setImageResource(R.drawable.sendbutton);
                    sendButton.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        sendButton.setOnClickListener((v) -> {
            String question = messageEditText.getText().toString().trim();
            sendMessage(question);
        });

        messageEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String question = messageEditText.getText().toString().trim();
                    sendMessage(question);
                    return true;
                }
                return false;
            }
        });

        // Set up the recommended prompts click listeners
        findViewById(R.id.prompt_one).setOnClickListener(v -> handlePromptClick(getString(R.string.recommended_prompt_one)));
        findViewById(R.id.prompt_two).setOnClickListener(v -> handlePromptClick(getString(R.string.recommended_prompt_two)));
        findViewById(R.id.prompt_three).setOnClickListener(v -> handlePromptClick(getString(R.string.recommended_prompt_three)));
        findViewById(R.id.prompt_four).setOnClickListener(v -> handlePromptClick(getString(R.string.recommended_prompt_four)));

    }

    private void handlePromptClick(String prompt) {
        sendMessage(prompt);
    }

    private void sendMessage(String message) {
        addToChat(message, Message.SENT_BY_ME);
        messageEditText.setText("");
        callAPI(message);
        welcometextView.setVisibility(View.GONE);
        findViewById(R.id.recommended_layout).setVisibility(View.GONE);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem displayModeItem = menu.findItem(R.id.action_settings_display_mode);

        // Check the current theme and set the appropriate title
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            displayModeItem.setTitle(R.string.switch_to_light_mode);
        } else {
            displayModeItem.setTitle(R.string.switch_to_dark_mode);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings_language) {
            // Show language selection dialog
            showLanguageSelectionDialog();
            return true;
        } else if (id == R.id.action_settings_display_mode) {
            // Toggle day-night mode
            toggleDayNightMode();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleDayNightMode() {
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
            // Change to night mode
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            // Change to day mode
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        // Recreate the activity to apply the new theme
        recreate();

        // Invalidate the options menu to refresh the menu titles
        invalidateOptionsMenu();
    }

    private void showLanguageSelectionDialog() {
        final String[] languages = {"English", "Türkçe", "Deutsch", "Español", "Français", "Italiano",};
        final String[] languageCodes = {"en", "tr", "de", "es", "fr", "it"};
        final int[] flagIcons = {
                R.drawable.flag_en,
                R.drawable.flag_tr,
                R.drawable.flag_de,
                R.drawable.flag_es,
                R.drawable.flag_fr,
                R.drawable.flag_it
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.action_settings_language)
                .setAdapter(new LanguageAdapter(this, languages, flagIcons), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String selectedLanguageCode = languageCodes[which];
                        // Change language
                        setLocale(selectedLanguageCode);
                        // Restart activity
                        recreate();
                    }
                });

        builder.create().show();
    }

    private void setLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        Resources resources = getResources();
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    void addToChat(String message, String sentBy) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageList.add(new Message(message, sentBy));
                messageAdapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());
            }
        });
    }

    void addResponse(String response) {
        runOnUiThread(() -> {
            messageAdapter.notifyDataSetChanged();
            recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());
            animateTyping(response);
        });
    }

    void animateTyping(String response) {
        final int delayMillis = 50; // Adjust the delay as needed for desired typing speed

        StringBuilder currentText = new StringBuilder();
        for (int i = 0; i <= response.length(); i++) {
            final String textToShow = response.substring(0, i); // Substring to current index
            Handler handler = new Handler();
            handler.postDelayed(() -> {
                messageList.set(messageList.size() - 1, new Message(textToShow, Message.SENT_BY_BOT));
                messageAdapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());
            }, delayMillis * i);
        }
    }

    void callAPI(String question) {

        messageList.add(new Message(" ... ", Message.SENT_BY_BOT));

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", "gpt-3.5-turbo");
            JSONArray messageArr = new JSONArray();
            JSONObject obj = new JSONObject();
            obj.put("role", "user");
            obj.put("content", question);
            messageArr.put(obj);

            jsonBody.put("messages", messageArr);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer  YOUR_API_KEY_HERE")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                addResponse("Failed to load response due to " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = null;
                        jsonArray = jsonObject.getJSONArray("choices");
                        String result = jsonArray.getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");
                        addResponse(result.trim());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                } else {
                    addResponse("Failed to load response due to " + response.body().string());
                }

            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(manager.getActiveNetwork());
        boolean isAvailable = false;

        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                isAvailable = true;
            }
        }
        return isAvailable;
    }

    private void showNetworkAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.network_alert_title)
                .setMessage(R.string.network_alert_message)
                .setPositiveButton(R.string.network_alert_positive_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Do nothing or handle accordingly
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();

        // Get the positive button and change its color according to the theme attribute
        Button positiveButton = alert.getButton(AlertDialog.BUTTON_POSITIVE);
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimaryVariant, typedValue, true);
        positiveButton.setTextColor(typedValue.data);
    }
}

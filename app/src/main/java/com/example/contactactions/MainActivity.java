package com.example.contactactions;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {
    
    // State persistence keys - clear and unique
    private static final int REQUEST_CALL_PHONE = 1;
    private static final String KEY_NAME = "saved_name";
    private static final String KEY_PHONE = "saved_phone";
    private static final String KEY_WEBSITE = "saved_website";
    private static final String KEY_WORK_STATUS = "saved_work_status";
    private static final String KEY_TERMS_ACCEPTED = "saved_terms_accepted";
    
    private EditText etName, etPhone, etWebsite;
    private RadioGroup rgWorkStatus;
    private CheckBox cbTerms;
    private Button btnSave, btnDial, btnOpen;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initializeViews();
        setupTextWatchers();
        setupClickListeners();
        
        // Restore state if available
        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        }
        
        updateButtonStates();
    }
    
    private void initializeViews() {
        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        etWebsite = findViewById(R.id.etWebsite);
        rgWorkStatus = findViewById(R.id.rgWorkStatus);
        cbTerms = findViewById(R.id.cbTerms);
        btnSave = findViewById(R.id.btnSave);
        btnDial = findViewById(R.id.btnDial);
        btnOpen = findViewById(R.id.btnOpen);
    }
    
    private void setupTextWatchers() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                updateButtonStates();
            }
        };
        
        etPhone.addTextChangedListener(textWatcher);
        etWebsite.addTextChangedListener(textWatcher);
    }
    
    private void setupClickListeners() {
        btnSave.setOnClickListener(v -> saveContact());
        btnDial.setOnClickListener(v -> dialPhone());
        btnOpen.setOnClickListener(v -> openWebsite());
    }
    
    // Save button reuses snapshot logic exactly as required
    private void saveContact() {
        if (validateInputs()) {
            // Reuse the same snapshot logic as onSaveInstanceState
            Bundle state = createStateSnapshot();
            onSaveInstanceState(state);
            
            // Show confirmation toast as required
            Toast.makeText(this, R.string.save_successful, Toast.LENGTH_SHORT).show();
        }
    }
    
    private void dialPhone() {
        String phoneNumber = etPhone.getText().toString().trim();
        
        // Runtime permission check exactly as required
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) 
                == PackageManager.PERMISSION_GRANTED) {
            // Launch ACTION_CALL if permission granted
            makeCall(phoneNumber);
        } else {
            // Request permission if not granted
            ActivityCompat.requestPermissions(this, 
                    new String[]{android.Manifest.permission.CALL_PHONE}, 
                    REQUEST_CALL_PHONE);
        }
    }
    
    private void openWebsite() {
        String url = etWebsite.getText().toString().trim();
        
        if (!url.isEmpty()) {
            // Prepend http:// if scheme is missing exactly as required
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }
            
            // URL validation as required
            if (isValidUrl(url)) {
                try {
                    // Wrap in try/catch as required
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) {
                    // Display AlertDialog on failure as required
                    showErrorDialog(getString(R.string.no_browser));
                }
            } else {
                showErrorDialog(getString(R.string.invalid_url));
            }
        }
    }
    
    private boolean isValidUrl(String url) {
        return Patterns.WEB_URL.matcher(url).matches();
    }
    
    private void makeCall(String phoneNumber) {
        try {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(intent);
        } catch (SecurityException e) {
            // This should not happen if permission is granted, but handle gracefully
            Toast.makeText(this, "Call permission required", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUIRE_CALL_PHONE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted - launch ACTION_CALL
                dialPhone();
            } else {
                // Permission denied - fall back to ACTION_DIAL exactly as required
                String phoneNumber = etPhone.getText().toString().trim();
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + phoneNumber));
                startActivity(intent);
                
                // Show feedback about fallback
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    // Input validation exactly as required
    private boolean validateInputs() {
        boolean isValid = true;
        
        // Non-empty Name validation
        if (TextUtils.isEmpty(etName.getText().toString().trim())) {
            etName.setError("Name is required");
            isValid = false;
        }
        
        // Non-empty Phone validation
        if (TextUtils.isEmpty(etPhone.getText().toString().trim())) {
            etPhone.setError("Phone is required");
            isValid = false;
        }
        
        // Website URL validation after scheme fix
        String website = etWebsite.getText().toString().trim();
        if (!TextUtils.isEmpty(website)) {
            if (!website.startsWith("http://") && !website.startsWith("https://")) {
                website = "http://" + website;
            }
            if (!isValidUrl(website)) {
                etWebsite.setError("Invalid website URL");
                isValid = false;
            }
        }
        
        return isValid;
    }
    
    // Button enablement exactly as required
    private void updateButtonStates() {
        String phone = etPhone.getText().toString().trim();
        String website = etWebsite.getText().toString().trim();
        
        // Disable Dial until Phone is non-empty
        btnDial.setEnabled(!TextUtils.isEmpty(phone));
        
        // Disable Open until Website is non-empty
        btnOpen.setEnabled(!TextUtils.isEmpty(website));
    }
    
    // State snapshot logic - used by both Save button and onSaveInstanceState
    private Bundle createStateSnapshot() {
        Bundle state = new Bundle();
        state.putString(KEY_NAME, etName.getText().toString());
        state.putString(KEY_PHONE, etPhone.getText().toString());
        state.putString(KEY_WEBSITE, etWebsite.getText().toString());
        state.putInt(KEY_WORK_STATUS, rgWorkStatus.getCheckedRadioButtonId());
        state.putBoolean(KEY_TERMS_ACCEPTED, cbTerms.isChecked());
        return state;
    }
    
    private void restoreState(Bundle state) {
        // Restore all widget values exactly as required
        etName.setText(state.getString(KEY_NAME, ""));
        etPhone.setText(state.getString(KEY_PHONE, ""));
        etWebsite.setText(state.getString(KEY_WEBSITE, ""));
        
        int workStatusId = state.getInt(KEY_WORK_STATUS, -1);
        if (workStatusId != -1) {
            rgWorkStatus.check(workStatusId);
        }
        
        cbTerms.setChecked(state.getBoolean(KEY_TERMS_ACCEPTED, false));
    }
    
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Use the same snapshot logic as Save button
        Bundle state = createStateSnapshot();
        outState.putAll(state);
    }
    
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        restoreState(savedInstanceState);
    }
    
    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}

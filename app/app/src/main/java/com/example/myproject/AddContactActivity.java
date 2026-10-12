package com.example.myproject;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AddContactActivity extends AppCompatActivity {

    private EditText nameEditText, phoneEditText;
    private Button saveButton;
    private ContactDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);

        nameEditText = findViewById(R.id.editTextName);
        phoneEditText = findViewById(R.id.editTextPhone);
        saveButton = findViewById(R.id.buttonSave);

        dbHelper = new ContactDatabaseHelper(this);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveContact();
            }
        });
    }

    private void saveContact() {
        String name = nameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean success = dbHelper.addContact(name, phone);
        if (success) {
            Toast.makeText(this, "Contact saved", Toast.LENGTH_SHORT).show();
            finish(); // Go back to previous screen
        } else {
            Toast.makeText(this, "Failed to save contact", Toast.LENGTH_SHORT).show();
        }
    }
}

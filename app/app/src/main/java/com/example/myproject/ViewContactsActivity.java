package com.example.myproject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class ViewContactsActivity extends AppCompatActivity {

    private ListView listView;
    private ContactDatabaseHelper dbHelper;
    private List<Contact> contactList;
    private ArrayAdapter<String> adapter;
    private List<String> formattedContacts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_contacts);

        listView = findViewById(R.id.contactListView);
        dbHelper = new ContactDatabaseHelper(this);
        contactList = dbHelper.getAllContacts();

        formattedContacts = getFormattedContacts(contactList);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, formattedContacts);
        listView.setAdapter(adapter);

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            Contact selectedContact = contactList.get(position);

            new AlertDialog.Builder(this)
                    .setTitle("Delete Contact")
                    .setMessage("Are you sure you want to delete " + selectedContact.getName() + "?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        boolean deleted = dbHelper.deleteContact(selectedContact.getId());
                        if (deleted) {
                            contactList.remove(position);
                            formattedContacts.remove(position);
                            adapter.notifyDataSetChanged();
                            Toast.makeText(this, "Contact deleted", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();

            return true;
        });
    }

    private List<String> getFormattedContacts(List<Contact> contactList) {
        List<String> formatted = new ArrayList<>();
        for (Contact contact : contactList) {
            formatted.add(contact.getName() + " - " + contact.getPhone());
        }
        return formatted;
    }
}

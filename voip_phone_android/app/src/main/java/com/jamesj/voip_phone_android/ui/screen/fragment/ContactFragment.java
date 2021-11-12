package com.jamesj.voip_phone_android.ui.screen.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.jamesj.voip_phone_android.R;
import com.jamesj.voip_phone_android.service.contact.ContactInfo;
import com.jamesj.voip_phone_android.service.contact.ContactManager;
import com.jamesj.voip_phone_android.ui.screen.activity.MasterFragmentActivity;
import com.jamesj.voip_phone_android.ui.screen.fragment.base.contact.ContactListAdapter;
import com.jamesj.voip_phone_android.ui.screen.fragment.base.contact.ContactManagerFragment;
import com.orhanobut.logger.Logger;

import org.apache.commons.lang3.StringUtils;

public class ContactFragment extends Fragment {

    ///////////////////////////////////////////////

    private ViewGroup rootView;
    private ContactManagerFragment contactManagerFragment = null;
    private ContactManager contactManager = null;
    private ContactListAdapter adapter;

    ///////////////////////////////////////////////

    private ListView listView;
    private Button addContactButton;
    private Button searchContactButton;

    ///////////////////////////////////////////////

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getContext() == null) {
            return null;
        }

        rootView = (ViewGroup) inflater.inflate(R.layout.activity_contact, container, false);

        //
        addContactButton = rootView.findViewById(R.id.contact_add_button); addContactButton.setBackgroundColor(Color.BLACK);
        addContactButton.setOnClickListener(this::addContact);

        searchContactButton = rootView.findViewById(R.id.contact_search_button); searchContactButton.setBackgroundColor(Color.BLACK);
        searchContactButton.setOnClickListener(this::searchContact);
        //

        //
        contactManager = new ContactManager(rootView.getContext());
        adapter = new ContactListAdapter(contactManager);
        listView = rootView.findViewById(R.id.list_view);
        listView.setAdapter(adapter);
        //

        //
        getParentFragmentManager().setFragmentResultListener(MasterFragmentActivity.CONTACT_ADD_KEY, this, (key, bundle) -> {
            String name = bundle.getString("NAME");
            String email = bundle.getString("EMAIL");
            String mdn = bundle.getString("MDN");
            String sipIp = bundle.getString("SIP_IP");
            String sipPort = bundle.getString("SIP_PORT");
            //Logger.d("ContactFragment: name=[%s], email=[%s], mdn=[%s], sipIp=[%s], sipPort=[%s]", name, email, mdn, sipIp, sipPort);

            if (!TextUtils.isDigitsOnly(sipPort)) { sipPort = "5555"; }
            if (name == null || name.length() == 0) { name = "none"; }
            if (email == null || email.length() == 0) { email = "none"; }
            if (mdn == null || mdn.length() == 0 || sipIp == null || sipIp.length() == 0 || sipPort == null || sipPort.length() == 0 || !StringUtils.isNumeric(sipPort)) { return; }

            ContactInfo contactInfo = contactManager.addContactInfo(name, email, mdn, sipIp, Integer.parseInt(sipPort), true);
            if (contactInfo == null) {
                Toast.makeText(getContext(), "Fail to add contact_info. (name=[" + name + "]\n" + "email=[" + email + "]\n" + "mdn=[" + mdn + "]\n" + "sipIp=[" + sipIp + "]\n" + "sipPort=[" + sipPort + "])", Toast.LENGTH_SHORT).show();
                return;
            }

            listView.setAdapter(adapter);
        });
        //

        return rootView;
    }

    public void addContact(View v){
        FragmentManager fragmentManager = getParentFragmentManager();
        contactManagerFragment = new ContactManagerFragment(this);
        fragmentManager.beginTransaction().add(R.id.container, contactManagerFragment).commit();
        fragmentManager.beginTransaction().hide(this).commit();
        fragmentManager.beginTransaction().show(contactManagerFragment).commit();
    }

    public void searchContact(View v) {
        // Search by mdn
        EditText searchEditText = rootView.findViewById(R.id.editText_search);
        String mdn = searchEditText.getText().toString();
        if (mdn.length() == 0) {
            return;
        }

        ContactInfo contactInfo = contactManager.getContactInfoByMdn(mdn);
        if (contactInfo == null) {
            return;
        }

        Logger.d("GET ContactInfo by MDN(%s). (%s)", mdn, contactInfo);
        int index = contactManager.getIndexByContactInfo(contactInfo);
        Logger.d("(%d) getItem [%s]", index, listView.getAdapter().getItem(index));
    }

    ///////////////////////////////////////////////

    public ContactManagerFragment getContactManagerFragment() {
        return contactManagerFragment;
    }

    public void setContactManagerFragment(ContactManagerFragment contactManagerFragment) {
        this.contactManagerFragment = contactManagerFragment;
    }

}

package com.jamesj.voip_phone_android.ui.screen.fragment;

import android.app.AlertDialog;
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

    private ListView contactListView;
    private Button addContactButton;
    private Button searchContactButton;
    private Button modifyContactButton;
    private Button removeContactButton;

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

        modifyContactButton = rootView.findViewById(R.id.contact_modify_button); modifyContactButton.setBackgroundColor(Color.BLACK);
        modifyContactButton.setOnClickListener(this::modifyContact);

        removeContactButton = rootView.findViewById(R.id.contact_remove_button); removeContactButton.setBackgroundColor(Color.BLACK);
        removeContactButton.setOnClickListener(this::alertToRemoveContact);
        //

        //
        contactManager = new ContactManager(rootView.getContext());

        adapter = new ContactListAdapter(getParentFragmentManager(), contactManager);

        contactListView = rootView.findViewById(R.id.list_view);
        contactListView.setAdapter(adapter);
        contactListView.setClickable(true);
        /*contactListView.setOnItemClickListener((parent, view, position, id) -> {
            // Toggle
            contactListView.setItemChecked(position, !contactListView.isItemChecked(position));
        });*/
        //

        //
        getParentFragmentManager().setFragmentResultListener(MasterFragmentActivity.CONTACT_ADD_KEY, this, (key, bundle) -> {
            String name = bundle.getString("NAME");
            String email = bundle.getString("EMAIL");
            String mdn = bundle.getString("MDN");
            String sipIp = bundle.getString("SIP_IP");
            String sipPort = bundle.getString("SIP_PORT");
            boolean isModified = bundle.getBoolean("IS_MODIFIED");
            //Logger.d("ContactFragment: name=[%s], email=[%s], mdn=[%s], sipIp=[%s], sipPort=[%s]", name, email, mdn, sipIp, sipPort);

            if (!TextUtils.isDigitsOnly(sipPort)) { sipPort = "5555"; }
            if (name == null || name.length() == 0) { name = "none"; }
            if (email == null || email.length() == 0) { email = "none"; }
            if (mdn == null || mdn.length() == 0 || sipIp == null || sipIp.length() == 0 || sipPort == null || sipPort.length() == 0 || !StringUtils.isNumeric(sipPort)) { return; }

            ContactInfo contactInfo = contactManager.addContactInfo(name, email, mdn, sipIp, Integer.parseInt(sipPort), true, isModified);
            if (contactInfo == null) {
                Toast.makeText(getContext(), "Fail to add contact_info. (name=[" + name + "]\n" + "email=[" + email + "]\n" + "mdn=[" + mdn + "]\n" + "sipIp=[" + sipIp + "]\n" + "sipPort=[" + sipPort + "])", Toast.LENGTH_SHORT).show();
                return;
            }

            contactListView.setAdapter(adapter);
        });
        //

        return rootView;
    }

    public void addContact(View v){
        FragmentManager fragmentManager = getParentFragmentManager();
        contactManagerFragment = new ContactManagerFragment(this);
        contactManagerFragment.setArguments(null);
        fragmentManager.beginTransaction().add(R.id.container, contactManagerFragment).commit();
        fragmentManager.beginTransaction().hide(this).commit();
        fragmentManager.beginTransaction().show(contactManagerFragment).commit();
    }

    public void searchContact(View v) {
        // Search by mdn
        EditText searchEditText = rootView.findViewById(R.id.editText_search);
        String mdn = searchEditText.getText().toString();
        if (mdn.length() == 0) {
            Toast.makeText(getContext(), "MDN is not specified.", Toast.LENGTH_SHORT).show();
            return;
        }

        ContactInfo contactInfo = contactManager.getContactInfoByMdn(mdn);
        if (contactInfo == null) {
            Toast.makeText(getContext(), "Fail to find the contact.", Toast.LENGTH_SHORT).show();
            return;
        }

        //Logger.d("GET ContactInfo by MDN(%s). (%s)", mdn, contactInfo);
        int index = contactManager.getIndexByContactInfo(contactInfo);
        Logger.d("[SEARCH] (%d) getItem [%s]", index, contactListView.getAdapter().getItem(index));

        contactListView.setSelection(index);
        contactListView.setItemChecked(index, true);
    }

    public void modifyContact(View v) {
        int count = adapter.getCount(), checked;
        if (count > 0) {
            // 현재 선택된 아이템의 position 획득
            checked = contactListView.getCheckedItemPosition();
            if (checked > -1 && checked < count) {
                ContactInfo contactInfo = contactManager.getContactInfoByIndex(checked);
                if (contactInfo == null) {
                    Logger.w("[MODIFY CONTACT] Not found... (index=%d)", checked);
                    return;
                }

                Logger.d("[MODIFY] (%d) getItem [%s]", checked, contactListView.getAdapter().getItem(checked));

                Bundle resultBundle = new Bundle();

                String name = contactInfo.getName();
                String email = contactInfo.getEmail();
                String mdn = contactInfo.getMdn();
                String sipIp = contactInfo.getSipIp();
                String sipPort = String.valueOf(contactInfo.getSipPort());

                resultBundle.putString("NAME", name);
                resultBundle.putString("EMAIL", email);
                resultBundle.putString("MDN", mdn);
                resultBundle.putString("SIP_IP", sipIp);
                resultBundle.putString("SIP_PORT", sipPort);

                FragmentManager fragmentManager = getParentFragmentManager();
                contactManagerFragment = new ContactManagerFragment(this);
                contactManagerFragment.setArguments(resultBundle);
                fragmentManager.beginTransaction().add(R.id.container, contactManagerFragment).commit();
                fragmentManager.beginTransaction().hide(this).commit();
                fragmentManager.beginTransaction().show(contactManagerFragment).commit();

                // listview 갱신
                adapter.notifyDataSetChanged();
            }
        }
    }

    public void removeContact(View v) {
        int count = adapter.getCount(), checked;
        if (count > 0) {
            // 현재 선택된 아이템의 position 획득
            checked = contactListView.getCheckedItemPosition();

            if (checked > -1 && checked < count) {
                ContactInfo contactInfo = contactManager.getContactInfoByIndex(checked);
                if (contactInfo == null) {
                    Logger.w("[MODIFY CONTACT] Not found... (index=%d)", checked);
                    return;
                }

                Logger.d("[REMOVE] (%d) getItem [%s]", checked, contactListView.getAdapter().getItem(checked));
                if (contactManager.removeContactInfoFromFile(contactInfo)) {
                    Logger.d("[REMOVE] Success to remove the contactInfo. (%s)", contactInfo);
                }
                contactManager.deleteContactInfo(contactInfo);

                // listview 선택 초기화
                contactListView.clearChoices();

                // listview 갱신
                adapter.notifyDataSetChanged();
            }
        }
    }

    public void alertToRemoveContact(View v) {
        int count = adapter.getCount(), checked;
        if (count > 0) {
            checked = contactListView.getCheckedItemPosition();
            if (checked > -1 && checked < count) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage("정말 삭제하시겠습니까?");
                builder.setPositiveButton("예", (dialog, which) -> {
                    removeContact(v);
                });
                builder.setNegativeButton("아니오", null);
                builder.create().show();
            }
        }
    }

    ///////////////////////////////////////////////

    public ContactManagerFragment getContactManagerFragment() {
        return contactManagerFragment;
    }

    public void setContactManagerFragment(ContactManagerFragment contactManagerFragment) {
        this.contactManagerFragment = contactManagerFragment;
    }

    public void clearContactListView() {
        for (int i = 0; i < contactListView.getCount(); i++) {
            if (contactListView.isItemChecked(i)) {
                contactListView.setItemChecked(i, false);
                break;
            }
        }

        contactListView.clearChoices();
        contactListView.clearFocus();
    }

}

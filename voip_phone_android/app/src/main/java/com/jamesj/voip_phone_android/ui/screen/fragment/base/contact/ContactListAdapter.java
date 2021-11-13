package com.jamesj.voip_phone_android.ui.screen.fragment.base.contact;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.FragmentManager;

import com.jamesj.voip_phone_android.R;
import com.jamesj.voip_phone_android.service.AppInstance;
import com.jamesj.voip_phone_android.service.contact.ContactInfo;
import com.jamesj.voip_phone_android.service.contact.ContactManager;
import com.jamesj.voip_phone_android.ui.screen.activity.MasterFragmentActivity;
import com.orhanobut.logger.Logger;

public class ContactListAdapter extends BaseAdapter {

    ///////////////////////////////////////////////

    private final FragmentManager fragmentManager;
    private final ContactManager contactManager;
    private ContactInfo curContactInfo = null;

    ///////////////////////////////////////////////

    @Override
    public int getCount() {
        return contactManager.getContactListSize();
    }

    @Override
    public Object getItem(int position) {
        return contactManager.getContactInfoByIndex(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    ///////////////////////////////////////////////

    public ContactListAdapter(FragmentManager fragmentManager, ContactManager contactManager) {
        this.fragmentManager = fragmentManager;
        this.contactManager = contactManager;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final Context context = parent.getContext();
        if (contactManager.getContactListSize() == 0) {
            Logger.w("ContactList is empty.");
            return null;
        }

        if(convertView == null) {
            LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(R.layout.contact_info_layout, parent, false);
        }

        ContactInfo contactInfo = contactManager.getContactInfoByIndex(position);
        if (contactInfo == null) {
            Logger.w("ContactInfo is null.");
            return null;
        }

        //
        //ImageView imageView = convertView.findViewById(R.id.contact_info_imageView);
        TextView nameTextView = convertView.findViewById(R.id.textView_name);
        TextView emailTextView = convertView.findViewById(R.id.textView_email);
        TextView mdnTextView = convertView.findViewById(R.id.textView_mdn);
        TextView sipIpTextView = convertView.findViewById(R.id.textView_sip_ip);
        TextView sipPortTextView = convertView.findViewById(R.id.textView_sip_port);
        //

        //
        Button callButton = convertView.findViewById(R.id.button_call); callButton.setBackgroundColor(Color.BLACK);
        this.curContactInfo = contactInfo;
        callButton.setOnClickListener(this::call);
        //

        //
        //imageView.setImageResource(R.drawable.unspecified_person);
        nameTextView.setText(contactInfo.getName());
        emailTextView.setText(contactInfo.getEmail());
        mdnTextView.setText(contactInfo.getMdn());
        sipIpTextView.setText(contactInfo.getSipIp());
        sipPortTextView.setText(String.valueOf(contactInfo.getSipPort()));
        //

        //
        /*CheckBox checkBox = convertView.findViewById(R.id.contact_checkbox);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                buttonView.setChecked(false);
            }
        });*/
        //

        //Logger.d("ContactListAdapter: contactInfo=[%s]", contactInfo);
        //Toast.makeText(context, "name=[" + contactInfo.getName() + "]\n" + "email=[" + contactInfo.getEmail() + "]\n" + "mdn=[" + contactInfo.getMdn() + "]\n" + "sipIp=[" + contactInfo.getSipIp() + "]\n" + "sipPort=[" + contactInfo.getSipPort() + "]", Toast.LENGTH_SHORT).show();

        return convertView;
    }

    public void call(View v) {
        AppInstance.getInstance().setCurContactInfo(curContactInfo);

        MasterFragmentActivity masterFragmentActivity = AppInstance.getInstance().getMasterFragmentActivity();
        if (masterFragmentActivity == null) {
            return;
        }

        // 1. PhoneFragment 의 remoteHostNameEditText 에 MDN 설정
        masterFragmentActivity.getPhoneFragment().getRemoteHostNameEditText().setText(curContactInfo.getMdn());
        //

        // 2. PhoneFragment 로 화면 전환
        masterFragmentActivity.getTabLayout().selectTab(
                masterFragmentActivity.getTabLayout().getTabAt(0)
        );
        masterFragmentActivity.hideContactFragment();
        masterFragmentActivity.getSupportFragmentManager().
                beginTransaction().show(masterFragmentActivity.getPhoneFragment()).commit();
        //
    }

}

package com.jamesj.voip_phone_android.ui.screen.fragment.base.contact;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.jamesj.voip_phone_android.R;
import com.jamesj.voip_phone_android.service.contact.ContactInfo;
import com.jamesj.voip_phone_android.service.contact.ContactManager;
import com.orhanobut.logger.Logger;

public class ContactListAdapter extends BaseAdapter {

    ///////////////////////////////////////////////

    private final ContactManager contactManager;

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

    public ContactListAdapter(ContactManager contactManager) {
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
        callButton.setOnClickListener(this::call);

        Button modifyContactButton = convertView.findViewById(R.id.contact_modify_button); modifyContactButton.setBackgroundColor(Color.BLACK);
        //modifyContactButton.setOnClickListener(this::modifyContact);

        Button removeContactButton = convertView.findViewById(R.id.contact_remove_button); removeContactButton.setBackgroundColor(Color.BLACK);
        //removeContactButton.setOnClickListener(this::removeContact);
        //

        //
        //imageView.setImageResource(R.drawable.unspecified_person);
        nameTextView.setText(contactInfo.getName());
        emailTextView.setText(contactInfo.getEmail());
        mdnTextView.setText(contactInfo.getMdn());
        sipIpTextView.setText(contactInfo.getSipIp());
        sipPortTextView.setText(String.valueOf(contactInfo.getSipPort()));
        //

        //Logger.d("ContactListAdapter: contactInfo=[%s]", contactInfo);
        //Toast.makeText(context, "name=[" + contactInfo.getName() + "]\n" + "email=[" + contactInfo.getEmail() + "]\n" + "mdn=[" + contactInfo.getMdn() + "]\n" + "sipIp=[" + contactInfo.getSipIp() + "]\n" + "sipPort=[" + contactInfo.getSipPort() + "]", Toast.LENGTH_SHORT).show();

        return convertView;
    }

    public void call(View v) {

    }

    /*public void modifyContact(View v) {
        int count, checked;
        count = getCount();

        if (count > 0) {
            // 현재 선택된 아이템의 position 획득
            checked = listView.getCheckedItemPosition();
            if (checked > -1 && checked < count) {
                // TODO : ContactInfo 수정
                ContactInfo contactInfo = contactManager.getContactInfoByIndex(checked);
                if (contactInfo == null) {
                    Logger.w("[MODIFY CONTACT] Not found... (index=%d)", checked);
                    return;
                }

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

                // listview 갱신
                notifyDataSetChanged();
            }
        }
    }

    public void removeContact(View v) {
        int count, checked;
        count = getCount();

        if (count > 0) {
            // 현재 선택된 아이템의 position 획득
            checked = listView.getCheckedItemPosition();

            if (checked > -1 && checked < count) {
                // TODO : ContactInfo 삭제


                // listview 선택 초기화
                listView.clearChoices();

                // listview 갱신
                notifyDataSetChanged();
            }
        }
    }*/

}

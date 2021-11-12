package com.jamesj.voip_phone_android.ui.screen.fragment.base.contact;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.jamesj.voip_phone_android.R;
import com.jamesj.voip_phone_android.ui.screen.activity.MasterFragmentActivity;
import com.jamesj.voip_phone_android.ui.screen.fragment.ContactFragment;

public class ContactManagerFragment extends Fragment {

    ///////////////////////////////////////////////

    private ContactFragment contactFragment;
    private ViewGroup rootView;

    ///////////////////////////////////////////////

    private Button saveButton;
    private Button cancelButton;

    ///////////////////////////////////////////////

    ImageView imageView;

    ///////////////////////////////////////////////

    EditText nameEditText;
    EditText emailEditText;
    EditText mdnEditText;
    EditText sipIpEditText;
    EditText sipPortEditText;

    ///////////////////////////////////////////////

    public ContactManagerFragment(ContactFragment contactFragment) {
        this.contactFragment = contactFragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = (ViewGroup) inflater.inflate(R.layout.add_contact_layout, container, false);

        saveButton = rootView.findViewById(R.id.button_save); saveButton.setBackgroundColor(Color.BLACK);
        saveButton.setOnClickListener(this::save);
        cancelButton = rootView.findViewById(R.id.button_cancel); cancelButton.setBackgroundColor(Color.BLACK);
        cancelButton.setOnClickListener(this::cancel);

        /*imageView = rootView.findViewById(R.id.add_contact_imageView);
        imageView.setImageResource(R.drawable.unspecified_person);*/

        nameEditText = rootView.findViewById(R.id.editText_name);
        emailEditText = rootView.findViewById(R.id.editText_email);
        mdnEditText = rootView.findViewById(R.id.editText_mdn);
        sipIpEditText = rootView.findViewById(R.id.editText_sip_ip);
        sipPortEditText = rootView.findViewById(R.id.editText_sip_port);
        
        return rootView;
    }

    ///////////////////////////////////////////////

    public void save(View v){
        Bundle resultBundle = new Bundle();

        String name = nameEditText.getText().toString();
        String email = emailEditText.getText().toString();
        String mdn = mdnEditText.getText().toString();
        String sipIp = sipIpEditText.getText().toString();
        String sipPort = sipPortEditText.getText().toString();

        resultBundle.putString("NAME", name);
        resultBundle.putString("EMAIL", email);
        resultBundle.putString("MDN", mdn);
        resultBundle.putString("SIP_IP", sipIp);
        resultBundle.putString("SIP_PORT", sipPort);

        //Logger.d("AddContactFragment: name=[%s], email=[%s], mdn=[%s], sipIp=[%s], sipPort=[%s]", name, email, mdn, sipIp, sipPort);
        //Toast.makeText(rootView.getContext(), "name=[" + name + "]\n" + "email=[" + email + "]\n" + "mdn=[" + mdn + "]\n" + "sipIp=[" + sipIp + "]\n" + "sipPort=[" + sipPort + "]", Toast.LENGTH_SHORT).show();

        getParentFragmentManager().setFragmentResult(MasterFragmentActivity.CONTACT_ADD_KEY, resultBundle);

        finish();
    }

    public void cancel(View v){
        finish();
    }

    public void finish() {
        if (getActivity() == null) {
            return;
        }

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        fragmentManager.beginTransaction().remove(this).commit();
        fragmentManager.popBackStack();
        fragmentManager.beginTransaction().show(contactFragment).commit();
        contactFragment.setContactManagerFragment(null);
    }

    ///////////////////////////////////////////////

}

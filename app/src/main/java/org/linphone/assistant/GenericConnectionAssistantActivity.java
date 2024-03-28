/*
 * Copyright (c) 2010-2019 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.linphone.assistant;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.annotation.Nullable;
import org.linphone.LinphoneManager;
import org.linphone.R;
import org.linphone.core.AccountCreator;
import org.linphone.core.Core;
import org.linphone.core.TransportType;
import org.linphone.core.tools.Log;
import org.linphone.settings.LinphonePreferences;

public class GenericConnectionAssistantActivity extends AssistantActivity implements TextWatcher {
    private TextView mLogin, action_adv;
    private EditText mUsername, mPassword, mDomain, mDisplayName, mAccountNum, portNum;
    private RadioGroup mTransport;

    private LinearLayout transport;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.assistant_generic_connection);

        mLogin = findViewById(R.id.assistant_login);
        mLogin.setEnabled(false);
        mLogin.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        configureAccount();
                    }
                });

        transport = findViewById(R.id.layout_protocol);
        action_adv = findViewById(R.id.action_adv);
        mUsername = findViewById(R.id.assistant_username);
        mUsername.addTextChangedListener(this);
        // mDisplayName = findViewById(R.id.assistant_display_name);
        // mDisplayName.addTextChangedListener(this);
        mPassword = findViewById(R.id.assistant_password);
        mPassword.addTextChangedListener(this);
        mDomain = findViewById(R.id.account_number);
        mDomain.addTextChangedListener(this);
        mTransport = findViewById(R.id.assistant_transports);
        portNum = findViewById(R.id.port_num);

        action_adv.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (transport.getVisibility() == View.GONE) {
                            transport.setVisibility(View.VISIBLE);
                        } else if (transport.getVisibility() == View.VISIBLE) {
                            transport.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void configureAccount() {
        Core core = LinphoneManager.getCore();
        if (core != null) {
            Log.i("[Generic Connection Assistant] Reloading configuration with default");
            reloadDefaultAccountCreatorConfig();
        }

        String portNo = portNum.getText().toString();

        if (portNo == null || portNo.isEmpty()) {
            portNo = "5065";
        }

        AccountCreator accountCreator = getAccountCreator();
        accountCreator.setUsername(mUsername.getText().toString());
        accountCreator.setDomain(mDomain.getText().toString() + ".netexem.com:" + portNo);
        accountCreator.setPassword(mPassword.getText().toString());
        // accountCreator.setDisplayName(mDisplayName.getText().toString());
        LinphonePreferences.instance().saveAccountNumber(mDomain.getText().toString());
        switch (mTransport.getCheckedRadioButtonId()) {
            case R.id.transport_udp:
                accountCreator.setTransport(TransportType.Udp);
                break;
            case R.id.transport_tcp:
                accountCreator.setTransport(TransportType.Tcp);
                break;
            case R.id.transport_tls:
                accountCreator.setTransport(TransportType.Tls);
                break;
        }

        createProxyConfigAndLeaveAssistant(true);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        mLogin.setEnabled(
                !mUsername.getText().toString().isEmpty()
                        && !mDomain.getText().toString().isEmpty());
    }

    @Override
    public void afterTextChanged(Editable s) {}
}

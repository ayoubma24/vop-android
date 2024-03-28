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
package org.linphone.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import org.linphone.LinphoneManager;
import org.linphone.R;
import org.linphone.activities.LinphoneLauncherActivity;
import org.linphone.assistant.PhoneAccountLinkingAssistantActivity;
import org.linphone.core.AVPFMode;
import org.linphone.core.Address;
import org.linphone.core.AuthInfo;
import org.linphone.core.Core;
import org.linphone.core.Factory;
import org.linphone.core.NatPolicy;
import org.linphone.core.ProxyConfig;
import org.linphone.core.TransportType;
import org.linphone.core.tools.Log;
import org.linphone.settings.widget.ListSetting;
import org.linphone.settings.widget.SettingListenerBase;
import org.linphone.settings.widget.SwitchSetting;
import org.linphone.settings.widget.TextSetting;

public class CustomSettingsFragment extends SettingsFragment {
    private View mRootView;
    private int mAccountIndex;
    private ProxyConfig mProxyConfig;
    private AuthInfo mAuthInfo;
    private boolean mIsNewlyCreatedAccount;
    private Vibrator vib;

    private TextSetting mPassword,
            mDomain,
            mDisplayName,
            mProxy,
            mStun,
            mExpire,
            mPrefix,
            mAvpfInterval;
    private TextView mUsername, mUserId;
    private SwitchSetting mDisable,
            mUseAsDefault,
            mOutboundProxy,
            mIce,
            mAvpf,
            mReplacePlusBy00,
            mPush;
    private TextSetting mChangePassword, mLinkAccount;
    private ListSetting mTransport;
    private Button mDeleteAccount;

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.ui_custom_settings, container, false);

        loadSettings();

        if (getActivity() != null)
            vib = (Vibrator) (getActivity().getSystemService(Context.VIBRATOR_SERVICE));

        mIsNewlyCreatedAccount = true;
        mAccountIndex = getArguments().getInt("Account", -1);
        if (mAccountIndex == -1 && savedInstanceState != null) {
            mAccountIndex = savedInstanceState.getInt("Account", 0);
        }

        mProxyConfig = null;
        Core core = LinphoneManager.getCore();
        if (mAccountIndex >= 0 && core != null) {
            ProxyConfig[] proxyConfigs = core.getProxyConfigList();
            if (proxyConfigs.length > mAccountIndex) {
                mProxyConfig = proxyConfigs[mAccountIndex];
                mIsNewlyCreatedAccount = false;
            } else {
                Log.e("[Account Settings] Proxy config not found !");
            }
        }

        return mRootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("Account", mAccountIndex);
    }

    @Override
    public void onResume() {
        super.onResume();

        updateValues();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mIsNewlyCreatedAccount) {
            Core core = LinphoneManager.getCore();
            if (core != null && mProxyConfig != null && mAuthInfo != null) {
                core.addAuthInfo(mAuthInfo);
                core.addProxyConfig(mProxyConfig);
                if (mUseAsDefault.isChecked()) {
                    core.setDefaultProxyConfig(mProxyConfig);
                }
            }
        }
    }

    private void loadSettings() {
        mUsername = mRootView.findViewById(R.id.pref_username);

        mUserId = mRootView.findViewById(R.id.pref_accNumber);

        mUserId.setEnabled(false);

        mUsername.setEnabled(false);
        mDeleteAccount = mRootView.findViewById(R.id.pref_delete_account);

        mDeleteAccount.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        vib.vibrate(5);
                        new AlertDialog.Builder(getActivity())
                                .setMessage("Are you sure you want to logout?")
                                .setCancelable(false)
                                .setPositiveButton(
                                        "Yes",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                Core core = LinphoneManager.getCore();
                                                if (core != null) {
                                                    if (mProxyConfig != null) {
                                                        core.removeProxyConfig(mProxyConfig);
                                                    }
                                                    if (mAuthInfo != null) {
                                                        core.removeAuthInfo(mAuthInfo);
                                                    }
                                                }

                                                // Set a new default proxy config if the current one
                                                // has been removed
                                                if (core != null
                                                        && core.getDefaultProxyConfig() == null) {
                                                    ProxyConfig[] proxyConfigs =
                                                            core.getProxyConfigList();
                                                    if (proxyConfigs.length > 0) {
                                                        for (ProxyConfig config : proxyConfigs) {
                                                            core.removeAuthInfo(
                                                                    config.findAuthInfo());
                                                            core.removeProxyConfig(config);
                                                        }
                                                    }
                                                }
                                                Intent intent2 =
                                                        new Intent(
                                                                getActivity(),
                                                                LinphoneLauncherActivity.class);
                                                intent2.addFlags(
                                                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                                                                | Intent
                                                                        .FLAG_ACTIVITY_REORDER_TO_FRONT
                                                                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                                                                | Intent.FLAG_ACTIVITY_NEW_TASK
                                                                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(intent2);
                                                getActivity().finish();
                                            }
                                        })
                                .setNegativeButton("No", null)
                                .show();
                    }
                });
    }

    private void setListeners() {

        mPassword.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onTextValueChanged(String newValue) {
                        if (mAuthInfo != null) {
                            mAuthInfo.setHa1(null);
                            mAuthInfo.setPassword(newValue);
                            // Reset algorithm to generate correct hash depending on
                            // algorithm set in next to come 401
                            mAuthInfo.setAlgorithm(null);
                            Core core = LinphoneManager.getCore();
                            if (core != null) {
                                core.addAuthInfo(mAuthInfo);
                                core.refreshRegisters();
                            }
                        } else {
                            Log.e("[Account Settings] No auth info !");
                        }
                    }
                });

        mDomain.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onTextValueChanged(String newValue) {
                        if (newValue.isEmpty()) {
                            return;
                        }
                        if (newValue.contains(":")) {
                            Log.e(
                                    "[Account Settings] Do not specify port information inside domain field !");
                            return;
                        }

                        if (mAuthInfo != null) {
                            mAuthInfo.setDomain(newValue);
                        } else {
                            Log.e("[Account Settings] No auth info !");
                        }

                        if (mProxyConfig != null) {
                            mProxyConfig.edit();
                            Address identity = mProxyConfig.getIdentityAddress();
                            if (identity != null) {
                                identity.setDomain(newValue);
                            }
                            mProxyConfig.setIdentityAddress(identity);
                            mProxyConfig.done();
                        } else {
                            Log.e("[Account Settings] No proxy config !");
                        }
                    }
                });

        mDisplayName.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onTextValueChanged(String newValue) {
                        if (mProxyConfig != null) {
                            mProxyConfig.edit();
                            Address identity = mProxyConfig.getIdentityAddress();
                            if (identity != null) {
                                identity.setDisplayName(newValue);
                            }
                            mProxyConfig.setIdentityAddress(identity);
                            mProxyConfig.done();
                        } else {
                            Log.e("[Account Settings] No proxy config !");
                        }
                    }
                });

        mProxy.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onTextValueChanged(String newValue) {
                        if (mProxyConfig != null) {
                            mProxyConfig.edit();
                            Address proxy = Factory.instance().createAddress(newValue);
                            if (proxy != null) {
                                mProxyConfig.setServerAddr(proxy.asString());
                                if (mOutboundProxy.isChecked()) {
                                    mProxyConfig.setRoute(proxy.asString());
                                }
                                mTransport.setValue(proxy.getTransport().toInt());
                            }
                            mProxyConfig.done();
                        } else {
                            Log.e("[Account Settings] No proxy config !");
                        }
                    }
                });

        mStun.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onTextValueChanged(String newValue) {
                        if (mProxyConfig != null) {
                            mProxyConfig.edit();
                            NatPolicy natPolicy = mProxyConfig.getNatPolicy();
                            if (natPolicy == null) {
                                Core core = LinphoneManager.getCore();
                                if (core != null) {
                                    natPolicy = core.createNatPolicy();
                                    mProxyConfig.setNatPolicy(natPolicy);
                                }
                            }
                            if (natPolicy != null) {
                                natPolicy.setStunServer(newValue);
                            }
                            if (newValue == null || newValue.isEmpty()) {
                                mIce.setChecked(false);
                            }
                            mIce.setEnabled(newValue != null && !newValue.isEmpty());
                            mProxyConfig.done();
                        } else {
                            Log.e("[Account Settings] No proxy config !");
                        }
                    }
                });

        mExpire.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onTextValueChanged(String newValue) {
                        if (mProxyConfig != null) {
                            mProxyConfig.edit();
                            try {
                                mProxyConfig.setExpires(Integer.parseInt(newValue));
                            } catch (NumberFormatException nfe) {
                                Log.e(nfe);
                            }
                            mProxyConfig.done();
                        } else {
                            Log.e("[Account Settings] No proxy config !");
                        }
                    }
                });

        mPrefix.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onTextValueChanged(String newValue) {
                        if (mProxyConfig != null) {
                            mProxyConfig.edit();
                            mProxyConfig.setDialPrefix(newValue);
                            mProxyConfig.done();
                        } else {
                            Log.e("[Account Settings] No proxy config !");
                        }
                    }
                });

        mAvpfInterval.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onTextValueChanged(String newValue) {
                        if (mProxyConfig != null) {
                            mProxyConfig.edit();
                            try {
                                mProxyConfig.setAvpfRrInterval(Integer.parseInt(newValue));
                            } catch (NumberFormatException nfe) {
                                Log.e(nfe);
                            }
                            mProxyConfig.done();
                        } else {
                            Log.e("[Account Settings] No proxy config !");
                        }
                    }
                });

        mDisable.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onBoolValueChanged(boolean newValue) {
                        if (mProxyConfig != null) {
                            mProxyConfig.edit();
                            mProxyConfig.enableRegister(!newValue);
                            mProxyConfig.done();
                        } else {
                            Log.e("[Account Settings] No proxy config !");
                        }
                    }
                });

        mUseAsDefault.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onBoolValueChanged(boolean newValue) {
                        if (mProxyConfig != null) {
                            Core core = LinphoneManager.getCore();
                            if (core != null && newValue) {
                                core.setDefaultProxyConfig(mProxyConfig);
                                mUseAsDefault.setEnabled(false);
                            }
                            ((SettingsActivity) getActivity())
                                    .getSideMenuFragment()
                                    .displayAccountsInSideMenu();
                        } else {
                            Log.e("[Account Settings] No proxy config !");
                        }
                    }
                });

        mOutboundProxy.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onBoolValueChanged(boolean newValue) {
                        if (mProxyConfig != null) {
                            mProxyConfig.edit();
                            if (newValue) {
                                mProxyConfig.setRoute(mProxy.getValue());
                            } else {
                                mProxyConfig.setRoute(null);
                            }
                            mProxyConfig.done();
                        } else {
                            Log.e("[Account Settings] No proxy config !");
                        }
                    }
                });

        mIce.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onBoolValueChanged(boolean newValue) {
                        if (mProxyConfig != null) {
                            mProxyConfig.edit();

                            NatPolicy natPolicy = mProxyConfig.getNatPolicy();
                            if (natPolicy == null) {
                                Core core = LinphoneManager.getCore();
                                if (core != null) {
                                    natPolicy = core.createNatPolicy();
                                    mProxyConfig.setNatPolicy(natPolicy);
                                }
                            }

                            if (natPolicy != null) {
                                natPolicy.enableIce(newValue);
                            }
                            mProxyConfig.done();
                        } else {
                            Log.e("[Account Settings] No proxy config !");
                        }
                    }
                });

        mAvpf.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onBoolValueChanged(boolean newValue) {
                        if (mProxyConfig != null) {
                            mProxyConfig.edit();
                            mProxyConfig.setAvpfMode(
                                    newValue ? AVPFMode.Enabled : AVPFMode.Disabled);
                            mAvpfInterval.setEnabled(mProxyConfig.avpfEnabled());
                            mProxyConfig.done();
                        } else {
                            Log.e("[Account Settings] No proxy config !");
                        }
                    }
                });

        mReplacePlusBy00.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onBoolValueChanged(boolean newValue) {
                        if (mProxyConfig != null) {
                            mProxyConfig.edit();
                            mProxyConfig.setDialEscapePlus(newValue);
                            mProxyConfig.done();
                        } else {
                            Log.e("[Account Settings] No proxy config !");
                        }
                    }
                });

        mPush.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onBoolValueChanged(boolean newValue) {
                        if (mProxyConfig != null) {
                            mProxyConfig.edit();
                            mProxyConfig.setPushNotificationAllowed(newValue);
                            mProxyConfig.done();
                        } else {
                            Log.e("[Account Settings] No proxy config !");
                        }
                    }
                });

        mChangePassword.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onClicked() {
                        // TODO add feature
                    }
                });

        mDeleteAccount.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Core core = LinphoneManager.getCore();
                        if (core != null) {
                            if (mProxyConfig != null) {
                                core.removeProxyConfig(mProxyConfig);
                            }
                            if (mAuthInfo != null) {
                                core.removeAuthInfo(mAuthInfo);
                            }
                        }

                        // Set a new default proxy config if the current one has been removed
                        if (core != null && core.getDefaultProxyConfig() == null) {
                            ProxyConfig[] proxyConfigs = core.getProxyConfigList();
                            if (proxyConfigs.length > 0) {
                                core.setDefaultProxyConfig(proxyConfigs[0]);
                            }
                        }

                        ((SettingsActivity) getActivity())
                                .getSideMenuFragment()
                                .displayAccountsInSideMenu();
                        ((SettingsActivity) getActivity()).goBack();
                    }
                });

        mLinkAccount.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onClicked() {
                        Intent assistant = new Intent();
                        assistant.setClass(
                                getActivity(), PhoneAccountLinkingAssistantActivity.class);
                        assistant.putExtra("AccountNumber", mAccountIndex);
                        startActivity(assistant);
                    }
                });

        mTransport.setListener(
                new SettingListenerBase() {
                    @Override
                    public void onListValueChanged(int position, String newLabel, String newValue) {
                        if (mProxyConfig != null) {
                            mProxyConfig.edit();
                            String server = mProxyConfig.getServerAddr();
                            Address serverAddr = Factory.instance().createAddress(server);
                            if (serverAddr != null) {
                                try {
                                    serverAddr.setTransport(
                                            TransportType.fromInt(Integer.parseInt(newValue)));
                                    server = serverAddr.asString();
                                    mProxyConfig.setServerAddr(server);
                                    if (mOutboundProxy.isChecked()) {
                                        mProxyConfig.setRoute(server);
                                    }
                                    mProxy.setValue(server);
                                } catch (NumberFormatException nfe) {
                                    Log.e(nfe);
                                }
                            }
                            mProxyConfig.done();
                        } else {
                            Log.e("[Account Settings] No proxy config !");
                        }
                    }
                });
    }

    private void updateValues() {
        Core core = LinphoneManager.getCore();
        if (core == null) return;

        // Create a proxy config if there is none
        if (mProxyConfig == null) {
            // Ensure the default configuration is loaded first
            String defaultConfig = LinphonePreferences.instance().getDefaultDynamicConfigFile();
            core.loadConfigFromXml(defaultConfig);
            mProxyConfig = core.createProxyConfig();
            mAuthInfo = Factory.instance().createAuthInfo(null, null, null, null, null, null);
            mIsNewlyCreatedAccount = true;
        }

        if (mProxyConfig != null) {
            Address identityAddress = mProxyConfig.getIdentityAddress();
            mAuthInfo = mProxyConfig.findAuthInfo();

            NatPolicy natPolicy = mProxyConfig.getNatPolicy();
            if (natPolicy == null) {
                natPolicy = core.createNatPolicy();
                core.setNatPolicy(natPolicy);
            }

            mUserId.setText(LinphonePreferences.instance().getAccountNum());

            mUsername.setText(identityAddress.getUsername());
        }

        // setListeners();
    }

    private void initTransportList() {
        List<String> entries = new ArrayList<>();
        List<String> values = new ArrayList<>();

        entries.add(getString(R.string.pref_transport_udp));
        values.add(String.valueOf(TransportType.Udp.toInt()));
        entries.add(getString(R.string.pref_transport_tcp));
        values.add(String.valueOf(TransportType.Tcp.toInt()));

        if (!getResources().getBoolean(R.bool.disable_all_security_features_for_markets)) {
            entries.add(getString(R.string.pref_transport_tls));
            values.add(String.valueOf(TransportType.Tls.toInt()));
        }

        mTransport.setItems(entries, values);
    }
}

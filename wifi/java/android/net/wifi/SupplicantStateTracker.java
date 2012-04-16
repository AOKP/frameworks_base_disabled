/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.net.wifi;

import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

import android.net.wifi.StateChangeResult;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;

/**
 * Tracks the state changes in supplicant and provides functionality
 * that is based on these state changes:
 * - detect a failed WPA handshake that loops indefinitely
 * - authentication failure handling
 */
class SupplicantStateTracker extends StateMachine {

    private static final String TAG = "SupplicantStateTracker";
    private static final boolean DBG = false;

    private WifiStateMachine mWifiStateMachine;
    private int mAuthenticationFailuresCount = 0;
    /* Indicates authentication failure in supplicant broadcast.
     * TODO: enhance auth failure reporting to include notification
     * for all type of failures: EAP, WPS & WPA networks */
    private boolean mAuthFailureInSupplicantBroadcast = false;

    /* Maximum retries on a authentication failure notification */
    private static final int MAX_RETRIES_ON_AUTHENTICATION_FAILURE = 4;

    /* Tracks if networks have been disabled during a connection */
    private boolean mNetworksDisabledDuringConnect = false;

    private Context mContext;

    private State mUninitializedState = new UninitializedState();
    private State mDefaultState = new DefaultState();
    private State mInactiveState = new InactiveState();
    private State mDisconnectState = new DisconnectedState();
    private State mScanState = new ScanState();
    private State mHandshakeState = new HandshakeState();
    private State mCompletedState = new CompletedState();
    private State mDormantState = new DormantState();

    public SupplicantStateTracker(Context context, WifiStateMachine wsm, Handler target) {
        super(TAG, target.getLooper());

        mContext = context;
        mWifiStateMachine = wsm;
        addState(mDefaultState);
            addState(mUninitializedState, mDefaultState);
            addState(mInactiveState, mDefaultState);
            addState(mDisconnectState, mDefaultState);
            addState(mScanState, mDefaultState);
            addState(mHandshakeState, mDefaultState);
            addState(mCompletedState, mDefaultState);
            addState(mDormantState, mDefaultState);

        setInitialState(mUninitializedState);

        //start the state machine
        start();
    }

    private void handleNetworkConnectionFailure(int netId) {
        /* If other networks disabled during connection, enable them */
        if (mNetworksDisabledDuringConnect) {
            WifiConfigStore.enableAllNetworks();
            mNetworksDisabledDuringConnect = false;
        }
        /* Disable failed network */
        WifiConfigStore.disableNetwork(netId, WifiConfiguration.DISABLED_AUTH_FAILURE);
    }

    private void transitionOnSupplicantStateChange(StateChangeResult stateChangeResult) {
        SupplicantState supState = (SupplicantState) stateChangeResult.state;

        if (DBG) Log.d(TAG, "Supplicant state: " + supState.toString() + "\n");

        switch (supState) {
           case DISCONNECTED:
                transitionTo(mDisconnectState);
                break;
            case INTERFACE_DISABLED:
                //we should have received a disconnection already, do nothing
                break;
            case SCANNING:
                transitionTo(mScanState);
                break;
            case AUTHENTICATING:
            case ASSOCIATING:
            case ASSOCIATED:
            case FOUR_WAY_HANDSHAKE:
            case GROUP_HANDSHAKE:
                transitionTo(mHandshakeState);
                break;
            case COMPLETED:
                transitionTo(mCompletedState);
                break;
            case DORMANT:
                transitionTo(mDormantState);
                break;
            case INACTIVE:
                transitionTo(mInactiveState);
                break;
            case UNINITIALIZED:
            case INVALID:
                transitionTo(mUninitializedState);
                break;
            default:
                Log.e(TAG, "Unknown supplicant state " + supState);
                break;
        }
    }

    private void sendSupplicantStateChangedBroadcast(SupplicantState state, boolean failedAuth) {
        Intent intent = new Intent(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT
                | Intent.FLAG_RECEIVER_REPLACE_PENDING);
        intent.putExtra(WifiManager.EXTRA_NEW_STATE, (Parcelable) state);
        if (failedAuth) {
            intent.putExtra(
                WifiManager.EXTRA_SUPPLICANT_ERROR,
                WifiManager.ERROR_AUTHENTICATING);
        }
        mContext.sendStickyBroadcast(intent);
    }

    /********************************************************
     * HSM states
     *******************************************************/

    class DefaultState extends State {
        @Override
         public void enter() {
             if (DBG) Log.d(TAG, getName() + "\n");
         }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) Log.d(TAG, getName() + message.toString() + "\n");
            switch (message.what) {
                case WifiMonitor.AUTHENTICATION_FAILURE_EVENT:
                    mAuthenticationFailuresCount++;
                    mAuthFailureInSupplicantBroadcast = true;
                    break;
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    StateChangeResult stateChangeResult = (StateChangeResult) message.obj;
                    SupplicantState state = stateChangeResult.state;
                    sendSupplicantStateChangedBroadcast(state, mAuthFailureInSupplicantBroadcast);
                    mAuthFailureInSupplicantBroadcast = false;
                    transitionOnSupplicantStateChange(stateChangeResult);
                    break;
                case WifiStateMachine.CMD_RESET_SUPPLICANT_STATE:
                    transitionTo(mUninitializedState);
                    break;
                case WifiStateMachine.CMD_CONNECT_NETWORK:
                    mNetworksDisabledDuringConnect = true;
                    break;
                default:
                    Log.e(TAG, "Ignoring " + message);
                    break;
            }
            return HANDLED;
        }
    }

    /*
     * This indicates that the supplicant state as seen
     * by the framework is not initialized yet. We are
     * in this state right after establishing a control
     * channel connection before any supplicant events
     * or after we have lost the control channel
     * connection to the supplicant
     */
    class UninitializedState extends State {
        @Override
         public void enter() {
             if (DBG) Log.d(TAG, getName() + "\n");
         }
    }

    class InactiveState extends State {
        @Override
         public void enter() {
             if (DBG) Log.d(TAG, getName() + "\n");
         }
    }

    class DisconnectedState extends State {
        @Override
         public void enter() {
             if (DBG) Log.d(TAG, getName() + "\n");
             /* If a disconnect event happens after authentication failure
              * exceeds maximum retries, disable the network
              */
             Message message = getCurrentMessage();
             StateChangeResult stateChangeResult = (StateChangeResult) message.obj;

             if (mAuthenticationFailuresCount >= MAX_RETRIES_ON_AUTHENTICATION_FAILURE) {
                 Log.d(TAG, "Failed to authenticate, disabling network " +
                         stateChangeResult.networkId);
                 handleNetworkConnectionFailure(stateChangeResult.networkId);
                 mAuthenticationFailuresCount = 0;
             }
         }
    }

    class ScanState extends State {
        @Override
         public void enter() {
             if (DBG) Log.d(TAG, getName() + "\n");
         }
    }

    class HandshakeState extends State {
        /**
         * The max number of the WPA supplicant loop iterations before we
         * decide that the loop should be terminated:
         */
        private static final int MAX_SUPPLICANT_LOOP_ITERATIONS = 4;
        private int mLoopDetectIndex;
        private int mLoopDetectCount;

        @Override
         public void enter() {
             if (DBG) Log.d(TAG, getName() + "\n");
             mLoopDetectIndex = 0;
             mLoopDetectCount = 0;
         }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) Log.d(TAG, getName() + message.toString() + "\n");
            switch (message.what) {
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    StateChangeResult stateChangeResult = (StateChangeResult) message.obj;
                    SupplicantState state = stateChangeResult.state;
                    if (SupplicantState.isHandshakeState(state)) {
                        if (mLoopDetectIndex > state.ordinal()) {
                            mLoopDetectCount++;
                        }
                        if (mLoopDetectCount > MAX_SUPPLICANT_LOOP_ITERATIONS) {
                            Log.d(TAG, "Supplicant loop detected, disabling network " +
                                    stateChangeResult.networkId);
                            handleNetworkConnectionFailure(stateChangeResult.networkId);
                        }
                        mLoopDetectIndex = state.ordinal();
                        sendSupplicantStateChangedBroadcast(state,
                                mAuthFailureInSupplicantBroadcast);
                    } else {
                        //Have the DefaultState handle the transition
                        return NOT_HANDLED;
                    }
                    break;
                default:
                    return NOT_HANDLED;
            }
            return HANDLED;
        }
    }

    class CompletedState extends State {
        @Override
         public void enter() {
             if (DBG) Log.d(TAG, getName() + "\n");
             /* Reset authentication failure count */
             mAuthenticationFailuresCount = 0;
             if (mNetworksDisabledDuringConnect) {
                 WifiConfigStore.enableAllNetworks();
                 mNetworksDisabledDuringConnect = false;
             }
        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) Log.d(TAG, getName() + message.toString() + "\n");
            switch(message.what) {
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    StateChangeResult stateChangeResult = (StateChangeResult) message.obj;
                    SupplicantState state = stateChangeResult.state;
                    sendSupplicantStateChangedBroadcast(state, mAuthFailureInSupplicantBroadcast);
                    /* Ignore any connecting state in completed state. Group re-keying
                     * events and other auth events that do not affect connectivity are
                     * ignored
                     */
                    if (SupplicantState.isConnecting(state)) {
                        break;
                    }
                    transitionOnSupplicantStateChange(stateChangeResult);
                    break;
                case WifiStateMachine.CMD_RESET_SUPPLICANT_STATE:
                    sendSupplicantStateChangedBroadcast(SupplicantState.DISCONNECTED, false);
                    transitionTo(mUninitializedState);
                    break;
                default:
                    return NOT_HANDLED;
            }
            return HANDLED;
        }
    }

    //TODO: remove after getting rid of the state in supplicant
    class DormantState extends State {
        @Override
        public void enter() {
            if (DBG) Log.d(TAG, getName() + "\n");
        }
    }
}

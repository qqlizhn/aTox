// SPDX-FileCopyrightText: 2021-2022 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.atox

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.RemoteInput
import im.tox.tox4j.av.exceptions.ToxavAnswerException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ltd.evilcorp.atox.ui.NotificationHelper
import ltd.evilcorp.core.repository.ContactRepository
import ltd.evilcorp.core.vo.Contact
import ltd.evilcorp.core.vo.UserStatus
import ltd.evilcorp.domain.feature.CallManager
import ltd.evilcorp.domain.feature.CallState
import ltd.evilcorp.domain.feature.ChatManager
import ltd.evilcorp.domain.feature.ContactManager
import ltd.evilcorp.domain.tox.PublicKey
import ltd.evilcorp.domain.tox.Tox

const val KEY_TEXT_REPLY = "text_reply"
const val KEY_CALL = "accept_or_reject_call"
const val KEY_CONTACT_PK = "contact_pk"
const val KEY_ACTION = "action"

enum class Action {
    MarkAsRead,
}

private const val TAG = "ActionReceiver"

class ActionReceiver : BroadcastReceiver() {
    @Inject
    lateinit var callManager: CallManager

    @Inject
    lateinit var chatManager: ChatManager

    @Inject
    lateinit var contactManager: ContactManager

    @Inject
    lateinit var contactRepository: ContactRepository

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var tox: Tox

    @Inject
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        (context.applicationContext as App).component.inject(this)

        RemoteInput.getResultsFromIntent(intent)?.let { results ->
            results.getCharSequence(KEY_TEXT_REPLY)?.toString()?.let { input ->
                val pk = intent.getStringExtra(KEY_CONTACT_PK) ?: return
                scope.launch {
                    contactRepository.setHasUnreadMessages(pk, false)
                }
                chatManager.sendMessage(PublicKey(pk), input)
                notificationHelper.showMessageNotification(Contact(pk, tox.getName()), input, outgoing = true)
            }
        }

        intent.getStringExtra(KEY_CALL)?.also { callChoice ->
            val pk = intent.getStringExtra(KEY_CONTACT_PK)?.let { PublicKey(it) } ?: return
            when (callChoice) {
                "accept" -> scope.launch {
                    val contact = contactManager.get(pk).firstOrNull().let {
                        if (it != null) {
                            it
                        } else {
                            Log.e(TAG, "Unable to get contact ${pk.fingerprint()} for call notification")
                            Contact(publicKey = pk.string(), name = pk.fingerprint())
                        }
                    }

                    if (callManager.inCall.value is CallState.InCall) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.error_simultaneous_calls),
                                Toast.LENGTH_LONG
                            ).show()
                            notificationHelper.showPendingCallNotification(UserStatus.Busy, contact)
                        }
                        return@launch
                    }

                    try {
                        callManager.startCall(pk)
                        notificationHelper.showOngoingCallNotification(contact)
                    } catch (e: ToxavAnswerException) {
                        Log.e(TAG, e.toString())
                        return@launch
                    }

                    val isSendingAudio =
                        context.hasPermission(Manifest.permission.RECORD_AUDIO) && callManager.startSendingAudio()
                    if (!isSendingAudio) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, R.string.call_mic_permission_needed, Toast.LENGTH_LONG).show()
                        }
                    }
                }
                "reject", "end call" -> {
                    callManager.endCall(pk)
                    notificationHelper.dismissCallNotification(pk)
                }
                "ignore" -> callManager.removePendingCall(pk)
            }
        }

        when (intent.getSerializableExtra(KEY_ACTION) as Action?) {
            Action.MarkAsRead -> scope.launch {
                val pk = intent.getStringExtra(KEY_CONTACT_PK) ?: return@launch
                contactRepository.setHasUnreadMessages(pk, false)
                notificationHelper.dismissNotifications(PublicKey(pk))
            }
            null -> Log.e(TAG, "Missing action in intent $intent")
        }
    }
}

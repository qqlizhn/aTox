package ltd.evilcorp.atox

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import ltd.evilcorp.atox.ui.NotificationHelper
import ltd.evilcorp.core.vo.Contact
import ltd.evilcorp.domain.tox.PublicKey
import javax.inject.Inject


class AudioRecordService : Service() {

    @Inject
    lateinit var notificationHelper: NotificationHelper
    override fun onCreate() {
        super.onCreate()
        (this.applicationContext as App).component.inject(this)
        if (contact==null)
            contact = Contact("EDD1B1A14CDA01B38F29D6005746CEA6A8A8269EF775968DEE3707FFF25EA75BD783A1AAF4FA")
        var notification = notificationHelper.showOngoingCallNotification(contact!!)
        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (contact==null)
            contact = Contact("EDD1B1A14CDA01B38F29D6005746CEA6A8A8269EF775968DEE3707FFF25EA75BD783A1AAF4FA")
        notificationHelper.dismissCallNotification(PublicKey(contact!!.publicKey))
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        var contact: Contact?=null
        private val TAG: String = "AudioRecordService"
        private val CHANNEL_ID: String = "AudioRecordServiceChannel"
    }
}

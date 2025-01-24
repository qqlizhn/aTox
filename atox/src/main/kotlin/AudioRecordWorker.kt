package ltd.evilcorp.atox
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class AudioRecordWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        val context: Context = applicationContext
        val intent = Intent(context, AudioRecordService::class.java)
        ContextCompat.startForegroundService(context, intent)
        return Result.success()
    }
}

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.iichirokawashima.ibeaconreceive.MainActivity
import com.iichirokawashima.ibeaconreceive.R

class NotificationWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        val pendingIntent = PendingIntent.getActivity(applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java), PendingIntent.FLAG_ONE_SHOT)

        val notification = NotificationCompat.Builder(applicationContext, "default")
            .setContentTitle(inputData.getString("title")) // enqueue元から渡されたタイトルテキストを通知にセット
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(1, notification)

        return Result.success()
    }
}
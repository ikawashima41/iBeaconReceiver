package com.iichirokawashima.ibeaconreceive

import NotificationWorker
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.preference.PreferenceFragmentCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.android.synthetic.main.activity_main.*
import org.altbeacon.beacon.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), BeaconConsumer, IBeaconActivityLifeCycle {
    private val TAG: String
        get() = MainActivity::class.java.simpleName

    companion object {
        const val IBEACON_FORMAT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"
    }

    private val uuidString: String = "E2C56DB5-DFFB-48D2-B060-D0F5A71096E0"
    private val uuid = Identifier.parse(uuidString)

    private lateinit var mBeaconManager: BeaconManager

    private val mLifeCycle: ActivityLifeCycle = ActivityLifeCycle(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lifecycle.addObserver(mLifeCycle)

    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(mLifeCycle)
    }

    override fun onCreated() {
        // BeaconManagerを取得する
        mBeaconManager = BeaconManager.getInstanceForApplication(this)
        // iBeaconの受信設定：iBeaconのフォーマットを登録する
        mBeaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(IBEACON_FORMAT))
    }

    /**
     * フォアグラウンド移行時
     */
    override fun onConnected() {
        // Beaconのイベント設定
        mBeaconManager.bind(this@MainActivity)
        Log.d(TAG, "onConnected")
    }

    /**
     * バックグラウンド移行時
     */
    override fun onDisconnect() {
        // Beaconのイベント解除
        mBeaconManager.unbind(this@MainActivity)
        Log.d(TAG, "onDisconnect")
    }

    override fun onBeaconServiceConnect() {
        val mRegion = Region("unique-id-001", uuid, null, null)

        Log.d(TAG, "onBeaconServiceConnect")
        //Beacon領域の入退場を検知するイベント設定
        mBeaconManager.addMonitorNotifier(object : MonitorNotifier {
            override fun didEnterRegion(region: Region) {
                //レンジングの開始
                mBeaconManager.startRangingBeaconsInRegion(mRegion);
                Log.d(TAG, "didEnterRegion")
            }

            override fun didExitRegion(region: Region) {
                //レンジングの終了
                mBeaconManager.stopRangingBeaconsInRegion(mRegion);
                Log.d(TAG, "didExitRegion")
            }

            override fun didDetermineStateForRegion(i: Int, region: Region) {
                Log.d(TAG, "didDetermineStateForRegion i = " + i)
            }
        })

        // レンジングのイベント設定
        mBeaconManager.addRangeNotifier { beacons, region ->
            beacons
                .map { "UUID:" + it.id1 + " major:" + it.id2 + " minor:" + it.id3 + " RSSI:" + it.rssi + " Distance:" + it.distance + " txPower" + it.txPower }
                .forEach {
                    Log.d(TAG, it)
                    textView.text = it
                }

            //fireNotification()
        }

        try {
            // 入退場検知イベントの登録
            mBeaconManager.startMonitoringBeaconsInRegion(mRegion)
        } catch (e: RemoteException) {
            Log.e(TAG, "Exception", e)
        }
    }

    private fun fireNotification() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel("default", "Default", NotificationManager.IMPORTANCE_DEFAULT)

        channel.description = "Local Notification Sample"
        manager.createNotificationChannel(channel)

        val inputData = Data.Builder().putString("title", "単発メッセージ").build()
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(5, TimeUnit.SECONDS)
            .setInputData(inputData)
            .build()
        WorkManager.getInstance(this).enqueue(workRequest)
    }
}

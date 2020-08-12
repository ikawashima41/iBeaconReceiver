package com.iichirokawashima.ibeaconreceive

import NotificationWorker
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import org.altbeacon.beacon.*
import java.util.concurrent.TimeUnit

class SettingsActivity : AppCompatActivity(), BeaconConsumer, IBeaconActivityLifeCycle {

    private val TAG: String
        get() = SettingsActivity::class.java.simpleName

    companion object {
        const val IBEACON_FORMAT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"
    }

    private lateinit var mBeaconManager: BeaconManager

    private val mLifeCycle: ActivityLifeCycle = ActivityLifeCycle(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        lifecycle.addObserver(mLifeCycle)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(mLifeCycle)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
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
        mBeaconManager.bind(this@SettingsActivity)
    }

    /**
     * バックグラウンド移行時
     */
    override fun onDisconnect() {
        // Beaconのイベント解除
        mBeaconManager.unbind(this@SettingsActivity)
    }

    override fun onBeaconServiceConnect() {
        val mRegion = Region(packageName, null, null, null)

        //Beacon領域の入退場を検知するイベント設定
        mBeaconManager.addMonitorNotifier(object : MonitorNotifier {
            override fun didEnterRegion(region: Region) {
                //レンジングの開始
                mBeaconManager.startRangingBeaconsInRegion(mRegion);
            }

            override fun didExitRegion(region: Region) {
                //レンジングの終了
                mBeaconManager.stopRangingBeaconsInRegion(mRegion);
            }

            override fun didDetermineStateForRegion(i: Int, region: Region) {
            }
        })

        // レンジングのイベント設定
        mBeaconManager.addRangeNotifier { beacons, region ->
            beacons
                    .map { "UUID:" + it.id1 + " major:" + it.id2 + " minor:" + it.id3 + " RSSI:" + it.rssi + " Distance:" + it.distance + " txPower" + it.txPower }
                    .forEach { Log.d(TAG, it) }

            fireNotification()
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

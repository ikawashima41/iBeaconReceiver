package com.iichirokawashima.ibeaconreceive

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

interface IBeaconActivityLifeCycle {

    fun onCreated()

    fun onConnected()

    fun onDisconnect()
}

class ActivityLifeCycle(lifeCycleCallback: IBeaconActivityLifeCycle) : LifecycleObserver {
    private val mCallback = lifeCycleCallback

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun create() {
        mCallback.onCreated()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun start() {
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun resume() {
        mCallback.onConnected()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun stop() {
        mCallback.onDisconnect()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun destroy() {
    }
}
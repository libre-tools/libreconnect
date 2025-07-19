package dev.libretools.connect.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LibreConnectServiceConnection(
        private val context: Context,
        private val onServiceConnected: (LibreConnectService) -> Unit = {},
        private val onServiceDisconnected: () -> Unit = {}
) : ServiceConnection, DefaultLifecycleObserver {

    companion object {
        private const val TAG = "ServiceConnection"
    }

    private var service: LibreConnectService? = null
    private var isBound = false

    // Stable StateFlow for connection status
    private val _connectionStatus = MutableStateFlow("Initializing...")
    val connectionStatus: StateFlow<String> get() = _connectionStatus

    // Service state flows - available after service is connected
    val isServiceRunning: StateFlow<Boolean>?
        get() = service?.isServiceRunning

    val discoveredDevices: StateFlow<List<dev.libretools.connect.data.Device>>?
        get() = service?.discoveredDevices

    val connectedDevices: StateFlow<List<dev.libretools.connect.data.Device>>?
        get() = service?.connectedDevices

    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        Log.d(TAG, "Service connected")
        val localBinder = binder as? LibreConnectService.LocalBinder
        if (localBinder != null) {
            service = localBinder.getService()
            isBound = true
            // Observe the real connectionStatus flow and update our own
            service?.connectionStatus?.let { flow ->
                // Use a coroutine to collect updates
                kotlinx.coroutines.GlobalScope.launch {
                    flow.collect { status ->
                        _connectionStatus.value = status
                    }
                }
            }
            onServiceConnected(service!!)
        } else {
            Log.e(TAG, "Failed to get service from binder")
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Log.d(TAG, "Service disconnected")
        service = null
        isBound = false
        _connectionStatus.value = "Disconnected"
        onServiceDisconnected()
    }

    fun bindService(): Boolean {
        Log.d(TAG, "Attempting to bind to LibreConnectService")
        val intent = Intent(context, LibreConnectService::class.java)
        return context.bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    fun unbindService() {
        if (isBound) {
            Log.d(TAG, "Unbinding from LibreConnectService")
            context.unbindService(this)
            isBound = false
            service = null
        }
    }

    fun startService() {
        Log.d(TAG, "Starting LibreConnectService")
        val intent =
                Intent(context, LibreConnectService::class.java).apply {
                    action = LibreConnectService.ACTION_START_SERVICE
                }
        context.startForegroundService(intent)
    }

    fun stopService() {
        Log.d(TAG, "Stopping LibreConnectService")
        val intent =
                Intent(context, LibreConnectService::class.java).apply {
                    action = LibreConnectService.ACTION_STOP_SERVICE
                }
        context.startService(intent)
    }

    fun startDeviceDiscovery() {
        Log.d(TAG, "Starting device discovery")
        val intent =
                Intent(context, LibreConnectService::class.java).apply {
                    action = LibreConnectService.ACTION_DISCOVER_DEVICES
                }
        context.startService(intent)
    }

    // Delegate methods to service
    fun connectToDevice(deviceId: String) {
        service?.connectToDevice(deviceId)
                ?: Log.w(TAG, "Service not available for connectToDevice")
    }

    fun disconnectFromDevice(deviceId: String) {
        service?.disconnectFromDevice(deviceId)
                ?: Log.w(TAG, "Service not available for disconnectFromDevice")
    }
    fun pairWithDevice(deviceId: String, pairingKey: String, callback: (Boolean, String?) -> Unit) {
        service?.pairWithDevice(deviceId, pairingKey, callback)
                ?: run {
                    Log.w(TAG, "Service not available for pairWithDevice")
                    callback(false, "Service not available")
                }
    }

    fun sendPluginMessage(deviceId: String, pluginType: String, message: Map<String, Any>) {
        service?.sendPluginMessage(deviceId, pluginType, message)
                ?: Log.w(TAG, "Service not available for sendPluginMessage")
    }

    // Lifecycle observer methods
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        Log.d(TAG, "Lifecycle onCreate - binding service")
        bindService()
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Log.d(TAG, "Lifecycle onStart")
        if (!isBound) {
            bindService()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Log.d(TAG, "Lifecycle onStop")
        // Keep service bound but don't unbind on stop
        // This allows the service to continue running in background
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        Log.d(TAG, "Lifecycle onDestroy - unbinding service")
        unbindService()
    }

    // Helper methods
    fun isServiceBound(): Boolean = isBound

    fun getService(): LibreConnectService? = service

    // Quick status checks
    fun isServiceActive(): Boolean = service?.isServiceRunning?.value ?: false

    fun getConnectedDeviceCount(): Int = service?.connectedDevices?.value?.size ?: 0

    fun getDiscoveredDeviceCount(): Int = service?.discoveredDevices?.value?.size ?: 0
}

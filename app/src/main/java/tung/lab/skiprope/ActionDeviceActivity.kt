package tung.lab.skiprope

import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import com.jstyle.blesdk1963.Util.BleSDK
import com.jstyle.blesdk1963.constant.ParamKey
import com.jstyle.blesdk1963.constant.ReceiveConst
import com.jstyle.blesdk1963.model.MyDeviceTime
import tung.lab.skiprope.Utils.BleData
import tung.lab.skiprope.Utils.RxBus
import tung.lab.skiprope.base.BaseActivity
import tung.lab.skiprope.ble.BleManager
import tung.lab.skiprope.ble.BleService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_action_device.*
import java.util.*

class ActionDeviceActivity : BaseActivity() {
    lateinit var subscription: Disposable
    var context: Context? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_action_device)
        subscription =
            RxBus.getInstance().toObservable(BleData::class.java).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribe { bleData ->
                    val action = bleData.action
                    if (action == BleService.ACTION_GATT_onDescriptorWrite) {
                        progressDialog!!.dismiss()
                    } else if (action == BleService.ACTION_GATT_DISCONNECTED) {
                        progressDialog!!.dismiss()
                    }
                }
        connectDevice()
        ClickButton()
        chooseMode()
    }

    fun ClickButton() {
        btnGetTime.setOnClickListener {
            sendValue(BleSDK.GetDeviceTime())
        }
       btnGetBattery.setOnClickListener {
            sendValue(BleSDK.GetDeviceBatteryLevel())
        }
        btnSyncTime.setOnClickListener {
            sendValue(BleSDK.SetDeviceTime(MyDeviceTime(Calendar.getInstance())))
        }
        btnGetVersion.setOnClickListener {
            sendValue(BleSDK.GetDeviceVersion())
        }
        btnGetMac.setOnClickListener {
            sendValue(BleSDK.GetDeviceMacAddress())
        }

        btnSkipTotalData.setOnClickListener {
            sendValue(BleSDK.GetDetailSkipData(0.toByte()))
            txt_log.text = ""
        }
        btnStartSkip.setOnClickListener {
            var mode: Int
            var second: Int
            var count: Int
            if (rbFreeMode.isChecked) {
                mode = 0x1
                second = 0
                count = 0
                sendValue(BleSDK.StartSkip(mode, second, count))
            } else if (rbTimeCountdown.isChecked) {
                mode = 0x02
                count = 0
                if (edtSecond.text.trim().length == 0) {
                    Toast.makeText(this@ActionDeviceActivity, "Input time", Toast.LENGTH_SHORT).show()
                } else {
                    second = Integer.parseInt(edtSecond.text.toString())
                    sendValue(BleSDK.StartSkip(mode, second, count))
                }
            } else if (rbSkipCountdown.isChecked) {
                mode = 0x03
                second = 0
                if (edtSkip.text.trim().length == 0) {
                    Toast.makeText(this@ActionDeviceActivity, "Input Skip", Toast.LENGTH_SHORT).show()
                } else {
                    count = (Integer.parseInt(edtSkip.text.trim().toString()))/100
                    sendValue(BleSDK.StartSkip(mode, second, count))
                }
            }

        }
        btnStopSkip.setOnClickListener {
            sendValue(BleSDK.StartSkip(0x99, 0, 0))
        }
        btnDisconnect.setOnClickListener{
            BleManager.getInstance().disconnectDevice()
            finish()
        }
    }

    override fun dataCallback(maps: MutableMap<String, Any>) {
        super.dataCallback(maps)
        var dataType = getDataType(maps)
        var data: Map<String, String>? = getData(maps)
        when (dataType) {
            ReceiveConst.GetDeviceTime -> {
                showDialogInfo(maps.toString())
            }
            ReceiveConst.GetDeviceMacAddress -> {
                var mac = data?.get(ParamKey.MacAddress)
                showDialogInfo(mac)
            }
            ReceiveConst.GetDeviceVersion -> {
                var version = data?.get(ParamKey.DeviceVersion)
                showDialogInfo(version)
            }
            ReceiveConst.GetDeviceBatteryLevel -> {
                var batt = data?.get(ParamKey.BatteryLevel)
                showDialogInfo(batt)
            }
            ReceiveConst.GetTotalSkipData -> {
                if (maps.containsKey(ParamKey.End)) {
                    txt_log.append("\nEnd")
                } else {
                    var date = maps.get(ParamKey.Date).toString()
                    var durationTime = maps.get(ParamKey.SkipDurationTime).toString()
                    var skipCount = maps.get(ParamKey.SkipCount).toString()
                    txt_log.append("\n $date === durationTime: $durationTime, skipCount: $skipCount")
                }
            }
            ReceiveConst.StartSkip -> {
                if (maps.containsKey(ParamKey.End)) {
                    txt_log.append("\n End")
                } else {
                    var durationTime = maps.get(ParamKey.SkipDurationTime).toString()
                    var skipCount = maps.get(ParamKey.SkipCount).toString()
                    var todayDurationTime = maps.get(ParamKey.TodaySkipDurationTime).toString()
                    var todaySkipCount = maps.get(ParamKey.TodaySkipCount).toString()
                    var mode = maps.get(ParamKey.Mode).toString()
                    var strMode = ""
                    when (mode) {
                        "1" -> strMode = "Free mode"
                        "2" -> strMode = "Time countdown mode"
                        "3" -> strMode = "Skipping countdown mode"
                    }
                    if (mode.toInt() > 0x30) {

                    } else {
                        txt_log.text = "\n Mode: $strMode \n durationTime: $durationTime \n" +
                                " skipCount: $skipCount \n todayDurationTime: $todayDurationTime \n" +
                                " todaySkipCount: $todaySkipCount"
                    }
                }
            }

        }
    }

    fun chooseMode() {
        rgSetMode.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.rbFreeMode -> {
                    edtSkip.visibility = View.GONE
                    edtSecond.visibility = View.GONE
                }
                R.id.rbTimeCountdown -> {
                    edtSkip.visibility = View.GONE
                    edtSecond.visibility = View.VISIBLE
                }
                R.id.rbSkipCountdown -> {
                    edtSkip.visibility = View.VISIBLE
                    edtSecond.visibility = View.GONE
                }
            }
        }
    }

    lateinit var address: String
    private fun connectDevice() {
        address = intent?.getStringExtra("macAddress")!!
        if (TextUtils.isEmpty(address)) {
            // Log.i(TAG, "onCreate: address null ");
            return
        }
        BleManager.getInstance().connectDevice(address)
        showConnectDialog()
    }

    var progressDialog: ProgressDialog? = null

    private fun showConnectDialog() {
        progressDialog = ProgressDialog(this)
        progressDialog!!.setMessage(getString(R.string.connectting) +" $address")
        if (!progressDialog!!.isShowing) progressDialog!!.show()
    }
}
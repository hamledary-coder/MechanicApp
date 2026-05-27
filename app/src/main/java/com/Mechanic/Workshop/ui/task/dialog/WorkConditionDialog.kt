package com.Mechanic.Workshop.ui.task.dialog

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.remote.Config

class WorkConditionDialog(
    private val context: Context,
    private val taskId: String,
    private val onConfirm: (heat: Int, pollution: Int) -> Unit,
    private val onApplyToAll: (heat: Int, pollution: Int) -> Unit
) {

    private var dialog: AlertDialog? = null
    private lateinit var prefs: SharedPreferences
    private var currentHeat = 30
    private var currentPollution = 0

    fun show() {
        prefs = context.getSharedPreferences("work_condition_$taskId", Context.MODE_PRIVATE)
        currentHeat = prefs.getInt("heat", 30)
        currentPollution = prefs.getInt("pollution", 0)

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_work_condition, null)

        val seekBarHeat = view.findViewById<SeekBar>(R.id.seekBarHeat)
        val seekBarPollution = view.findViewById<SeekBar>(R.id.seekBarPollution)
        val tvHeatValue = view.findViewById<TextView>(R.id.tvHeatValue)
        val tvPollutionValue = view.findViewById<TextView>(R.id.tvPollutionValue)
        val btnApplyToAll = view.findViewById<Button>(R.id.btnApplyToAll)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirm)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)

        // تنظیم مقادیر اولیه
        seekBarHeat.progress = currentHeat - 30  // تبدیل 30-50 به 0-20
        seekBarPollution.progress = currentPollution
        tvHeatValue.text = "مقدار فعلی: $currentHeat درجه"
        tvPollutionValue.text = "مقدار فعلی: $currentPollution ppm"

        seekBarHeat.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentHeat = progress + 30
                tvHeatValue.text = "مقدار فعلی: $currentHeat درجه"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarPollution.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentPollution = progress
                tvPollutionValue.text = "مقدار فعلی: $currentPollution ppm"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnApplyToAll.setOnClickListener {
            onApplyToAll(currentHeat, currentPollution)
            prefs.edit().putInt("heat", currentHeat).putInt("pollution", currentPollution).apply()
            dialog?.dismiss()
        }

        btnConfirm.setOnClickListener {
            prefs.edit().putInt("heat", currentHeat).putInt("pollution", currentPollution).apply()
            onConfirm(currentHeat, currentPollution)
            dialog?.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog?.dismiss()
        }

        dialog = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(true)
            .create()

        dialog?.show()
    }
}
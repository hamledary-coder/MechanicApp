package com.Mechanic.Workshop.ui.task.dialog

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.widget.*
import com.Mechanic.Workshop.R

class WorkConditionDialog(
    private val context: Context,
    private val taskId: String,
    private val onConfirm: (heat: Int, pollution: Int, workType: String) -> Unit,
    private val onApplyToAll: (heat: Int, pollution: Int, workType: String) -> Unit
) {

    private var dialog: AlertDialog? = null
    private lateinit var prefs: SharedPreferences
    private var currentHeat = 30
    private var currentPollution = 0
    private var selectedWorkType = "fixed_equipment"  // مقدار پیش‌فرض

    fun show() {
        prefs = context.getSharedPreferences("work_condition_$taskId", Context.MODE_PRIVATE)
        currentHeat = prefs.getInt("heat", 30)
        currentPollution = prefs.getInt("pollution", 0)
        selectedWorkType = prefs.getString("work_type", "fixed_equipment") ?: "fixed_equipment"

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_work_condition, null)

        val seekBarHeat = view.findViewById<SeekBar>(R.id.seekBarHeat)
        val seekBarPollution = view.findViewById<SeekBar>(R.id.seekBarPollution)
        val tvHeatValue = view.findViewById<TextView>(R.id.tvHeatValue)
        val tvPollutionValue = view.findViewById<TextView>(R.id.tvPollutionValue)
        val spinnerWorkType = view.findViewById<Spinner>(R.id.spinnerWorkType)
        val btnApplyToAll = view.findViewById<Button>(R.id.btnApplyToAll)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirm)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)

        // تنظیم اسپینر نوع کار
        val workTypes = arrayOf("تجهیزات ثابت", "عیب‌یابی تجهیزات دوار", "بررسی")
        val workTypeValues = arrayOf("1", "2", "3")  // ← کدهای عددی

        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, workTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerWorkType.adapter = adapter

        val index = workTypeValues.indexOf(selectedWorkType)
        if (index >= 0) spinnerWorkType.setSelection(index)

        // تنظیم مقادیر اولیه
        seekBarHeat.progress = currentHeat - 30
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
            val selectedWorkTypeValue = workTypeValues[spinnerWorkType.selectedItemPosition]
            onApplyToAll(currentHeat, currentPollution, selectedWorkTypeValue)
            prefs.edit().putInt("heat", currentHeat).putInt("pollution", currentPollution).putString("work_type", selectedWorkTypeValue).apply()
            dialog?.dismiss()
        }

        btnConfirm.setOnClickListener {
            val selectedWorkTypeValue = workTypeValues[spinnerWorkType.selectedItemPosition]
            prefs.edit().putInt("heat", currentHeat).putInt("pollution", currentPollution).putString("work_type", selectedWorkTypeValue).apply()
            onConfirm(currentHeat, currentPollution, selectedWorkTypeValue)
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
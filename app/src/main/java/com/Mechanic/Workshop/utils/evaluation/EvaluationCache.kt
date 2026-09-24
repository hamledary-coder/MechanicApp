package com.Mechanic.Workshop.utils.evaluation

import android.content.Context
import com.Mechanic.Workshop.data.remote.Config
import com.Mechanic.Workshop.utils.VolleySingleton
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import org.json.JSONObject

/**
 * کش ضرایب ارزیابی
 * جایگزین CoefficientCache قدیمی
 */
object EvaluationCache {

    private var coefficients: EvaluationModels.Coefficients? = null
    private var isLoading = false
    private val pendingCallbacks = mutableListOf<() -> Unit>()

    /**
     * دریافت ضرایب (با کش)
     */
    fun getCoefficients(context: Context, callback: (EvaluationModels.Coefficients?) -> Unit) {
        coefficients?.let {
            callback(it)
            return
        }

        if (isLoading) {
            pendingCallbacks.add {
                callback(coefficients)  // ← ممکنه null باشه
            }
            return
        }

        isLoading = true
        loadFromServer(context) { coeffs ->
            coefficients = coeffs
            isLoading = false
            callback(coeffs)  // ← ممکنه null باشه
            pendingCallbacks.forEach { it() }
            pendingCallbacks.clear()
        }
    }

    /**
     * بارگذاری از سرور
     */
    private fun loadFromServer(
        context: Context,
        onComplete: (EvaluationModels.Coefficients?) -> Unit  // ← nullable
    ) {
        val url = "${Config.BASE_URL}?action=getCoefficients"

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val json = JSONObject(response)
                    val coeffs = EvaluationModels.Coefficients(
                        physicalDifficulty = json.getInt("physical_difficulty"),  // ← getInt (نه optInt)
                        technicalComplexity = json.getInt("technical_complexity"),
                        temperature = json.getInt("temperature"),
                        pollution = json.getInt("pollution")
                    )
                    onComplete(coeffs)
                } catch (e: Exception) {
                    onComplete(null)  // ← خطا → null برگردون
                }
            },
            { error ->
                onComplete(null)  // ← خطا → null برگردون
            }
        )

        VolleySingleton.getInstance(context).add(request)
    }

    /**
     * دریافت ضرایب به صورت همزمان (برای مواردی که کش پر شده)
     */
    fun getCachedCoefficients(): EvaluationModels.Coefficients? {
        return coefficients
    }

    /**
     * پاک کردن کش
     */
    fun clear() {
        coefficients = null
    }
}
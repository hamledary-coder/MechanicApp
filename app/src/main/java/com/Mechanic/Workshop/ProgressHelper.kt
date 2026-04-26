package com.Mechanic.Workshop

import android.app.ProgressDialog
import android.content.Context

object ProgressHelper {
    private var progressDialog: ProgressDialog? = null

    fun show(context: Context, message: String = "لطفاً منتظر بمانید...") {
        dismiss()
        progressDialog = ProgressDialog(context, R.style.ProgressDialogTheme).apply {
            setMessage(message)
            setCancelable(false)
            show()
        }
    }

    fun dismiss() {
        progressDialog?.dismiss()
        progressDialog = null
    }
}
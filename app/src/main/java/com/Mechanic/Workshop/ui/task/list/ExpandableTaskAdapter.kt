package com.Mechanic.Workshop.ui.task.list

import TaskModel
import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.data.remote.Config
import com.Mechanic.Workshop.utils.UserCache  // ← اضافه کن
import androidx.core.graphics.toColorInt

class ExpandableTaskAdapter(
    private val tasks: List<TaskModel>,
    private val tabType: String,  // "unassigned", "inProgress", "myCartable", "archived"
    private val onEditClick: (TaskModel) -> Unit,
    private val onDeleteClick: (TaskModel) -> Unit,
    private val onReferClick: (TaskModel) -> Unit,
    private val onItemClick: ((TaskModel) -> Unit)? = null
) : RecyclerView.Adapter<ExpandableTaskAdapter.ViewHolder>() {

    private val expandedPosition = mutableSetOf<Int>()
    private lateinit var userRole: String

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Header
        val tvId: TextView = itemView.findViewById(R.id.tvTaskId)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTaskTitle)
        val ivExpand: ImageView = itemView.findViewById(R.id.ivExpand)
        val ivMenu: ImageView = itemView.findViewById(R.id.ivMenu)
        val divider: View = itemView.findViewById(R.id.divider)
        val detailLayout: View = itemView.findViewById(R.id.detailLayout)

        // Detail
        val tvDescription: TextView = itemView.findViewById(R.id.tvTaskDescription)
        val tvResponsible: TextView = itemView.findViewById(R.id.tvTaskResponsible)
        val tvAssignees: TextView = itemView.findViewById(R.id.tvTaskAssignees)
        val tvUnit: TextView = itemView.findViewById(R.id.tvTaskUnit)
        val tvPriority: TextView = itemView.findViewById(R.id.tvTaskPriority)
        val tvRequestDate: TextView = itemView.findViewById(R.id.tvRequestDate)
        val tvRequester: TextView = itemView.findViewById(R.id.tvRequester)
        val tvDeclarationMethod: TextView = itemView.findViewById(R.id.tvDeclarationMethod)
        val tvSystemNumber: TextView = itemView.findViewById(R.id.tvSystemNumber)
        val tvInitialReview: TextView = itemView.findViewById(R.id.tvInitialReview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task_expandable, parent, false)


        val sharedPref = view.context.getSharedPreferences(Config.PrefKeys.USER_PREFS, Context.MODE_PRIVATE)
        userRole = sharedPref.getString(Config.PrefKeys.USER_ROLE, "") ?: ""



        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = tasks[position]
        val isExpanded = expandedPosition.contains(position)

        holder.tvId.text = "#${task.id}"
        holder.tvTitle.text = task.title

        // تنظیم رنگ نوار
        setStripColor(holder, task)

        // ✅ تنظیم رنگ نوار بالایی (اضافه کن)
        setTopStripColor(holder, task)



        if (isExpanded) {
            holder.ivExpand.setImageResource(R.drawable.ic_chevron_up)
            holder.divider.visibility = View.VISIBLE
            holder.detailLayout.visibility = View.VISIBLE
            showAllFields(holder, task)
        } else {
            holder.ivExpand.setImageResource(R.drawable.ic_chevron_down)
            holder.divider.visibility = View.VISIBLE

            when (tabType) {
                "unassigned" -> {
                    holder.detailLayout.visibility = View.GONE
                }
                "inProgress", "myCartable" -> {
                    holder.detailLayout.visibility = View.VISIBLE
                    showCollapsedFields(holder, task)
                }
                else -> {
                    holder.detailLayout.visibility = View.GONE
                }
            }
        }

        // تنظیم منوی سه نقطه
        setupMenu(holder, task)

        // کلیک روی هدر
        holder.itemView.findViewById<View>(R.id.headerLayout).setOnClickListener {
            if (isExpanded) {
                expandedPosition.remove(position)
            } else {
                expandedPosition.clear()
                expandedPosition.add(position)
            }
            notifyDataSetChanged()
        }

        // کلیک روی آیتم
        if (tabType == "inProgress" || tabType == "myCartable" || tabType == "archived") {
            holder.itemView.setOnClickListener {
                onItemClick?.invoke(task)
            }
        }
    }

    private fun setStripColor(holder: ViewHolder, task: TaskModel) {
        val colorStrip = holder.itemView.findViewById<View>(R.id.colorStrip)
        val sharedPref = holder.itemView.context.getSharedPreferences(Config.PrefKeys.USER_PREFS, Context.MODE_PRIVATE)
        val userRole = sharedPref.getString(Config.PrefKeys.USER_ROLE, "") ?: ""

        // ✅ برای کارمند در وضعیت 41، نوار سمت راست رو مخفی کن
        if (task.status == "41" && userRole == Config.RoleCode.EMPLOYEE) {
            colorStrip.visibility = View.GONE
            return
        }

        // گزارش جدید دارد → آبی (برای سرشیفت و مدیر)
        if (task.hasUnseenReport && (userRole == Config.RoleCode.SUPERVISOR || userRole == Config.RoleCode.MANAGER)) {
            colorStrip.setBackgroundColor(Color.parseColor("#2196F3"))
            colorStrip.visibility = View.VISIBLE
            return
        }

        // ✅ درخواست تایید (status=41) برای سرشیفت → بنفش (تغییر از قرمز)
        if (task.status == "41" && userRole == Config.RoleCode.SUPERVISOR) {
            colorStrip.setBackgroundColor(Color.parseColor("#9C27B0"))  // ← بنفش
            colorStrip.visibility = View.VISIBLE
            return
        }

        // حالت عادی → مخفی
        colorStrip.visibility = View.GONE
    }

    private fun setTopStripColor(holder: ViewHolder, task: TaskModel) {
        val topStrip = holder.itemView.findViewById<View>(R.id.topColorStrip)

        // بررسی کن فوریت چقدر است
        val urgency = task.urgency

        when {
            urgency == "خیلی زیاد" -> {
                topStrip.setBackgroundColor("#F44336".toColorInt())  // قرمز
                topStrip.visibility = View.VISIBLE
            }
            urgency == "زیاد" -> {
                topStrip.setBackgroundColor("#FFCC80".toColorInt())  // نارنجی
                topStrip.visibility = View.VISIBLE
            }

            else -> {
                topStrip.visibility = View.GONE  // ← مخفی کن
            }
        }

        // ✅ وضعیت 41 برای کارمند → نوار سبز
        if (task.status == "41" && userRole == Config.RoleCode.EMPLOYEE) {
            topStrip.setBackgroundColor(Color.parseColor("#4CAF50"))
            topStrip.visibility = View.VISIBLE
            return
        }

    }
    private fun setupMenu(holder: ViewHolder, task: TaskModel) {
        if (tabType != "archived" && (userRole == Config.RoleCode.MANAGER || userRole == Config.RoleCode.SUPERVISOR)) {
            holder.ivMenu.visibility = View.VISIBLE
            holder.ivMenu.setOnClickListener { view ->
                PopupMenu(view.context, view).apply {
                    try {
                        val field = PopupMenu::class.java.getDeclaredField("mPopup")
                        field.isAccessible = true
                        val menuPopup = field.get(this)
                        menuPopup.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                            .invoke(menuPopup, true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    menu.add(0, 1, 0, "ویرایش").setIcon(R.drawable.ic_edit).also {
                        it.icon?.setTint(Color.MAGENTA)
                    }
                    menu.add(0, 2, 0, "حذف").setIcon(R.drawable.ic_delete).also {
                        it.icon?.setTint(Color.RED)
                    }
                    menu.add(0, 3, 0, "ارجاع").setIcon(R.drawable.ic_refer).also {
                        it.icon?.setTint(Color.BLUE)
                    }

                    setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            1 -> onEditClick(task)
                            2 -> onDeleteClick(task)
                            3 -> onReferClick(task)
                        }
                        true
                    }
                    show()
                }
            }
        } else {
            holder.ivMenu.visibility = View.GONE
        }
    }

    // ========== تغییر اصلی اینجاست ==========
    // نمایش فیلدهای محدود در حالت بسته
    private fun showCollapsedFields(holder: ViewHolder, task: TaskModel) {
        // مخفی کردن همه فیلدها اول
        holder.tvDescription.visibility = View.GONE
        holder.tvAssignees.visibility = View.GONE
        holder.tvRequestDate.visibility = View.GONE
        holder.tvRequester.visibility = View.GONE
        holder.tvDeclarationMethod.visibility = View.GONE
        holder.tvSystemNumber.visibility = View.GONE
        holder.tvInitialReview.visibility = View.GONE
        // مخفی کردن دیوایدرها
        holder.itemView.findViewById<View>(R.id.dividerDescription)?.visibility = View.GONE
        holder.itemView.findViewById<View>(R.id.dividerAssignee)?.visibility = View.GONE

        // مسئول - استفاده از UserCache
        // مسئول - با رنگ ملایم
        // مسئول
        if (task.responsible.isNotEmpty() && task.responsible != "0") {
            val responsibleName = UserCache.getName(task.responsible)
            holder.tvResponsible.text = "مسئول: $responsibleName"
            holder.tvResponsible.visibility = View.VISIBLE
        } else {
            holder.tvResponsible.visibility = View.GONE
        }

        // انجام‌دهندگان - استفاده از UserCache
        if (task.assignedTo.isNotEmpty()) {
            val assigneeNames = task.assignedTo.split(",").map {
                UserCache.getName(it.trim())  // ← تغییر
            }
            holder.tvAssignees.text = "گروه: ${assigneeNames.joinToString("، ")}"
            holder.tvAssignees.visibility = View.VISIBLE
        } else {
            holder.tvAssignees.visibility = View.GONE
        }

        // واحد - نمایش مستقیم نام مجموعه
        if (task.unit.isNotEmpty() && task.unit != "null") {
            val unitText = Config.UnitCode.getText(task.unit)
            if (unitText.isNotEmpty()) {
                // اگر واحد از نوع "مجموعه‌ها" است، مقدار sub_unit را نشان بده
                if (task.unit == "مجموعه\u200Cها" && task.sub_unit.isNotEmpty()) {
                    holder.tvUnit.text = "مجموعه: ${task.sub_unit}"
                } else {
                    holder.tvUnit.text = "واحد: $unitText"
                }
                holder.tvUnit.visibility = View.VISIBLE
            } else {
                holder.tvUnit.visibility = View.GONE
            }
        } else {
            holder.tvUnit.visibility = View.GONE
        }

        // فوریت
        if (task.urgency.isNotEmpty() && task.urgency != "null" && task.urgency != "عادی") {
            holder.tvPriority.text = "فوریت: ${task.urgency}"

            // ✅ شرط رنگ‌آمیزی همرنگ نوار
            when (task.urgency) {
                "خیلی زیاد" -> holder.tvPriority.setTextColor("#F44336".toColorInt())
                "زیاد" -> holder.tvPriority.setTextColor("#FF9800".toColorInt())
                else -> holder.tvPriority.setTextColor("#666666".toColorInt())
            }

            holder.tvPriority.visibility = View.VISIBLE
        } else {
            holder.tvPriority.visibility = View.GONE
        }
    }

    // نمایش همه فیلدها در حالت باز
    private fun showAllFields(holder: ViewHolder, task: TaskModel) {
        // توضیحات
        holder.tvDescription.text = task.description.ifEmpty { "توضیحاتی وارد نشده" }
        holder.tvDescription.visibility = View.VISIBLE

        // نمایان کردن دیوایدرها
        holder.itemView.findViewById<View>(R.id.dividerDescription)?.visibility = View.VISIBLE
        holder.itemView.findViewById<View>(R.id.dividerAssignee)?.visibility = View.VISIBLE

        // مسئول - استفاده از UserCache
        // مسئول - با رنگ ملایم
        if (task.responsible.isNotEmpty() && task.responsible != "0") {
            val responsibleName = UserCache.getName(task.responsible)

            // ایجاد یک SpannableStringBuilder برای رنگ‌آمیزی
            val text = SpannableStringBuilder("مسئول انجام کار: ")

            // رنگ آبی تیره برای برچسب "مسئول:"
            text.setSpan(
                ForegroundColorSpan(Color.parseColor("#1565C0")),
                0, text.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // اضافه کردن نام با رنگ تیره‌تر
            val nameSpan = SpannableStringBuilder(responsibleName)
            nameSpan.setSpan(
                ForegroundColorSpan(Color.parseColor("#333333")),
                0, nameSpan.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            text.append(nameSpan)

            holder.tvResponsible.text = text
            holder.tvResponsible.visibility = View.VISIBLE
        } else {
            holder.tvResponsible.visibility = View.GONE
        }

        // انجام‌دهندگان - استفاده از UserCache
        if (task.assignedTo.isNotEmpty()) {
            val assigneeNames = task.assignedTo.split(",").map {
                UserCache.getName(it.trim())  // ← تغییر
            }
            holder.tvAssignees.text = "گروه انجام دهنده: ${assigneeNames.joinToString("، ")}"
            holder.tvAssignees.visibility = View.VISIBLE
        } else {
            holder.tvAssignees.visibility = View.GONE
        }

        // واحد - نمایش مستقیم نام مجموعه
        if (task.unit.isNotEmpty() && task.unit != "null") {
            val unitText = Config.UnitCode.getText(task.unit)
            if (unitText.isNotEmpty()) {
                if (task.unit == "مجموعه‌ها" && task.sub_unit.isNotEmpty()) {
                    holder.tvUnit.text = "مجموعه: ${task.sub_unit}"
                } else {
                    holder.tvUnit.text = "واحد: $unitText"
                }
                holder.tvUnit.visibility = View.VISIBLE
            } else {
                holder.tvUnit.visibility = View.GONE
            }
        } else {
            holder.tvUnit.visibility = View.GONE
        }

        // فوریت
        if (task.urgency.isNotEmpty() && task.urgency != "null" && task.urgency != "عادی") {
            holder.tvPriority.text = "فوریت: ${task.urgency}"

            // ✅ شرط رنگ‌آمیزی همرنگ نوار
            when (task.urgency) {
                "خیلی زیاد" -> holder.tvPriority.setTextColor("#F44336".toColorInt())
                "زیاد" -> holder.tvPriority.setTextColor("#FF9800".toColorInt())
                else -> holder.tvPriority.setTextColor("#666666".toColorInt())
            }

            holder.tvPriority.visibility = View.VISIBLE
        } else {
            holder.tvPriority.visibility = View.GONE
        }

        // تاریخ اعلام
        if (task.request_date.isNotEmpty()) {
            val displayDate = task.request_date.replace("-", "/")
            holder.tvRequestDate.text = "تاریخ اعلام: $displayDate"
            holder.tvRequestDate.visibility = View.VISIBLE
        } else {
            holder.tvRequestDate.visibility = View.GONE
        }

        // صادرکننده
        if (task.requester.isNotEmpty()) {
            holder.tvRequester.text = "صادرکننده: ${task.requester}"
            holder.tvRequester.visibility = View.VISIBLE
        } else {
            holder.tvRequester.visibility = View.GONE
        }

        // نحوه اعلام
        if (task.declaration_method.isNotEmpty()) {
            holder.tvDeclarationMethod.text = "نحوه اعلام: ${task.declaration_method}"
            holder.tvDeclarationMethod.visibility = View.VISIBLE
        } else {
            holder.tvDeclarationMethod.visibility = View.GONE
        }

        // شماره سامانه
        if (task.system_request_number.isNotEmpty()) {
            holder.tvSystemNumber.text = "شماره سامانه: ${task.system_request_number}"
            holder.tvSystemNumber.visibility = View.VISIBLE
        } else {
            holder.tvSystemNumber.visibility = View.GONE
        }

        // بررسی اولیه
        if (task.initial_review.isNotEmpty()) {
            holder.tvInitialReview.text = "بررسی اولیه: ${task.initial_review}"
            holder.tvInitialReview.visibility = View.VISIBLE
        } else {
            holder.tvInitialReview.visibility = View.GONE
        }
    }



    private fun getColorForUserId(userId: String): Int {
        val colors = listOf(
            "#E91E63", "#9C27B0", "#673AB7", "#3F51B5",
            "#2196F3", "#03A9F4", "#00BCD4", "#009688",
            "#4CAF50", "#8BC34A", "#CDDC39", "#FFEB3B",
            "#FFC107", "#FF9800", "#FF5722", "#795548"
        )
        val index = userId.hashCode().mod(colors.size)
        return Color.parseColor(colors[index])
    }

    fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

    override fun getItemCount() = tasks.size
}
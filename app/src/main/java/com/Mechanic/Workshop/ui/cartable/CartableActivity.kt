package com.Mechanic.Workshop.ui.cartable

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.Mechanic.Workshop.ui.task.create.CreateTaskActivity
import com.Mechanic.Workshop.R
import com.Mechanic.Workshop.ui.task.list.WorkListFragment
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator


class CartableActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cartable)

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val fabAddTask = findViewById<ExtendedFloatingActionButton>(R.id.fabAddTask)

        // تنظیم جهت اسکرول برای فارسی
        viewPager.layoutDirection = ViewPager2.LAYOUT_DIRECTION_RTL

        // اتصال آداپتور
        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter

        // تنظیم تب‌ها با آیکون
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = "کارتابل من"
                    tab.setIcon(R.drawable.ic_cartable)
                }
                1 -> {
                    tab.text = "در حال انجام"
                    tab.setIcon(R.drawable.ic_progress)
                }
                2 -> {
                    tab.text = "انتخاب نشده"
                    tab.setIcon(R.drawable.ic_pending)
                }
            }
        }.attach()

        // باز کردن صفحه ایجاد کار (فعلاً غیرفعال)
        fabAddTask.visibility = View.VISIBLE
        fabAddTask.setOnClickListener {
            val intent = Intent(this, CreateTaskActivity::class.java)
            startActivity(intent)
        }
    }
}

// آداپتور برای مدیریت تب‌ها
class ViewPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> WorkListFragment.Companion.newInstance("کارتابل من")
            1 -> WorkListFragment.Companion.newInstance("در حال انجام")
            2 -> WorkListFragment.Companion.newInstance("انتخاب نشده")
            else -> WorkListFragment.Companion.newInstance("انتخاب نشده")
        }
    }
}
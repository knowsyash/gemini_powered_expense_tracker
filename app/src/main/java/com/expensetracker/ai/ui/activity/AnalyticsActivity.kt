package com.expensetracker.ai.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.expensetracker.ai.R
import com.expensetracker.ai.ui.fragment.DailyAnalyticsFragment
import com.expensetracker.ai.ui.fragment.MonthlyAnalyticsFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var adapter: AnalyticsPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)

        setupToolbar()
        setupTabs()
    }

    override fun onResume() {
        super.onResume()
        // Refresh the current fragment when activity resumes
        refreshCurrentFragment()
    }

    private fun refreshCurrentFragment() {
        try {
            val currentItem = viewPager.currentItem
            val fragment = supportFragmentManager.findFragmentByTag("f$currentItem")

            when (fragment) {
                is DailyAnalyticsFragment -> {
                    fragment.refreshData()
                }
                is MonthlyAnalyticsFragment -> {
                    fragment.refreshData()
                }
            }
        } catch (e: Exception) {
            println("Error refreshing fragment: ${e.message}")
        }
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Analytics"
        }
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupTabs() {
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        adapter = AnalyticsPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                    tab.text =
                            when (position) {
                                0 -> "Daily"
                                1 -> "Monthly"
                                else -> ""
                            }
                }
                .attach()
    }

    private inner class AnalyticsPagerAdapter(activity: FragmentActivity) :
            FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> DailyAnalyticsFragment()
                1 -> MonthlyAnalyticsFragment()
                else -> DailyAnalyticsFragment()
            }
        }
    }
}

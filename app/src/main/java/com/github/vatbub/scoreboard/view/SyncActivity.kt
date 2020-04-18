package com.github.vatbub.scoreboard.view

import android.os.Bundle
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import com.github.vatbub.scoreboard.R
import com.github.vatbub.scoreboard.view.viewModels.SectionsPagerAdapter
import kotlinx.android.synthetic.main.activity_sync.*

class SyncActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sync)
        setSupportActionBar(sync_toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = findViewById(R.id.view_pager)
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = findViewById(R.id.tabs)
        tabs.setupWithViewPager(viewPager)
    }
}
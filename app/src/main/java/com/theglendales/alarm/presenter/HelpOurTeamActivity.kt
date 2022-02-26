package com.theglendales.alarm.presenter

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.theglendales.alarm.R

private const val TAG="HelpOurTeamActivity"
/*Chips
0.99 Buy us a coffee
4.99
9.99
19.99
99.99*/
class HelpOurTeamActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_our_team)
    }
}
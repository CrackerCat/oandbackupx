/*
 * OAndBackupX: open-source apps backup and restore app.
 * Copyright (C) 2020  Antonios Hazim
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.machiav3lli.backup.activities

import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import com.machiav3lli.backup.Constants
import com.machiav3lli.backup.Constants.classAddress
import com.machiav3lli.backup.Constants.classTag
import com.machiav3lli.backup.databinding.ActivitySplashBinding
import com.machiav3lli.backup.utils.PrefUtils
import com.machiav3lli.backup.utils.UIUtils

class SplashActivity : BaseActivity() {
    private var binding: ActivitySplashBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        UIUtils.setDayNightTheme(PrefUtils.getPrivateSharedPrefs(this).getString(Constants.PREFS_THEME, "system"))
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        val prefs = PrefUtils.getPrivateSharedPrefs(this)
        val powerManager = this.getSystemService(POWER_SERVICE) as PowerManager
        val introIntent = Intent(applicationContext, IntroActivityX::class.java)
        if (prefs.getBoolean(Constants.PREFS_FIRST_LAUNCH, true)) {
            startActivity(introIntent)
        } else if (PrefUtils.checkStoragePermissions(this) &&
                PrefUtils.isStorageDirSetAndOk(this) &&
                PrefUtils.checkUsageStatsPermission(this) &&
                (prefs.getBoolean(Constants.PREFS_IGNORE_BATTERY_OPTIMIZATION, false)
                        || powerManager.isIgnoringBatteryOptimizations(packageName))) {
            introIntent.putExtra(classAddress(".fragmentNumber"), 3)
            startActivity(introIntent)
        } else {
            introIntent.putExtra(classAddress(".fragmentNumber"), 2)
            startActivity(introIntent)
        }
        overridePendingTransition(0, 0)
    }

    companion object {
        private val TAG = classTag(".SplashActivity")
    }
}
/*
 * Copyright (c) 2016 Marien Raat <marienraat@riseup.net>
 * Copyright (c) 2017  Stephen Michel <s@smichel.me>
 *
 *  This file is free software: you may copy, redistribute and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This file is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 *  FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.jmstudios.redmoon.model

import android.content.Context
import android.content.SharedPreferences

import com.jmstudios.redmoon.R

import com.jmstudios.redmoon.helper.Logger
import com.jmstudios.redmoon.helper.Profile
import com.jmstudios.redmoon.util.*

/**
 * This singleton manages the SharedPreference that store all custom
 * filter profiles added by the user.
 *
 * The profiles are stored in a separate SharedPreference, with one key-value
 * pair per profile. The key is their position in the list of profiles
 * (an integer as a string). The value is a JSON string.
 */

object ProfilesModel: Logger() {

    private const val PREFERENCE_NAME = "com.jmstudios.redmoon.PROFILES_PREFERENCE"
    private const val MODE = Context.MODE_PRIVATE

    private val prefs: SharedPreferences
        get() = appContext.getSharedPreferences(PREFERENCE_NAME, MODE)

    private val defaultProfiles: List<Profile> =
            listOf(Profile(getString(R.string.filter_name_custom     ), 10, 30, 40, false),
                   Profile(getString(R.string.filter_name_default    ), 10, 30, 40, false),
                   Profile(getString(R.string.filter_name_bed_reading), 20, 60, 78, false),
                   Profile(getString(R.string.filter_name_dim_only   ),  0,  0, 60, false))

    private val mProfiles: ArrayList<Profile> = ArrayList(prefs.all.run {
        if (isEmpty()) {
            Log.i("Creating default ProfilesModel")
            defaultProfiles
        } else {
            Log.i("Restoring ProfilesModel")
            mapKeys{ (k, _) -> k.toInt() }.toSortedMap().map{ (_, v) -> Profile.parse(v as String) }
        }
    })

    private fun updateSharedPreferences() {
        Log.i("Updating SharedPreferences")
        val editor = prefs.edit()
        editor.run {
            clear()
            mProfiles.forEachIndexed { index, profile ->
                Log.i("Storing profile $index, ${profile.name}")
                putString(index.toString(), profile.toString())
            }
        }
        editor.apply()
        Log.d("Done updating SharedPreferences")
    }

    fun getProfileName(index: Int): String  = mProfiles[index].name
    fun getProfile    (index: Int): Profile = mProfiles[index]

    fun setCustom() {
        mProfiles[0] = Profile(color           = Config.color,
                               intensity       = Config.intensity,
                               dimLevel        = Config.dimLevel,
                               lowerBrightness = Config.lowerBrightness)
        updateSharedPreferences()
    }

    private val custom: Profile
        get() = mProfiles[0]

    fun addProfile(newName: String, fail: Int = 0): Pair<Int, Int> = mProfiles.run {
        Log.i("addProfile $newName; Current Size: ${mProfiles.size}")
        val profile = custom.copy(name = newName)

        val success = add(profile)
        if (success) { updateSharedPreferences() }

        Pair(size, if (success) indexOf(profile) else fail)
    }

    fun removeProfile(index: Int): Int = mProfiles.run {
        val profile = removeAt(index)
        Log.i("removed profile $index: ${profile.name}")
        setCustom()
        updateSharedPreferences()
        size
    }

    // de-dupe, then append defaults
    fun reset(): Int = mProfiles.run {
        remove(custom)
        removeAll(defaultProfiles)
        addAll(0, defaultProfiles)
        updateSharedPreferences()
        size
    }
}

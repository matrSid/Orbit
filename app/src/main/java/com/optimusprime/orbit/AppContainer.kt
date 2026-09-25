package com.optimusprime.orbit

import android.content.Context
import com.optimusprime.orbit.data.db.AppDatabase
import com.optimusprime.orbit.data.repository.StudyRepository

/**
 * Deliberately not Hilt/Dagger. A three-person hackathon team needs to be
 * able to explain every moving part on demand — this is the whole
 * dependency graph, built by hand, in one small file.
 */
class AppContainer(context: Context) {
    val repository: StudyRepository by lazy {
        StudyRepository(AppDatabase.getInstance(context))
    }
}

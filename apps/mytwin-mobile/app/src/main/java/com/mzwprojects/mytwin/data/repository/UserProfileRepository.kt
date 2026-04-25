package com.mzwprojects.mytwin.data.repository

import com.mzwprojects.mytwin.data.datasource.UserProfileDataSource
import com.mzwprojects.mytwin.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

class UserProfileRepository(private val dataSource: UserProfileDataSource) {

    val profile: Flow<UserProfile> = dataSource.profile

    suspend fun update(transform: (UserProfile) -> UserProfile) {
        dataSource.update(transform)
    }
}
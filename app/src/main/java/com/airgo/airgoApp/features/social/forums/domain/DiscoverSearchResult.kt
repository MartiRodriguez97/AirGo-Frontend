package com.airgo.airgoApp.features.social.domain

import com.airgo.airgoApp.features.social.data.UserSearchResult
import com.airgo.airgoApp.features.social.forums.data.Forum

sealed class DiscoverSearchResult {
    data class UserResult(val user: UserSearchResult) : DiscoverSearchResult()
    data class ForumResult(val forum: Forum) : DiscoverSearchResult()
}
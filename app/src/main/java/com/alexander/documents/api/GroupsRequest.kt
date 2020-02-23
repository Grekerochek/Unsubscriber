package com.alexander.documents.api

import com.alexander.documents.entity.Group
import com.vk.api.sdk.VKApiManager
import com.vk.api.sdk.VKApiResponseParser
import com.vk.api.sdk.VKMethodCall
import com.vk.api.sdk.exceptions.VKApiIllegalResponseException
import com.vk.api.sdk.internal.ApiCommand
import org.json.JSONException
import org.json.JSONObject

/**
 * author alex
 */
class GroupsRequest : ApiCommand<List<Group>>() {
    override fun onExecute(manager: VKApiManager): List<Group> {
        val call = VKMethodCall.Builder()
            .method("groups.get")
            .args("extended", 1)
            .args("fields", "description,members_count,site,city")
            .version(manager.config.version)
            .build()
        return manager.execute(call, ResponseApiParser())
    }
}

class GroupsLeave(
    private val groupsIds: List<Int>
) : ApiCommand<List<Int>>() {
    override fun onExecute(manager: VKApiManager): List<Int> {
        return groupsIds.map { groupId ->
            val call = VKMethodCall.Builder()
                .method("groups.leave")
                .args("group_id", groupId)
                .version(manager.config.version)
                .build()
            manager.execute(call, ResponseApiParserLeave())
        }
    }
}

private class ResponseApiParser : VKApiResponseParser<List<Group>> {
    override fun parse(response: String): List<Group> {
        try {
            val groupsResponse = JSONObject(response).getJSONObject("response")
            val items = groupsResponse.getJSONArray("items")
            val groups = ArrayList<Group>(items.length())
            for (i in 0 until items.length()) {
                val group = Group.parse(items.getJSONObject(i))
                groups.add(group)
            }
            return groups
        } catch (ex: JSONException) {
            throw VKApiIllegalResponseException(ex)
        }
    }
}

private class ResponseApiParserLeave : VKApiResponseParser<Int> {
    override fun parse(response: String): Int {
        try {
            return JSONObject(response).optInt("response")
        } catch (ex: JSONException) {
            throw VKApiIllegalResponseException(ex)
        }
    }
}
package com.alexander.documents.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.vk.api.sdk.VK
import com.vk.api.sdk.VKApiCallback
import com.vk.api.sdk.auth.VKAccessToken
import com.vk.api.sdk.auth.VKAuthCallback
import com.vk.api.sdk.auth.VKScope
import kotlinx.android.synthetic.main.activity_main.*
import androidx.recyclerview.widget.GridLayoutManager
import com.alexander.documents.R
import com.alexander.documents.api.GroupsLeave
import com.alexander.documents.api.GroupsRequest
import com.alexander.documents.entity.Group
import com.google.android.material.appbar.AppBarLayout

/**
 * author alex
 */
class MainActivity : AppCompatActivity(), GroupDetailsBottomSheetDialogFragment.OpenClickListener {

    private val selectedGroupsIdsAndPositions: MutableMap<Int, Int> = mutableMapOf()

    private val groupsAdapter by lazy(LazyThreadSafetyMode.NONE) {
        GroupsAdapter(::onGroupClick, ::onGroupLongClick)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            containerView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        if (!VK.isLoggedIn()) {
            VK.login(this, arrayListOf(VKScope.GROUPS))
        } else {
            init()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val callback = object : VKAuthCallback {
            override fun onLogin(token: VKAccessToken) {
                init()
            }

            override fun onLoginFailed(errorCode: Int) {
                showAuthError()
            }
        }
        if (data == null || !VK.onActivityResult(requestCode, resultCode, data, callback)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onOpenClick(group: Group) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(group.site))
        startActivity(intent)
    }

    private fun init() {
        recyclerViewGroups.layoutManager = GridLayoutManager(this, 3)
        recyclerViewGroups.adapter = groupsAdapter
        unsubscribeButton.setOnClickListener { unsubscribe() }
        appbar.addOnOffsetChangedListener(object : AppBarStateChangeListener() {
            override fun onStateChanged(appBarLayout: AppBarLayout, state: State) {
                appNameView.text = if (state == State.COLLAPSED) getString(R.string.app_name) else ""
            }
        })
        containerView.setOnRefreshListener {
            requestGroups()
        }
        requestGroups()
    }

    private fun requestGroups() {
        containerView.isRefreshing = true
        VK.execute(GroupsRequest(), object : VKApiCallback<List<Group>> {
            override fun success(result: List<Group>) {
                if (!isFinishing) {
                    containerView.isRefreshing = false
                    groupsAdapter.groups = result.reversed().toMutableList()
                    unsubscribeButton.visibility = View.GONE
                    selectedGroupsIdsAndPositions.clear()
                }
            }

            override fun fail(error: Exception) {
                showError()
            }
        })
    }

    private fun showError() {
        containerView.isRefreshing = false
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.error))
            .setMessage(getString(R.string.error_message))
            .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showAuthError() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.error))
            .setMessage(getString(R.string.error_auth_message))
            .setPositiveButton(R.string.ok) { _, _ -> VK.login(this, arrayListOf(VKScope.GROUPS)) }
            .setNegativeButton(R.string.cancel) { _, _ -> finish() }
            .show()
    }

    private fun onGroupClick(position: Int, group: Group) {
        if (selectedGroupsIdsAndPositions.contains(group.id)) {
            selectedGroupsIdsAndPositions.remove(group.id)
            groupsAdapter.unSelectGroup(position)
        } else {
            selectedGroupsIdsAndPositions[group.id] = position
            groupsAdapter.selectGroup(position)
        }
        groupsCountView.text = selectedGroupsIdsAndPositions.count().toString()
        if (selectedGroupsIdsAndPositions.isEmpty()) {
            unsubscribeButton.visibility = View.GONE
        } else {
            unsubscribeButton.visibility = View.VISIBLE
        }
    }

    private fun onGroupLongClick(group: Group): Boolean {
        GroupDetailsBottomSheetDialogFragment.newInstance(group)
            .show(supportFragmentManager, null)
        return true
    }

    private fun unsubscribe() {
        containerView.isRefreshing = true
        VK.execute(GroupsLeave(selectedGroupsIdsAndPositions.keys.toList()), object : VKApiCallback<List<Int>> {
            override fun success(result: List<Int>) {
                if (!isFinishing && result.all { it == 1 }) {
                    containerView.isRefreshing = false
                    groupsAdapter.deleteGroups(selectedGroupsIdsAndPositions.values.toList())
                    selectedGroupsIdsAndPositions.clear()
                    unsubscribeButton.visibility = View.GONE
                }
            }

            override fun fail(error: Exception) {
                showError()
            }
        })
    }

    companion object {

        fun createIntent(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }
    }
}

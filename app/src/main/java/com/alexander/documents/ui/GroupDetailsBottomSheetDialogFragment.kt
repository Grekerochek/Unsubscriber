package com.alexander.documents.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.alexander.documents.R
import com.alexander.documents.entity.Group
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.bottom_sheet_layout.*

/**
 * author alex
 */
class GroupDetailsBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private val group: Group by lazy(LazyThreadSafetyMode.NONE) {
        requireArguments()[ARG_GROUP] as Group
    }

    private var openClickListener: OpenClickListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        openClickListener = context as OpenClickListener
    }

    override fun getTheme(): Int {
        return R.style.BottomSheetDialog
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        dialog.setOnShowListener {
            Handler().post {
                val bottomSheet =
                    (dialog as? BottomSheetDialog)?.findViewById<View>(R.id.design_bottom_sheet) as? FrameLayout
                bottomSheet?.let {
                    BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        titleView.text = group.name
        followersView.text = view.context.getString(R.string.members, group.memberCount.toString())
        articleView.text = group.description
        articleView.visibility = if (group.description.isEmpty()) View.GONE else View.VISIBLE
        newsfeedView.visibility = if (group.city.isEmpty()) View.GONE else View.VISIBLE
        newsfeedView.text = group.city
        dismissView.setOnClickListener { dismiss() }
        openButton.setOnClickListener { openClickListener?.onOpenClick(group) }
    }

    override fun onDetach() {
        super.onDetach()
        openClickListener = null
    }

    companion object {
        private const val ARG_GROUP = "arg_group"

        fun newInstance(group: Group) = GroupDetailsBottomSheetDialogFragment()
            .apply {
                val args = Bundle()
                args.putParcelable(ARG_GROUP, group)
                arguments = args
            }
    }

    interface OpenClickListener {
        fun onOpenClick(group: Group)
    }
}
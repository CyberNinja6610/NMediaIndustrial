package ru.netology.nmedia.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.viewmodel.PostViewModel

class FeedFragment : Fragment() {

    private val viewModel: PostViewModel by viewModels(ownerProducer = ::requireParentFragment)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentFeedBinding.inflate(inflater, container, false)

        val adapter = PostsAdapter(object : OnInteractionListener {
            override fun onEdit(post: Post) {
                viewModel.edit(post)
            }

            override fun onLike(post: Post) {
                viewModel.likeById(post.id)
            }

            override fun onRemove(post: Post) {
                viewModel.removeById(post.id)
            }

            override fun onShare(post: Post) {
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, post.content)
                    type = "text/plain"
                }

                val shareIntent =
                    Intent.createChooser(intent, getString(R.string.chooser_share_post))
                startActivity(shareIntent)
            }
        })
        binding.list.adapter = adapter
        viewModel.data.observe(viewLifecycleOwner, { state ->
            adapter.submitList(state.posts)
            binding.progress.isVisible = state.loading
            binding.errorGroup.isVisible = state.error
            binding.emptyText.isVisible = state.empty
            binding.listRefresh.isRefreshing = false;
        })

        binding.retryButton.setOnClickListener {
            viewModel.loadPosts()
        }

        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_feedFragment_to_newPostFragment)
        }

        binding.listRefresh.setOnRefreshListener {
            viewModel.loadPosts()
        }

        viewModel.retryLikeById.observe(viewLifecycleOwner) {
            it?.let {
                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(getString(R.string.error_title))
                    .setMessage(getString(R.string.request_failure))
                    .setNegativeButton(getString(R.string.cancel_title)) { dialog, which ->
                        dialog.cancel()
                    }
                    .setPositiveButton(getString(R.string.retry_title)) { dialog, which ->
                        viewModel.likeById(it)
                    }
                    .show()
            }
        }

        viewModel.retryRemoveById.observe(viewLifecycleOwner) {
            it?.let {
                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(getString(R.string.error_title))
                    .setMessage(getString(R.string.request_failure))
                    .setNegativeButton(getString(R.string.cancel_title)) { dialog, which ->
                        dialog.cancel()
                    }
                    .setPositiveButton(getString(R.string.retry_title)) { dialog, which ->
                        viewModel.removeById(it)
                    }
                    .show()
            }
        }

        return binding.root
    }
}

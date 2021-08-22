package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.*
import retrofit2.Call
import retrofit2.Callback
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.repository.*
import ru.netology.nmedia.repository.PostRepository.*
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.IOException
import kotlin.concurrent.thread

private val empty = Post(
    id = 0,
    content = "",
    author = "",
    likedByMe = false,
    likes = 0,
    published = "",
    authorAvatar = "",
    attachment = null
)

class PostViewModel(application: Application) : AndroidViewModel(application) {
    // упрощённый вариант
    private val repository: PostRepository = PostRepositoryImpl()
    private val _data = MutableLiveData(FeedModel())
    val data: LiveData<FeedModel>
        get() = _data
    val edited = MutableLiveData(empty)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    private val _retryLikeById = SingleLiveEvent<Long>()
    val retryLikeById: LiveData<Long>
        get() = _retryLikeById

    private val _retrySave = SingleLiveEvent<Post>()
    val retrySave: LiveData<Post>
        get() = _retrySave

    private val _retryRemoveById = SingleLiveEvent<Long>()
    val retryRemoveById: LiveData<Long>
        get() = _retryRemoveById


    init {
        loadPosts()
    }

    fun loadPosts() {
        // Начинаем загрузку
        _data.postValue(FeedModel(loading = true))
        repository.getAllAsync(object : GetAllCallback {
            override fun onSuccess(posts: List<Post>) {
                _data.postValue(FeedModel(posts = posts, empty = posts.isEmpty()))
            }

            override fun onError(e: Throwable) {
                _data.postValue(FeedModel(error = true))
            }
        })
    }


    fun save() {
        edited.value?.let {
            repository.saveAsync(it, object : SaveCallback {
                override fun onSuccess() {
                    _postCreated.postValue(Unit)
                }
                override fun onError(e: Throwable) {
                    _retrySave.postValue(it)
                }
            })
        }
        edited.value = empty
    }

    fun edit(post: Post) {
        edited.value = post
    }

    fun changeContent(content: String) {
        val text = content.trim()
        if (edited.value?.content == text) {
            return
        }
        edited.value = edited.value?.copy(content = text)
    }

    fun likeById(id: Long) {
        val old = _data.value?.posts.orEmpty()
        val oldPost = _data.value?.posts.orEmpty().find { it.id == id }
        _data.postValue(
            _data.value?.copy(
                posts = _data.value?.posts.orEmpty()
                    .map {
                        if (oldPost?.id == it.id) {
                            it.copy(
                                likedByMe = !it.likedByMe,
                                likes = if (it.likedByMe) it.likes - 1 else it.likes + 1
                            )
                        } else {
                            it
                        }
                    }
            )
        )
        if (oldPost?.likedByMe == true) {
            repository.unlikeByIdAsync(id, object : UnlikeByIdCallback {
                override fun onError(e: Throwable) {
                    _data.postValue(_data.value?.copy(posts = old))
                }
            })
        } else if (oldPost?.likedByMe == false) {
            repository.likeByIdAsync(id, object : LikeByIdCallback {
                override fun onError(e: Throwable) {
                    _data.postValue(_data.value?.copy(posts = old))
                    _retryLikeById.postValue(id)
                }
            })
        }

    }

    fun removeById(id: Long) {
        // Оптимистичная модель
        val old = _data.value?.posts.orEmpty()
        _data.postValue(
            _data.value?.copy(posts = _data.value?.posts.orEmpty()
                .filter { it.id != id }
            )
        )
        repository.removeByIdAsync(id, object : RemoveByIdCallback {
            override fun onError(e: Throwable) {
                _data.postValue(_data.value?.copy(posts = old))
                _retryRemoveById.postValue(id)
            }
        })
    }

}

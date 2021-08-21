package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.*
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
    published = ""
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

            override fun onError(e: Exception) {
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
                override fun onError(e: Exception) {
                    _postCreated.postValue(Unit)
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
                override fun onError(e: Exception) {
                    _data.postValue(_data.value?.copy(posts = old))
                }
            })
        } else if (oldPost?.likedByMe == false) {
            repository.likeByIdAsync(id, object : LikeByIdCallback {
                override fun onError(e: Exception) {
                    _data.postValue(_data.value?.copy(posts = old))
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
            override fun onError(e: Exception) {
                _data.postValue(_data.value?.copy(posts = old))
            }
        })
    }
}

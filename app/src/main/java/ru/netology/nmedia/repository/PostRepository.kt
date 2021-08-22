package ru.netology.nmedia.repository

import retrofit2.Call
import ru.netology.nmedia.dto.Post

interface PostRepository {
    fun getAll(): List<Post>
    fun likeById(id: Long)
    fun unlikeById(id: Long)
    fun save(post: Post)
    fun removeById(id: Long)

    fun getAllAsync(callback: GetAllCallback)
    fun likeByIdAsync(id: Long, callback: LikeByIdCallback)
    fun unlikeByIdAsync(id: Long, callback: UnlikeByIdCallback)
    fun saveAsync(post: Post, callback: SaveCallback)
    fun removeByIdAsync(id: Long, callback: RemoveByIdCallback)

    interface GetAllCallback {
        fun onSuccess(posts: List<Post>) {}
        fun onError(e: Throwable) {}
    }

    interface SaveCallback {
        fun onSuccess() {}
        fun onError(e: Throwable) {}
    }

    interface LikeByIdCallback {
        fun onSuccess() {}
        fun onError(e: Throwable) {}
    }

    interface UnlikeByIdCallback {
        fun onSuccess() {}
        fun onError(e: Throwable) {}
    }

    interface RemoveByIdCallback {
        fun onSuccess() {}
        fun onError(e: Throwable) {}
    }

}
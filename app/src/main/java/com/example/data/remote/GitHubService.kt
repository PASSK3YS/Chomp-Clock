package com.example.data.remote

import com.squareup.moshi.Json
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

interface GitHubService {
    @Headers(
        "Accept: application/vnd.github.v3+json",
        "User-Agent: ChompClock-Android-App",
        "Cache-Control: no-cache, no-store, must-revalidate",
        "Pragma: no-cache"
    )
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String = "PASSK3YS",
        @Path("repo") repo: String = "Chomp-Clock"
    ): GitHubReleaseResponse

    @Headers(
        "Accept: application/vnd.github.v3+json",
        "User-Agent: ChompClock-Android-App",
        "Cache-Control: no-cache, no-store, must-revalidate",
        "Pragma: no-cache"
    )
    @GET("repos/{owner}/{repo}/releases")
    suspend fun getAllReleases(
        @Path("owner") owner: String = "PASSK3YS",
        @Path("repo") repo: String = "Chomp-Clock"
    ): List<GitHubReleaseResponse>

    @Headers(
        "Accept: application/vnd.github.v3+json",
        "User-Agent: ChompClock-Android-App",
        "Cache-Control: no-cache, no-store, must-revalidate",
        "Pragma: no-cache"
    )
    @GET("repos/{owner}/{repo}/tags")
    suspend fun getTags(
        @Path("owner") owner: String = "PASSK3YS",
        @Path("repo") repo: String = "Chomp-Clock"
    ): List<GitHubTagResponse>

    @Headers(
        "Accept: application/vnd.github.v3+json",
        "User-Agent: ChompClock-Android-App",
        "Cache-Control: no-cache, no-store, must-revalidate",
        "Pragma: no-cache"
    )
    @GET("repos/{owner}/{repo}/commits")
    suspend fun getCommits(
        @Path("owner") owner: String = "PASSK3YS",
        @Path("repo") repo: String = "Chomp-Clock"
    ): List<GitHubCommitResponse>

    companion object {
        private const val BASE_URL = "https://api.github.com/"

        fun create(): GitHubService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(GitHubService::class.java)
        }
    }
}

data class GitHubTagResponse(
    @Json(name = "name") val name: String?,
    @Json(name = "zipball_url") val zipballUrl: String?,
    @Json(name = "tarball_url") val tarballUrl: String?,
    @Json(name = "node_id") val nodeId: String?
)

data class GitHubReleaseResponse(
    @Json(name = "id") val id: Long?,
    @Json(name = "tag_name") val tagName: String?,
    @Json(name = "name") val name: String?,
    @Json(name = "body") val body: String?,
    @Json(name = "html_url") val htmlUrl: String?,
    @Json(name = "published_at") val publishedAt: String?,
    @Json(name = "prerelease") val prerelease: Boolean? = false,
    @Json(name = "assets") val assets: List<GitHubReleaseAsset>? = emptyList()
)

data class GitHubReleaseAsset(
    @Json(name = "id") val id: Long?,
    @Json(name = "name") val name: String?,
    @Json(name = "size") val size: Long?,
    @Json(name = "browser_download_url") val browserDownloadUrl: String?,
    @Json(name = "content_type") val contentType: String?
)

data class GitHubCommitResponse(
    @Json(name = "sha") val sha: String?,
    @Json(name = "commit") val commit: GitHubCommitDetail?,
    @Json(name = "html_url") val htmlUrl: String?
)

data class GitHubCommitDetail(
    @Json(name = "message") val message: String?,
    @Json(name = "author") val author: GitHubCommitAuthor?
)

data class GitHubCommitAuthor(
    @Json(name = "name") val name: String?,
    @Json(name = "date") val date: String?
)


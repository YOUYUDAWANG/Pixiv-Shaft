package ceui.pixiv.api

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface FanboxApi {

    /** 首页「投稿」流。需要登录态;未登录 401。 */
    @GET("post.listHome")
    suspend fun postListHome(@Query("limit") limit: Int = 10): FanboxPostListResponse

    /** 翻页:服务端在 [FanboxPostList.nextUrl] 里给的是 api.fanbox.cc 绝对 URL,照着打即可。 */
    @GET
    suspend fun postListHomeByUrl(@Url url: String): FanboxPostListResponse

    /**
     * 首页「为您推荐的创作者」。未登录也能拿到(内容会变成通用推荐)。
     * 响应里没有翻页游标,就是单页。
     */
    @GET("creator.listRecommended")
    suspend fun creatorListRecommended(@Query("limit") limit: Int = 10): FanboxCreatorListResponse

    /**
     * 帖子元数据。响应是 `body.post`,post 对象里没有 body 字段。
     */
    @GET("post.get")
    suspend fun postGet(@Query("postId") postId: String): FanboxPostDetailResponse

    /** 帖子评论。 */
    @GET("post.getComments")
    suspend fun postGetComments(
        @Query("postId") postId: String,
        @Query("limit") limit: Int = 20,
    ): FanboxCommentResponse

    /** 创作者的赞助方案。 */
    @GET("plan.listCreator")
    suspend fun planListCreator(@Query("creatorId") creatorId: String): FanboxPlanListResponse
}

data class FanboxPostDetailResponse(
    val body: FanboxPostWrapper?
)

data class FanboxPostWrapper(
    val post: FanboxPost?
)

data class FanboxCommentResponse(
    val body: FanboxCommentBody?
)

data class FanboxCommentBody(
    val viewMode: String?,
    val commentList: FanboxCommentList?,
)

data class FanboxCommentList(
    val items: List<FanboxComment>?,
    val nextUrl: String?,
)

data class FanboxComment(
    val id: String,
    val body: String?,
    val createdDatetime: String?,
    val likeCount: Int,
    val isLiked: Boolean,
    val user: FanboxUser?,
    val replies: List<FanboxComment>?,
)

data class FanboxPlanListResponse(
    val body: FanboxPlanList?
)

data class FanboxPlanList(
    val plans: List<FanboxPlan>?
)

data class FanboxPlan(
    val id: String,
    val title: String?,
    val fee: Int,
    val description: String?,
    val coverImageUrl: String?,
    val creatorId: String?,
    val hasAdultContent: Boolean,
)

data class FanboxPostListResponse(
    val body: FanboxPostList?
)

data class FanboxPostList(
    val items: List<FanboxPost>?,
    val nextUrl: String?,
)

data class FanboxPost(
    val id: String,
    val title: String?,
    val feeRequired: Int,
    val publishedDatetime: String?,
    val tags: List<String>?,
    val likeCount: Int,
    val commentCount: Int,
    val isRestricted: Boolean,
    val user: FanboxUser?,
    val creatorId: String?,
    val hasAdultContent: Boolean,
    val cover: FanboxCover?,
    val excerpt: String?,
    val type: String?,
    val body: FanboxPostBody?,
    val coverImageUrl: String?,
) {
    val coverUrl: String get() = cover?.url?.takeIf { it.isNotEmpty() } ?: coverImageUrl.orEmpty()
}

data class FanboxPostBody(
    val text: String?,
    val html: String?,
    val images: List<FanboxImage>?,
    val files: List<FanboxFile>?,
    val blocks: List<FanboxBlock>?,
    val imageMap: Map<String, FanboxImage>?,
    val fileMap: Map<String, FanboxFile>?,
    val embedMap: Map<String, FanboxEmbed>?,
    val urlEmbedMap: Map<String, FanboxUrlEmbed>?,
)

data class FanboxBlock(
    val type: String?,
    val text: String?,
    val imageId: String?,
    val fileId: String?,
    val embedId: String?,
    val urlEmbedId: String?,
    val links: List<FanboxBlockLink>?,
    val styles: List<FanboxBlockStyle>?,
)

data class FanboxBlockStyle(
    val type: String?,
    val offset: Int,
    val length: Int,
)

data class FanboxBlockLink(
    val offset: Int,
    val length: Int,
    val url: String?,
)

data class FanboxImage(
    val id: String?,
    val extension: String?,
    val width: Int,
    val height: Int,
    val originalUrl: String?,
    val thumbnailUrl: String?,
)

data class FanboxFile(
    val id: String?,
    val name: String?,
    val extension: String?,
    val size: Long,
    val url: String?,
)

data class FanboxEmbed(
    val id: String?,
    val serviceProvider: String?,
    val contentId: String?,
)

data class FanboxUrlEmbed(
    val id: String?,
    val type: String?,
    val url: String?,
    val host: String?,
)

data class FanboxUser(
    val userId: String?,
    val name: String?,
    val iconUrl: String?,
)

data class FanboxCover(
    val type: String?,
    val url: String?,
)

data class FanboxCreatorListResponse(
    val body: FanboxCreatorList?
)

data class FanboxCreatorList(
    val creators: List<FanboxCreator>?
)

data class FanboxCreator(
    val user: FanboxUser?,
    val creatorId: String?,
    val description: String?,
    val hasAdultContent: Boolean,
    val coverImageUrl: String?,
    val profileItems: List<FanboxProfileItem>?,
    val isFollowed: Boolean,
    val isSupported: Boolean,
    val hasPublishedPost: Boolean,
    val category: String?,
)

data class FanboxProfileItem(
    val id: String?,
    val type: String?,
    val imageUrl: String?,
    val thumbnailUrl: String?,
)

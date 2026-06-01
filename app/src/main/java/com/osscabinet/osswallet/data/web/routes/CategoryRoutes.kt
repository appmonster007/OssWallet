package com.osscabinet.ossWallet.data.web.routes

import com.osscabinet.ossWallet.data.web.AuthSessionManager
import com.osscabinet.ossWallet.data.web.authenticate
import com.osscabinet.ossWallet.data.web.dto.ApiResponse
import com.osscabinet.ossWallet.data.web.dto.CreateCategoryRequest
import com.osscabinet.ossWallet.data.web.dto.ReorderCategoriesRequest
import com.osscabinet.ossWallet.data.web.dto.UpdateCategoryRequest
import com.osscabinet.ossWallet.data.web.toDto
import com.osscabinet.ossWallet.domain.model.Category
import com.osscabinet.ossWallet.domain.model.CategoryNature
import com.osscabinet.ossWallet.domain.repository.CategoryRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.categoryRoutes(
    authSessionManager: AuthSessionManager,
    categoryRepository: CategoryRepository
) {
    route("/categories") {
        get {
            if (!authenticate(authSessionManager)) return@get
            try {
                val categories = categoryRepository.getAllCategoriesSync()
                val dtos = categories.map { it.toDto() }
                call.respond(ApiResponse(success = true, data = dtos))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(
                        success = false,
                        error = "Internal error"
                    )
                )
            }
        }

        post {
            if (!authenticate(authSessionManager)) return@post
            try {
                val req = call.receive<CreateCategoryRequest>()
                val nature = CategoryNature.entries.find {
                    it.name.equals(
                        req.nature,
                        ignoreCase = true
                    )
                }
                    ?: CategoryNature.NEED
                val category = Category(
                    id = 0,
                    name = req.name,
                    iconName = req.iconName,
                    colorHex = req.colorHex,
                    isDefault = false,
                    parentId = req.parentId,
                    nature = nature
                )
                categoryRepository.upsertCategory(category)
                call.respond(
                    HttpStatusCode.Created,
                    ApiResponse(success = true, data = category.toDto())
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(
                        success = false,
                        error = "Internal error"
                    )
                )
            }
        }

        put("/{id}") {
            if (!authenticate(authSessionManager)) return@put
            try {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@put call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, error = "Invalid id")
                    )
                val existing =
                    categoryRepository.getAllCategoriesSync().find { it.id == id }
                        ?: return@put call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse<Unit>(success = false, error = "Category not found")
                        )
                val req = call.receive<UpdateCategoryRequest>()
                val updated = existing.copy(
                    name = req.name ?: existing.name,
                    iconName = req.iconName ?: existing.iconName,
                    colorHex = req.colorHex ?: existing.colorHex,
                    nature = req.nature?.let { n ->
                        CategoryNature.entries.find { it.name.equals(n, ignoreCase = true) }
                            ?: existing.nature
                    } ?: existing.nature,
                    parentId = req.parentId ?: existing.parentId
                )
                categoryRepository.upsertCategory(updated)
                call.respond(ApiResponse(success = true, data = updated.toDto()))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(
                        success = false,
                        error = "Internal error"
                    )
                )
            }
        }

        delete("/{id}") {
            if (!authenticate(authSessionManager)) return@delete
            try {
                val id = call.parameters["id"]?.toLongOrNull()
                    ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, error = "Invalid id")
                    )
                val category =
                    categoryRepository.getAllCategoriesSync().find { it.id == id }
                        ?: return@delete call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse<Unit>(success = false, error = "Category not found")
                        )
                categoryRepository.deleteCategory(category)
                call.respond(ApiResponse(success = true, data = mapOf("deleted" to id)))
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(
                        success = false,
                        error = "Internal error"
                    )
                )
            }
        }

        put("/reorder") {
            if (!authenticate(authSessionManager)) return@put
            try {
                val req = call.receive<ReorderCategoriesRequest>()
                val allCategories = categoryRepository.getAllCategoriesSync()
                val categoryMap = allCategories.associateBy { it.id }
                req.categoryIds.forEach { catId ->
                    val cat = categoryMap[catId] ?: return@forEach
                    categoryRepository.upsertCategory(cat)
                }
                call.respond(
                    ApiResponse(
                        success = true,
                        data = mapOf("reordered" to req.categoryIds.size)
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(
                        success = false,
                        error = "Internal error"
                    )
                )
            }
        }
    }
}

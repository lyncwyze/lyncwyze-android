package com.intelly.lyncwyze.Assest.modals

data class PaginatedResponse<T>(
    var data: MutableList<T>,
    val totalCount: Int,
    val pageSize: Int,
    val currentPage: Int,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

val emptyPagination = PaginatedResponse<List<Child>>(
    data = mutableListOf(),
    totalCount = 0,
    pageSize = 0,
    currentPage = 1,
    totalPages = 0,
    hasNext = false,
    hasPrevious = false
)

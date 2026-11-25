package be.sgl.backend.news.domain

import be.sgl.backend.shared.exception.NotFoundException

class NewsItemNotFoundException : NotFoundException("News item not found")

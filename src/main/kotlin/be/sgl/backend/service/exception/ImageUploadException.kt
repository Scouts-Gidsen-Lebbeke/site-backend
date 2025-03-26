package be.sgl.backend.service.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
abstract class ImageException(message: String) : Exception(message)

class ImageUploadException(fileName: String, directory: String) : ImageException("Image upload of $fileName to $directory failed.")

class ImageDeleteException(fileName: String, directory: String) : Throwable("Image delete of $fileName from $directory failed.")

class ImageMoveException(fileName: String, source: String, target: String) : ImageException("Image move of $fileName from $source to $target failed.")

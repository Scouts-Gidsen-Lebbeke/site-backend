package be.sgl.backend.service.exception

abstract class ImageException(message: String) : Throwable(message)

class ImageUploadException(fileName: String, directory: String) : ImageException("Image upload of $fileName to $directory failed.")

class ImageDeleteException(fileName: String, directory: String) : Throwable("Image delete of $fileName from $directory failed.")
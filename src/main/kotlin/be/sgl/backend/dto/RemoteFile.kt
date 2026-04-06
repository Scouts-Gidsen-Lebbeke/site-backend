package be.sgl.backend.dto

import java.io.File
import java.nio.file.Path

// read-only
data class RemoteFile(val fileName: String, val directory: String) {
    constructor(file: File, path: Path) : this(file.name, path.toString())
    constructor(path: Path): this(path.fileName.toFile(), path.parent)
}

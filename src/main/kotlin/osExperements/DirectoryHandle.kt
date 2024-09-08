package osExperements

data class DirectoryEntry(
    val directoryIndex : Int,           // Location within the directory
    val inodeNumber: Int,
    val isDirectory : Boolean,
    val name: String)

class DirectoryHandle(val fileSystem:FileSystem, initialDirectory : Int) {
    val currentDirectory = initialDirectory

    private fun makeDirectoryEntry(byteArray: ByteArray, directoryIndex:Int) : DirectoryEntry {
        return DirectoryEntry(
            inodeNumber = byteArray.readInt(0),
            directoryIndex = directoryIndex,
            isDirectory = (byteArray.readInt(1) and 1) == 1,
            name = byteArray.readString(16)
        )
    }

    /** read a directory entry from the disk */
    fun readDirectory() : List<DirectoryEntry> {
        val iNode = fileSystem.openInode(currentDirectory)
        val byteBuffer = ByteArray(64)
        val ret = mutableListOf<DirectoryEntry>()
        for (i in 0..<iNode.fileSize step 64) {
            val length = iNode.read(i, 64, byteBuffer, 0)
            if (length!=64)
                error("read $length in dirEntry")
            ret += makeDirectoryEntry(byteBuffer,i)
        }
        iNode.close()
        return ret
    }

    /** Scan a directory for a given file name.
     * Returns the offset to the file entry in the directory, or -1 if not found */
    private fun lookupFilename(fileName: String): DirectoryEntry? {
        val iNode = fileSystem.openInode(currentDirectory)
        val byteBuffer = ByteArray(64)
        for (i in 0..<iNode.fileSize step 64) {
            val length = iNode.read(i, 64, byteBuffer, 0)
            if (length != 64)
                error("read $length in dirEntry")
            if (byteBuffer.compareString(16, fileName)) {
                val ret = makeDirectoryEntry(byteBuffer, i)
                iNode.close()
                return ret
            }
        }
        iNode.close()
        return null
    }

    /** Create a new file in the directory. return its inode number, or zero for failure */
    fun createFile(fileName: String): FileHandle {
        val byteBuffer = ByteArray(512)

        val fileInodeNumber = fileSystem.newInodeNumber()


        // TODO - code to check if the file already exists

        // Add the file to the current directory
        val iNode = fileSystem.openInode(currentDirectory)
        byteBuffer.writeInt(0, fileInodeNumber)
        byteBuffer.writeString(16, fileName)
        iNode.write(iNode.fileSize, 64, byteBuffer, 0)
        iNode.close()

        val fileInode = fileSystem.openInode(fileInodeNumber)
        return FileHandle(fileInode, true, 0)
    }

    fun openFile(fileName: String) : FileHandle? {
        val dirEntry = lookupFilename(fileName) ?: return null
        val fileInode = fileSystem.openInode(dirEntry.inodeNumber)
        return FileHandle(fileInode, false, 0)
    }
}
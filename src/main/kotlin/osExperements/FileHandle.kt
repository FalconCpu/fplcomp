package osExperements

class FileHandle(
    private val inode: Inode,               // Keeps inode open as a lock to prevent it being deleted
    private val writeEnabled: Boolean,
    private var currentPosition: Int
) {
    /** Read from the file into a buffer */
    fun read(destBuffer: ByteArray, destBufferOffset: Int, numBytes: Int): Int {
        val bytesRead = inode.read(currentPosition, numBytes, destBuffer, destBufferOffset)
        currentPosition += bytesRead
        return bytesRead
    }

    /** Write to the file from a buffer */
    fun write(srcBuffer: ByteArray, srcBufferOffset: Int, numBytes: Int): Int {
        if (!writeEnabled)
            throw Exception("Attempt to write to a file that was opened for reading only")

        val bytesWritten = inode.write(currentPosition, numBytes, srcBuffer, srcBufferOffset)
        currentPosition += bytesWritten
        return bytesWritten
    }

    fun close() {
        inode.close()
    }
}
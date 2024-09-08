package osExperements

class Inode (
    val fileSystem: FileSystem,
    val sectorNumber: Int,
    val diskBuffer: DiskBuffer,
    var fileSize : Int,             // Offset = 4
    var creationDate : Int,         // Offset = 8
    var modificationDate : Int,     // Offset = 12
    var sectorPointers : Array<Int>,// Offset = 16
    var indirectPointer : Int,      // Offset = 48
    var doubleIndirectPointer : Int,// Offset = 52
    var tripleIndirectPointer : Int,// Offset = 56
    var numSectors : Int            // Offset = 60
) {
    companion object {
        private const val NUM_SECTORS = 64    // TODO - this should not be hardcoded
        private const val SECTOR_SIZE = 512
        private const val INT_PER_SECTOR = SECTOR_SIZE / 4
        private const val SECTORS_DIRECT = 8  // Number of sectors that can be accessed through direct pointers
        private const val SECTORS_INDIRECT = SECTORS_DIRECT + INT_PER_SECTOR
        private const val SECTORS_INDIRECT2 = SECTORS_INDIRECT + INT_PER_SECTOR * INT_PER_SECTOR
        private const val BITS_PER_SECTOR = SECTOR_SIZE * 8
        private const val FIRST_SECTOR_OF_BITMAP = 1
        private const val BITS_PER_INT = 32
        private val BITMAP_SECTORS = NUM_SECTORS.divideRoundUp(BITS_PER_SECTOR)
        private const val DATA_OFFSET_IN_INODE = 128
        private const val SIZE_DATA_IN_INODE = SECTOR_SIZE - DATA_OFFSET_IN_INODE
    }

    var dirty = false

    /** For a given offset, return the disk buffer that contains the data */
    private fun getDiskBuffer(offset: Int): DiskBuffer {
        val sectorIndex = (offset - SIZE_DATA_IN_INODE) / SECTOR_SIZE

        if (offset < SIZE_DATA_IN_INODE)
            error("Should have used short file block")
        else if (sectorIndex < SECTORS_DIRECT) {
            // In the range of the sector pointers
            return fileSystem.openSector(sectorPointers[sectorIndex])

        } else if (sectorIndex < SECTORS_INDIRECT) {
            // In the range of the indirect pointers
            val indexS1 = sectorIndex - SECTORS_DIRECT
            val indexBlock = fileSystem.openSector(indirectPointer)
            val index1 = indexBlock.readInt(indexS1)
            indexBlock.release()
            return fileSystem.openSector(index1)

        } else if (sectorIndex < SECTORS_INDIRECT2) {
            // In the range of the double indirect pointers
            val indexS1 = (sectorIndex - SECTORS_INDIRECT) / INT_PER_SECTOR
            val indexBlock1 = fileSystem.openSector(doubleIndirectPointer)
            val index1 = indexBlock1.readInt(indexS1)
            indexBlock1.release()

            val indexS2 = (sectorIndex - SECTORS_INDIRECT) % INT_PER_SECTOR
            val indexBlock2 = fileSystem.openSector(index1)
            val index2 = indexBlock2.readInt(indexS2)
            indexBlock2.release()

            return fileSystem.openSector(index2)
        } else {
            // In the range of triple indirect block
            val indexS1 = (sectorIndex - SECTORS_INDIRECT2) / (INT_PER_SECTOR * INT_PER_SECTOR)
            val indexBlock1 = fileSystem.openSector(tripleIndirectPointer)
            val index1 = indexBlock1.readInt(indexS1)
            indexBlock1.release()

            val indexS2 = ((sectorIndex - SECTORS_INDIRECT2) / INT_PER_SECTOR) % INT_PER_SECTOR
            val indexBlock2 = fileSystem.openSector(index1)
            val index2 = indexBlock2.readInt(indexS2)
            indexBlock2.release()

            val indexS3 = (sectorIndex - SECTORS_INDIRECT2) % INT_PER_SECTOR
            val indexBlock3 = fileSystem.openSector(index2)
            val index3 = indexBlock2.readInt(indexS3)
            indexBlock3.release()

            return fileSystem.openSector(index3)
        }
    }

    /** Write a chunk of data into a single block in the inode. Return the number of bytes written. */
    private fun readChunk(offset: Int, numBytes: Int, destBuffer: ByteArray, destBufferOffset: Int): Int {
        if (offset < SIZE_DATA_IN_INODE) {
            // In the range of the data stored in the inode block
            val numBytes = minOf(numBytes, SIZE_DATA_IN_INODE - offset)
            diskBuffer.readBytes(offset + DATA_OFFSET_IN_INODE, numBytes, destBuffer, destBufferOffset)
            return numBytes
        }

        val sectorOffset = (offset - SIZE_DATA_IN_INODE) % SECTOR_SIZE
        val numBytes = minOf(numBytes, SECTOR_SIZE - sectorOffset)
        val diskBuffer = getDiskBuffer(offset)
        diskBuffer.readBytes(sectorOffset, numBytes, destBuffer, destBufferOffset)
        diskBuffer.release()
        return numBytes
    }

    /** Read 'numbytes' of data, starting at 'offset' from this inode.
     * Write the result into destBuffer - startting at offset destBufferOffset.
     * Return the number of bytes read. */
    fun read(offset: Int, numBytes: Int, destBuffer: ByteArray, destBufferOffset: Int): Int {
        var vOffset = offset
        var vNumBytes = minOf(numBytes, fileSize - offset)
        var vDestBufferOffset = destBufferOffset
        var ret = 0
        while (vNumBytes > 0) {
            val numBytesRead = readChunk(vOffset, vNumBytes, destBuffer, vDestBufferOffset)
            vOffset += numBytesRead
            vNumBytes -= numBytesRead
            vDestBufferOffset += numBytesRead
            ret += numBytesRead
        }
        return ret
    }

    /** Write a chunk of data into a single block in the inode. Return the number of bytes written. */
    private fun writeChunk(offset: Int, numBytes: Int, srcBuffer: ByteArray, srcBufferOffset: Int): Int {
        // Writes `size` bytes into inode `inodeNumber` starting at `offset` from  `srcBuffer`
        // Assumes that the blocks have already been allocated
        if (offset < SIZE_DATA_IN_INODE) {
            val numBytes = minOf(numBytes, SIZE_DATA_IN_INODE - offset)
            diskBuffer.writeBytes(offset + DATA_OFFSET_IN_INODE, numBytes, srcBuffer, srcBufferOffset)
            return numBytes
        }

        val sectorOffset = (offset - SIZE_DATA_IN_INODE) % SECTOR_SIZE
        val numBytes = minOf(numBytes, SECTOR_SIZE - sectorOffset)
        val diskBuffer = getDiskBuffer(offset)
        diskBuffer.writeBytes(sectorOffset, numBytes, srcBuffer, srcBufferOffset)
        diskBuffer.release()
        return numBytes
    }

    /** Write 'numbytes' of data, starting at 'offset' into this inode.
     * Get data from srcBuffer - starting at offset srcBufferOffset.
     * Return the number of bytes written.
     */
    fun write(offset: Int, numBytes: Int, srcBuffer: ByteArray, srcBufferOffset: Int): Int {
        val newFileSize = maxOf(fileSize, offset + numBytes)
        val neededSectors = (newFileSize - SIZE_DATA_IN_INODE).divideRoundUp(SECTOR_SIZE)
        while (numSectors < neededSectors)
            addSector()

        if (fileSize!= newFileSize) {
            fileSize = newFileSize
            dirty = true
        }

        var vOffset = offset
        var vNumBytes = numBytes
        var vSrcBufferOffset = srcBufferOffset
        var ret = 0
        while (vNumBytes > 0) {
            val numBytesWritten = writeChunk(vOffset, vNumBytes, srcBuffer, vSrcBufferOffset)
            vOffset += numBytesWritten
            vNumBytes -= numBytesWritten
            vSrcBufferOffset += numBytesWritten
            ret += numBytesWritten
        }
        return ret
    }

    /** Grow this inode by adding a new sector. */
    private fun addSector() {
        val newSector = fileSystem.allocateSector()

        if (numSectors < SECTORS_DIRECT) {
            // New block goes into a sector pointer
            sectorPointers[numSectors] = newSector

        } else if (numSectors < SECTORS_INDIRECT) {
            val indirectIndex = numSectors - SECTORS_DIRECT
            if (indirectIndex == 0)
                indirectPointer = fileSystem.allocateSector()

            val indirectBlock = fileSystem.openSector(indirectPointer)
            indirectBlock.writeInt(indirectIndex, newSector)
            indirectBlock.release()

        } else if (numSectors < SECTORS_INDIRECT2) {
            val indirectIndex1 = (numSectors - SECTORS_INDIRECT) / INT_PER_SECTOR
            val indirectIndex2 = (numSectors - SECTORS_INDIRECT) % INT_PER_SECTOR
            if (indirectIndex1 == 0 && indirectIndex2 == 0)
                doubleIndirectPointer = fileSystem.allocateSector()
            if (indirectIndex2 == 0) {
                val diskBuffer = fileSystem.openSector(doubleIndirectPointer)
                diskBuffer.writeInt(indirectIndex1, fileSystem.allocateSector())
                diskBuffer.release()
            }

            val doubleIndirectBlock = fileSystem.openSector(doubleIndirectPointer)
            val index1 = doubleIndirectBlock.readInt(indirectIndex1)
            doubleIndirectBlock.release()

            val indirectBlock = fileSystem.openSector(index1)
            indirectBlock.writeInt(indirectIndex2, newSector)
            indirectBlock.release()
        } else {
            TODO("Triple indirect pointers not implemented")
        }

        numSectors++
        dirty = true
    }

    fun close() {
        println("Closing inode $sectorNumber $dirty")
        if (dirty) {
            diskBuffer.writeInt(1, fileSize)
            diskBuffer.writeInt(2, creationDate)
            diskBuffer.writeInt(3, modificationDate)
            diskBuffer.writeInt(4, sectorPointers[0])
            diskBuffer.writeInt(5, sectorPointers[1])
            diskBuffer.writeInt(6, sectorPointers[2])
            diskBuffer.writeInt(7, sectorPointers[3])
            diskBuffer.writeInt(8, sectorPointers[4])
            diskBuffer.writeInt(9, sectorPointers[5])
            diskBuffer.writeInt(10, sectorPointers[6])
            diskBuffer.writeInt(11, sectorPointers[7])
            diskBuffer.writeInt(12, indirectPointer)
            diskBuffer.writeInt(13, doubleIndirectPointer)
            diskBuffer.writeInt(14, tripleIndirectPointer)
            diskBuffer.writeInt(15, numSectors)
        }
        diskBuffer.release()
    }
}


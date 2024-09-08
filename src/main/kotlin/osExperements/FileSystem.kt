package osExperements

fun Int.divideRoundUp(denominator:Int) = (this+denominator-1)/denominator

fun ByteArray.readInt(offset: Int): Int {
    val b0 = this[offset].toInt() and 0xff
    val b1 = this[offset + 1].toInt() and 0xff
    val b2 = this[offset + 2].toInt() and 0xff
    val b3 = this[offset + 3].toInt() and 0xff
    return (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
}

fun ByteArray.writeInt(offset: Int, value: Int) {
    this[offset] = (value and 0xff).toByte()
    this[offset + 1] = ((value shr 8) and 0xff).toByte()
    this[offset + 2] = ((value shr 16) and 0xff).toByte()
    this[offset + 3] = ((value shr 24) and 0xff).toByte()
}

/** Extract a null terminated string from a byte array */
fun ByteArray.readString(offset:Int) : String {
    var i = offset
    while (i < this.size && this[i] != 0.toByte())
        i++
    return sliceArray(offset ..< i).toString(Charsets.UTF_8)
}

/** Write a null terminated string into a byte array */
fun ByteArray.writeString(offset: Int, value: String) {
    val bytes = value.toByteArray(Charsets.UTF_8)
    bytes.copyInto(this, offset, 0, bytes.size)
    this[offset + bytes.size] = 0.toByte()
}

/** Compare a string to a byte array */
fun ByteArray.compareString(offset: Int, string: String): Boolean {
    check(string.length <= this.size - offset)
    var i = 0
    while(i< string.length) {
        if (this[offset + i] != string[i].code.toByte())
            return false
        i++
    }
    return this[offset + i] == 0.toByte()
}

/** Acts as an interface to a disk device, maintaining a cache of recently accessed sectors */
class FileSystem(val diskDevice: DiskDevice) {
    val numberOfSectors = diskDevice.numSectors
    val sectorSize = 512
    val intPerSector = sectorSize / 4
    val bitsPerSector = sectorSize * 8
    val bitmapSize = numberOfSectors.divideRoundUp(bitsPerSector)

    private var bufferList = ArrayDeque<DiskBuffer>()

    fun addBuffers(numBuffers:Int) {
        for(i in 0..<numBuffers) {
            val new = DiskBuffer(diskDevice)
            bufferList += new
        }
    }

    fun openSector(sectorNumber: Int) : DiskBuffer {
        // Walk the list to see if the requested sector is already in a buffer
        var lastAvailable : DiskBuffer? = null
        for(buffer in bufferList) {
            if (buffer.sectorNumber == sectorNumber) {
                if (buffer.inUse)
                    throw Error("Sector $sectorNumber already in use")
                bufferList.remove(buffer)
                bufferList.addFirst(buffer)
                buffer.inUse = true
                return buffer
            } else if (!buffer.inUse)
                lastAvailable = buffer
        }

        // The requested sector is not in a buffer - so take the last available buffer
        // and load the sector into it
        if (lastAvailable== null)
            throw Error("No disk buffers available")

        lastAvailable.loadSector(sectorNumber)
        lastAvailable.inUse = true
        bufferList.remove(lastAvailable)
        bufferList.addFirst(lastAvailable)
        return lastAvailable
    }

    /** Search a bitmap block for a free sector. If found mark it allocated and return its number */
    fun allocateSector() : Int {
        // TODO - bitmaps more than one block
        val bitmapBlock = openSector(1)
        val numEntries = numberOfSectors / 32
        for(index in 0..<numEntries) {
            val word = bitmapBlock.readInt(index)
            if (word != -1) {
                val lmo = word.inv().countTrailingZeroBits() // Gets the bit index of the first '0' bit in the word
                bitmapBlock.writeInt(index, word or (1 shl lmo))
                bitmapBlock.release()
                val ret = index*32 + lmo
                println("Allocating node $ret")
                return ret
            }
        }
        bitmapBlock.release()
        throw Error("Disk Full")
    }

    fun flush() {
        for (buffer in bufferList)
            buffer.flush()
    }

    /** Allocate a block for a new inode */
    fun newInodeNumber() : Int {
        val inodeNumber = allocateSector()

        println("New Inode $inodeNumber")
        val zeroBlock = ByteArray(sectorSize) { 0 }
        val diskBuffer = openSector(inodeNumber)
        diskBuffer.writeBytes(0, sectorSize, zeroBlock, 0)
        diskBuffer.release()
        return inodeNumber
    }

    /** Create a new inode instance from a disk buffer */
    fun openInode(inodeNumber: Int): Inode {
        // Read the inode from disk
        val diskBuffer = openSector(inodeNumber)

        // extract the inode fields from the disk buffer
        val ret = Inode(
            fileSystem = this,
            sectorNumber = inodeNumber,
            diskBuffer = diskBuffer,
            fileSize = diskBuffer.readInt(1),
            creationDate = diskBuffer.readInt(2),
            modificationDate = diskBuffer.readInt(3),
            sectorPointers = arrayOf(
                diskBuffer.readInt(4),
                diskBuffer.readInt(5),
                diskBuffer.readInt(6),
                diskBuffer.readInt(7),
                diskBuffer.readInt(8),
                diskBuffer.readInt(9),
                diskBuffer.readInt(10),
                diskBuffer.readInt(11)
            ),
            indirectPointer = diskBuffer.readInt(12),
            doubleIndirectPointer = diskBuffer.readInt(13),
            tripleIndirectPointer = diskBuffer.readInt(14),
            numSectors = diskBuffer.readInt(15)
        )
        return ret
    }

    /** format the disk */
    fun format() {
        val dataBlock = ByteArray(512){0x6B}   // Fill the disk with an arbitary pattern
        for(i in 0..<numberOfSectors) {
            val diskBuffer = openSector(i)
            diskBuffer.writeBytes(0, 512, dataBlock, 0)
            diskBuffer.release()
        }

        // set the root node and bitmap to clear
        val zeroBlock = ByteArray(sectorSize){0}
        for (i in 0..7) {
            val diskBuffer = openSector(i)
            diskBuffer.writeBytes(0, 512, zeroBlock, 0)
            diskBuffer.release()
        }

        // Mark the first few blocks as Allocated
        // TODO - should really calculate how many bits to set here
        zeroBlock[0] = -1
        val diskBuffer = openSector(1)
        diskBuffer.writeBytes(0, 512, zeroBlock, 0)
        diskBuffer.release()

        // Create the root directory
        val rootInodeNumber = 7
        val dirEntry = ByteArray(64)
        dirEntry.writeInt(0, rootInodeNumber)
        dirEntry.writeInt(4, 1)         // mark as directory
        dirEntry.writeString(16,".")

        val iNode = openInode(rootInodeNumber)
        iNode.write(0, 64, dirEntry, 0)
        iNode.close()
    }

    fun getRootDirectory() : DirectoryHandle
        = DirectoryHandle(this,7)
}


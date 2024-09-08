package osExperements

/** Hold an in memory copy of a disk sector */
class DiskBuffer (val diskDevice: DiskDevice) {
    var sectorNumber = -1
    var dirty = false
    var inUse = false
    val data = ByteArray(512)

    /** Read a value from a DiskBuffer as if it were an array of Ints */
    fun readInt(index:Int) =
        (data[4*index].toInt() and 0xff) or
                ((data[4*index+1].toInt() and 0xff) shl 8) or
                ((data[4*index+2].toInt() and 0xff) shl 16) or
                ((data[4*index+3].toInt() and 0xff) shl 24)


    /** Write a value into a DiskBuffer as if it were an array of Ints */
    fun writeInt(index: Int, value: Int) {
        data[4 * index] = value.toByte()
        data[4 * index + 1] = (value shr 8).toByte()
        data[4 * index + 2] = (value shr 16).toByte()
        data[4 * index + 3] = (value shr 24).toByte()
        dirty = true
    }

    /** Copy a block of data from a DiskBuffer into a byte array */
    fun readBytes(offset:Int, numBytes:Int, dest:ByteArray, destOffset:Int) {
        data.copyInto(dest, destOffset, offset, offset+numBytes)
    }

    /** Copy a block of data from a byte array into a DiskBuffer */
    fun writeBytes(offset:Int, numBytes:Int, src:ByteArray, srcOffset:Int) {
        src.copyInto(data, offset, srcOffset, srcOffset+numBytes)
        dirty = true
    }

    /** Flush a DiskBuffer to disk */
    fun flush() {
        if (!dirty)
            return
        diskDevice.writeSector(sectorNumber, data)
        dirty = false
    }

    /** Load a DiskBuffer from disk */
    fun loadSector(sectorNumber: Int) {
        flush()
        diskDevice.readSector(sectorNumber, data)
        this.sectorNumber = sectorNumber
        dirty = false
        inUse = true
    }

    /** Release a DiskBuffer */
    fun release() {
        if (inUse== false)
            throw Error("Attempt to release a buffer that is not in use")
        inUse = false
    }
}

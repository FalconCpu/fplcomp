package osExperements

import java.io.FileInputStream
import java.io.FileOutputStream

class DiskDevice (val name:String) {
    val numSectors = 64
    val sectorSize = 512                    // In bytes
    val image = ByteArray(numSectors * sectorSize)

    fun initialize(filename: String) {
        val fh = FileInputStream(filename)
        fh.read(image)
        fh.close()
    }

    fun updateDiskImage(filename: String) {
        // This method is for emulation only = on the FPGA writeSector will go to the disk device directly
        // Just for emulation we keep the whole disk image in memory - and only write it back at the end.
        val fh = FileOutputStream(filename)
        fh.write(image)
        fh.close()
    }

    fun readSector(sector: Int, destBuffer: ByteArray) {
        println("Reading Sector $sector")
        check(sector >= 0 && sector < numSectors)
        val base = sector * sectorSize
        for (i in 0..<sectorSize)
            destBuffer[i] = image[base + i]
    }

    fun writeSector(sector: Int, srcBuffer: ByteArray) {
        println("Writing Sector $sector")
        check(sector >= 0 && sector < numSectors)
        check(srcBuffer.size == sectorSize)
        val base = sector * sectorSize
        for (i in 0..<sectorSize)
            image[base + i] = srcBuffer[i];

    }

    fun dumpSector(sector: Int) {
        println("Sector $sector")
        val base = sector * sectorSize
        for (i in 0..<sectorSize step 32) {
            for (j in 0..<32) {
                val c = image[base + i + j]
                print("%02X".format(c))
                if (j % 4 == 3)
                    print(" ")
            }
            print("   ")
            for (j in 0..<32) {
                val c = (image[base + i + j])
                print(if (c < 32 || c > 127) "." else c.toInt().toChar())
            }
            println("")
        }
    }

    fun dumpAllSectors() {
        for (i in 0..<numSectors)
            dumpSector(i)
    }

}

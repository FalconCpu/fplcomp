package osExperements

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.FileReader


class OsExperementsTest {

    @Test
    fun createDiskImage() {
        val dd = DiskDevice("dh0")
        dd.initialize("diskImage.bin")

        val fs = FileSystem(dd)
        fs.addBuffers(16)
        fs.format()

        val rootDirectory = fs.getRootDirectory()

        val data1 = "Mary had a little lamb"
        val fh1 = rootDirectory.createFile("mary.txt")
        fh1.write(data1.toByteArray(), 0,  data1.length)
        fh1.close()

        val data2 = FileReader("fpl.md").readText()
        val fh2 = rootDirectory.createFile("fpl.md")
        fh2.write(data2.toByteArray(), 0, data2.length)
        fh2.close()

        val ls = rootDirectory.readDirectory()
        for(item in ls)
            println("$item.name ${item.isDirectory} {${item.inodeNumber}}")

        val dataBuffer = ByteArray(20000)
        val fh3 = rootDirectory.openFile("mary.txt")
        if (fh3==null) throw Exception("Failed to open file")
        val length3 = fh3.read(dataBuffer, 0, dataBuffer.size)
        fh3.close()
        val readData1s = dataBuffer.sliceArray(0..<length3).toString(Charsets.UTF_8)


        val fh4 = rootDirectory.openFile("fpl.md")
        if (fh4 == null) throw Exception("Failed to open file")
        val length4 = fh4.read(dataBuffer, 0, dataBuffer.size)
        fh4.close()
        val readData2s = dataBuffer.sliceArray(0..<length4).toString(Charsets.UTF_8)

        assertEquals(data1, readData1s)
        assertEquals(data2, readData2s)

        fs.flush()
        dd.dumpAllSectors()
        dd.updateDiskImage("diskImage.bin")

//        assertEquals(1,2)
    }

}
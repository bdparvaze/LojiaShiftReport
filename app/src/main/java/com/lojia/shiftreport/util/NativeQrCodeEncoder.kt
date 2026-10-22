package com.lojia.shiftreport.util

import java.nio.charset.StandardCharsets
import kotlin.math.abs

/**
 * 100% Native Pure-Kotlin ISO/IEC 18004 Standard QR Code Matrix Generator.
 * Completely independent with zero third-party external dependencies.
 */
class QrMatrix(val width: Int, val height: Int, private val bits: BooleanArray) {
    fun get(x: Int, y: Int): Boolean {
        if (x in 0 until width && y in 0 until height) {
            return bits[y * width + x]
        }
        return false
    }
}

object NativeQrCodeEncoder {

    enum class EcLevel(val bits: Int, val formatBits: Int) {
        L(0b01, 1),
        M(0b00, 0),
        Q(0b11, 3),
        H(0b10, 2)
    }

    // Capacity table for Byte mode (Version 1 to 10, EC Level L, M, Q, H)
    // Total data capacity in bytes for each version and EC level
    private val DATA_CODEWORDS = mapOf(
        // Version -> (L, M, Q, H)
        1 to intArrayOf(19, 16, 13, 9),
        2 to intArrayOf(34, 28, 22, 16),
        3 to intArrayOf(55, 44, 34, 26),
        4 to intArrayOf(80, 64, 48, 36),
        5 to intArrayOf(108, 86, 62, 46),
        6 to intArrayOf(136, 108, 76, 60),
        7 to intArrayOf(156, 124, 88, 66),
        8 to intArrayOf(194, 154, 110, 86),
        9 to intArrayOf(232, 182, 132, 100),
        10 to intArrayOf(274, 216, 154, 122),
        11 to intArrayOf(324, 254, 180, 140),
        12 to intArrayOf(370, 290, 206, 158),
        13 to intArrayOf(428, 334, 244, 180),
        14 to intArrayOf(461, 365, 261, 197)
    )

    // EC block info: (numBlocksGroup1, dataCodewordsGroup1, numBlocksGroup2, dataCodewordsGroup2, ecCodewordsPerBlock)
    private class EcScheme(
        val totalEcCodewords: Int,
        val ecCodewordsPerBlock: Int,
        val g1Blocks: Int,
        val g1DataCodewords: Int,
        val g2Blocks: Int = 0,
        val g2DataCodewords: Int = 0
    )

    private val EC_SCHEMES = mapOf(
        // Key: version * 10 + ecLevelOrdinal (0: L, 1: M, 2: Q, 3: H)
        10 to EcScheme(7, 7, 1, 19),
        11 to EcScheme(10, 10, 1, 16),
        12 to EcScheme(13, 13, 1, 13),
        13 to EcScheme(17, 17, 1, 9),

        20 to EcScheme(10, 10, 1, 34),
        21 to EcScheme(16, 16, 1, 28),
        22 to EcScheme(22, 22, 1, 22),
        23 to EcScheme(28, 28, 1, 16),

        30 to EcScheme(15, 15, 1, 55),
        31 to EcScheme(26, 26, 1, 44),
        32 to EcScheme(36, 18, 2, 17),
        33 to EcScheme(44, 22, 2, 13),

        40 to EcScheme(20, 20, 1, 80),
        41 to EcScheme(36, 18, 2, 32),
        42 to EcScheme(52, 26, 2, 24),
        43 to EcScheme(64, 16, 4, 9),

        50 to EcScheme(26, 26, 1, 108),
        51 to EcScheme(48, 24, 2, 43),
        52 to EcScheme(72, 18, 2, 15, 2, 16),
        53 to EcScheme(88, 22, 2, 11, 2, 12),

        60 to EcScheme(36, 18, 2, 68),
        61 to EcScheme(64, 16, 4, 27),
        62 to EcScheme(96, 24, 4, 19),
        63 to EcScheme(112, 28, 4, 15),

        70 to EcScheme(40, 20, 2, 78),
        71 to EcScheme(72, 18, 4, 31),
        72 to EcScheme(108, 18, 2, 14, 4, 15),
        73 to EcScheme(130, 26, 4, 13, 1, 14),

        80 to EcScheme(48, 24, 2, 97),
        81 to EcScheme(88, 22, 2, 38, 2, 39),
        82 to EcScheme(132, 22, 4, 18, 2, 19),
        83 to EcScheme(156, 26, 4, 14, 2, 15),

        90 to EcScheme(60, 30, 2, 116),
        91 to EcScheme(110, 22, 3, 36, 2, 37),
        92 to EcScheme(160, 20, 4, 16, 4, 17),
        93 to EcScheme(192, 24, 4, 12, 4, 13),

        100 to EcScheme(72, 18, 2, 68, 2, 69),
        101 to EcScheme(130, 26, 4, 43, 1, 44),
        102 to EcScheme(192, 24, 6, 19, 2, 20),
        103 to EcScheme(224, 28, 6, 15, 2, 16),

        110 to EcScheme(80, 20, 4, 81),
        111 to EcScheme(150, 30, 1, 50, 4, 51),
        112 to EcScheme(224, 28, 4, 22, 4, 23),
        113 to EcScheme(264, 24, 3, 12, 8, 13),

        120 to EcScheme(96, 24, 2, 92, 2, 93),
        121 to EcScheme(176, 22, 6, 36, 2, 37),
        122 to EcScheme(260, 26, 4, 20, 6, 21),
        123 to EcScheme(308, 28, 7, 14, 4, 15),

        130 to EcScheme(104, 26, 4, 107),
        131 to EcScheme(198, 22, 8, 37, 1, 38),
        132 to EcScheme(288, 24, 8, 20, 4, 21),
        133 to EcScheme(352, 22, 12, 11, 4, 12),

        140 to EcScheme(120, 30, 3, 115, 1, 116),
        141 to EcScheme(216, 24, 4, 40, 5, 41),
        142 to EcScheme(320, 20, 11, 16, 5, 17),
        143 to EcScheme(384, 24, 11, 12, 5, 13)
    )

    private val ALIGNMENT_PATTERN_POSITIONS = mapOf(
        1 to intArrayOf(),
        2 to intArrayOf(6, 18),
        3 to intArrayOf(6, 22),
        4 to intArrayOf(6, 26),
        5 to intArrayOf(6, 30),
        6 to intArrayOf(6, 34),
        7 to intArrayOf(6, 22, 38),
        8 to intArrayOf(6, 24, 42),
        9 to intArrayOf(6, 26, 46),
        10 to intArrayOf(6, 28, 50),
        11 to intArrayOf(6, 30, 54),
        12 to intArrayOf(6, 32, 58),
        13 to intArrayOf(6, 34, 62),
        14 to intArrayOf(6, 26, 46, 66)
    )

    // Galois Field GF(256) tables
    private val EXP_TABLE = IntArray(512)
    private val LOG_TABLE = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            EXP_TABLE[i] = x
            LOG_TABLE[x] = i
            x = x shl 1
            if (x >= 256) {
                x = x xor 0x11D // Primitive polynomial x^8 + x^4 + x^3 + x^2 + 1
            }
        }
        for (i in 255 until 512) {
            EXP_TABLE[i] = EXP_TABLE[i - 255]
        }
    }

    private fun gfMultiply(x: Int, y: Int): Int {
        if (x == 0 || y == 0) return 0
        return EXP_TABLE[LOG_TABLE[x] + LOG_TABLE[y]]
    }

    private fun rsComputeGeneratorPolynomial(degree: Int): IntArray {
        var poly = intArrayOf(1)
        for (i in 0 until degree) {
            val factor = intArrayOf(1, EXP_TABLE[i])
            val newPoly = IntArray(poly.size + 1)
            for (j in poly.indices) {
                newPoly[j] = newPoly[j] xor gfMultiply(poly[j], factor[0])
                newPoly[j + 1] = newPoly[j + 1] xor gfMultiply(poly[j], factor[1])
            }
            poly = newPoly
        }
        return poly
    }

    private fun rsCalculateRemainder(data: IntArray, numEcCodewords: Int): IntArray {
        val gen = rsComputeGeneratorPolynomial(numEcCodewords)
        val remainder = IntArray(numEcCodewords)
        for (d in data) {
            val factor = d xor remainder[0]
            System.arraycopy(remainder, 1, remainder, 0, numEcCodewords - 1)
            remainder[numEcCodewords - 1] = 0
            if (factor != 0) {
                for (j in 0 until numEcCodewords) {
                    remainder[j] = remainder[j] xor gfMultiply(gen[j + 1], factor)
                }
            }
        }
        return remainder
    }

    fun encode(text: String, ecLevel: EcLevel = EcLevel.M): QrMatrix {
        val rawBytes = text.toByteArray(StandardCharsets.UTF_8)
        val ecIndex = ecLevel.ordinal // 0=L, 1=M, 2=Q, 3=H

        // Find smallest version that fits
        var version = -1
        for (v in 1..14) {
            val cap = DATA_CODEWORDS[v]?.get(ecIndex) ?: continue
            val headerBits = 4 + (if (v < 10) 8 else 16)
            val totalBitsNeeded = headerBits + (rawBytes.size * 8)
            if (totalBitsNeeded <= (cap * 8)) {
                version = v
                break
            }
        }
        if (version == -1) {
            // Fallback to max version 14 with truncation if necessary
            version = 14
        }

        val totalDataCodewords = DATA_CODEWORDS[version]!![ecIndex]
        val schemeKey = version * 10 + ecIndex
        val scheme = EC_SCHEMES[schemeKey] ?: EC_SCHEMES[10 + ecIndex]!!

        // Construct bit stream
        val bitBuffer = mutableListOf<Int>()
        fun addBits(value: Int, numBits: Int) {
            for (i in numBits - 1 downTo 0) {
                bitBuffer.add((value shr i) and 1)
            }
        }

        // Mode: 0100 for Byte
        addBits(0b0100, 4)
        // Character count indicator
        val charCountBits = if (version < 10) 8 else 16
        addBits(rawBytes.size.coerceAtMost((1 shl charCountBits) - 1), charCountBits)

        // Raw bytes
        for (b in rawBytes) {
            addBits(b.toInt() and 0xFF, 8)
        }

        // Terminator: up to 4 zeroes
        val totalCapacityBits = totalDataCodewords * 8
        val terminatorBits = (totalCapacityBits - bitBuffer.size).coerceIn(0, 4)
        addBits(0, terminatorBits)

        // Pad to byte boundary
        while (bitBuffer.size % 8 != 0 && bitBuffer.size < totalCapacityBits) {
            bitBuffer.add(0)
        }

        // Pad bytes 0xEC, 0x11
        val padBytes = intArrayOf(0xEC, 0x11)
        var padIndex = 0
        while (bitBuffer.size < totalCapacityBits) {
            addBits(padBytes[padIndex % 2], 8)
            padIndex++
        }

        // Convert bit buffer to data codewords
        val dataCodewords = IntArray(totalDataCodewords)
        for (i in 0 until totalDataCodewords) {
            var byteVal = 0
            for (b in 0 until 8) {
                byteVal = (byteVal shl 1) or bitBuffer[i * 8 + b]
            }
            dataCodewords[i] = byteVal
        }

        // Divide into blocks and compute EC codewords
        val totalBlocks = scheme.g1Blocks + scheme.g2Blocks
        val blocksData = Array(totalBlocks) { IntArray(0) }
        val blocksEc = Array(totalBlocks) { IntArray(0) }

        var offset = 0
        for (i in 0 until scheme.g1Blocks) {
            val len = scheme.g1DataCodewords
            val chunk = dataCodewords.copyOfRange(offset, offset + len)
            offset += len
            blocksData[i] = chunk
            blocksEc[i] = rsCalculateRemainder(chunk, scheme.ecCodewordsPerBlock)
        }
        for (i in 0 until scheme.g2Blocks) {
            val blockIdx = scheme.g1Blocks + i
            val len = scheme.g2DataCodewords
            val chunk = dataCodewords.copyOfRange(offset, offset + len)
            offset += len
            blocksData[blockIdx] = chunk
            blocksEc[blockIdx] = rsCalculateRemainder(chunk, scheme.ecCodewordsPerBlock)
        }

        // Interleave data codewords
        val interleaved = mutableListOf<Int>()
        var maxDataLen = 0
        for (b in blocksData) {
            if (b.size > maxDataLen) maxDataLen = b.size
        }
        for (col in 0 until maxDataLen) {
            for (b in blocksData) {
                if (col < b.size) interleaved.add(b[col])
            }
        }

        // Interleave EC codewords
        for (col in 0 until scheme.ecCodewordsPerBlock) {
            for (b in blocksEc) {
                if (col < b.size) interleaved.add(b[col])
            }
        }

        // Matrix dimension
        val size = 21 + (version - 1) * 4
        val matrix = Array(size) { BooleanArray(size) }
        val isFunction = Array(size) { BooleanArray(size) }

        fun setModule(r: Int, c: Int, isDark: Boolean, isFunc: Boolean = true) {
            matrix[r][c] = isDark
            if (isFunc) isFunction[r][c] = true
        }

        // 1. Finder patterns (7x7) + separators
        fun drawFinder(r: Int, c: Int) {
            for (dr in -1..7) {
                for (dc in -1..7) {
                    val row = r + dr
                    val col = c + dc
                    if (row in 0 until size && col in 0 until size) {
                        val isBorder = dr in 0..6 && (dc == 0 || dc == 6)
                        val isHorizontalBorder = dc in 0..6 && (dr == 0 || dr == 6)
                        val isCenter = dr in 2..4 && dc in 2..4
                        setModule(row, col, isBorder || isHorizontalBorder || isCenter)
                    }
                }
            }
        }

        drawFinder(0, 0)
        drawFinder(0, size - 7)
        drawFinder(size - 7, 0)

        // 2. Alignment patterns
        val alignPositions = ALIGNMENT_PATTERN_POSITIONS[version] ?: intArrayOf()
        for (r in alignPositions) {
            for (c in alignPositions) {
                // Skip if overlapping finder patterns
                if ((r == 6 && c == 6) || (r == 6 && c == size - 7) || (r == size - 7 && c == 6)) continue
                if (isFunction[r][c]) continue

                for (dr in -2..2) {
                    for (dc in -2..2) {
                        val isDark = abs(dr) == 2 || abs(dc) == 2 || (dr == 0 && dc == 0)
                        setModule(r + dr, c + dc, isDark)
                    }
                }
            }
        }

        // 3. Timing patterns
        for (i in 8 until size - 8) {
            if (!isFunction[6][i]) setModule(6, i, i % 2 == 0)
            if (!isFunction[i][6]) setModule(i, 6, i % 2 == 0)
        }

        // 4. Dark module
        setModule(4 * version + 9, 8, true)

        // 5. Reserve format information areas
        for (i in 0..8) {
            if (i <= 8) isFunction[8][i] = true
            if (i <= 7) isFunction[i][8] = true
        }
        for (i in 0..7) {
            isFunction[8][size - 1 - i] = true
            isFunction[size - 1 - i][8] = true
        }

        // 6. Place data bits in zigzag order
        val allBits = mutableListOf<Boolean>()
        for (byteVal in interleaved) {
            for (b in 7 downTo 0) {
                allBits.add(((byteVal shr b) and 1) == 1)
            }
        }

        var bitIndex = 0
        var col = size - 1
        var movingUp = true

        while (col > 0) {
            if (col == 6) col-- // Skip vertical timing column
            val rows = if (movingUp) (size - 1 downTo 0) else (0 until size)
            for (row in rows) {
                for (c in intArrayOf(col, col - 1)) {
                    if (!isFunction[row][c]) {
                        val bit = if (bitIndex < allBits.size) allBits[bitIndex++] else false
                        matrix[row][c] = bit
                    }
                }
            }
            movingUp = !movingUp
            col -= 2
        }

        // Mask patterns (using Mask 0: (row + col) % 2 == 0 as standard default)
        val selectedMask = 0
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (!isFunction[r][c]) {
                    val maskCondition = when (selectedMask) {
                        0 -> (r + c) % 2 == 0
                        1 -> r % 2 == 0
                        2 -> c % 3 == 0
                        3 -> (r + c) % 3 == 0
                        4 -> ((r / 2) + (c / 3)) % 2 == 0
                        5 -> ((r * c) % 2) + ((r * c) % 3) == 0
                        6 -> (((r * c) % 2) + ((r * c) % 3)) % 2 == 0
                        7 -> (((r + c) % 2) + ((r * c) % 3)) % 2 == 0
                        else -> false
                    }
                    if (maskCondition) {
                        matrix[r][c] = !matrix[r][c]
                    }
                }
            }
        }

        // Format information
        // Format bits: 5 data bits (2 for EC + 3 for mask) + 10 BCH bits xor 0x5412
        val formatData = (ecLevel.formatBits shl 3) or selectedMask
        var formatBch = formatData shl 10
        val generator = 0x537
        for (i in 14 downTo 10) {
            if (((formatBch shr i) and 1) == 1) {
                formatBch = formatBch xor (generator shl (i - 10))
            }
        }
        val formatFinal = ((formatData shl 10) or formatBch) xor 0x5412

        // Write format bits
        // Around top-left finder
        val bitsSeq = (0..14).map { ((formatFinal shr it) and 1) == 1 }
        // Horizontal: bits 0..7 at (8, 0) to (8, 8) skipping (8, 6)
        val hCoords = listOf(
            8 to 0, 8 to 1, 8 to 2, 8 to 3, 8 to 4, 8 to 5,
            8 to 7, 8 to 8, 7 to 8, 5 to 8, 4 to 8, 3 to 8, 2 to 8, 1 to 8, 0 to 8
        )
        for (idx in 0..14) {
            val (r, c) = hCoords[idx]
            matrix[r][c] = bitsSeq[idx]
        }

        // Second copy around top-right and bottom-left
        val vCoords = listOf(
            8 to (size - 1), 8 to (size - 2), 8 to (size - 3), 8 to (size - 4),
            8 to (size - 5), 8 to (size - 6), 8 to (size - 7), 8 to (size - 8),
            (size - 7) to 8, (size - 6) to 8, (size - 5) to 8, (size - 4) to 8,
            (size - 3) to 8, (size - 2) to 8, (size - 1) to 8
        )
        for (idx in 0..14) {
            val (r, c) = vCoords[idx]
            matrix[r][c] = bitsSeq[idx]
        }

        // Flat array for QrMatrix
        val flat = BooleanArray(size * size)
        for (r in 0 until size) {
            for (c in 0 until size) {
                flat[r * size + c] = matrix[r][c]
            }
        }

        return QrMatrix(size, size, flat)
    }
}

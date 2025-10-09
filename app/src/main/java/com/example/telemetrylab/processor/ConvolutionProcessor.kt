package com.example.telemetrylab.processor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class ConvolutionProcessor @Inject constructor() {

    suspend fun performConvolution(computeLoad: Int): Float = withContext(Dispatchers.Default) {
        val size = 256
        val matrix = FloatArray(size * size) { Random.nextFloat() }
        val kernel = floatArrayOf(1f, 0f, -1f, 2f, 0f, -2f, 1f, 0f, -1f)

        var result = 0f

        repeat(computeLoad) { load ->
            val output = FloatArray(size * size)

            for (i in 1 until size - 1) {
                for (j in 1 until size - 1) {
                    var sum = 0f
                    for (ki in 0..2) {
                        for (kj in 0..2) {
                            val row = i + ki - 1
                            val col = j + kj - 1
                            val matrixIndex = row * size + col
                            val kernelIndex = ki * 3 + kj
                            sum += matrix[matrixIndex] * kernel[kernelIndex]
                        }
                    }
                    output[i * size + j] = sum
                    result += sum
                }
            }

            // Use the output as input for next iteration to increase computation
            if (load < computeLoad - 1) {
                System.arraycopy(output, 0, matrix, 0, output.size)
            }
        }

        result
    }
}
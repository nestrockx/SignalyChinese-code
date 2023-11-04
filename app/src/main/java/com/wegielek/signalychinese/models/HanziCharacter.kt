package com.wegielek.signalychinese.models

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.PathMeasure
import java.util.Arrays

class HanziCharacter(characterPaths: List<Path>) {
    var index = 0
    val paths: List<Path>
    var isFinished = false
    private val size: Int
    private val pathMeasure = PathMeasure()
    private val pos = FloatArray(2)
    private val tan = FloatArray(2)
    private val matchStart: BooleanArray
    private val matchControlPoint: BooleanArray
    private val matchEnd: BooleanArray

    init {
        paths = ArrayList(characterPaths)
        size = characterPaths.size
        matchStart = BooleanArray(size)
        matchControlPoint = BooleanArray(size)
        matchEnd = BooleanArray(size)
        Arrays.fill(matchStart, false)
        Arrays.fill(matchControlPoint, false)
        Arrays.fill(matchEnd, false)
        initDefaultResize()
    }

    fun nextIndex(): Boolean {
        if (index < size - 1) {
            index++
            return true
        }
        isFinished = true
        return false
    }

    fun prevIndex(): Boolean {
        if (index > 0) {
            index--
            return true
        }
        return false
    }

    val currPath: Path
        get() = paths[index]

    fun matchStart(x: Float, y: Float, r: Float): Boolean {
        pathMeasure.setPath(paths[index], false)
        pathMeasure.getPosTan(0.0f, pos, tan)
        if (Math.pow((x - pos[0]).toDouble(), 2.0) + Math.pow(
                (y - pos[1]).toDouble(),
                2.0
            ) <= Math.pow(r.toDouble(), 2.0)
        ) {
            matchStart[index] = true
            return true
        }
        return false
    }

    fun matchControlPoint(x: Float, y: Float, r: Float): Boolean {
        pathMeasure.setPath(paths[index], false)
        pathMeasure.getPosTan(pathMeasure.length / 2, pos, tan)
        if (Math.pow((x - pos[0]).toDouble(), 2.0) + Math.pow(
                (y - pos[1]).toDouble(),
                2.0
            ) <= Math.pow(r.toDouble(), 2.0)
        ) {
            matchControlPoint[index] = true
            return true
        }
        return false
    }

    fun matchEnd(x: Float, y: Float, r: Float): Boolean {
        pathMeasure.setPath(paths[index], false)
        pathMeasure.getPosTan(pathMeasure.length, pos, tan)
        if (Math.pow((x - pos[0]).toDouble(), 2.0) + Math.pow(
                (y - pos[1]).toDouble(),
                2.0
            ) <= Math.pow(r.toDouble(), 2.0)
        ) {
            matchEnd[index] = true
            return true
        }
        return false
    }

    fun matchReset() {
        matchStart[index] = false
        matchControlPoint[index] = false
        matchEnd[index] = false
    }

    val isMatchedStart: Boolean
        get() = matchStart[index]
    val isMatchedControlPoint: Boolean
        get() = matchControlPoint[index]
    val isMatched: Boolean
        get() = matchStart[index] && matchEnd[index]

    fun reset() {
        Arrays.fill(matchStart, false)
        Arrays.fill(matchControlPoint, false)
        Arrays.fill(matchEnd, false)
        index = 0
        isFinished = false
    }

    private fun initDefaultResize() {
        val transformMatrix = Matrix()
        transformMatrix.setScale(3.2f, 3.2f)
        for (i in paths.indices) {
            paths[i].transform(transformMatrix)
        }
        transformMatrix.setTranslate(28f, 25f)
        for (i in paths.indices) {
            paths[i].transform(transformMatrix)
        }
    }
}


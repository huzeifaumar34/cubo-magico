package com.huzeifa.cubomagico.solver

object NativeSolver {
    init { System.loadLibrary("cubomagico") }
    @JvmStatic external fun nativeSolve(facelets: String): String
    fun solve(facelets: String): String = nativeSolve(facelets)
}

package com.lojia.shiftreport.util

fun isValidMonthlySalary(minor: Long): Boolean = minor > 0L

fun <T> List<T>.withValidSalary(salaryOf: (T) -> Long): List<T> = filter { isValidMonthlySalary(salaryOf(it)) }

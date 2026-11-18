package com.lojia.shiftreport.util

fun shouldWriteDraft(isEmpty: Boolean, snapshotChanged: Boolean): Boolean =
    !isEmpty && snapshotChanged

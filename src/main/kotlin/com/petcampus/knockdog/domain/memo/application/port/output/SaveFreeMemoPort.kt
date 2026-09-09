package com.petcampus.knockdog.domain.memo.application.port.output

import com.petcampus.knockdog.domain.memo.domain.FreeMemo

interface SaveFreeMemoPort {
    fun save(memo: FreeMemo): FreeMemo
}

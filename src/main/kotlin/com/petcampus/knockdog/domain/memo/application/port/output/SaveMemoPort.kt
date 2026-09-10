package com.petcampus.knockdog.domain.memo.application.port.output

import com.petcampus.knockdog.domain.memo.domain.Memo

interface SaveMemoPort {
    fun save(memo: Memo): Memo
}

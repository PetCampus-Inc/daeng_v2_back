package com.petcampus.knockdog.domain.memo.application.port.output

import com.petcampus.knockdog.domain.memo.domain.ChecklistTemplate

interface LoadChecklistTemplatePort {
    fun load(): ChecklistTemplate
}

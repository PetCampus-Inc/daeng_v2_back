package com.petcampus.knockdog.domain.memo.adapter.outbound.persistence

import com.petcampus.knockdog.domain.memo.domain.Memo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

@SpringBootTest
class MemoUpsertConcurrencyTest {
    @Autowired
    private lateinit var adapter: MemoPersistenceAdapter

    @Autowired
    private lateinit var memoJpaRepository: MemoJpaRepository

    @AfterEach
    fun cleanUp() {
        memoJpaRepository.deleteAll()
    }

    @Test
    fun `같은 user_code target_id로 동시 최초 저장해도 하나의 row로 upsert된다`() {
        val executor = Executors.newFixedThreadPool(8)
        val threadCount = 8
        val errors = mutableListOf<Throwable>()

        repeat(threadCount) { index ->
            executor.submit {
                runCatching { adapter.save(Memo.create("CONC0001", "place-1", "v$index")) }
                    .onFailure { synchronized(errors) { errors.add(it) } }
            }
        }
        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)

        assertEquals(emptyList(), errors)
        assertEquals(1, memoJpaRepository.findAllByUserCodeOrderByUpdatedAtDescIdDesc("CONC0001").size)
    }
}
